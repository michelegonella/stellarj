package com.consumimurigni.stellarj.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.consuminurigni.stellarj.common.Assert;
import com.consuminurigni.stellarj.xdr.HmacSha256Key;
import com.consuminurigni.stellarj.xdr.Uint256;

public class SHA256 {
	private MessageDigest digest;
	private SHA256() {
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
	public static SHA256 create() {
		return new SHA256();
	}

    
    public void reset() {
    	digest.reset();
    }
    public void add(byte[] bin) {
    	digest.update(bin);
    }

    public Uint256 finish() {
    	Uint256 ret =  Uint256.of(digest.digest());
    	digest = null;
    	return ret;
    }
 // Unsalted HKDF-extract(bytes) == HMAC(<zero>,bytes)
    public static HmacSha256Key
    hkdfExtract(byte[] bin)
    {
    	HmacSha256Key zerosalt = HmacSha256Key.of(new byte[32]);
        byte[] mac = CryptoUtils.hmacSHA256(zerosalt, bin).getMac();
        return HmacSha256Key.of(mac);
    }

    // Single-step HKDF-expand(key,bytes) == HMAC(key,bytes|0x1)
    //HmacSha256Key
    public static HmacSha256Key
    hkdfExpand(HmacSha256Key key, byte[] bin)
    {
    	Assert.assertTrue(key.getKey().length == 32);
    	byte[] b2 = Arrays.copyOf(bin, bin.length + 1);
    	b2[bin.length] = 0x1;
        return HmacSha256Key.of(CryptoUtils.hmacSHA256(key, b2).getMac());
    }

}
