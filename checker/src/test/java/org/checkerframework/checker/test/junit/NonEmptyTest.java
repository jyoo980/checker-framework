package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Non-Empty Checker */
public class NonEmptyTest extends CheckerFrameworkPerDirectoryTest {

  public NonEmptyTest(List<File> testFiles) {
    super(testFiles, org.checkerframework.checker.nonempty.NonEmptyChecker.class, "nonempty");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nonempty"};
  }
}
