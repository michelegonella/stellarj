package com.consumimurigni.stellarj.crypto;

import com.consuminurigni.stellarj.common.Assert;
import com.consuminurigni.stellarj.common.Tuple2;
import com.consuminurigni.stellarj.xdr.PublicKey;
import com.consuminurigni.stellarj.xdr.PublicKeyType;
import com.consuminurigni.stellarj.xdr.Signature;
import com.consuminurigni.stellarj.xdr.Uint64;

//TODO caching
public class PubKeyUtils {
	// Return true iff `signature` is valid for `bin` under `key`.
	public static boolean verifySig(PublicKey key, Signature signature,
	               byte[] bin) {
		return KeyPair.fromPublicKey(key.getEd25519().getUint256()).verify(bin, signature.getSignature());
//	    Assert.assertTrue(key.getDiscriminant() == PublicKeyType.PUBLIC_KEY_TYPE_ED25519);
//	    if (signature.getSignature().length != 64)
//	    {
//	        return false;
//	    }
//
//	    auto cacheKey = verifySigCacheKey(key, signature, bin);
//
//	    {
//	        std::lock_guard<std::mutex> guard(gVerifySigCacheMutex);
//	        if (gVerifySigCache.exists(cacheKey))
//	        {
//	            ++gVerifyCacheHit;
//	            return gVerifySigCache.get(cacheKey);
//	        }
//	    }
//
//	    ++gVerifyCacheMiss;
//	    bool ok =
//	        (crypto_sign_verify_detached(signature.data(), bin.data(), bin.size(),
//	                                     key.ed25519().data()) == 0);
//	    std::lock_guard<std::mutex> guard(gVerifySigCacheMutex);
//	    gVerifySigCache.put(cacheKey, ok);
//	    return ok;
	}


	// Process-wide global Ed25519 signature-verification cache.
	//
	// This is a pure mathematical function and has no relationship
	// to the state of the process; caching its results centrally
	// makes all signature-verification in the program faster and
	// has no effect on correctness.
//	static std::mutex gVerifySigCacheMutex;
//	static cache::lru_cache<Hash, bool> gVerifySigCache(0xffff);
//	static std::unique_ptr<SHA256> gHasher = SHA256::create();
//	static uint64_t gVerifyCacheHit = 0;
//	static uint64_t gVerifyCacheMiss = 0;

//	static Hash
//	verifySigCacheKey(PublicKey key, Signature signature,
//	                  ByteSlice bin)
//	{
//	    assert(key.type() == PUBLIC_KEY_TYPE_ED25519);
//
//	    gHasher->reset();
//	    gHasher->add(key.ed25519());
//	    gHasher->add(signature);
//	    gHasher->add(bin);
//	    return gHasher->finish();
//	}

//	public static void clearVerifySigCache() {
//		
//	}
//	public static Tuple2<Uint64, Uint64> flushVerifySigCacheCounts(Uint64 hits, Uint64 misses) {
//		return null;
//	}

	public static PublicKey random() {
		return KeyPair.random().getXdrPublicKey();
	}

}
