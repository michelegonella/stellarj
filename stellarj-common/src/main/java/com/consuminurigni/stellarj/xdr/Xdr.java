package com.consuminurigni.stellarj.xdr;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


import com.consumimurigni.stellarj.ledger.xdr.TransactionSet;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;

public class Xdr {
////////////////////////////////////////////////////////////////
//XDR containers (xvector, xarrray, pointer) and bytes (xstring,
//opaque_vec, opaque_array)
////////////////////////////////////////////////////////////////

//! Maximum length of vectors.  (The RFC says 0xffffffff, but out of
//! paranoia for integer overflows we chose something that still fits
//! in 32 bits when rounded up to a multiple of four.)
public static final Uint32 XDR_MAX_LEN = Uint32.of2ComplRepr(0xfffffffc);

	public static byte[] pack(byte[]... bufs) {
		 int size = 0;

		    for (byte[] bytes : bufs)
		    {
		        size += bytes.length;
		    }

		    ByteBuffer byteBuffer = ByteBuffer.allocate(size);

		    for (byte[] bytes : bufs)
		    {
		        byteBuffer.put(bytes);
		    }

		    return byteBuffer.array();
	}

	@SafeVarargs
	public static byte[] packObjects(Collection<? extends Encodable>... xdrObjs) {
		return null;
	}

	public static void xdr_from_opaque(byte[] buffer, List<SCPEnvelope> latestEnvs, List<TransactionSet> latestTxSets,
			List<SCPQuorumSet> latestQSets) {
		// TODO Auto-generated method stub
		
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

}
