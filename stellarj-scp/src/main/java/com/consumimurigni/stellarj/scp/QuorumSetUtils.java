package com.consumimurigni.stellarj.scp;

import com.consumimurigni.stellarj.scp.xdr.SCPQuorumSet;

public class QuorumSetUtils {

// helper function that:
//  * simplifies singleton inner set into outerset
//      { t: n, v: { ... }, { t: 1, X }, ... }
//        into
//      { t: n, v: { ..., X }, .... }
//  * simplifies singleton innersets
//      { t:1, { innerSet } } into innerSet

	public static void normalizeQSet(SCPQuorumSet qSet)
	{
//	    PublicKey[] v = qSet.getValidators();
//	    SCPQuorumSet[] i = qSet.getInnerSets();
	    for(int j = 0; j < qSet.numValidators();)
	    {
	    	SCPQuorumSet it = qSet.getInnerSetAt(j);
	        normalizeQSet(it);
	        // merge singleton inner sets into validator list
	        if (it.getThreshold().getUint32() == 1 && it.getValidators().length == 1 &&
	            it.getInnerSets().length == 0)
	        {
	        	qSet.pushValidator(it.getValidators()[0]);
	        	qSet.removeSetAt(j);
	        }
	        else
	        {
	            j++;
	        }
	    }

	    // simplify quorum set if needed
	    if (qSet.getThreshold().eq(1) && qSet.numValidators() == 0 && qSet.numInnerSets() == 1)
	    {
	    	qSet.setStateFrom(qSet.getInnerSetAt(0));
	    }
	}
}
