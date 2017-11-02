package com.consuminurigni.stellarj.common;

public class MathUtils {
	public static long randLong(long max) {
		return new Double(Math.random() * max).longValue();
	}

	public static int randInt(int max) {
		return (int)randLong(max);
	}

}
