package org.checkerframework.framework.flow;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.analysis.TransferResult;

/** The default transfer function used in the Checker Framework. */
public class CFTransfer extends CFAbstractTransfer<CFValue, CFStore, CFTransfer> {

  public CFTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    super(analysis);
  }

  /**
   * Returns a copy of the given TransferResult, but with a result whose annotation is the given
   * annotation.
   *
   * @param tr a TransferResult
   * @param resultAnno the annotation for the new result value
   * @return a copy of the given TransferResult, but with a result whose annotation is the given
   *     annotation
   */
  public TransferResult<CFValue, CFStore> withResultAnnotation(
      TransferResult<CFValue, CFStore> tr, AnnotationMirror resultAnno) {
    CFValue newResultValue =
        analysis.createSingleAnnotationValue(resultAnno, tr.getResultValue().getUnderlyingType());
    return tr.withResultValue(newResultValue);
  }
}
