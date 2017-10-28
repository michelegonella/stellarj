package com.consumimurigni.stellarj.scp;

import java.util.HashSet;
import java.util.Set;

import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.xdr.NodeID;
import com.consuminurigni.stellarj.xdr.PublicKey;

public class QuorumSetSanityChecker {
	  private final boolean mExtraChecks;
	  private final Set<NodeID> mKnownNodes = new HashSet<>();
	private final boolean mIsSane;
	private int mCount = 0;

	public QuorumSetSanityChecker(SCPQuorumSet qSet, boolean extraChecks) {
		this.mExtraChecks = extraChecks;
		this.mIsSane = checkSanity(qSet, 0) && mCount >= 1 && mCount <= 1000;//TODO ugly mCount initialized to 0 and incremented inside checkSanity
	}

    public boolean isSane()
    {
        return mIsSane;
    }

	private boolean checkSanity(SCPQuorumSet qSet, int depth)
	{
	    if (depth > 2) {
	        return false;
	    }

	    if (qSet.getThreshold().lt(1)) {
	        return false;
	    }

	    PublicKey[] v = qSet.getValidators();
	    SCPQuorumSet[] i = qSet.getInnerSets();

	    int totEntries = v.length + i.length;
	    int vBlockingSize = totEntries - qSet.getThreshold().intValue() + 1;
	    mCount += v.length;

	    if (qSet.getThreshold().gt(totEntries)) {
	        return false;
	    }

	    // threshold is within the proper range
	    if (mExtraChecks && qSet.getThreshold().lt(vBlockingSize)) {
	        return false;
	    }

	    for (PublicKey n : v)
	    {
	        boolean r = mKnownNodes.add(NodeID.of(n));
	        if (!r)
	        {
	            // n was already present
	            return false;
	        }
	    }

	    for (SCPQuorumSet iSet : i)
	    {
	        if (!checkSanity(iSet, depth + 1))
	        {
	            return false;
	        }
	    }

	    return true;
	}
}
