package com.consumimurigni.stellarj.scp;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.consumimurigni.stellarj.scp.SCP.EnvelopeState;
import com.consuminurigni.stellarj.common.Assert;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.scp.xdr.SCPStatement;
import com.consuminurigni.stellarj.scp.xdr.SCPStatementType;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.NodeID;
import com.consuminurigni.stellarj.xdr.Uint32;
import com.consuminurigni.stellarj.xdr.Uint64;
import com.consuminurigni.stellarj.xdr.Value;

public class Slot {
	public enum timerIDs
    {
        NOMINATION_TIMER,//TODO = 0,
        BALLOT_PROTOCOL_TIMER// = 1
    };
	private static final Logger log = LogManager.getLogger();

    Uint64 mSlotIndex = new Uint64(); // the index this slot is tracking
    SCP mSCP;

    BallotProtocol mBallotProtocol;
    NominationProtocol mNominationProtocol;

    // keeps track of all statements seen so far for this slot.
    // it is used for debugging purpose
    // second: if the slot was fully validated at the time
    LinkedHashMap<SCPStatement, Boolean> mStatementsHistory = new LinkedHashMap<>();

    // true if the Slot was fully validated
    boolean mFullyValidated;

	public Slot(Uint64 slotIndex, SCP scp) {
    this.mSlotIndex.setUint64(slotIndex.getUint64());
    this.mSCP = scp;
    this.mBallotProtocol = new BallotProtocol(this);
    this.mNominationProtocol = new NominationProtocol(this);
    this.mFullyValidated = (scp.getLocalNode().isValidator());
}

Value getLatestCompositeCandidate()
{
    return mNominationProtocol.getLatestCompositeCandidate();
}

List<SCPEnvelope> getLatestMessagesSend() 
{
	List<SCPEnvelope> res = new LinkedList<>();
    if (mFullyValidated)
    {
        SCPEnvelope e = mNominationProtocol.getLastMessageSend();
        if (e != null)
        {
            res.add(e);
        }
        e = mBallotProtocol.getLastMessageSend();
        if (e != null)
        {
            res.add(e);
        }
    }
    return res;
}

void setStateFromEnvelope(SCPEnvelope e)
{
    if (e.getStatement().getNodeID().eq(getSCP().getLocalNodeID()) &&
    		e.getStatement().getSlotIndex().eq(mSlotIndex))
    {
        if (e.getStatement().getPledges().getDiscriminant() == SCPStatementType.SCP_ST_NOMINATE)
        {
            mNominationProtocol.setStateFromEnvelope(e);
        }
        else
        {
            mBallotProtocol.setStateFromEnvelope(e);
        }
    }
    else
    {
        log.debug("SCP Slot::setStateFromEnvelope invalid envelope i: {} {}",getSlotIndex()//TODO
                               , mSCP.envToStr(e));
    }
}

List<SCPEnvelope> getCurrentState() 
{
    List<SCPEnvelope> res = mNominationProtocol.getCurrentState();
    List<SCPEnvelope> r2 = mBallotProtocol.getCurrentState();
    res.addAll(r2);
    return res;
}

List<SCPEnvelope> getExternalizingState()
{
    return mBallotProtocol.getExternalizingState();
}

void recordStatement(SCPStatement st)
{
    mStatementsHistory.put(st, mFullyValidated);
}

EnvelopeState processEnvelope(SCPEnvelope envelope, boolean self)
{
    Assert.assertTrue(envelope.getStatement().getSlotIndex().eq(mSlotIndex));

        log.debug("SCP Slot::processEnvelope i: {} {}",getSlotIndex()/*TODO*/, mSCP.envToStr(envelope));

    EnvelopeState res;

    try
    {

        if (envelope.getStatement().getPledges().getDiscriminant() ==
            SCPStatementType.SCP_ST_NOMINATE)
        {
            res = mNominationProtocol.processEnvelope(envelope);
        }
        else
        {
            res = mBallotProtocol.processEnvelope(envelope, self);
        }
    }
    catch (Exception e)
    {
        Map<String, Object> info = new LinkedHashMap<>();
        dumpInfo(info);

        log.error("SCP Exception in processEnvelope state: {} processing envelope: {}", info.toString()
                           ,mSCP.envToStr(envelope));

        throw e;
    }
    return res;
}

boolean abandonBallot()
{
	
    return mBallotProtocol.abandonBallot(Uint32.ZERO);
}

boolean bumpState(Value value, boolean force)
{

    return mBallotProtocol.bumpState(value, force);
}

boolean nominate(Value value, Value previousValue, boolean timedout)
{
    return mNominationProtocol.nominate(value, previousValue, timedout);
}

void stopNomination()
{
    mNominationProtocol.stopNomination();
}

boolean isFullyValidated()
{
    return mFullyValidated;
}

void setFullyValidated(boolean fullyValidated)
{
    mFullyValidated = fullyValidated;
}

SCP.TriBool isNodeInQuorum(NodeID node)
{
    // build the mapping between nodes and envelopes
	LinkedHashMap<NodeID, List<SCPStatement>> m = new LinkedHashMap<>();
    // this may be reduced to the pair (at most) of the latest
    // statements for each protocol
    for (SCPStatement e : mStatementsHistory.keySet())
    {
    	List<SCPStatement> l = m.get(e.getNodeID());
    	if(l == null) {
    		l = new LinkedList<>();
    		m.put(e.getNodeID(), l);
    	}
        l.add(e);
    }
    return mSCP.getLocalNode().isNodeInQuorum(
        node,
        (SCPStatement st) -> {
            // uses the companion set here as we want to consider
            // nodes that were used up to EXTERNALIZE
            Hash h = getCompanionQuorumSetHashFromStatement(st);
            return getSCPDriver().getQSet(h);
        },
        m);
}

SCPEnvelope createEnvelope(SCPStatement statement)
{
    SCPEnvelope envelope = new SCPEnvelope();

    envelope.setStatement(statement);
    SCPStatement mySt = envelope.getStatement();
    mySt.setNodeID(getSCP().getLocalNodeID());
    mySt.setSlotIndex(getSlotIndex());

    mSCP.getDriver().signEnvelope(envelope);

    return envelope;
}

public static Hash getCompanionQuorumSetHashFromStatement(SCPStatement st)
{
    Hash h;
    switch (st.getPledges().getDiscriminant())
    {
    case SCP_ST_PREPARE:
        h = st.getPledges().getPrepare().getQuorumSetHash();
        break;
    case SCP_ST_CONFIRM:
        h = st.getPledges().getConfirm().getQuorumSetHash();
        break;
    case SCP_ST_EXTERNALIZE:
        h = st.getPledges().getExternalize().getCommitQuorumSetHash();
        break;
    case SCP_ST_NOMINATE:
        h = st.getPledges().getNominate().getQuorumSetHash();
        break;
    default:
    	h = null;
        Assert.abort();
    }
    return h;
}

public static List<Value> getStatementValues(SCPStatement st)
{
    List<Value> res = new LinkedList<>();
    if (st.getPledges().getDiscriminant() == SCPStatementType.SCP_ST_NOMINATE)
    {
        res = NominationProtocol.getStatementValues(st);
    }
    else
    {
        res.add(BallotProtocol.getWorkingBallot(st).getValue());
    }
    return res;
}

SCPQuorumSet getQuorumSetFromStatement(SCPStatement st)
{
    SCPQuorumSet res;
    SCPStatementType t = st.getPledges().getDiscriminant();

    if (t == SCPStatementType.SCP_ST_EXTERNALIZE)
    {
        res = SCPQuorumSet.buildSingletonQSet(st.getNodeID());
    }
    else
    {
        Hash h;
        if (t == SCPStatementType.SCP_ST_PREPARE)
        {
            h = st.getPledges().getPrepare().getQuorumSetHash();
        }
        else if (t == SCPStatementType.SCP_ST_CONFIRM)
        {
            h = st.getPledges().getConfirm().getQuorumSetHash();
        }
        else if (t == SCPStatementType.SCP_ST_NOMINATE)
        {
            h = st.getPledges().getNominate().getQuorumSetHash();
        }
        else
        {
        	h=null;;//javac quiet
            Assert.abort();
        }
        res = getSCPDriver().getQSet(h);
    }
    return res;
}

void dumpInfo(Map<String, Object> ret)
{
//    auto& slots = ret["slots"];
//
//    Json::Value& slotValue = slots[std::to_string(mSlotIndex)];
//
//    std::map<Hash, SCPQuorumSetPtr> qSetsUsed;
//
//    int count = 0;
//    for (auto const& item : mStatementsHistory)
//    {
//        Json::Value& v = slotValue["statements"][count++];
//        v.append(mSCP.envToStr(item.first));
//        v.append(item.second);
//
//        Hash const& qSetHash =
//            getCompanionQuorumSetHashFromStatement(item.first);
//        auto qSet = getSCPDriver().getQSet(qSetHash);
//        if (qSet)
//        {
//            qSetsUsed.insert(std::make_pair(qSetHash, qSet));
//        }
//    }
//
//    auto& qSets = slotValue["quorum_sets"];
//    for (auto const& q : qSetsUsed)
//    {
//        auto& qs = qSets[hexAbbrev(q.first)];
//        getLocalNode()->toJson(*q.second, qs);
//    }
//
//    slotValue["validated"] = mFullyValidated;
//    mNominationProtocol.dumpInfo(slotValue);
//    mBallotProtocol.dumpInfo(slotValue);
//TODO
}

void dumpQuorumInfo(Map<String,Object> ret, NodeID id, boolean summary)
{
	//TODO
//    std::string i = std::to_string(static_cast<uint32>(mSlotIndex));
//    mBallotProtocol.dumpQuorumInfo(ret[i], id, summary);
}

boolean federatedAccept(Predicate<SCPStatement> voted, Predicate<SCPStatement> accepted,
                      Map<NodeID, SCPEnvelope> envs)
{
    // Checks if the nodes that claimed to accept the statement form a
    // v-blocking set
    if (getLocalNode().getQuorumSet().isVBlocking(envs, accepted))
    {
        return true;
    }

    // Checks if the set of nodes that accepted or voted for it form a quorum

    Predicate<SCPStatement> ratifyFilter = (SCPStatement st) -> {
        boolean res = accepted.test(st) || voted.test(st);
        return res;
    };

    if (getLocalNode().getQuorumSet().isQuorum(envs, (SCPStatement st) -> {
            	return getQuorumSetFromStatement(st);
            },
            ratifyFilter))
    {
        return true;
    }

    return false;
}

boolean federatedRatify(Predicate<SCPStatement> voted,
                      Map<NodeID, SCPEnvelope> envs)
{
    return getLocalNode().getQuorumSet().isQuorum(
        envs,(SCPStatement st) -> {
        	return getQuorumSetFromStatement(st);
        }, voted);
}

LocalNode getLocalNode()
{
    return mSCP.getLocalNode();
}

List<SCPEnvelope> getEntireCurrentState()
{
    boolean old = mFullyValidated;
    // fake fully validated to force returning all envelopes
    mFullyValidated = true;
    List<SCPEnvelope> r = getCurrentState();
    mFullyValidated = old;
    return r;
}
Uint64 getSlotIndex()
{
    return mSlotIndex;
}

SCP getSCP()
{
    return mSCP;
}

SCPDriver getSCPDriver()
{
    return mSCP.getDriver();
}

BallotProtocol
getBallotProtocol()
{
    return mBallotProtocol;
}
int
getStatementCount() 
{
    return mStatementsHistory.size();
}
}
