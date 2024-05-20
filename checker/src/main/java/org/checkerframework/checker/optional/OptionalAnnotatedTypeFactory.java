package org.checkerframework.checker.optional;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.nonempty.NonEmptyAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.optional.qual.Present;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

/** OptionalAnnotatedTypeFactory for the Optional Checker. */
public class OptionalAnnotatedTypeFactory extends NonEmptyAnnotatedTypeFactory {

  /** The @{@link Present} annotation. */
  protected final AnnotationMirror PRESENT = AnnotationBuilder.fromClass(elements, Present.class);

  /** The element for java.util.Optional.map(). */
  private final ExecutableElement optionalMap;

  /** The element for java.util.stream.Stream.max(), or null. */
  private final @Nullable ExecutableElement streamMax;

  /** The element for java.util.stream.Stream.min(), or null. */
  private final @Nullable ExecutableElement streamMin;

  /** The element for java.util.stream.Stream.reduce(BinaryOperator&lt;T&gt;), or null. */
  private final @Nullable ExecutableElement streamReduceNoIdentity;

  /** The element for java.util.stream.Stream.findFirst(), or null. */
  private final @Nullable ExecutableElement streamFindFirst;

  /** The element for java.util.stream.Stream.findAny(), or null. */
  private final @Nullable ExecutableElement streamFindAny;

  /** Stream methods such that if the input is @NonEmpty, the result is @Present. */
  @SuppressWarnings("UnusedVariable")
  private final List<ExecutableElement> nonEmptyToPresentStreamMethods;

  /**
   * Creates an OptionalAnnotatedTypeFactory.
   *
   * @param checker the Optional Checker associated with this type factory
   */
  public OptionalAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    postInit();

    ProcessingEnvironment env = getProcessingEnv();

    optionalMap = TreeUtils.getMethod("java.util.Optional", "map", 1, env);

    streamMax = TreeUtils.getMethodOrNull("java.util.stream.Stream", "max", 1, env);
    streamMin = TreeUtils.getMethodOrNull("java.util.stream.Stream", "min", 1, env);
    streamReduceNoIdentity = TreeUtils.getMethodOrNull("java.util.stream.Stream", "reduce", 1, env);
    streamFindFirst = TreeUtils.getMethodOrNull("java.util.stream.Stream", "findFirst", 0, env);
    streamFindAny = TreeUtils.getMethodOrNull("java.util.stream.Stream", "findAny", 0, env);
    nonEmptyToPresentStreamMethods =
        Arrays.asList(streamMax, streamMin, streamReduceNoIdentity, streamFindFirst, streamFindAny);
  }

  @Override
  public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
    AnnotatedTypeMirror result = super.getAnnotatedType(tree);
    optionalMapNonNull(tree, result);
    return result;
  }

  /**
   * If {@code tree} is {@code someOptional.map(methodReference)}, then this method adds
   * {@code @Present} to {@code type} if:
   *
   * <ul>
   *   <li>{@code someOptional} is {@code @Present} and
   *   <li>{@code methodReference}'s return type is non-null.
   * </ul>
   *
   * @param tree a tree
   * @param type the type of the tree, which may be side-effected by this method
   */
  private void optionalMapNonNull(Tree tree, AnnotatedTypeMirror type) {
    System.out.printf("optionalMapNonNull#1(%s, %s)%n", tree, type);
    if (!TreeUtils.isMethodInvocation(tree, optionalMap, processingEnv)) {
      return;
    }
    System.out.printf("optionalMapNonNull#2(%s, %s)%n", tree, type);
    MethodInvocationTree mapTree = (MethodInvocationTree) tree;
    ExpressionTree argTree = mapTree.getArguments().get(0);
    if (argTree.getKind() == Kind.MEMBER_REFERENCE) {
      System.out.printf("optionalMapNonNull#3(%s, %s)%n", tree, type);
      MemberReferenceTree memberReferenceTree = (MemberReferenceTree) argTree;
      AnnotatedTypeMirror optType = getReceiverType(mapTree);
      if (optType == null || !optType.hasEffectiveAnnotation(Present.class)) {
        return;
      }
      System.out.printf("optionalMapNonNull#4(%s, %s)%n", tree, type);
      if (!returnHasNullable(memberReferenceTree)) {
        System.out.printf("optionalMapNonNull#5(%s, %s)%n", tree, type);
        // The method still could have a @PolyNull on the return and might return null.
        // If @PolyNull is the primary annotation on the parameter and not on any type
        // arguments or array elements, then it is still safe to mark the optional type as
        // present.
        // TODO: Add the check for poly null on arguments.
        type.replaceAnnotation(PRESENT);
        System.out.printf("optionalMapNonNull#6(%s, %s)%n", tree, type);
      }
    }
  }

  /**
   * Returns true if the return type of the function type of {@code memberReferenceTree} is
   * annotated with {@code @Nullable}.
   *
   * @param memberReferenceTree a member reference
   * @return true if the return type of the function type of {@code memberReferenceTree} is
   *     annotated with {@code @Nullable}
   */
  private boolean returnHasNullable(MemberReferenceTree memberReferenceTree) {
    if (TreeUtils.MemberReferenceKind.getMemberReferenceKind(memberReferenceTree)
        .isConstructorReference()) {
      return false;
    }
    ExecutableElement memberReferenceFuncType = TreeUtils.elementFromUse(memberReferenceTree);
    if (memberReferenceFuncType.getEnclosingElement().getKind() == ElementKind.ANNOTATION_TYPE) {
      // Annotation element accessor are always non-null;
      return false;
    }

    if (!checker.hasOption("optionalMapAssumeNonNull")) {
      return true;
    }
    return containsNullable(memberReferenceFuncType.getAnnotationMirrors())
        || containsNullable(memberReferenceFuncType.getReturnType().getAnnotationMirrors());
  }

  /**
   * Returns true if {@code annos} contains a nullable annotation.
   *
   * @param annos a collection of annotations
   * @return true if {@code annos} contains a nullable annotation
   */
  private boolean containsNullable(Collection<? extends AnnotationMirror> annos) {
    for (AnnotationMirror anno : annos) {
      if (anno.getAnnotationType().asElement().getSimpleName().contentEquals("Nullable")) {
        return true;
      }
    }
    return false;
  }

  @Override
  public CFTransfer createFlowTransferFunction(
      CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    return new OptionalTransfer(analysis);
  }

  public boolean isTreeAnnotatedWithNonEmpty(ExpressionTree tree) {
    return super.isAnnotatedWithNonEmpty(tree);
  }
}
