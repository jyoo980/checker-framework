package javaexpression;

import org.checkerframework.framework.testchecker.javaexpression.qual.FlowExp;

public class SimpleVPA {

  class MyClass {
    // :: error: (expression.unparsable)
    @FlowExp("this.bad") Object field;
  }

  class Use {
    Object bad = new Object();
    MyClass myClass = new MyClass();

    @FlowExp("bad")
    // :: error: (assignment)
    Object o = myClass.field;
  }
}
