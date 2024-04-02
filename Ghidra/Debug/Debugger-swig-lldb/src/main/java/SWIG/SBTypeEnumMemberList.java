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

public class SBTypeEnumMemberList {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected SBTypeEnumMemberList(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(SBTypeEnumMemberList obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected static long swigRelease(SBTypeEnumMemberList obj) {
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
        lldbJNI.delete_SBTypeEnumMemberList(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public SBTypeEnumMemberList() {
    this(lldbJNI.new_SBTypeEnumMemberList__SWIG_0(), true);
  }

  public SBTypeEnumMemberList(SBTypeEnumMemberList rhs) {
    this(lldbJNI.new_SBTypeEnumMemberList__SWIG_1(SBTypeEnumMemberList.getCPtr(rhs), rhs), true);
  }

  public boolean IsValid() {
    return lldbJNI.SBTypeEnumMemberList_IsValid(swigCPtr, this);
  }

  public void Append(SBTypeEnumMember entry) {
    lldbJNI.SBTypeEnumMemberList_Append(swigCPtr, this, SBTypeEnumMember.getCPtr(entry), entry);
  }

  public SBTypeEnumMember GetTypeEnumMemberAtIndex(long index) {
    return new SBTypeEnumMember(lldbJNI.SBTypeEnumMemberList_GetTypeEnumMemberAtIndex(swigCPtr, this, index), true);
  }

  public long GetSize() {
    return lldbJNI.SBTypeEnumMemberList_GetSize(swigCPtr, this);
  }

}
