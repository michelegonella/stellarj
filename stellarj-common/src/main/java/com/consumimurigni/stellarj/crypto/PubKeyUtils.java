package com.consumimurigni.stellarj.crypto;

import com.consuminurigni.stellarj.common.Tuple2;
import com.consuminurigni.stellarj.xdr.PublicKey;
import com.consuminurigni.stellarj.xdr.Signature;
import com.consuminurigni.stellarj.xdr.Uint64;

public class PubKeyUtils {
	// Return true iff `signature` is valid for `bin` under `key`.
	public static boolean verifySig(PublicKey key, Signature signature,
	               byte[] bin) {
		return false;
	}

	public static void clearVerifySigCache() {
		
	}
	public static Tuple2<Uint64, Uint64> flushVerifySigCacheCounts(Uint64 hits, Uint64 misses) {
		return null;
	}

	public static PublicKey random() {
		return null;
	}

}
