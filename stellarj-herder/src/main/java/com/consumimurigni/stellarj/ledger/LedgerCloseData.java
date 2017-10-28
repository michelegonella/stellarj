package com.consumimurigni.stellarj.ledger;

import java.util.Collections;
import java.util.List;

import com.consumimurigni.stellarj.herder.TxSetFrame;
import com.consumimurigni.stellarj.ledger.xdr.LedgerUpgrade;
import com.consumimurigni.stellarj.ledger.xdr.StellarValue;
import com.consumimurigni.stellarj.ledger.xdr.UpgradeType;
import com.consuminurigni.stellarj.xdr.Uint32;

/**
* Helper class that describes a single ledger-to-close -- a set of transactions
* and auxiliary values -- as decided by the Herder (and ultimately: SCP). This
* does not include the effects of _performing_ any transactions, merely the
* values that the network has agreed _to apply_ to the current ledger,
* atomically, in order to produce the next ledger.
*/
public class LedgerCloseData {
	public static List<UpgradeType> emptyUpgradeSteps = Collections.emptyList();
	
	private final Uint32 mLedgerSeq;
	private final TxSetFrame mTxSet;
	private final StellarValue mValue;
	
	public LedgerCloseData(Uint32 ledgerSeq, TxSetFrame txSet, StellarValue v) {
		this.mLedgerSeq = ledgerSeq;
		this.mTxSet = txSet;
		this.mValue = v;
		
	}
    Uint32
    getLedgerSeq()
    {
        return mLedgerSeq;
    }
    TxSetFrame
    getTxSet()
    {
        return mTxSet;
    }
    StellarValue
    getValue() 
    {
        return mValue;
    }


	public static String stellarValueToString(StellarValue sv) {
    StringBuilder res = new StringBuilder();

    res.append("[ ")
       .append(" txH: ").append(sv.getTxSetHash().hexAbbrev()).append(", ct: ").append(sv.getCloseTime())
       .append(", upgrades: [");
    for (UpgradeType upgrade : sv.getUpgrades())
    {
        if (upgrade.isEmpty())
        {
            // should not happen as this is not valid
            res.append("<empty>");
        }
        else
        {
            try
            {
                LedgerUpgrade lupgrade = LedgerUpgrade.build(upgrade);
                //TODO cpp xdr::xdr_from_opaque(upgrade, lupgrade);
                switch (lupgrade.getDiscriminant())
                {
                case LEDGER_UPGRADE_VERSION:
                    res.append("VERSION=").append(lupgrade.getNewLedgerVersion());
                    break;
                case LEDGER_UPGRADE_BASE_FEE:
                    res.append("BASE_FEE=").append(lupgrade.getNewBaseFee());
                    break;
                case LEDGER_UPGRADE_MAX_TX_SET_SIZE:
                    res.append("MAX_TX_SET_SIZE=").append(lupgrade.getNewMaxTxSetSize());
                    break;
                default:
                    res.append("<unsupported>");
                }
            }
            catch (Exception e)
            {
                res.append("<unknown>");
            }
        }
        res.append(", ");
    }
    res.append(" ] ]");

    return res.toString();
}

}
