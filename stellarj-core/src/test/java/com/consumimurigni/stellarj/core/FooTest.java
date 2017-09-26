package com.consumimurigni.stellarj.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class FooTest {

	@Test
	public void test() {
		long x = 1;
		for(int i = 0; i < 63; i++) {
			x *= 2;
			System.err.println(i+" - "+(x -1));
		}
		System.err.println(Long.MAX_VALUE);
	}

}
