package com.consumimurigni.stellarj.transactions;

import javax.annotation.Nullable;

import com.consumimurigni.stellarj.role.ILedgerManager;
import com.consuminurigni.stellarj.metering.Metrics;
import com.consuminurigni.stellarj.xdr.AccountID;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.SequenceNumber;
import com.consuminurigni.stellarj.xdr.TransactionEnvelope;
import com.consuminurigni.stellarj.xdr.TransactionResult;
import com.consuminurigni.stellarj.xdr.TransactionResultCode;
import com.consuminurigni.stellarj.xdr.Uint64;

public class TransactionFrame {

	public TransactionFrame(Hash networkID, TransactionEnvelope msg) {
		// TODO Auto-generated constructor stub
	}

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

	public boolean checkValid(ILedgerManager ledgerManager, Metrics metrics, SequenceNumber highSeq) {
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

	//TODO
//	public StellarMessage toStellarMessage() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public static @Nullable TransactionFrame makeTransactionFromWire(Hash networkID, TransactionEnvelope msg) {
	    TransactionFrame res = new TransactionFrame(networkID, msg);
	    return res;
	}

	public double getFeeRatio(ILedgerManager lm) {
		// TODO Auto-generated method stub
		return 0;
	}

	public TransactionEnvelope getEnvelope() {
		// TODO Auto-generated method stub
		return null;
	}

	public TransactionResultCode getResultCode() {
		// TODO Auto-generated method stub
		return null;
	}

}
