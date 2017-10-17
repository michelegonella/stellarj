package com.consumimurigni.stellarj.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtils {
	private static final HashingFunction SHA256 = (data) -> {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		byte[] hash = digest.digest(data);
		return hash;
	};
//	public static byte[] sha256(byte[] data) {
//		MessageDigest digest;
//		try {
//			digest = MessageDigest.getInstance("SHA-256");
//		} catch (NoSuchAlgorithmException e) {
//			throw new RuntimeException(e);
//		}
//		byte[] hash = digest.digest(data);
//		return hash;
//	}

	public static HashingFunction sha256() {
		return SHA256;
	}
}
