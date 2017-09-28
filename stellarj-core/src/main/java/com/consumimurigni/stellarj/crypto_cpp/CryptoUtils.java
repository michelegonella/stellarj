package com.consumimurigni.stellarj.crypto_cpp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtils {
	public static byte[] sha256(byte[] data) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		byte[] hash = digest.digest(data);
		return hash;
	}

	public static String hexAbbrev(byte[] hash) {//upto 4 bytes
		// TODO Auto-generated method stub
		return null;
	}
}
