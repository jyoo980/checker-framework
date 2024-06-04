package org.checkerframework.dataflow.expression;

import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.plumelib.util.CollectionsPlume;

/**
 * This class calls {@link #convert(JavaExpression)} on each subexpression of the {@link
 * JavaExpression} and returns a new {@code JavaExpression} built from the result of calling {@code
 * convert} on each subexpression. (If an expression has no subexpression, then the expression
 * itself is returned.)
 *
 * <p>This class makes it easy to implement a subclass that converts subexpressions of a {@link
 * JavaExpression} based on which kind of {@code JavaExpression} the subexpression is. Subclasses
 * should override the visit method of kinds of JavaExpressions to convert.
 */
public abstract class JavaExpressionConverter extends JavaExpressionVisitor<JavaExpression, Void> {

  /**
   * Converts {@code javaExpr} and returns the resulting {@code JavaExpression}.
   *
   * @param javaExpr the expression to convert
   * @return the converted expression
   */
  public JavaExpression convert(JavaExpression javaExpr) {
    return super.visit(javaExpr, null);
  }

  /**
   * Converts all the expressions in {@code list} and returns the resulting list.
   *
   * @param list the list of expressions to convert
   * @return the list of converted expressions
   */
  public List<@PolyNull JavaExpression> convert(List<@PolyNull JavaExpression> list) {
    return CollectionsPlume.mapList(
        expression -> {
          // Can't use a ternary operator because of:
          // https://github.com/typetools/checker-framework/issues/1170
          if (expression == null) {
            return null;
          }
          return convert(expression);
        },
        list);
  }

  @Override
  protected JavaExpression visitArrayAccess(ArrayAccess arrayAccessExpr, Void unused) {
    JavaExpression array = convert(arrayAccessExpr.getArray());
    JavaExpression index = convert(arrayAccessExpr.getIndex());
    return new ArrayAccess(arrayAccessExpr.type, array, index);
  }

  @Override
  protected JavaExpression visitArrayCreation(ArrayCreation arrayCreationExpr, Void unused) {
    List<@Nullable JavaExpression> dims = convert(arrayCreationExpr.getDimensions());
    List<JavaExpression> inits = convert(arrayCreationExpr.getInitializers());
    return new ArrayCreation(arrayCreationExpr.getType(), dims, inits);
  }

  @Override
  protected JavaExpression visitBinaryOperation(BinaryOperation binaryOpExpr, Void unused) {
    JavaExpression left = convert(binaryOpExpr.getLeft());
    JavaExpression right = convert(binaryOpExpr.getRight());
    return new BinaryOperation(
        binaryOpExpr.getType(), binaryOpExpr.getOperationKind(), left, right);
  }

  @Override
  protected JavaExpression visitClassName(ClassName classNameExpr, Void unused) {
    return classNameExpr;
  }

  @Override
  protected JavaExpression visitFieldAccess(FieldAccess fieldAccessExpr, Void unused) {
    JavaExpression receiver = convert(fieldAccessExpr.getReceiver());
    return new FieldAccess(receiver, fieldAccessExpr.getType(), fieldAccessExpr.getField());
  }

  @Override
  protected JavaExpression visitFormalParameter(FormalParameter parameterExpr, Void unused) {
    return parameterExpr;
  }

  @Override
  protected JavaExpression visitLocalVariable(LocalVariable localVarExpr, Void unused) {
    return localVarExpr;
  }

  @Override
  protected JavaExpression visitMethodCall(MethodCall methodCallExpr, Void unused) {
    JavaExpression receiver = convert(methodCallExpr.getReceiver());
    List<JavaExpression> args = convert(methodCallExpr.getArguments());
    return new MethodCall(methodCallExpr.getType(), methodCallExpr.getElement(), receiver, args);
  }

  @Override
  protected JavaExpression visitThisReference(ThisReference thisExpr, Void unused) {
    return thisExpr;
  }

  @Override
  protected JavaExpression visitLambda(Lambda lambdaExpr, Void unused) {
    return lambdaExpr;
  }

  @Override
  protected JavaExpression visitMethodReference(MethodReference methodReferenceExpr, Void unused) {
    MethodReferenceScope scope = convert(methodReferenceExpr.scope);
    MethodReferenceTarget target = convert(methodReferenceExpr.target);
    return new MethodReference(
        methodReferenceExpr.type,
        scope,
        target); // TODO: where is the type of a method reference even set?
  }

  /**
   * Converts a method reference scope for use in the creation of a MethodReference JavaExpression.
   *
   * @param scope the method reference scope
   * @return a method reference scope to be used in the creation of a MethodReference JavaExpression
   */
  private MethodReferenceScope convert(MethodReferenceScope scope) {
    JavaExpression expression = null;
    if (scope.getExpression() != null) {
      expression = convert(scope.getExpression());
    }
    TypeMirror type = null;
    if (scope.getType() != null) {
      type = scope.getType();
    }
    return new MethodReferenceScope(expression, type, scope.isSuper());
  }

  /**
   * Converts a method reference target for use in the creation of a method reference
   * JavaExpression.
   *
   * @param target the method reference target
   * @return a method reference target to be used in the creation of a method reference
   *     JavaExpression
   */
  private MethodReferenceTarget convert(MethodReferenceTarget target) {
    return new MethodReferenceTarget(
        target.getTypeArguments(), target.getIdentifier(), target.isConstructor());
  }

  @Override
  protected JavaExpression visitSuperReference(SuperReference superExpr, Void unused) {
    return superExpr;
  }

  @Override
  protected JavaExpression visitUnaryOperation(UnaryOperation unaryOpExpr, Void unused) {
    JavaExpression operand = convert(unaryOpExpr.getOperand());
    return new UnaryOperation(unaryOpExpr.getType(), unaryOpExpr.getOperationKind(), operand);
  }

  @Override
  protected JavaExpression visitUnknown(Unknown unknownExpr, Void unused) {
    return unknownExpr;
  }

  @Override
  protected JavaExpression visitValueLiteral(ValueLiteral literalExpr, Void unused) {
    return literalExpr;
  }
}
