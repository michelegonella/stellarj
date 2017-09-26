package com.consumimurigni.stellarj.core;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.stellar.sdk.xdr.Hash;
import org.stellar.sdk.xdr.NodeID;
import org.stellar.sdk.xdr.SCPBallot;
import org.stellar.sdk.xdr.SCPEnvelope;
import org.stellar.sdk.xdr.SCPNomination;
import org.stellar.sdk.xdr.SCPQuorumSet;
import org.stellar.sdk.xdr.SCPStatement;
import org.stellar.sdk.xdr.SCPStatement.SCPStatementPledges.SCPStatementConfirm;
import org.stellar.sdk.xdr.SCPStatement.SCPStatementPledges.SCPStatementExternalize;
import org.stellar.sdk.xdr.SCPStatement.SCPStatementPledges.SCPStatementPrepare;
import org.stellar.sdk.xdr.Uint64;
import org.stellar.sdk.xdr.Value;

import com.consumimurigni.stellarj.crypto.CryptoUtils;
import com.consumimurigni.stellarj.crypto.SecretKey;
import com.consumimurigni.stellarj.xdr.Assertions;
import com.consumimurigni.stellarj.xdr.XdrUtils;
import com.google.gson.JsonObject;

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

    SCPDriver mDriver;
    protected LocalNode mLocalNode;
    protected LinkedHashMap<Uint64, Slot> mKnownSlots = new LinkedHashMap<>();//LinkedHashMap needed for iterator style modification

    public SCP(SCPDriver driver, SecretKey secretKey, boolean isValidator,
            SCPQuorumSet qSetLocal)
   {
    	this.mDriver = driver;
       this.mLocalNode =
           new LocalNode(secretKey, isValidator, qSetLocal, this);
   }

    EnvelopeState receiveEnvelope(SCPEnvelope envelope)
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

    boolean 
    nominate(Uint64 slotIndex, Value  value, Value  previousValue)
    {
        Assertions.assertTrue(isValidator());
        return getSlot(slotIndex, true).nominate(value, previousValue, false);
    }

    void
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

    NodeID 
    getLocalNodeID()
    {
        return mLocalNode.getNodeID();
    }

    //TODO ensure synch
    void purgeSlots(Uint64 maxSlotIndex)
    {
    	//TODO test
    	Iterator<Entry<Uint64, Slot>> itr = mKnownSlots.entrySet().iterator();
        while (itr.hasNext())
        {
            if (XdrUtils.uint64LesserThan(itr.next().getKey(), maxSlotIndex))
            {
                itr.remove();
            }
        }
    }

    LocalNode
    getLocalNode()
    {
        return mLocalNode;
    }

    JsonObject dumpInfo(JsonObject ret, int limit)
    {
    	JsonObject slots = new JsonObject();
    	for(Entry<Uint64,Slot> sEntry : mKnownSlots.entrySet()) {
    		if(limit-- <= 0) {
    			break;
    		}
    		sEntry.getValue().dumpInfo(ret);
    	}
    	ret.add("slots", slots);
        return ret;
    }

    JsonObject dumpQuorumInfo(NodeID id, boolean summary,
                        Uint64 index)
    {
    	JsonObject ret = new JsonObject();
    	JsonObject slots = new JsonObject();
        if (index.getUint64() == 0)
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
    	ret.add("slots", slots);
        return ret;
    }

    SecretKey getSecretKey()
    {
        return mLocalNode.getSecretKey();
    }

    boolean isValidator()
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

    List<SCPEnvelope> getLatestMessagesSend(Uint64 slotIndex)
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
    
    void setStateFromEnvelope(Uint64 slotIndex, SCPEnvelope e)
    {
        if (mDriver.verifyEnvelope(e))
        {
            Slot slot = getSlot(slotIndex, true);
            slot.setStateFromEnvelope(e);
        }
    }

    List<SCPEnvelope> getCurrentState(Uint64 slotIndex)
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

    List<SCPEnvelope> getExternalizingState(Uint64 slotIndex)
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
		return "(" + ballot.getCounter().getUint32() + "," + getValueString(ballot.getValue()) + ")";
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
	        + " i: " + XdrUtils.uint64ToString(st.getSlotIndex()));
	    switch (st.getPledges().getDiscriminant())
	    {
	    case SCP_ST_PREPARE:
	    {
	        SCPStatementPrepare p = st.getPledges().getPrepare();
	        oss.append(" | PREPARE"
	            + " | D: " + CryptoUtils.hexAbbrev(qSetHash.getHash())
	            + " | b: " + ballotToStr(p.getBallot())
	            + " | p: " + ballotToStr(p.getPrepared())
	            + " | p': " + ballotToStr(p.getPreparedPrime()) + " | c.n: " + p.getNC().getUint32()
	            + " | h.n: " + p.getNH().getUint32());
	    }
	    break;
	    case SCP_ST_CONFIRM:
	    {
	        SCPStatementConfirm c = st.getPledges().getConfirm();
	        oss.append(" | CONFIRM"
	            + " | D: " + CryptoUtils.hexAbbrev(qSetHash.getHash())
	            + " | b: " + ballotToStr(c.getBallot()) + " | p.n: " + c.getNPrepared().getUint32()
	            + " | c.n: " + c.getNCommit().getUint32() + " | h.n: " + c.getNH().getUint32());
	    }
	    break;
	    case SCP_ST_EXTERNALIZE:
	    {
	    	SCPStatementExternalize ex = st.getPledges().getExternalize();
	        oss.append(" | EXTERNALIZE"
	            + " | c: " + ballotToStr(ex.getCommit()) + " | h.n: " + ex.getNH().getUint32()
	            + " | (lastD): " + CryptoUtils.hexAbbrev(qSetHash.getHash()));
	    }
	    break;
	    case SCP_ST_NOMINATE:
	    {
	    	SCPNomination nom = st.getPledges().getNominate();
	        oss.append(" | NOMINATE"
	            + " | D: " + CryptoUtils.hexAbbrev(qSetHash.getHash()) + " | X: {");
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
