package com.consumimurigni.stellarj.xdr_cpp;

import org.stellar.sdk.xdr.Uint32;

public class UInt32Interval {
	private Uint32 first;
	private Uint32 second;

	public void setFirst(Uint32 first) {
		this.first = first;
	}

	public void setSecond(Uint32 second) {
		this.second = second;
	}

	public UInt32Interval(Uint32 one, Uint32 two) {
		first = one;
		second = two;
	}

	public Uint32 getFirst() {
		return first;
	}

	public Uint32 getSecond() {
		return second;
	}

}
