package com.consuminurigni.stellarj.xdr;

import java.io.IOException;
import java.util.Arrays;

import com.consuminurigni.stellarj.common.Assert;
import com.consuminurigni.stellarj.common.Hex;

// === xdr source ============================================================

//  typedef opaque Hash[32];

//  ===========================================================================
public class Hash implements XdrValue {
	private static final int HASH_SIZE = 32;
	private final byte[] Hash;
	private static final byte[] ZEROES = new byte[] {
		0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,
		0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,
	};
	public static Hash createEmpty() {
		return new Hash(ZEROES);
	}
	public Hash(byte[] b) {
		Hash = b;
		Assert.assertTrue(b.length == HASH_SIZE);
	}

	public static void encode(XdrDataOutputStream stream, Hash encodedHash) throws IOException {
		stream.write(encodedHash.Hash);
	}

	public static Hash decode(XdrDataInputStream stream) throws IOException {
		byte[] _Hash = new byte[HASH_SIZE];
		stream.read(_Hash, 0, HASH_SIZE);
		return new Hash(_Hash);
	}

	public String hexAbbrev() {
		return Hex.hexAbbrev(Hash);
	}

	@Override
	public byte[] encode() {
		return Arrays.copyOf(Hash, Hash.length);
	}

	@Override
	public String toString() {
		return Hex.encode(Hash);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(Hash);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Hash && Arrays.equals(((Hash) obj).Hash, Hash);
	}

	public static Hash decode(byte[] apply) {
		// TODO Auto-generated method stub
		return null;
	}
	public String toHex() {
		// TODO Auto-generated method stub
		return null;
	}
}
