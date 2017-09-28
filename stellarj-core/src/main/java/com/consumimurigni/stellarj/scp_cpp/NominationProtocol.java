package com.consumimurigni.stellarj.scp_cpp;

import static com.google.common.base.Preconditions.checkNotNull;

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
import org.stellar.sdk.xdr.Int32;
import org.stellar.sdk.xdr.NodeID;
import org.stellar.sdk.xdr.SCPEnvelope;
import org.stellar.sdk.xdr.SCPNomination;
import org.stellar.sdk.xdr.SCPQuorumSet;
import org.stellar.sdk.xdr.SCPStatement;
import org.stellar.sdk.xdr.SCPStatementType;
import org.stellar.sdk.xdr.Uint64;
import org.stellar.sdk.xdr.Value;

import com.consumimurigni.stellarj.common_cpp.Assertions;
import com.consumimurigni.stellarj.common_cpp.Vectors;
import com.consumimurigni.stellarj.scp_cpp.SCPDriver.ValidationLevel;
import com.consumimurigni.stellarj.xdr_cpp.XdrUtils;
import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class NominationProtocol {
	private static final Logger log = LogManager.getLogger();

    final Slot mSlot;

    Int32 mRoundNumber = new Int32();
    LinkedHashSet<Value> mVotes = new LinkedHashSet<>();                           // X
    LinkedHashSet<Value> mAccepted = new LinkedHashSet<>();                        // Y
    LinkedHashSet<Value> mCandidates = new LinkedHashSet<>();                      // Z
    Map<NodeID, SCPEnvelope> mLatestNominations = new LinkedHashMap<>(); // N

    SCPEnvelope
        mLastEnvelope = null; // last envelope emitted by this node

    // nodes from quorum set that have the highest priority this round
    LinkedHashSet<NodeID> mRoundLeaders = new LinkedHashSet<NodeID>();

    // true if 'nominate' was called
    boolean mNominationStarted;

    // the latest (if any) candidate value
    Value mLatestCompositeCandidate = null;

    // the value from the previous slot
    Value mPreviousValue = null;

    public NominationProtocol(Slot slot)
	{
	  mSlot = slot;
	  mRoundNumber.setInt32(0);
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

    boolean isSubsetHelper(Value[] p, Value[] v, AtomicBoolean notEqual)
    {
        boolean res;
        if (p.length <= v.length)
        {
            res = true;//TODO std::includes(v.begin(), v.end(), p.begin(), p.end());
            for(int i = 0; i < p.length; i++) {
            	if(! Arrays.equals(p[i].getValue(), v[i].getValue())) {
            		res = false;
            	}
            }
            if (res)
            {
                notEqual.set(p.length != v.length);
            }
            else
            {
                notEqual.set(true);
            }
        }
        else
        {
            notEqual.set(true);
            res = false;
        }
        return res;
    }

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

        if (isSubsetHelper(oldst.getVotes(), st.getVotes(), g))
        {
            grows = g.get();
            if (isSubsetHelper(oldst.getAccepted(), st.getAccepted(), g))
            {
                grows = grows || g.get();
                res = grows; //  true only if one of the sets grew
            }
        }

        return res;
    }

    boolean isSane(SCPStatement st)
    {
       SCPNomination nom = Preconditions.checkNotNull(st.getPledges().getNominate());
       boolean res = (nom.getVotes().length + nom.getAccepted().length) != 0;

//TODO ?? correct to compare Value this way ? for is sorted ??
//        res = res && std::is_sorted(nom.votes.begin(), nom.votes.end());
//        res = res && std::is_sorted(nom.accepted.begin(), nom.accepted.end());
       
        res = res && XdrUtils.isSorted(nom.getVotes());
        res = res && XdrUtils.isSorted(nom.getAccepted());

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
        SCPNomination nom = checkNotNull(st.getPledges().getNominate());

        nom.setQuorumSetHash(mSlot.getLocalNode().getQuorumSetHash());

        for (Value v : mVotes)
        {
        	nom.setVotes(Vectors.emplace_back(nom.getVotes(), v));
        }
        for (Value a : mAccepted)
        {
        	nom.setAccepted(Vectors.emplace_back(nom.getAccepted(), a));
        }

        SCPEnvelope envelope = mSlot.createEnvelope(st);

        if (mSlot.processEnvelope(envelope, true) == SCP.EnvelopeState.VALID)
        {
            if (mLastEnvelope == null ||
                isNewerStatement(
                	checkNotNull(mLastEnvelope.getStatement().getPledges().getNominate()),
                	checkNotNull(st.getPledges().getNominate())))
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
        	Assertions.abort();
            // there is a bug in the application if it queued up
            // a statement for itself that it considers invalid
            throw new RuntimeException("moved to a bad state (nomination)");
        }
    }
    boolean acceptPredicate(Value v, SCPStatement st)
    {
        for(Value a : st.getPledges().getNominate().getAccepted()) {
        	if(XdrUtils.valueEquals(a, v)) {
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

        LocalNode.forAllNodes(myQSet, (NodeID cur) -> {
            Uint64 w = getNodePriority(cur, myQSet);
            if (XdrUtils.uint64GreaterThan(w, topPriority))
            {
                topPriority.setUint64(w.getUint64());
                mRoundLeaders.clear();
            }//else
            if (XdrUtils.uint64Equals(w, topPriority) && XdrUtils.uint64GreaterThan(w, XdrUtils.UINT64_ZERO))
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
    	
        Assertions.assertTrue(mPreviousValue != null);//TODO empty ??dbgAssert(!mPreviousValue.isEmpty());
        return mSlot.getSCPDriver().computeHashNode(
            mSlot.getSlotIndex(), mPreviousValue, isPriority, mRoundNumber, nodeID);
    }

    Uint64 hashValue(Value value)
    {
        Assertions.assertTrue(mPreviousValue != null);//TODO empty ??dbgAssert(!mPreviousValue.isEmpty());
        return mSlot.getSCPDriver().computeValueHash(
            mSlot.getSlotIndex(), mPreviousValue, mRoundNumber, value);
    }

    Uint64 getNodePriority(NodeID nodeID, SCPQuorumSet qset)
    {
        Uint64 w = LocalNode.getNodeWeight(nodeID, qset);

        if (XdrUtils.uint64LesserThan(hashNode(false, nodeID), w))
        {
            return hashNode(true, nodeID);
        }
        else
        {
            return XdrUtils.UINT64_ZERO;//TODO singe mutable subst with zero() builder
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
            if (! XdrUtils.valueEmpty(valueToNominate))
            {
                if (! XdrUtils.contains(mVotes, valueToNominate))
                {
                    Uint64 curHash = hashValue(valueToNominate);
                    if (XdrUtils.uint64GreaterThanOrEquals(curHash, newHash))
                    {
                        newHash.setUint64(curHash.getUint64());
                        newVote.setValue(valueToNominate.getValue());
                    }
                }
            }
        });
        //TODO possible ?
        Assertions.assertNotNull(newVote.getValue());
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
                        if (XdrUtils.contains(mAccepted, v))
                        { // v is already accepted
                            continue;
                        }
                        if (mSlot.federatedAccept(
                                (SCPStatement st1) -> {
                                    SCPNomination nom1 = st1.getPledges().getNominate();
                                    return XdrUtils.contains(nom.getVotes(), v);
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
                                if (! XdrUtils.valueEmpty(toVote))
                                {
                                    if (mVotes.add(toVote))//TODO here we use add for emplace. ordering problems ??
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
                        if (XdrUtils.contains(mCandidates, a))
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
                    if (mCandidates.isEmpty() && XdrUtils.contains(mRoundLeaders, st.getNodeID().getNodeID()))
                    {
                        Value newVote = getNewValueFromNomination(nom);
                        if (! XdrUtils.valueEmpty(newVote))
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
        applyAll(Assertions.assertNotNull(st.getPledges().getNominate()),
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

        mRoundNumber.setInt32(mRoundNumber.getInt32()+1);//TODO ugly ++
        updateRoundLeaders();

        Value nominatingValue = new Value();

        if (XdrUtils.contains(mRoundLeaders, mSlot.getLocalNode().getNodeID().getNodeID()))
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
                        Assertions.assertNotNull(env.getStatement().getPledges().getNominate()));
                    if (! XdrUtils.valueEmpty(nominatingValue))
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
        nomState.addProperty("roundnumber", mRoundNumber.getInt32());
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
            if (!(XdrUtils.nodeIDEquals(n.getKey(), mSlot.getSCP().getLocalNodeID())) ||
                mSlot.isFullyValidated())
            {
                res.add(n.getValue());
            }
        }
        return res;
    }

    
    
    
}
