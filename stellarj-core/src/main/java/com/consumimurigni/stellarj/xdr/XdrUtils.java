package com.consumimurigni.stellarj.xdr;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.stellar.sdk.xdr.Int32;
import org.stellar.sdk.xdr.NodeID;
import org.stellar.sdk.xdr.PublicKey;
import org.stellar.sdk.xdr.SCPBallot;
import org.stellar.sdk.xdr.SCPEnvelope;
import org.stellar.sdk.xdr.SCPQuorumSet;
import org.stellar.sdk.xdr.Uint32;
import org.stellar.sdk.xdr.Uint64;
import org.stellar.sdk.xdr.Value;
import org.stellar.sdk.xdr.XdrDataOutputStream;

public class XdrUtils {

	public static final Uint32 UINT32_MAX = new Uint32();
	public static final Uint32 UINT32_ZERO = new Uint32();
	public static final Uint64 UINT64_ZERO = new Uint64();
	public static final Value NO_VALUE = new Value();
	static {
		UINT32_MAX.setUint32(Integer.MAX_VALUE);
		UINT32_ZERO.setUint32(0);
		UINT64_ZERO.setUint64(0L);
		NO_VALUE.setValue(new byte[0]);
	}

	public static boolean uint32Equals(Uint32 x, Uint32 y) {
		return x.getUint32().equals(y.getUint32());
	}

	public static boolean uint32NotEquals(Uint32 x, Uint32 y) {
		return ! uint32Equals(x, y);
	}

	public static boolean uint64Equals(Uint64 x, Uint64 y) {
		return x.getUint64().equals(y.getUint64());
	}

	public static boolean uint64GreaterThan(Uint64 x, Uint64 y) {
		return x.getUint64() > (y.getUint64());
	}
	public static boolean uint64GreaterThanOrEquals(Uint64 x, Uint64 y) {
		return x.getUint64() >= (y.getUint64());
	}
	public static boolean uint64LesserThan(Uint64 x, Uint64 y) {
		return x.getUint64() < (y.getUint64());
	}

	public static boolean uint32LesserThan(Uint32 x, Uint32 y) {
		return x.getUint32().intValue() < y.getUint32().intValue();
	}

	public static boolean uint32LesserThanOrEquals(Uint32 x, Uint32 y) {
		return uint32LesserThan(x, y) || uint32Equals(x, y);
	}

	public static boolean uint32GreaterThan(Uint32 x1, Uint32 x2) {
		return ! uint32LesserThanOrEquals(x2, x1);
	}

	//TODO
	public static boolean valueLesserThan(Value x, Value y) {
		return true;
	}

	//TODO
	public static boolean valueEquals(Value x, Value y) {
		return true;
	}

	
//	//TODO
//	public static boolean valueGreaterThanOrEquals(Value x, Value y) {
//		return true;
//	}

	public static boolean uint32GtZero(Uint32 x) {
		return x.getUint32() > 0;
	}
	public static boolean uint32EqualsZero(Uint32 x) {
		return x.getUint32() > 0;
	}

	public static boolean valueEmpty(@Nullable Value v) {
		return v == null || v.getValue().length == 0;
	}

	public static boolean ballotEquals(SCPBallot b1, SCPBallot b2) {
		return valueEquals(b1.getValue(), b2.getValue()) && uint32Equals(b1.getCounter(), b2.getCounter());
	}

	public static boolean envelopeEquals(SCPEnvelope lastEnv, SCPEnvelope envelope) {
		// TODO Auto-generated method stub
		return false;
	}

	public static SCPBallot newSCPBallot(Uint32 counter, Value value) {
		SCPBallot b = new SCPBallot();
		b.setCounter(counter);
		b.setValue(value);
		return b;
	}

	public static boolean nodeIDEquals(NodeID key, NodeID localNodeID) {
		// TODO Auto-generated method stub
		return false;
	}

	public static void scpQuorumSetCopy(SCPQuorumSet src, SCPQuorumSet dst) {
		dst.setInnerSets(src.getInnerSets());
		dst.setThreshold(src.getThreshold());
		dst.setValidators(src.getValidators());
	}
	
	public static <T> byte[] xdrEncode(T t, BiConsumer<XdrDataOutputStream, T> encoder) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		encoder.accept(new XdrDataOutputStream(baos), t);
		byte[] barr = baos.toByteArray();
		return barr;

	}

	public static Uint32 newUint32(int i) {
		Uint32 res = new Uint32();
		res.setUint32(i);
		return res;
	}

	public static boolean publicKeyEquals(PublicKey k1, PublicKey k2) {
		return k1.getDiscriminant() == k2.getDiscriminant()
				&& Arrays.equals(k1.getEd25519().getUint256(), k2.getEd25519().getUint256())
			;
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
	public static boolean contains(Collection<NodeID> nodeSet, PublicKey validator) {
		for(NodeID nid : nodeSet) {
    		if(XdrUtils.publicKeyEquals(validator, nid.getNodeID())) {
	                return true;
    		}
    	}
		return false;
	}

	public static boolean contains(Collection<Value> coll, Value v) {
		for(Value x : coll) {
    		if(XdrUtils.valueEquals(v, x)) {
	                return true;
    		}
    	}
		return false;
	}

	public static boolean contains(Value[] coll, Value v) {
		for(Value x : coll) {
    		if(XdrUtils.valueEquals(v, x)) {
	                return true;
    		}
    	}
		return false;
	}

	//TODO ????
	public static boolean isSorted(Value[] vals) {
		if(vals.length < 2) {
			return true;
		}
		byte[] prec = vals[0].getValue();
		for(int i = 1; i < vals.length; i++) {
			byte[] cur = vals[i].getValue();
			if(Arrays.equals(prec, cur)) {
				//TODO correct ?
				prec = cur;
				continue;
			} else {
				for(int j = 0; j < prec.length; j++) {
					if(cur.length > j) {
						if(Byte.toUnsignedInt(prec[j]) > Byte.toUnsignedInt(cur[j])) {
							return false;
						}
					}
				}
				prec = cur;
			}
		}
		return true;
	}

	public static String uint64ToString(Uint64 x) {
		// TODO Auto-generated method stub
		return x.getUint64().toString();
	}

	public static byte[] uint64ToOpaque(Uint64 x) {
		ByteBuffer bb = ByteBuffer.allocate(8);
		bb.putLong(x.getUint64());
		return bb.array();
	}

	public static byte[] uint32ToOpaque(Uint32 x) {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(x.getUint32());
		return bb.array();
	}

	public static byte[] int32ToOpaque(Int32 x) {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(x.getInt32());
		return bb.array();
	}

}
