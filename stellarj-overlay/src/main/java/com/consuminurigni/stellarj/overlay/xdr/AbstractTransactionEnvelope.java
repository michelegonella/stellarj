package com.consuminurigni.stellarj.overlay.xdr;

import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

public interface AbstractTransactionEnvelope {

	static void encode(XdrDataOutputStream stream, AbstractTransactionEnvelope transaction) {
		// TODO Auto-generated method stub
		
	}

	static AbstractTransactionEnvelope decode(XdrDataInputStream stream) {
		// TODO Auto-generated method stub
		return null;
	}

}
