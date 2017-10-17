package com.consumimurigni.stellarj.ledger;

import java.util.Collections;
import java.util.List;

import com.consumimurigni.stellarj.ledger.xdr.StellarValue;
import com.consumimurigni.stellarj.ledger.xdr.UpgradeType;
import com.consuminurigni.stellarj.xdr.Uint64;

public class LedgerCloseData {
	public static List<UpgradeType> emptyUpgradeSteps = Collections.emptyList();
	public LedgerCloseData(Uint64 lastConsensusLedgerIndex, TxSetFrame externalizedSet, StellarValue b) {
		// TODO Auto-generated constructor stub
	}

}
