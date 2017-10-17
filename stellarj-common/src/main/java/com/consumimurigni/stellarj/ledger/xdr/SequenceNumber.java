package com.consumimurigni.stellarj.ledger.xdr;

import java.io.IOException;

import com.consuminurigni.stellarj.xdr.Uint64;
import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

public class SequenceNumber extends Uint64 {
	private static final long serialVersionUID = -4764230025848350065L;

	  public static void encode(XdrDataOutputStream stream, SequenceNumber  encodedUint64) throws IOException {
		  stream.writeLong(encodedUint64.uint64);
		  }
		  public static SequenceNumber decode(XdrDataInputStream stream) throws IOException {
			  SequenceNumber decodedUint64 = new SequenceNumber();
		  decodedUint64.uint64 = stream.readLong();
		    return decodedUint64;
		  }

			@Override
			public boolean equals(Object obj) {
				return obj instanceof Uint64 && ((Uint64) obj).uint64 == uint64;
			}
			public static SequenceNumber of(long i) {
				// TODO Auto-generated method stub
				return null;
			}
			public static SequenceNumber max(SequenceNumber seqNum, SequenceNumber mMaxSeq) {
				// TODO Auto-generated method stub
				return null;
			}

}
