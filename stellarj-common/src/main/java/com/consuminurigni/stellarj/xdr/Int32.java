package com.consuminurigni.stellarj.xdr;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import com.consuminurigni.stellarj.common.Assert;

// === xdr source ============================================================

//  typedef int int32;

//  ===========================================================================
public class Int32 extends XdrInteger {
	private static final long serialVersionUID = -9099107630096799360L;

	final int int32;

	private Int32(int i) {
		int32 = i;
	}

	public static Int32 of(int i) {
		return new Int32(i);
	}

	public static void encode(XdrDataOutputStream stream, Int32 encodedInt32) throws IOException {
		stream.writeInt(encodedInt32.int32);
	}

	public static Int32 decode(XdrDataInputStream stream) throws IOException {
		return new Int32(stream.readInt());
	}

	public byte[] encode() {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(int32);
		return bb.array();
	}

	public Int32 plus(int i) {
		return new Int32(int32 + 1);
	}

	@Override
	public String toString() {
		return Integer.toString(int32);
	}

	@Override
	public int hashCode() {
		return int32;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Int32 && ((Int32) obj).int32 == int32;
	}

	public Uint32 toUint() {
		Assert.assertTrue(int32 > -1);
		return Uint32.ofPositiveInt(int32);
	}

	@Override
	public int intValue() {
		return int32;
	}

	@Override
	public long longValue() {
		return (long) int32;
	}

	@Override
	public BigInteger asBigInteger() {
		return BigInteger.valueOf(longValue());
	}
}
