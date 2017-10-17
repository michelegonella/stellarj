package com.consuminurigni.stellarj.xdr;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import com.consuminurigni.stellarj.common.Assert;

// === xdr source ============================================================

//  typedef unsigned int uint32;

//  ===========================================================================
public class Uint32 extends XdrInteger {
	private static final long serialVersionUID = -1422108012957424233L;
	public static final Uint32 ZERO = new Uint32(0);
	public static final Uint32 ONE = new Uint32(1);
	public static final Uint32 UINT32_MAX = new Uint32(0b1111_1111__1111_1111__1111_1111__1111_1111);

	private int uint32;

	private Uint32(int i) {
		uint32 = i;
	}

	public static Uint32 ofPositiveInt(int int32) {
		Assert.assertTrue(int32 > -1);
		return new Uint32(int32);
	}

	public static Uint32 of2ComplRepr(int int32) {
		return new Uint32(int32);
	}

	public static void encode(XdrDataOutputStream stream, Uint32 encodedUint32) throws IOException {
		stream.writeInt(encodedUint32.uint32);
	}

	public static Uint32 decode(XdrDataInputStream stream) throws IOException {
		Uint32 decodedUint32 = new Uint32(stream.readInt());
		return decodedUint32;
	}

	public byte[] encode() {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(uint32);
		return bb.array();
	}

	public boolean eq(Uint32 uint) {
		return uint32 == uint.uint32;
	}

	public boolean ne(Uint32 uint) {
		return uint32 != uint.uint32;
	}

	@SuppressWarnings("unused")
	private boolean eq(int i) {
		return i >= 0 && uint32 == i;
	}

	public boolean eqZero() {
		return uint32 == 0;
	}

	public boolean eqOne() {
		return uint32 == 1;
	}

	public boolean ne(int i) {
		return i < 0 || uint32 != i;
	}

	public Uint32 plus(int i) throws IllegalArgumentException /*overflow*/ {
		Assert.assertTrue(i > -1);
		int res = i + uint32;
		if (Integer.compareUnsigned(res, i) < 0 || Integer.compareUnsigned(res, uint32) < 0) {
			throw new IllegalArgumentException(
					"unsigned sum of " + toString() + " and " + Integer.toUnsignedString(i) + " overflowed");
		}
		return new Uint32(res);
	}

	public Uint32 plus(Uint32 uint) throws IllegalArgumentException /*overflow*/ {
		int res = uint.uint32 + uint32;
		if (Integer.compareUnsigned(res, uint.uint32) < 0 || Integer.compareUnsigned(res, uint32) < 0) {
			throw new IllegalArgumentException(
					"unsigned sum of " + toString() + " and " + uint.toString() + " overflowed");
		}
		return new Uint32(res);
	}

	public boolean gte(Uint32 uint) {
		return Integer.compareUnsigned(uint32, uint.uint32) >= 0;
	}

	public boolean gt(Uint32 uint) {
		return Integer.compareUnsigned(uint32, uint.uint32) > 0;
	}

	public boolean lt(Uint32 uint) {
		return Integer.compareUnsigned(uint32, uint.uint32) < 0;
	}

	public boolean lte(Uint32 uint) {
		return Integer.compareUnsigned(uint32, uint.uint32) <= 0;
	}

	public boolean gt(int i) {
		return i < 0 || Integer.compareUnsigned(uint32, i) > 0;
	}

	@Override
	public String toString() {
		return Integer.toUnsignedString(uint32);
	}

	@Override
	public int hashCode() {
		return uint32;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Uint32 && ((Uint32) obj).uint32 == uint32;
	}

	@Override
	public int intValue() {
		Assert.assertTrue(uint32 > -1);
		return uint32;
	}

	@Override
	public long longValue() {
		return Integer.toUnsignedLong(uint32);
	}

	@Override
	public BigInteger asBigInteger() {
		return BigInteger.valueOf(longValue());
	}

	// >= 0
	public Uint32 mul(double d) {
		// TODO Auto-generated method stub
		return null;
	}
	// >= 0
	public Uint32 mul(int d) {
		// TODO Auto-generated method stub
		return null;
	}

	public Uint64 toUint64() {
		// TODO Auto-generated method stub
		return null;
	}

	public Uint32 minus(Uint32 maxSlotsToRemember) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean lt(Uint64 consensusIndex) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean lt(int size) {
		// TODO Auto-generated method stub
		return false;
	}

	public Uint32 minus(int i) {
		// TODO Auto-generated method stub
		return null;
	}

}
