package com.consuminurigni.stellarj.xdr;


import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

// === xdr source ============================================================

//  typedef unsigned hyper uint64;

//  ===========================================================================
public class Uint64 extends XdrInteger {
	private static final long serialVersionUID = 1790875541700710224L;
public static final Uint64 ZERO = null;
private Long uint64;
  public Long getUint64() {
    return this.uint64;
  }
  public void setUint64(Long value) {
    this.uint64 = value;
  }
  public static void encode(XdrDataOutputStream stream, Uint64  encodedUint64) throws IOException {
  stream.writeLong(encodedUint64.uint64);
  }
  public static Uint64 decode(XdrDataInputStream stream) throws IOException {
    Uint64 decodedUint64 = new Uint64();
  decodedUint64.uint64 = stream.readLong();
    return decodedUint64;
  }
	public byte[] toOpaque() {
		ByteBuffer bb = ByteBuffer.allocate(8);
		bb.putLong(uint64);
		return bb.array();
	}
	public boolean lt(Uint64 maxSlotIndex) {
		// TODO Auto-generated method stub
		return false;
	}
	public boolean eq(Uint64 mSlotIndex) {
		// TODO Auto-generated method stub
		return false;
	}
	public boolean eq(long l) {
		// TODO Auto-generated method stub
		return false;
	}
	public BigInteger toBigInteger() {
		// TODO Auto-generated method stub
		return null;
	}
	public boolean neq(long i) {
		// TODO Auto-generated method stub
		return false;
	}
	public boolean gt(Uint64 topPriority) {
		// TODO Auto-generated method stub
		return false;
	}
	public boolean gt(long i) {
		// TODO Auto-generated method stub
		return false;
	}
	public boolean gteq(Uint64 newHash) {
		// TODO Auto-generated method stub
		return false;
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

}
