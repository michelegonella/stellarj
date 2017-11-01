package com.consumimurigni.stellarj.transactions;

import com.consumimurigni.stellarj.ledger.AccountFrame;
import com.consumimurigni.stellarj.ledger.LedgerManager;
import com.consumimurigni.stellarj.ledger.xdr.AccountID;
import com.consumimurigni.stellarj.ledger.xdr.SequenceNumber;
import com.consumimurigni.stellarj.ledger.xdr.TransactionResult;
import com.consumimurigni.stellarj.ledger.xdr.TransactionResultCode;
import com.consumimurigni.stellarj.ledgerimpl.xdr.TransactionEnvelope;
import com.consumimurigni.stellarj.main.Application;
import com.consuminurigni.stellarj.metering.Metrics;
import com.consuminurigni.stellarj.overlay.xdr.StellarMessage;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.xdr.Hash;
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

	public boolean checkValid(LedgerManager ledgerManager, Metrics metrics, SequenceNumber highSeq) {
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

	public static TransactionFrame makeTransactionFromWire(Hash networkID, TransactionEnvelope msg) {
	    TransactionFrame res = new TransactionFrame(networkID, msg);
	    return res;
	}

	public double getFeeRatio(LedgerManager lm) {
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
