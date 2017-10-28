package com.consumimurigni.stellarj.crypto;

import static com.consuminurigni.stellarj.common.Assert.assertNotNull;

import com.consuminurigni.stellarj.common.AbstractTransaction;
import com.consuminurigni.stellarj.xdr.SignerKey;
import com.consuminurigni.stellarj.xdr.SignerKeyType;
import com.consuminurigni.stellarj.xdr.Uint256;

/**
 * Signer is a helper class that creates {@link org.stellar.sdk.xdr.SignerKey} objects.
 */
public class Signer {
    /**
     * Create <code>ed25519PublicKey</code> {@link org.stellar.sdk.xdr.SignerKey} from
     * a {@link org.stellar.sdk.KeyPair}
     * @param keyPair
     * @return org.stellar.sdk.xdr.SignerKey
     */
    public static SignerKey ed25519PublicKey(KeyPair keyPair) {
        assertNotNull(keyPair, "keyPair cannot be null");
        return keyPair.getXdrSignerKey();
    }

    /**
     * Create <code>sha256Hash</code> {@link org.stellar.sdk.xdr.SignerKey} from
     * a sha256 hash of a preimage.
     * @param hash
     * @return org.stellar.sdk.xdr.SignerKey
     */
    public static SignerKey sha256Hash(byte[] hash) {
    	assertNotNull(hash, "hash cannot be null");
        SignerKey signerKey = new SignerKey();
        Uint256 value = Signer.createUint256(hash);

        signerKey.setDiscriminant(SignerKeyType.SIGNER_KEY_TYPE_HASH_X);
        signerKey.setHashX(value);

        return signerKey;
    }

    /**
     * Create <code>preAuthTx</code> {@link org.stellar.sdk.xdr.SignerKey} from
     * a {@link com.consumimurigni.stellarj.ledger.xdr.stellar.sdk.xdr.Transaction} hash.
     * @param tx
     * @return org.stellar.sdk.xdr.SignerKey
     */
    public static SignerKey preAuthTx(AbstractTransaction tx) {
    	assertNotNull(tx, "tx cannot be null");
        SignerKey signerKey = new SignerKey();
        throw new UnsupportedOperationException("TODO");//TODO
//        Uint256 value = Signer.createUint256(tx.hash());
//
//        signerKey.setDiscriminant(SignerKeyType.SIGNER_KEY_TYPE_PRE_AUTH_TX);
//        signerKey.setPreAuthTx(value);
//
//        return signerKey;
    }

    /**
     * Create <code>preAuthTx</code> {@link org.stellar.sdk.xdr.SignerKey} from
     * a transaction hash.
     * @param hash
     * @return org.stellar.sdk.xdr.SignerKey
     */
    public static SignerKey preAuthTx(byte[] hash) {
    	assertNotNull(hash, "hash cannot be null");
        SignerKey signerKey = new SignerKey();
        Uint256 value = Signer.createUint256(hash);

        signerKey.setDiscriminant(SignerKeyType.SIGNER_KEY_TYPE_PRE_AUTH_TX);
        signerKey.setPreAuthTx(value);

        return signerKey;
    }

    private static Uint256 createUint256(byte[] hash) {
        if (hash.length != 32) {
            throw new RuntimeException("hash must be 32 bytes long");
        }
        Uint256 value = new Uint256();
        value.setUint256(hash);
        return value;
    }
}
