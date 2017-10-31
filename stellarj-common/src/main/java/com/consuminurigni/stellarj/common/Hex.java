package com.consuminurigni.stellarj.common;

import static com.consuminurigni.stellarj.common.Assert.assertTrue;
import static java.lang.Math.min;

import com.consuminurigni.stellarj.xdr.Uint256;

public class Hex {


	private static final char[] ALPHABET = new char[]
		{'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	/**
	 * 
	 * @param barr
	 * @return the hex string lowercase
	 */
	public static String encode(byte[] barr) {
		char[] result = new char[barr.length * 2];
		for (int i = 0; i < barr.length; i++) {
			int v = barr[i] & 0xFF;
			result[i * 2] = ALPHABET[v >>> 4];
			result[i * 2 + 1] = ALPHABET[v & 0x0F];
		}
		return new String(result);
	}

	public static byte[] decode(String hex) {
		int len = hex.length();
		byte[] bytes = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i+1), 16));
		}
		return bytes;
	}

	public static String hexAbbrev(byte[] barr) {
		String enc = encode(barr);
		return enc.substring(0, min(4, enc.length()));
	}


public static Uint256 hexToBin256(String hex)
{
	if(hex.length() != 64) {
		throw new IllegalArgumentException("wrong number of hex bytes when decoding uint256: " + hex.length());
	}
    return Uint256.of(decode(hex));
}

public static byte[] decode256(String hex) {
	byte[] res = decode(hex);
	assertTrue(res.length == 32);
	return res;
}
}
