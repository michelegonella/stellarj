package com.consumimurigni.stellarj.scp;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.Nullable;

import com.consumimurigni.stellarj.crypto.HashingFunction;
import com.consumimurigni.stellarj.crypto.SecretKey;
import com.consuminurigni.stellarj.common.Assert;
import com.consuminurigni.stellarj.scp.xdr.SCPBallot;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.scp.xdr.SCPNomination;
import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.scp.xdr.SCPStatement;
import com.consuminurigni.stellarj.scp.xdr.SCPStatement.SCPStatementPledges.SCPStatementConfirm;
import com.consuminurigni.stellarj.scp.xdr.SCPStatement.SCPStatementPledges.SCPStatementExternalize;
import com.consuminurigni.stellarj.scp.xdr.SCPStatement.SCPStatementPledges.SCPStatementPrepare;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.NodeID;
import com.consuminurigni.stellarj.xdr.Uint64;
import com.consuminurigni.stellarj.xdr.Value;

public class SCP {

	enum EnvelopeState
    {
        INVALID, // the envelope is considered invalid
        VALID    // the envelope is valid
    };

    enum TriBool
    {
        TB_TRUE,
        TB_FALSE,
        TB_MAYBE
    }
    
	private static final Logger log = LogManager.getLogger();

    protected final SCPDriver mDriver;
    protected final LocalNode mLocalNode;
    protected final LinkedHashMap<Uint64, Slot> mKnownSlots = new LinkedHashMap<>();//LinkedHashMap needed for iterator style modification

    public SCP(SCPDriver driver, SecretKey secretKey, boolean isValidator,
            SCPQuorumSet qSetLocal)
   {
    	this.mDriver = driver;
       this.mLocalNode =
           new LocalNode(secretKey, isValidator, qSetLocal, this);
   }
	 public HashingFunction getHashingFunction() {
		 return mDriver.getHashingFunction();
	 }


    public EnvelopeState receiveEnvelope(SCPEnvelope envelope)
    {
        // If the envelope is not correctly signed, we ignore it.
        if (!mDriver.verifyEnvelope(envelope))
        {
            log.debug("SCP receiveEnvelope invalid");
            return EnvelopeState.INVALID;
        }

        Uint64 slotIndex = envelope.getStatement().getSlotIndex();
        //TODO if slot not found ????
        return getSlot(slotIndex, true).processEnvelope(envelope, false);
    }

    public boolean 
    nominate(Uint64 slotIndex, Value  value, Value  previousValue)
    {
        Assert.assertTrue(isValidator());
        return getSlot(slotIndex, true).nominate(value, previousValue, false);
    }

    public void
    stopNomination(Uint64 slotIndex)
    {
        Slot s = getSlot(slotIndex, false);
        if (s != null)
        {
            s.stopNomination();
        }
    }

    private @Nullable Slot getSlot(Uint64 slotIndex, boolean create) {
        Slot res = null;
        Slot it = mKnownSlots.get(slotIndex);
        if (it == null)
        {
            if (create)
            {
                res = new Slot(slotIndex, this);
                mKnownSlots.put(slotIndex, res);
            }
        }
        else
        {
            res = it;
        }
        return res;
	}

	void
    updateLocalQuorumSet(SCPQuorumSet qSet)
    {
        mLocalNode.updateQuorumSet(qSet);
    }

    SCPQuorumSet 
    getLocalQuorumSet()
    {
        return mLocalNode.getQuorumSet();
    }

    public NodeID 
    getLocalNodeID()
    {
        return mLocalNode.getNodeID();
    }

    //TODO ensure synch
    public void purgeSlots(Uint64 maxSlotIndex)
    {
    	//TODO test
    	Iterator<Entry<Uint64, Slot>> itr = mKnownSlots.entrySet().iterator();
        while (itr.hasNext())
        {
            if (itr.next().getKey().lt(maxSlotIndex))
            {
                itr.remove();
            }
        }
    }

    public LocalNode
    getLocalNode()
    {
        return mLocalNode;
    }

    public void dumpInfo(Map<String, Object> ret, int limit)
    {
    	Map<String, Object> slots = new LinkedHashMap<String, Object>();
    	for(Entry<Uint64,Slot> sEntry : mKnownSlots.entrySet()) {
    		if(limit-- <= 0) {
    			break;
    		}
    		sEntry.getValue().dumpInfo(ret);
    	}
    	ret.put("slots", slots);
    }

    public void dumpQuorumInfo(LinkedHashMap<String, Object> ret, NodeID id, boolean summary,
                        Uint64 index)
    {
    	Map<String, Object> slots = new LinkedHashMap<>();
    	ret.put("slots", slots);
        if (index.eq(0))
        {
        	for(Entry<Uint64,Slot> sEntry : mKnownSlots.entrySet()) {
        		sEntry.getValue().dumpQuorumInfo(ret, id, summary);
//        		slots.add(XdrUtils.uint64ToString(sEntry.getKey()), sEntry.getValue().dumpQuorumInfo(ret, id, summary));
        	}
        }
        else
        {
            Slot s = getSlot(index, false);
            if (s != null)
            {
//        		slots.add(XdrUtils.uint64ToString(index), s.dumpQuorumInfo(ret, id, summary));
        		s.dumpQuorumInfo(ret, id, summary);
            }
        }
    }

