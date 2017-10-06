package com.consumimurigni.stellarj.scp;


import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.consumimurigni.stellarj.scp.SCPDriver.ValidationLevel;
import com.consumimurigni.stellarj.scp.xdr.NodeSet;
import com.consumimurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consumimurigni.stellarj.scp.xdr.SCPNomination;
import com.consumimurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consumimurigni.stellarj.scp.xdr.SCPStatement;
import com.consumimurigni.stellarj.scp.xdr.SCPStatementType;
import com.consumimurigni.stellarj.scp.xdr.ValueSet;
import com.consuminurigni.stellarj.common.Assert;
import com.consuminurigni.stellarj.xdr.Int32;
import com.consuminurigni.stellarj.xdr.NodeID;
import com.consuminurigni.stellarj.xdr.Uint64;
import com.consuminurigni.stellarj.xdr.Value;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class NominationProtocol {
	private static final Logger log = LogManager.getLogger();

    final Slot mSlot;

    Int32 mRoundNumber;
    ValueSet mVotes = new ValueSet();                           // X
    ValueSet mAccepted = new ValueSet();                        // Y
    ValueSet mCandidates = new ValueSet();                      // Z
    Map<NodeID, SCPEnvelope> mLatestNominations = new LinkedHashMap<>(); // N

    SCPEnvelope
        mLastEnvelope = null; // last envelope emitted by this node

    // nodes from quorum set that have the highest priority this round
    NodeSet mRoundLeaders = new NodeSet();

    // true if 'nominate' was called
    boolean mNominationStarted;

    // the latest (if any) candidate value
    Value mLatestCompositeCandidate = null;

    // the value from the previous slot
    Value mPreviousValue = null;

    public NominationProtocol(Slot slot)
	{
	  mSlot = slot;
	  mRoundNumber = Int32.of(0);
	  mNominationStarted = false;
	}

    Value getLatestCompositeCandidate()
    {
        return mLatestCompositeCandidate;
    }

    SCPEnvelope getLastMessageSend()
    {
        return mLastEnvelope;
    }

    boolean isNewerStatement(NodeID nodeID,
                                         SCPNomination st)
    {
        SCPEnvelope oldp = mLatestNominations.get(nodeID);
        boolean res = false;

        if (oldp == null)
        {
            res = true;
        }
        else
        {
            res = isNewerStatement(oldp.getStatement().getPledges().getNominate(), st);
        }
        return res;
    }

//    boolean isSubsetHelper(Value[] p, Value[] v, AtomicBoolean notEqual)
//    {
//        boolean res;
//        if (p.length <= v.length)
//        {
//            res = true;//TODO std::includes(v.begin(), v.end(), p.begin(), p.end());
//            for(int i = 0; i < p.length; i++) {
//            	if(! Arrays.equals(p[i].getValue(), v[i].getValue())) {
//            		res = false;
//            	}
//            }
//            if (res)
//            {
//                notEqual.set(p.length != v.length);
//            }
//            else
//            {
//                notEqual.set(true);
//            }
//        }
//        else
//        {
//            notEqual.set(true);
//            res = false;
//        }
//        return res;
//    }

    SCPDriver.ValidationLevel validateValue(Value v)
    {
        return mSlot.getSCPDriver().validateValue(mSlot.getSlotIndex(), v);
    }

    Value extractValidValue(Value value)
    {
        return mSlot.getSCPDriver().extractValidValue(mSlot.getSlotIndex(), value);
    }

    boolean isNewerStatement(SCPNomination oldst,
                                         SCPNomination st)
    {
        boolean res = false;
        boolean grows;
        AtomicBoolean g = new AtomicBoolean(false);

        if (oldst.getVotes().isSubsetOf(st.getVotes(), g))
        {
            grows = g.get();
            if (oldst.getAccepted().isSubsetOf(st.getAccepted(), g))
            {
                grows = grows || g.get();
                res = grows; //  true only if one of the sets grew
            }
        }

        return res;
    }

    boolean isSane(SCPStatement st)
    {
       SCPNomination nom = Assert.assertNotNull(st.getPledges().getNominate());
       boolean res = nom.getNumVotes() + nom.getNumAccepted() != 0;

//TODO ?? correct to compare Value this way ? for is sorted ??
//        res = res && std::is_sorted(nom.votes.begin(), nom.votes.end());
//        res = res && std::is_sorted(nom.accepted.begin(), nom.accepted.end());
       
        res = res && nom.getVotes().isSorted();
        res = res && nom.getAccepted().isSorted();

        return res;
    }

    // only called after a call to isNewerStatement so safe to replace the
    // mLatestNomination
    void recordEnvelope(SCPEnvelope env)
    {
        SCPStatement st = env.getStatement();
        mLatestNominations.put(st.getNodeID(), env);
        mSlot.recordStatement(env.getStatement());
    }

    void emitNomination()
    {
        SCPStatement st = new SCPStatement();
        st.setNodeID(mSlot.getLocalNode().getNodeID());
        st.getPledges().setDiscriminant(SCPStatementType.SCP_ST_NOMINATE);
        SCPNomination nom = Assert.assertNotNull(st.getPledges().getNominate());

        nom.setQuorumSetHash(mSlot.getLocalNode().getQuorumSetHash());

        for (Value v : mVotes)
        {
        	nom.addVote(v);
        }
        for (Value a : mAccepted)
        {
        	nom.addAccepted(a);
        }

        SCPEnvelope envelope = mSlot.createEnvelope(st);

        if (mSlot.processEnvelope(envelope, true) == SCP.EnvelopeState.VALID)
        {
            if (mLastEnvelope == null ||
                isNewerStatement(
                		Assert.assertNotNull(mLastEnvelope.getStatement().getPledges().getNominate()),
                		Assert.assertNotNull(st.getPledges().getNominate())))
            {
                mLastEnvelope = envelope;
                if (mSlot.isFullyValidated())
                {
                    mSlot.getSCPDriver().emitEnvelope(envelope);
                }
            }
        }
        else
        {
        	Assert.abort();
            // there is a bug in the application if it queued up
            // a statement for itself that it considers invalid
            throw new RuntimeException("moved to a bad state (nomination)");
        }
    }
    boolean acceptPredicate(Value v, SCPStatement st)
    {
        for(Value a : st.getPledges().getNominate().getAccepted()) {
        	if(a.equals(v)) {
        		return true;
        	}
        }
        return false;
    }

    static void applyAll(SCPNomination nom, Consumer<Value> processor)
    {
        for (Value v : nom.getVotes())
        {
            processor.accept(v);
        }
        for (Value a : nom.getAccepted())
        {
            processor.accept(a);
        }
    }

    void updateRoundLeaders()
    {
        mRoundLeaders.clear();
        Uint64 topPriority = new Uint64();
        topPriority.setUint64(0L);
        SCPQuorumSet myQSet = mSlot.getLocalNode().getQuorumSet();

        myQSet.forAllNodes((NodeID cur) -> {
            Uint64 w = getNodePriority(cur, myQSet);
            if (w.gt(topPriority))
            {
                topPriority.setUint64(w.getUint64());
                mRoundLeaders.clear();
            }//else
            if (w.eq(topPriority) && w.gt(0))
            {
                mRoundLeaders.add(cur);
            }
        });
        log.debug("SCP updateRoundLeaders: {}", mRoundLeaders.size());
        if (log.isDebugEnabled()) {
            for (NodeID rl : mRoundLeaders)
            {
                log.debug("SCP leader {}", mSlot.getSCPDriver().toShortString(rl.getNodeID()));
            }
        }
    }

    Uint64 hashNode(boolean isPriority, NodeID nodeID)
    {
    	
        Assert.assertTrue(mPreviousValue != null);//TODO empty ??dbgAssert(!mPreviousValue.isEmpty());
        return mSlot.getSCPDriver().computeHashNode(
            mSlot.getSlotIndex(), mPreviousValue, isPriority, mRoundNumber, nodeID);
    }

    Uint64 hashValue(Value value)
    {
    	Assert.assertTrue(mPreviousValue != null);//TODO empty ??dbgAssert(!mPreviousValue.isEmpty());
        return mSlot.getSCPDriver().computeValueHash(
            mSlot.getSlotIndex(), mPreviousValue, mRoundNumber, value);
    }

    Uint64 getNodePriority(NodeID nodeID, SCPQuorumSet qset)
    {
        Uint64 w = qset.getNodeWeight(nodeID);

        if (hashNode(false, nodeID).lt(w))
        {
            return hashNode(true, nodeID);
        }
        else
        {
            return Uint64.ZERO;//TODO singe mutable subst with zero() builder
        }
    }

    Value getNewValueFromNomination(SCPNomination nom)
    {
        // pick the highest value we don't have from the leader
        // sorted using hashValue.
        Value newVote = new Value();
        Uint64 newHash = new Uint64();
        newHash.setUint64(0L);

        applyAll(nom, (Value value) -> {
            Value valueToNominate;
            ValidationLevel vl = validateValue(value);
            if (vl == SCPDriver.ValidationLevel.kFullyValidatedValue)
            {
                valueToNominate = value;
            }
            else
            {
                valueToNominate = extractValidValue(value);
            }
            if (! valueToNominate.isEmpty())
            {
                if (! mVotes.contains(valueToNominate))
                {
                    Uint64 curHash = hashValue(valueToNominate);
                    if (curHash.gteq(newHash))
                    {
                        newHash.setUint64(curHash.getUint64());
                        newVote.setValue(valueToNominate.getValue());
                    }
                }
            }
        });
        //TODO possible ?
        Assert.assertNotNull(newVote.getValue());
        return newVote;
    }

    SCP.EnvelopeState processEnvelope(SCPEnvelope envelope)
    {
        SCPStatement st = envelope.getStatement();
        SCPNomination nom = st.getPledges().getNominate();

        SCP.EnvelopeState res = SCP.EnvelopeState.INVALID;

        if (isNewerStatement(st.getNodeID(), nom))
        {
            if (isSane(st))
            {
                recordEnvelope(envelope);
                res = SCP.EnvelopeState.VALID;

                if (mNominationStarted)
                {
                    boolean modified =
                        false; // tracks if we should emit a new nomination message
                    boolean newCandidates = false;

                    // attempts to promote some of the votes to accepted
                    for (Value v : nom.getVotes())
                    {
                        if (mAccepted.contains(v))
                        { // v is already accepted
                            continue;
                        }
                        if (mSlot.federatedAccept(
                                (SCPStatement st1) -> {
                                    SCPNomination nom1 = st1.getPledges().getNominate();
                                    return nom1.getVotes().contains(v);
                                },
                                (SCPStatement st2) -> {
                                    return acceptPredicate(v, st2);
                                },
                                mLatestNominations))
                        {
                            ValidationLevel vl = validateValue(v);
                            if (vl == SCPDriver.ValidationLevel.kFullyValidatedValue)
                            {
                                mAccepted.add(v);
                                mVotes.add(v);
                                modified = true;
                            }
                            else
                            {
                                // the value made it pretty far:
                                // see if we can vote for a variation that
                                // we consider valid
                                Value toVote = extractValidValue(v);
                                if (! toVote.isEmpty())
                                {
                                    if (mVotes.add(toVote))
                                    {
                                        modified = true;
                                    }
                                }
                            }
                        }
                    }
                    // attempts to promote accepted values to candidates
                    for (Value a : mAccepted)
                    {
                        if (mCandidates.contains(a))
                        {
                            continue;
                        }
                        if (mSlot.federatedRatify((SCPStatement stx) -> acceptPredicate(a, stx),
                                mLatestNominations))
                        {
                            mCandidates.add(a);
                            newCandidates = true;
                        }
                    }

                    // only take round leader votes if we're still looking for
                    // candidates
                    if (mCandidates.isEmpty() && mRoundLeaders.contains(st.getNodeID()))
                    {
                        Value newVote = getNewValueFromNomination(nom);
                        if (! newVote.isEmpty())
                        {
                            mVotes.add(newVote);
                            modified = true;
                        }
                    }

                    if (modified)
                    {
                        emitNomination();
                    }

                    if (newCandidates)
                    {
                        mLatestCompositeCandidate =
                            mSlot.getSCPDriver().combineCandidates(
                                mSlot.getSlotIndex(), mCandidates);

                        mSlot.getSCPDriver().updatedCandidateValue(
                            mSlot.getSlotIndex(), mLatestCompositeCandidate);

                        mSlot.bumpState(mLatestCompositeCandidate, false);
                    }
                }
            }
            else
            {
                log.debug("SCP NominationProtocol: message didn't pass sanity check");
            }
        }
        return res;
    }

    static List<Value> getStatementValues(SCPStatement st)
    {
    	List<Value> res = new LinkedList<>();
        applyAll(Assert.assertNotNull(st.getPledges().getNominate()),
                 (Value v) -> { res.add(v); });
        return res;
    }

 // attempts to nominate a value for consensus
    boolean nominate(Value value, Value previousValue,
                                 boolean timedout)
    {
            log.debug("SCP NominationProtocol::nominate {}",
                                mSlot.getSCP().getValueString(value));

        boolean updated = false;

        if (timedout && !mNominationStarted)
        {
        	log.debug("SCP NominationProtocol::nominate (TIMED OUT)");
            return false;
        }

        mNominationStarted = true;

        mPreviousValue = previousValue;

        mRoundNumber = mRoundNumber.plus(1);
        updateRoundLeaders();

        Value nominatingValue = new Value();

        if (mRoundLeaders.contains(mSlot.getLocalNode().getNodeID()))
        {
            if (mVotes.add(value))
            {
                updated = true;
            }
            nominatingValue.setValue(value.getValue());
        }
        else
        {
            for (NodeID leader : mRoundLeaders)
            {
                SCPEnvelope env = mLatestNominations.get(leader);
                if (env != null)
                {
                    nominatingValue = getNewValueFromNomination(
                        Assert.assertNotNull(env.getStatement().getPledges().getNominate()));
                    if (! nominatingValue.isEmpty())
                    {
                        mVotes.add(nominatingValue);
                        updated = true;
                    }
                }
            }
        }

        long timeout =
            mSlot.getSCPDriver().computeTimeout(mRoundNumber);

        mSlot.getSCPDriver().nominatingValue(mSlot.getSlotIndex(), nominatingValue);

        Slot slot = mSlot;
        mSlot.getSCPDriver().setupTimer(
            mSlot.getSlotIndex(), Slot.timerIDs.NOMINATION_TIMER, timeout,
            () -> {
                slot.nominate(value, previousValue, true);
            });

        if (updated)
        {
            emitNomination();
        }
        else
        {
            log.debug("SCP NominationProtocol::nominate (SKIPPED)");
        }

        return updated;
    }

    void stopNomination()
    {
        mNominationStarted = false;
    }

    JsonObject dumpInfo()
    {
    	JsonObject ret = new JsonObject();
    	JsonObject nomState = new JsonObject();
    	ret.add("nomination", nomState);
        nomState.addProperty("roundnumber", mRoundNumber.intValue());
        nomState.addProperty("started", mNominationStarted);

        JsonArray X = new JsonArray();
        for (Value v : mVotes)
        {
            X.add(mSlot.getSCP().getValueString(v));
        }
        nomState.add("X", X);
        JsonArray Y = new JsonArray();
        for (Value v : mAccepted)
        {
            Y.add(mSlot.getSCP().getValueString(v));
        }
        nomState.add("Y", Y);


        JsonArray Z = new JsonArray();
        for (Value v : mCandidates)
        {
            Z.add(mSlot.getSCP().getValueString(v));
        }
        nomState.add("Z", Z);
        return ret;
    }

    void setStateFromEnvelope(SCPEnvelope e)
    {
        if (mNominationStarted)
        {
            throw new RuntimeException(
                "Cannot set state after nomination is started");
        }
        recordEnvelope(e);
        SCPNomination nom = e.getStatement().getPledges().getNominate();
        for (Value a : nom.getAccepted())
        {
            mAccepted.add(a);
        }
        for (Value v : nom.getVotes())
        {
            mVotes.add(v);
        }

        mLastEnvelope = e;
    }

    List<SCPEnvelope> getCurrentState()
    {
    	LinkedList<SCPEnvelope> res = new LinkedList<>();
        //res.reserve(mLatestNominations.size());
        for (Entry<NodeID, SCPEnvelope> n : mLatestNominations.entrySet())
        {
            // only return messages for self if the slot is fully validated
            if (! n.getKey().equals(mSlot.getSCP().getLocalNodeID()) ||
                mSlot.isFullyValidated())
            {
                res.add(n.getValue());
            }
        }
        return res;
    }

    
    
    
}
