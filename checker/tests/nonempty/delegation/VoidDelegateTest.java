import java.util.ArrayList;
import org.checkerframework.checker.nonempty.qual.Delegate;

public class VoidDelegateTest<E> extends ArrayList<E> {

  @Delegate private ArrayList<E> array;

  public VoidDelegateTest(ArrayList<E> array) {
    this.array = array;
  }

  @Override
  public void clear() {
    this.array.clear(); // This should be OK
  }
}
