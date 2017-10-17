package com.consuminurigni.stellarj.xdr;


import java.io.IOException;
import java.math.BigInteger;

import com.consuminurigni.stellarj.common.Assert;

// === xdr source ============================================================

//  typedef opaque uint256[32];

//  ===========================================================================
public class Uint256 extends XdrInteger {
  /**
	 * 
	 */
	private static final long serialVersionUID = -947089438259001789L;
private byte[] uint256;
  public Uint256(byte[] bs) {
	  Assert.assertTrue(bs.length == 32);
	uint256 = bs;
}
public Uint256() {
	// TODO Auto-generated constructor stub
}
public byte[] getUint256() {
    return this.uint256;
  }
  public void setUint256(byte[] value) {
    this.uint256 = value;
  }
  public static void encode(XdrDataOutputStream stream, Uint256  encodedUint256) throws IOException {
  int uint256size = encodedUint256.uint256.length;
  stream.write(encodedUint256.getUint256(), 0, uint256size);
  }
  public static Uint256 decode(XdrDataInputStream stream) throws IOException {
    Uint256 decodedUint256 = new Uint256();
  int uint256size = 32;
  decodedUint256.uint256 = new byte[uint256size];
  stream.read(decodedUint256.uint256, 0, uint256size);
    return decodedUint256;
  }
  @Override
  public String toString() {
  	// TODO Auto-generated method stub
  	return super.toString();
  }
  @Override
  public int hashCode() {
  	// TODO Auto-generated method stub
  	return super.hashCode();
  }
  @Override
  public boolean equals(Object obj) {
  	// TODO Auto-generated method stub
  	return super.equals(obj);
  }
@Override
public int intValue() {
	// TODO Auto-generated method stub
	return 0;
}
@Override
public long longValue() {
	// TODO Auto-generated method stub
	return 0;
}
@Override
public BigInteger asBigInteger() {
	// TODO Auto-generated method stub
	return null;
}
public static Uint256 of(byte[] apply) {
	// TODO Auto-generated method stub
	return null;
}
public Object hexAbbrev() {
	// TODO Auto-generated method stub
	return null;
}

boolean isZero()
{
    for (byte i : uint256)
        if (i != 0) {
            return false;
        }

    return true;
}

}
