package com.consumimurigni.stellarj.ledger;

import java.util.List;

import com.consumimurigni.stellarj.ledger.xdr.TransactionSet;
import com.consumimurigni.stellarj.main.Application;
import com.consumimurigni.stellarj.transactions.TransactionFrame;
import com.consuminurigni.stellarj.xdr.Hash;

public class TxSetFrame {

	//copy constructor
	public TxSetFrame(TxSetFrame highestTxSet) {
		// TODO Auto-generated constructor stub
	}

	public TxSetFrame(Hash hash) {
		// TODO Auto-generated constructor stub
	}

	public TxSetFrame(Hash networkID, TransactionSet txset) {
		// TODO Auto-generated constructor stub
	}

	public Hash getContentsHash() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean checkValid(Application mApp) {
		// TODO Auto-generated method stub
		return false;
	}

	public List<TransactionFrame> getTransactions() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<TransactionFrame> trimInvalid(Application mApp) {
		// TODO Auto-generated method stub
		return null;
	}

	public Hash previousLedgerHash() {
		// TODO Auto-generated method stub
		return null;
	}

	public void add(TransactionFrame tx) {
		// TODO Auto-generated method stub
		
	}

	public void trimInvalid(Application mApp, List<TransactionFrame> removed) {
		// TODO Auto-generated method stub
		
	}

	public void surgePricingFilter(LedgerManager mLedgerManager) {
		// TODO Auto-generated method stub
		
	}

	public void toXDR(TransactionSet ts) {
//	    txSet.txs.resize(xdr::size32(mTransactions.size()));
//	    for (unsigned int n = 0; n < mTransactions.size(); n++)
//	    {
//	        txSet.txs[n] = mTransactions[n]->getEnvelope();
//	    }
//	    txSet.previousLedgerHash = mPreviousLedgerHash;
	}

	public List<TransactionFrame> sortForApply() {
		// TODO Auto-generated method stub
		return null;
	}

}
