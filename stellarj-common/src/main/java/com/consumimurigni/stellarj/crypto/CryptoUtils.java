package com.consumimurigni.stellarj.crypto;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.consuminurigni.stellarj.xdr.HmacSha256Key;
import com.consuminurigni.stellarj.xdr.HmacSha256Mac;

public class CryptoUtils {
	private static final HashingFunction SHA256 = (data) -> {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
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

	public static HmacSha256Mac hmacSHA256(HmacSha256Key key, byte[] data) throws IllegalArgumentException /*invalid key*/{
		try {
			  Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			  SecretKeySpec secret_key = new SecretKeySpec(key.getKey(), "HmacSHA256");
			  sha256_HMAC.init(secret_key);

			  return HmacSha256Mac.of(sha256_HMAC.doFinal(data));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		} catch (InvalidKeyException e) {
			throw new IllegalArgumentException(e);
		}
		}
	public static HashingFunction sha256() {
		return SHA256;
	}

	public static byte[] sha256(byte[] in) {
		return SHA256.apply(in);
	}
	//TODO rename to Sha
	public static boolean hmacSHA256Verify(HmacSha256Key key, byte[] data, byte[] sig) {
		try {
			  Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			  SecretKeySpec secret_key = new SecretKeySpec(key.getKey(), "HmacSHA256");
			  sha256_HMAC.init(secret_key);

			  return Arrays.equals(sha256_HMAC.doFinal(data), sig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		} catch (InvalidKeyException e) {
			throw new IllegalArgumentException(e);
		}
	}
	public static boolean hmacSHA256Verify(HmacSha256Key key, byte[] data, HmacSha256Mac sig) {
		return hmacSHA256Verify(key, data, sig.getMac());
	}
	public static byte[] randomBytes(int i) {
		// TODO Auto-generated method stub
		return null;
	}

}
