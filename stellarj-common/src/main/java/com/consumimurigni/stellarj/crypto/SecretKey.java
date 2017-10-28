package com.consumimurigni.stellarj.crypto;

import com.consuminurigni.stellarj.xdr.PublicKey;
import com.consuminurigni.stellarj.xdr.Signature;

//TODO
public class SecretKey {
	private KeyPair keyPair;


	public SecretKey(KeyPair keyPair) {
		this.keyPair = keyPair;
	}
	public PublicKey getPublicKey() {
		return keyPair.getXdrPublicKey();
	}

	public Signature sign(byte[] pack) {
//		assert(mKeyType == PUBLIC_KEY_TYPE_ED25519);
//
//	    Signature out(crypto_sign_BYTES, 0);
//	    if (crypto_sign_detached(out.data(), NULL, bin.data(), bin.size(),
//	                             mSecretKey.data()) != 0)
//	    {
//	        throw std::runtime_error("error while signing");
//	    }
//	    return out;
		Signature sig = new Signature();
		sig.setSignature(keyPair.sign(pack));
		return sig;
	}
	public static SecretKey fromSeed(byte[] hash) {
		return new SecretKey(KeyPair.fromSecretSeed(hash));
	}

}
