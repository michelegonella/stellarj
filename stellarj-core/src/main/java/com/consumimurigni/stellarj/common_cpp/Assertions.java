package com.consumimurigni.stellarj.common_cpp;

import org.stellar.sdk.xdr.Uint64;

import com.consumimurigni.stellarj.xdr_cpp.XdrUtils;

public class Assertions {

	public static void assertTrue(boolean cond) throws IllegalStateException {
		if(! cond) {
			throw new IllegalStateException();
		}
	}

	public static void assertEquals(Uint64 x, Uint64 y) throws IllegalStateException {
		if(! XdrUtils.uint64Equals(x, y)) {
			throw new IllegalStateException();
		}
	}
//
	public static void abort() {
		assert false;
	}

	public static void dbgAbort() {
		assert false;
	}

	public static <T> T assertNotNull(T value) {
		if(value == null) {
			throw new NullPointerException();
		}
		return value;
	}

}
