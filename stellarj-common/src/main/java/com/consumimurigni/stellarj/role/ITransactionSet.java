package com.consumimurigni.stellarj.role;

import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

public interface ITransactionSet {

	static void encode(XdrDataOutputStream stream, ITransactionSet txSet) {
		// TODO Auto-generated method stub
		
	}

	static ITransactionSet decode(XdrDataInputStream stream) {
		// TODO Auto-generated method stub
		return null;
	}

}
