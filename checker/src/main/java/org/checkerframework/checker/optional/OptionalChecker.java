package org.checkerframework.checker.optional;

import java.util.Optional;
import java.util.Set;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * A type-checker that prevents misuse of the {@link java.util.Optional} class.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
// TODO: For a call to `@Optional#ofNullable`, if the argument has type
// @NonNull, make the return type have type @Present.
@RelevantJavaTypes(Optional.class)
@StubFiles({"javaparser.astub"})
@SupportedOptions("optionalMapAssumeNonNull")
public class OptionalChecker extends BaseTypeChecker {
  /** Create an OptionalChecker. */
  public OptionalChecker() {}

  @Override
  protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();
    checkers.add(AliasingChecker.class);
    return checkers;
  }
}
