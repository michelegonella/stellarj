package com.consuminurigni.stellarj.xdr;

import com.consuminurigni.stellarj.common.Assert;

public class UInt32Interval {
	private Uint32 lower;
	private Uint32 upper;

	public UInt32Interval(Uint32 low, Uint32 hi) {
		Assert.assertTrue(hi.gteq(low));
		lower = low;
		upper = hi;
	}

	public Uint32 getFirst() {
		return lower;
	}

	public Uint32 getSecond() {
		return upper;
	}

}
