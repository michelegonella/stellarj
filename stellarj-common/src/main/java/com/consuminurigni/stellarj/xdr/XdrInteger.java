package com.consuminurigni.stellarj.xdr;

import java.math.BigInteger;

public abstract class XdrInteger extends Number {
	private static final long serialVersionUID = 2483770288666595149L;

	@Override
	public int intValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long longValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float floatValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double doubleValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	public BigInteger asBigInteger() {
		return new BigInteger("34613645132864313256431286341325465135");
	}
	
	public enum Rounding
	{
	    ROUND_DOWN,
	    ROUND_UP
	};
	public static final BigInteger UINT64_MAX = new BigInteger("18446744073709551615");
	public static final BigInteger INT64_MAX = new BigInteger("9223372036854775807");
	//TODO test
	private static boolean bigDivide64(Uint64 result, Uint64 A, Uint64 B, Uint64 C,
	          Rounding rounding)
	{
		BigInteger a = new BigInteger(1, new BigInteger(Long.toHexString(A.getUint64()), 16).toByteArray());
		BigInteger b = new BigInteger(1, new BigInteger(Long.toHexString(B.getUint64()), 16).toByteArray());
		BigInteger c = new BigInteger(1, new BigInteger(Long.toHexString(C.getUint64()), 16).toByteArray());
	    // update when moving to (signed) int128
		BigInteger x = rounding == Rounding.ROUND_DOWN
			? a.multiply(b).divide(c)
			: a.multiply(b).add(c).add(new BigInteger("1")).divide(c);

	    if(x.compareTo(INT64_MAX) > 0) {
	    	//TODO !!! positive longs > Long.MAX_VALUE
	    	return false;
	    } else {
			result.setUint64(x.longValue());
			return true;
	    }
	}

	public static Uint64 bigDivide64(Uint64 A, Uint64 B, Uint64 C, Rounding rounding)
	{
		Uint64 res = new Uint64();
	    if (!bigDivide64(res, A, B, C, rounding))
	    {
	        throw new ArithmeticException("overflow while performing bigDivide");
	    }
	    return res;
	}


	public static Uint64 bigDivide64(BigInteger a, BigInteger b, BigInteger c, Rounding rounding)
	{
		Uint64 res = new Uint64();
		BigInteger x = rounding == Rounding.ROUND_DOWN
				? a.multiply(b).divide(c)
				: a.multiply(b).add(c).add(new BigInteger("1")).divide(c);

		    if(x.compareTo(INT64_MAX) > 0) {
		    	//TODO !!! positive longs > Long.MAX_VALUE
		        throw new ArithmeticException("overflow while performing bigDivide");
		    } else {
				res.setUint64(x.longValue());
		    }
	    return res;
	}

}
