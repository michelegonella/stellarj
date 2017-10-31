package com.consumimurigni.stellarj.crypto;

import java.io.Writer;

import com.consuminurigni.stellarj.common.Hex;
import com.consuminurigni.stellarj.xdr.PublicKey;
import com.consuminurigni.stellarj.xdr.PublicKeyType;
import com.consuminurigni.stellarj.xdr.Uint256;

public class StrKeyUtils {
	void logKey(Writer w, String key)//TODO
	{
//	    // if it's a hex string, display it in all forms
//	    try
//	    {
//	        Uint256 data = Hex.hexToBin256(key);
//	        PublicKey pk = new PublicKey();
//	        pk.setDiscriminant(PublicKeyType.PUBLIC_KEY_TYPE_ED25519);
//	        pk.setEd25519(data);
//	        SecretKey.logPublicKey(w, pk);
//
//	        SecretKey sk = SecretKey.fromSeed(data.getUint256());
//	        SecretKey.logSecretKey(w, sk);
//	        return;
//	    }
//	    catch (Exception e)
//	    {
//	    	//TODO ignore ??
//	    }
//
//	    // see if it's a public key
//	    try
//	    {
//	        PublicKey pk = KeyUtils.fromStrKey<PublicKey>(key);
//	        logPublicKey(w, pk);
//	        return;
//	    }
//	    catch (Exception e)
//	    {
//	    	//TODO ignore ??
//	    }
//
//	    // see if it's a seed
//	    try
//	    {
//	        SecretKey sk = SecretKey.fromStrKeySeed(key);
//	        logSecretKey(s, sk);
//	        return;
//	    }
//	    catch (Exception e)
//	    {
//	    	//TODO ignore ??
//	    }
//	    s << "Unknown key type" << std::endl;
	}

}
