package org.checkerframework.checker.nonempty;

import java.util.Set;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A type-checker that prevents {@link java.util.NoSuchElementException} in the use of container
 * classes.
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
public class NonEmptyChecker extends BaseTypeChecker {
  @Override
  protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends BaseTypeChecker>> subcheckers = super.getImmediateSubcheckerClasses();
    subcheckers.add(DelegationChecker.class);
    return subcheckers;
  }
}
