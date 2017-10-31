package com.consumimurigni.stellarj.crypto;

import java.io.IOException;
import java.io.Writer;

import com.consuminurigni.stellarj.common.Hex;
import com.consuminurigni.stellarj.common.SecretValue;
import com.consuminurigni.stellarj.xdr.PublicKey;
import com.consuminurigni.stellarj.xdr.Signature;

//TODO
public class SecretKey {
	private final KeyPair keyPair;


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

	public static void logPublicKey(Writer w, PublicKey pk) throws IOException
	{
		w.write("PublicKey:+\nstrKey: ");
		w.write(KeyUtils.toStrKey(pk));
		w.write("\nhex: ");
		w.write(Hex.encode(pk.getEd25519().getUint256()));
		w.write("\n");
	}

	public static void logSecretKey(Writer w, SecretKey sk) throws IOException
	{
		w.write("Seed:+\nstrKey: ");
		w.write(sk.getStrKeySeed().getValue());
		w.write("\n");
	    logPublicKey(w, sk.getPublicKey());
	}
	public SecretValue getStrKeySeed() {
		return new SecretValue(new String(keyPair.getSecretSeed()));
	}
	public static SecretKey fromStrKeySeed(String key) {
		return new SecretKey(KeyPair.fromSecretSeed(key));
	}
	public static SecretKey random() {
		return new SecretKey(KeyPair.random());
	}

	@Override
	public int hashCode() {
		return keyPair.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		return obj instanceof SecretKey
			&& ((SecretKey)obj).keyPair.equals(keyPair);
	}

}
