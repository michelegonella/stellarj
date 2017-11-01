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
	public static Hash of(byte[] b) {
		return new Hash(b);
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


	public Hash xor(Hash r)
	{
	    Hash res = createEmpty();
	    for (int i = 0; i < HASH_SIZE; i++)
	    {
	        res.Hash[i] = (byte)(this.Hash[i] ^ r.Hash[i]);
	    }

	    return res;
	}

	public static boolean lessThanXored(Hash l, Hash r, Hash x)
	{
	    Hash v1 = createEmpty();
	    Hash v2 = createEmpty();
	    for (int i = 0; i < HASH_SIZE; i++)
	    {
	        v1.Hash[i] = (byte)(x.Hash[i] ^ l.Hash[i]);
	        v2.Hash[i] = (byte)(x.Hash[i] ^ r.Hash[i]);
	    }

	    return v1.lt(v2);
	}

	public boolean lt(Hash other) {
		boolean differ = false;
		for(int j = 0; j < HASH_SIZE; j++) {
			if(Byte.toUnsignedInt(this.Hash[j]) > Byte.toUnsignedInt(other.Hash[j])) {
				return false;
			} else if(this.Hash[j] != other.Hash[j]) {
				differ = true;
			}
		}
		return differ;
	}
}
