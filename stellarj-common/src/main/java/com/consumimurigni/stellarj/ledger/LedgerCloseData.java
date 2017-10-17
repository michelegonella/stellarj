package com.consumimurigni.stellarj.ledger;

import java.util.Collections;
import java.util.List;

import com.consumimurigni.stellarj.ledger.xdr.LedgerUpgrade;
import com.consumimurigni.stellarj.ledger.xdr.StellarValue;
import com.consumimurigni.stellarj.ledger.xdr.UpgradeType;
import com.consuminurigni.stellarj.xdr.Uint64;

public class LedgerCloseData {
	public static List<UpgradeType> emptyUpgradeSteps = Collections.emptyList();
	public LedgerCloseData(Uint64 lastConsensusLedgerIndex, TxSetFrame externalizedSet, StellarValue b) {
		// TODO Auto-generated constructor stub
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