    public SecretKey getSecretKey()
    {
        return mLocalNode.getSecretKey();
    }

    public boolean isValidator()
    {
        return mLocalNode.isValidator();
    }

    int getKnownSlotsCount()
    {
        return mKnownSlots.size();
    }

    int getCumulativeStatemtCount()
    {
        int c = 0;
        for (Slot s : mKnownSlots.values())
        {
            c += s.getStatementCount();
        }
        return c;
    }

    public List<SCPEnvelope> getLatestMessagesSend(Uint64 slotIndex)
    {
        Slot slot = getSlot(slotIndex, false);
        if (slot != null)
        {
            return slot.getLatestMessagesSend();
        }
        else
        {
            return new LinkedList<SCPEnvelope>();
        }
    }
    
    public void setStateFromEnvelope(Uint64 slotIndex, SCPEnvelope e)
    {
        if (mDriver.verifyEnvelope(e))
        {
            Slot slot = getSlot(slotIndex, true);
            slot.setStateFromEnvelope(e);
        }
    }

    public List<SCPEnvelope> getCurrentState(Uint64 slotIndex)
    {
        Slot slot = getSlot(slotIndex, false);
        if (slot != null)
        {
            return slot.getCurrentState();
        }
        else
        {
            return new LinkedList<SCPEnvelope>();
        }
    }

    public List<SCPEnvelope> getExternalizingState(Uint64 slotIndex)
    {
        Slot slot = getSlot(slotIndex, false);
        if (slot != null)
        {
            return slot.getExternalizingState();
        }
        else
        {
            return new LinkedList<SCPEnvelope>();
        }
    }

    TriBool
    isNodeInQuorum(NodeID node)
    {
        TriBool res = TriBool.TB_MAYBE;
        for (Slot slot : mKnownSlots.values())
        {
            res = slot.isNodeInQuorum(node);
            if (res == TriBool.TB_TRUE || res == TriBool.TB_FALSE)
            {
                break;
            }
        }
        return res;
    }

    String
    getValueString(Value v)
    {
        return mDriver.getValueString(v);
    }

	public SCPDriver getDriver() {
		return mDriver;
	}

	private String ballotToStrInternal(SCPBallot ballot)
	{
		return "(" + ballot.getCounter().toString() + "," + getValueString(ballot.getValue()) + ")";
	}

	String ballotToStr(@Nullable SCPBallot ballot)
	{
	    return ballot == null ? "(<null_ballot>)" : ballotToStrInternal(ballot);
	}

	String envToStr(SCPEnvelope envelope)
	{
	    return envToStr(envelope.getStatement());
	}

	String envToStr(SCPStatement st) 
	{
		StringBuilder oss = new StringBuilder();

	    Hash qSetHash = Slot.getCompanionQuorumSetHashFromStatement(st);

	    oss.append("{ENV@" + mDriver.toShortString(st.getNodeID().getNodeID()) + " | "
	        + " i: " + st.getSlotIndex().toString());
	    switch (st.getPledges().getDiscriminant())
	    {
	    case SCP_ST_PREPARE:
	    {
	        SCPStatementPrepare p = st.getPledges().getPrepare();
	        oss.append(" | PREPARE"
	            + " | D: " + qSetHash.hexAbbrev()
	            + " | b: " + ballotToStr(p.getBallot())
	            + " | p: " + ballotToStr(p.getPrepared())
	            + " | p': " + ballotToStr(p.getPreparedPrime()) + " | c.n: " + p.getNC().toString()
	            + " | h.n: " + p.getNH().toString());
	    }
	    break;
	    case SCP_ST_CONFIRM:
	    {
	        SCPStatementConfirm c = st.getPledges().getConfirm();
	        oss.append(" | CONFIRM"
	            + " | D: " + qSetHash.hexAbbrev()
	            + " | b: " + ballotToStr(c.getBallot()) + " | p.n: " + c.getNPrepared().toString()
	            + " | c.n: " + c.getNCommit().toString() + " | h.n: " + c.getNH().toString());
	    }
	    break;
	    case SCP_ST_EXTERNALIZE:
	    {
	    	SCPStatementExternalize ex = st.getPledges().getExternalize();
	        oss.append(" | EXTERNALIZE"
	            + " | c: " + ballotToStr(ex.getCommit()) + " | h.n: " + ex.getNH().toString()
	            + " | (lastD): " + qSetHash.hexAbbrev());
	    }
	    break;
	    case SCP_ST_NOMINATE:
	    {
	    	SCPNomination nom = st.getPledges().getNominate();
	        oss.append(" | NOMINATE"
	            + " | D: " + qSetHash.hexAbbrev() + " | X: {");
	        boolean first = true;
	        for (Value v : nom.getVotes())
	        {
	            if (!first)
	            {
	                oss.append(" ,");
	            }
	            oss.append("'" + getValueString(v) + "'");
	            first = false;
	        }
	        oss.append("}"
	            + " | Y: {");
	        first = true;
	        for (Value a : nom.getAccepted())
	        {
	            if (!first)
	            {
	                oss.append(" ,");
	            }
	            oss.append("'" + getValueString(a) + "'");
	            first = false;
	        }
	        oss.append("}");
	    }
	    break;
	    }

        oss.append("}");
	    return oss.toString();
	}
	
}
