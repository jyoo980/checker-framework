package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Non-Empty Checker */
public class NonEmptyTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a NonEmptyTest.
   *
   * @param testFiles the files containing test code to be type-checked
   */
  public NonEmptyTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nonempty.NonEmptyChecker.class,
        "nonempty",
        // The Non-Empty test suite checks the standalone behavior of the Non-Empty Checker,
        // As a result, all method definitions should be checked, not just those collected by its
        // subchecker (the OptionalImplChecker).
        "-AcheckAllDefs");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nonempty", "all-systems"};
  }
}
