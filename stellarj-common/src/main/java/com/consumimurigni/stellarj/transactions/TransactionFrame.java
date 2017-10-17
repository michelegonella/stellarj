package com.consumimurigni.stellarj.transactions;

import com.consumimurigni.stellarj.ledger.AccountFrame;
import com.consumimurigni.stellarj.ledger.xdr.AccountID;
import com.consumimurigni.stellarj.ledger.xdr.SequenceNumber;
import com.consumimurigni.stellarj.ledger.xdr.TransactionResult;
import com.consumimurigni.stellarj.main.Application;
import com.consuminurigni.stellarj.overlay.xdr.StellarMessage;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.Uint64;

public class TransactionFrame {

	public Hash getFullHash() {
		// TODO Auto-generated method stub
		return null;
	}

	public SequenceNumber getSeqNum() {
		// TODO Auto-generated method stub
		return null;
	}

	public Uint64 getFee() {
		// TODO Auto-generated method stub
		return null;
	}

	public AccountID getSourceID() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean checkValid(Application mApp, SequenceNumber highSeq) {
		// TODO Auto-generated method stub
		return false;
	}

	public TransactionResult getResult() {
		// TODO Auto-generated method stub
		return null;
	}

	public AccountFrame getSourceAccount() {
		// TODO Auto-generated method stub
		return null;
	}

	public StellarMessage toStellarMessage() {
		// TODO Auto-generated method stub
		return null;
	}

}
