package com.consumimurigni.stellarj.ledger;

import com.consumimurigni.stellarj.ledger.xdr.LedgerHeaderHistoryEntry;
import com.consuminurigni.stellarj.xdr.Uint32;

public class LedgerManager {
	public enum State
	    {
	        // Loading state from database, not yet active
	        LM_BOOTING_STATE,

	        // local state is in sync with view of consensus coming from herder
	        // desynchronization will cause transition to CATCHING_UP_STATE.
	        LM_SYNCED_STATE,

	        // local state doesn't match view of consensus from herder
	        // catchup is in progress
	        LM_CATCHING_UP_STATE
	    }

	public void setState(State state) {
		// TODO Auto-generated method stub
		
	}

	public boolean isSynced() {
		// TODO Auto-generated method stub
		return false;
	}

	public LedgerHeaderHistoryEntry getLastClosedLedgerHeader() {
		// TODO Auto-generated method stub
		return null;
	}

	public void valueExternalized(LedgerCloseData ledgerData) {
		// TODO Auto-generated method stub
		
	}

	public Uint32 getLedgerNum() {
		// TODO Auto-generated method stub
		return null;
	};
}
