// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.consuminurigni.stellarj.xdr;


import java.io.IOException;

// === xdr source ============================================================

//  typedef hyper int64;

//  ===========================================================================
public class Int64  {
  private Long int64;
  public Long getInt64() {
    return this.int64;
  }
  public void setInt64(Long value) {
    this.int64 = value;
  }
  public static void encode(XdrDataOutputStream stream, Int64  encodedInt64) throws IOException {
  stream.writeLong(encodedInt64.int64);
  }
  public static Int64 decode(XdrDataInputStream stream) throws IOException {
    Int64 decodedInt64 = new Int64();
  decodedInt64.int64 = stream.readLong();
    return decodedInt64;
  }
}
