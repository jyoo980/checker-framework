import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nonempty.qual.*;
import org.checkerframework.checker.optional.qual.*;
import org.checkerframework.dataflow.qual.Pure;

// @skip-test: ignore until we have the correct checkers in-place, that is, until we run the
// RevisedOptionalChecker by default as the Optional Checker.
// This currently verifies via javacheck -processor revisedoptional OptionalRelyGuaranteeTest.java
// at e97204746a33f0fc374e6e74e0bd028f49f4de53

class OptionalRelyGuaranteeTest {

  void a(ContainerWrapper wrapper) {
    if (!wrapper.getStrs().isEmpty()) {
      b("test", wrapper);
    }
    // :: error: (assignment)
    @NonEmpty List<String> ne = wrapper.getStrs();
  }

  @RequiresNonEmpty("#2.getStrs()")
  void b(String ignored, ContainerWrapper nonEmptyWrapper) {
    @Present Optional<Integer> maxStrLen = c(nonEmptyWrapper.getStrs());
    maxStrLen.get();
  }

  @Present Optional<Integer> c(@NonEmpty List<String> strs) {
    return strs.stream().map(String::length).max(Integer::compareTo);
  }

  class ContainerWrapper {

    List<String> strs;

    public ContainerWrapper(List<String> strs) {
      this.strs = strs;
    }

    @Pure
    public List<String> getStrs() {
      return strs;
    }
  }
}
