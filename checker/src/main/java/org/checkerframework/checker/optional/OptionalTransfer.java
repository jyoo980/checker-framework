package org.checkerframework.checker.optional;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BinaryOperator;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.optional.qual.Present;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGLambda;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

/** The transfer function for the Optional Checker. */
public class OptionalTransfer extends CFTransfer {

  /** The {@link OptionalAnnotatedTypeFactory} instance for this transfer class. */
  private final OptionalAnnotatedTypeFactory optionalTypeFactory;

  /** The @{@link Present} annotation. */
  private final AnnotationMirror PRESENT;

  /** The @{@link NonEmpty} annotation. */
  private final AnnotationMirror NON_EMPTY;

  /** The element for java.util.Optional.ifPresent(). */
  private final ExecutableElement optionalIfPresent;

  /** The element for java.util.Optional.ifPresentOrElse(), or null. */
  private final @Nullable ExecutableElement optionalIfPresentOrElse;

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
  private final List<ExecutableElement> nonEmptyToPresentStreamMethods;

  /**
   * Create an OptionalTransfer.
   *
   * @param analysis the Optional Checker instance
   */
  public OptionalTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    super(analysis);
    optionalTypeFactory = (OptionalAnnotatedTypeFactory) analysis.getTypeFactory();
    Elements elements = optionalTypeFactory.getElementUtils();
    PRESENT = AnnotationBuilder.fromClass(elements, Present.class);
    NON_EMPTY = AnnotationBuilder.fromClass(elements, NonEmpty.class);
    ProcessingEnvironment env = optionalTypeFactory.getProcessingEnv();
    optionalIfPresent = TreeUtils.getMethod("java.util.Optional", "ifPresent", 1, env);
    optionalIfPresentOrElse =
        TreeUtils.getMethodOrNull("java.util.Optional", "ifPresentOrElse", 2, env);
    streamMax = TreeUtils.getMethodOrNull("java.util.stream.Stream", "max", 1, env);
    streamMin = TreeUtils.getMethodOrNull("java.util.stream.Stream", "min", 1, env);
    streamReduceNoIdentity = TreeUtils.getMethodOrNull("java.util.stream.Stream", "reduce", 1, env);
    streamFindFirst = TreeUtils.getMethodOrNull("java.util.stream.Stream", "findFirst", 0, env);
    streamFindAny = TreeUtils.getMethodOrNull("java.util.stream.Stream", "findAny", 0, env);
    nonEmptyToPresentStreamMethods =
        Arrays.asList(streamMax, streamMin, streamReduceNoIdentity, streamFindFirst, streamFindAny);
  }

  @Override
  public CFStore initialStore(UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {

    CFStore result = super.initialStore(underlyingAST, parameters);

    if (underlyingAST.getKind() == UnderlyingAST.Kind.LAMBDA) {
      // Check whether this lambda is an argument to `Optional.ifPresent()` or
      // `Optional.ifPresentOrElse()`.  If so, then within the lambda, the receiver of the
      // `ifPresent*` method is @Present.
      CFGLambda cfgLambda = (CFGLambda) underlyingAST;
      LambdaExpressionTree lambdaTree = cfgLambda.getLambdaTree();
      List<? extends VariableTree> lambdaParams = lambdaTree.getParameters();
      if (lambdaParams.size() == 1) {
        TreePath lambdaPath = optionalTypeFactory.getPath(lambdaTree);
        Tree lambdaParent = lambdaPath.getParentPath().getLeaf();
        if (lambdaParent.getKind() == Tree.Kind.METHOD_INVOCATION) {
          MethodInvocationTree invok = (MethodInvocationTree) lambdaParent;
          ExecutableElement methodElt = TreeUtils.elementFromUse(invok);
          if (methodElt.equals(optionalIfPresent) || methodElt.equals(optionalIfPresentOrElse)) {
            // `underlyingAST` is an invocation of `Optional.ifPresent()` or
            // `Optional.ifPresentOrElse()`.  In the lambda, the receiver is @Present.
            JavaExpression receiverJe = JavaExpression.fromTree(TreeUtils.getReceiverTree(invok));
            result.insertValue(receiverJe, PRESENT);
          }
        }
      }
    }

    // TODO: Similar logic to the above can be applied in the Nullness Checker.
    // Some methods take a function as an argument, guaranteeing that, if the function is
    // called:
    //  * the value passed to the function is non-null
    //  * some other argument to the method is non-null
    // Examples:
    //  * Jodd's `StringUtil.ifNotNull()`
    //  * `Opt.ifPresent()`
    //  * `Opt.map()`

    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);
    if (n.getTree() == null) {
      return result;
    }
    result = refineStreamOperations(n, result);
    System.out.printf(
        "OptionalTransfer.visitMethodInvocation(%s):%n"
            + "  TransferResult after Stream operation refinement = %s%n",
        n, result);
    return result;
  }

  /**
   * Refines the result of a call to {@link java.util.stream.Stream#max(Comparator)}, {@link
   * java.util.stream.Stream#min(Comparator)}, or {@link
   * java.util.stream.Stream#reduce(BinaryOperator)}.
   *
   * <p>The presence/emptiness of the Optional value returned in the method invocations above are
   * dependent on whether the initial stream (i.e., the receiver) is empty (or not). That is,
   * invoking the methods above on a {@code @NonEmpty} stream will return a {@code @Present}
   * Optional, while invoking them on an empty stream will return an empty Optional.
   *
   * @param n the method invocation node
   * @param result the transfer result to side effect
   */
  private TransferResult<CFValue, CFStore> refineStreamOperations(
      MethodInvocationNode n, TransferResult<CFValue, CFStore> result) {
    if (NodeUtils.isMethodInvocation(
        n, nonEmptyToPresentStreamMethods, optionalTypeFactory.getProcessingEnv())) {
      if (isReceiverNonEmpty(n)) {
        // The receiver of the stream operation is @Non-Empty, therefore the result is @Present.
        JavaExpression internalRepr = JavaExpression.fromNode(n);
        System.out.printf(
            "OptionalTransfer.refineStreamOperations: Has non-empty receiver: %s%n", internalRepr);
        System.out.printf(
            "isAssumePureOrAssumeDeterministicEnabled() = %s%n",
            isAssumePureOrAssumeDeterministicEnabled());
        if (isAssumePureOrAssumeDeterministicEnabled()) {
          insertIntoStoresPermitNonDeterministic(result, internalRepr, PRESENT);
        } else {
          insertIntoStores(result, internalRepr, PRESENT);
        }
        return withResultAnnotation(result, PRESENT);
      }
    }
    return result;
  }

  /**
   * Determine whether this analysis is being executed with the {@literal -AassumePure  or {@literal -AassumeDeterministic} flags.
   * @return true if the {@literal -AassumePure} or {@literal -AassumeDeterministic} flags are passed to this analysis
   */
  private boolean isAssumePureOrAssumeDeterministicEnabled() {
    BaseTypeChecker checker = analysis.getTypeFactory().getChecker();
    return checker.hasOption("assumePure") || checker.hasOption("assumeDeterministic");
  }

  /**
   * Returns true if the receiver of the given method invocation is annotated with @{@link
   * NonEmpty}.
   *
   * @param methodInvok a method invocation node
   * @return true if the receiver of the given method invocation is annotated with @{@link NonEmpty}
   */
  /*package-private*/ boolean isReceiverNonEmpty(MethodInvocationNode methodInvok) {
    ExpressionTree receiverTree = TreeUtils.getReceiverTree(methodInvok.getTree());
    JavaExpression receiver;
    if (receiverTree instanceof MethodInvocationTree) {
      receiver = JavaExpression.getInitialReceiverOfMethodInvocation(receiverTree);
    } else {
      receiver = JavaExpression.fromTree(receiverTree);
    }
    VariableTree receiverDeclaration = getReceiverDeclaration(methodInvok, receiver);
    if (receiverDeclaration == null) {
      return false;
    }
    List<? extends AnnotationTree> receiverAnnotationTrees =
        receiverDeclaration.getModifiers().getAnnotations();
    List<AnnotationMirror> annotationMirrors =
        TreeUtils.annotationsFromTypeAnnotationTrees(receiverAnnotationTrees);
    return AnnotationUtils.containsSame(annotationMirrors, NON_EMPTY);
  }

  /**
   * Returns the declaration of the initial receiver of the given method invocation node.
   *
   * <p>An attempt is first made to find the declaration of the receiver in the method that
   * immediately encloses the given method invocation node. If this is unsuccessful, an attempt is
   * made to look for the receiver in the fields of the class that immediately encloses the given
   * method invocation node.
   *
   * @param methodInvok a method invocation node
   * @param initialReceiver the initial receiver in the method invocation node
   * @return the declaration of the receiver if found, else null
   */
  private @Nullable VariableTree getReceiverDeclaration(
      MethodInvocationNode methodInvok, JavaExpression initialReceiver) {
    // Look in the method, first
    MethodTree methodTree = TreePathUtil.enclosingMethod(methodInvok.getTreePath());
    VariableTree declarationInMethod =
        JavaExpression.getReceiverDeclarationInMethod(methodTree, initialReceiver);
    if (declarationInMethod != null) {
      return declarationInMethod;
    }
    // If the declaration can't be found in the method, look in the class
    ClassTree classTree = TreePathUtil.enclosingClass(methodInvok.getTreePath());
    return JavaExpression.getReceiverDeclarationInClass(classTree, initialReceiver);
  }
}
