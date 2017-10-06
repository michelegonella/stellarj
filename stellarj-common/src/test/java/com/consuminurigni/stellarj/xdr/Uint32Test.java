package com.consuminurigni.stellarj.xdr;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class Uint32Test {
	static {
		System.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "INFO");
	}
	@Test
	public void testIntValue() {
		try {
			Uint32.of2ComplRepr(0b1000_0000__0000_0000__0000_0000__0000_0000)
				.intValue();
			fail();
		} catch(IllegalStateException e) {}
		assertEquals(1, 
				Uint32.of2ComplRepr(0b0000_0000__0000_0000__0000_0000__0000_0001)
					.intValue());
		try {
			Uint32.UINT32_MAX.intValue();
			fail();
		} catch(IllegalStateException e) {}
		assertEquals(0b1111_1111__1111_1111__1111_1111__1111_1111L, 
				Uint32.UINT32_MAX.longValue());
	}

	@Test
	public void testBuild() {
		Uint32.ofPositiveInt(0);
		Uint32.ofPositiveInt(Integer.MAX_VALUE);
		try {
			Uint32.ofPositiveInt(-1);
			fail();
		} catch(IllegalStateException e) {}
		assertEquals(2147483648L, Uint32.of2ComplRepr(Integer.MIN_VALUE).longValue());
	}

	@Test
	public void testEqualsAndHashCode() {
		Set<Uint32> s = new HashSet<>();
		s.add(Uint32.ofPositiveInt(10));
		for(int i = 0; i < 100; i++) {
			s.add(Uint32.of2ComplRepr(-10));
			s.add(Uint32.ofPositiveInt(3));
		}
		assertEquals(3, s.size());
		
		assertFalse(Uint32.ofPositiveInt(32).equals(new Integer(32)));
		assertFalse(Uint32.ofPositiveInt(32).equals(Int32.of(32)));
		assertTrue(Uint32.ofPositiveInt(32).equals(Uint32.of2ComplRepr(32)));
	}

	@Test
	public void testEncode() {
		assertArrayEquals(new byte[] {(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,}, Uint32.of2ComplRepr(-1).encode());
	}

	@Test
	public void testEqualities() {
		assertTrue(Uint32.of2ComplRepr(1).eqOne());
		assertTrue(Uint32.ofPositiveInt(1).eqOne());
		assertFalse(Uint32.of2ComplRepr(-1).eqOne());
		assertFalse(Uint32.ofPositiveInt(0).eqOne());

		assertTrue(Uint32.of2ComplRepr(0).eqZero());
		assertTrue(Uint32.ofPositiveInt(0).eqZero());
		assertFalse(Uint32.of2ComplRepr(-1).eqZero());
		assertFalse(Uint32.ofPositiveInt(1).eqZero());
		
		assertTrue(Uint32.of2ComplRepr(1).eq(Uint32.ofPositiveInt(1)));
		assertFalse(Uint32.of2ComplRepr(1).eq(Uint32.ofPositiveInt(1001)));

		assertTrue(Uint32.of2ComplRepr(1).ne(Uint32.ofPositiveInt(10)));
		assertFalse(Uint32.of2ComplRepr(1).ne(Uint32.ofPositiveInt(1)));

		assertTrue(Uint32.of2ComplRepr(1).ne(-11));
		assertFalse(Uint32.of2ComplRepr(1).ne(1));

	}

	@Test
	public void testBigInteger() {
		BigInteger bi = BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.valueOf(Integer.MAX_VALUE));
		assertEquals(bi, Uint32.of2ComplRepr(-2).asBigInteger());
	}

	@Test
	public void testInequalities() {
		assertTrue(Uint32.of2ComplRepr(-2).gt(Uint32.ofPositiveInt(Integer.MAX_VALUE)));
		assertFalse(Uint32.of2ComplRepr(-2).lte(Uint32.ofPositiveInt(Integer.MAX_VALUE)));
		
		assertTrue(Uint32.of2ComplRepr(-2).lte(Uint32.of2ComplRepr(-2)));
		
	}

	@Test
	public void testSum() {
		assertEquals(Uint32.of2ComplRepr(-2), Uint32.ofPositiveInt(Integer.MAX_VALUE).plus(Uint32.ofPositiveInt(Integer.MAX_VALUE)));
		assertEquals(Uint32.of2ComplRepr(-1), Uint32.ofPositiveInt(Integer.MAX_VALUE).plus(Uint32.of2ComplRepr(Integer.MAX_VALUE + 1)));
		assertEquals(Uint32.of2ComplRepr(0x80000000), Uint32.ofPositiveInt(Integer.MAX_VALUE).plus(Uint32.ONE));
		try {
			Uint32.ONE.plus(-1);
			fail();
		} catch(IllegalStateException e) {}
		
		Uint32 u = Uint32.ofPositiveInt(Integer.MAX_VALUE);
		try {//overflow
			u.plus(u).plus(u);
			fail();
		} catch(IllegalArgumentException e) {}
		u.plus(Integer.MAX_VALUE);
		try {//overflow
			u.plus(Integer.MAX_VALUE).plus(10);
			fail();
		} catch(IllegalArgumentException e) {}
	}


}
