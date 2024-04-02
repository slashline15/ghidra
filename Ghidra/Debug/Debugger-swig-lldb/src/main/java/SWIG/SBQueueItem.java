/* ###
 * IP: Apache License 2.0 with LLVM Exceptions
 */
/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (https://www.swig.org).
 * Version 4.1.1
 *
 * Do not make changes to this file unless you know what you are doing - modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package SWIG;

public class SBQueueItem {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected SBQueueItem(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(SBQueueItem obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected static long swigRelease(SBQueueItem obj) {
    long ptr = 0;
    if (obj != null) {
      if (!obj.swigCMemOwn)
        throw new RuntimeException("Cannot release ownership as memory is not owned");
      ptr = obj.swigCPtr;
      obj.swigCMemOwn = false;
      obj.delete();
    }
    return ptr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        lldbJNI.delete_SBQueueItem(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public SBQueueItem() {
    this(lldbJNI.new_SBQueueItem(), true);
  }

  public boolean IsValid() {
    return lldbJNI.SBQueueItem_IsValid(swigCPtr, this);
  }

  public void Clear() {
    lldbJNI.SBQueueItem_Clear(swigCPtr, this);
  }

  public QueueItemKind GetKind() {
    return QueueItemKind.swigToEnum(lldbJNI.SBQueueItem_GetKind(swigCPtr, this));
  }

  public void SetKind(QueueItemKind kind) {
    lldbJNI.SBQueueItem_SetKind(swigCPtr, this, kind.swigValue());
  }

  public SBAddress GetAddress() {
    return new SBAddress(lldbJNI.SBQueueItem_GetAddress(swigCPtr, this), true);
  }

  public void SetAddress(SBAddress addr) {
    lldbJNI.SBQueueItem_SetAddress(swigCPtr, this, SBAddress.getCPtr(addr), addr);
  }

  public SBThread GetExtendedBacktraceThread(String type) {
    return new SBThread(lldbJNI.SBQueueItem_GetExtendedBacktraceThread(swigCPtr, this, type), true);
  }

}
