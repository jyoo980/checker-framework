// Test case for issue #871: https://github.com/typetools/checker-framework/issues/871

import java.nio.file.Files;
import java.nio.file.Path;

interface Issue871 {
  default Iterable<Path> a() {
    return f(Files::isRegularFile);
  }

  Iterable<Path> f(Predicate<Path> condition);

  interface Predicate<T> {
    boolean test(T t);
  }
}
