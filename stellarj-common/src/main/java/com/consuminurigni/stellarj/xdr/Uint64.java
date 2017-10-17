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
public /*TODO final*/ long uint64;
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
		return Long.toUnsignedString(uint64);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(uint64);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Uint64 && ((Uint64) obj).uint64 == uint64;
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
	public boolean eq(Uint32 plus) {
		// TODO Auto-generated method stub
		return false;
	}
	public boolean lte(Uint64 lastCloseTime) {
		// TODO Auto-generated method stub
		return false;
	}
	public static Uint64 of(long sz) {
		// TODO Auto-generated method stub
		return null;
	}
	public Uint64 minus(long l) {//l positive
		// TODO Auto-generated method stub
		return null;
	}
	public Int64 toInt64() {
		// TODO Auto-generated method stub
		return null;
	}
	public boolean gt(Uint32 maxLedgerSeq) {
		// TODO Auto-generated method stub
		return false;
	}
	public boolean lt(Uint32 minLedgerSeq) {
		// TODO Auto-generated method stub
		return false;
	}
	public Uint64 plus(long i) {
		// TODO Auto-generated method stub
		return null;
	}
	public Uint32 toUint32() {
		// TODO Auto-generated method stub
		return null;
	}
	public boolean ne(Uint32 nextConsensusLedgerIndex) {
		// TODO Auto-generated method stub
		return false;
	}

}
