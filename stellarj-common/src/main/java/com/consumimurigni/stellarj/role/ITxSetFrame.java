package com.consumimurigni.stellarj.role;

import com.consuminurigni.stellarj.xdr.Hash;

public interface ITxSetFrame {

	ITransactionSet toXDR();

	Hash getContentsHash();

	static ITxSetFrame build(Hash networkID, ITransactionSet txSet) {
		// TODO Auto-generated method stub
		return null;
	}

}
