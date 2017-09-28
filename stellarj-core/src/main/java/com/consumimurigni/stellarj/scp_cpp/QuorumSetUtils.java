package com.consumimurigni.stellarj.scp_cpp;

import org.stellar.sdk.xdr.PublicKey;
import org.stellar.sdk.xdr.SCPQuorumSet;

import com.consumimurigni.stellarj.common_cpp.Vectors;
import com.consumimurigni.stellarj.xdr_cpp.XdrUtils;

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
	    PublicKey[] v = qSet.getValidators();
	    SCPQuorumSet[] i = qSet.getInnerSets();
	    for(int j = 0; j < i.length;)
	    {
	    	SCPQuorumSet it = i[j];
	        normalizeQSet(it);
	        // merge singleton inner sets into validator list
	        if (it.getThreshold().getUint32() == 1 && it.getValidators().length == 1 &&
	            it.getInnerSets().length == 0)
	        {
	        	qSet.setValidators(v = Vectors.emplace_back(v, it.getValidators()[0]));
	        	qSet.setInnerSets(i = Vectors.erase(i, j));
	        }
	        else
	        {
	            j++;
	        }
	    }

	    // simplify quorum set if needed
	    if (qSet.getThreshold().getUint32() == 1 && v.length == 0 && i.length == 1)
	    {
	    	XdrUtils.scpQuorumSetCopy(i[0], qSet);
	    }
	}
}
