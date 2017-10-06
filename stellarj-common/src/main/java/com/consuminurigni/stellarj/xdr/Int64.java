package com.consuminurigni.stellarj.xdr;


import java.io.IOException;
import java.math.BigInteger;

// === xdr source ============================================================

//  typedef hyper int64;

//  ===========================================================================
public class Int64 extends XdrInteger {
	private static final long serialVersionUID = 286936415117986394L;
private long int64;
	private Int64(long l) {
		int64 = l;
	}
  public static void encode(XdrDataOutputStream stream, Int64  encodedInt64) throws IOException {
  stream.writeLong(encodedInt64.int64);
  }
  public static Int64 decode(XdrDataInputStream stream) throws IOException {
    Int64 decodedInt64 = new Int64(stream.readLong());
    return decodedInt64;
  }
  @Override
  public String toString() {
  	return Long.toString(int64);
  }
  @Override
  public int hashCode() {
  	return Long.hashCode(int64);
  }
  @Override
  public boolean equals(Object obj) {
  	return obj instanceof Int64 && ((Int64)obj).int64 == int64;
  }
@Override
public int intValue() {
	return (int)int64;
}
@Override
public long longValue() {
	return int64;
}
@Override
public BigInteger asBigInteger() {
	// TODO Auto-generated method stub
	return null;
}

}
