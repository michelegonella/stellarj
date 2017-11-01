package com.consumimurigni.stellarj.herder;

import com.consuminurigni.stellarj.metering.Counter;
import com.consuminurigni.stellarj.metering.Meter;
import com.consuminurigni.stellarj.metering.Metrics;
import com.consuminurigni.stellarj.metering.Timer;

public class SCPMetrics
{
    Meter mValueValid;
    Meter mValueInvalid;
    Meter mNominatingValue;
    Meter mValueExternalize;

    Meter mUpdatedCandidate;
    Meter mStartBallotProtocol;
    Meter mAcceptedBallotPrepared;
    Meter mConfirmedBallotPrepared;
    Meter mAcceptedCommit;

    Meter mBallotExpire;

    Meter mQuorumHeard;

    Meter mLostSync;

    Meter mEnvelopeEmit;
    Meter mEnvelopeReceive;
    Meter mEnvelopeSign;
    Meter mEnvelopeValidSig;
    Meter mEnvelopeInvalidSig;

    // Counters for stuff in parent class (SCP)
    // that we monitor on a best-effort basis from
    // here.
    Counter mKnownSlotsSize;

    // Counters for things reached-through the
    // SCP maps: Slots and Nodes
    Counter mCumulativeStatements;

    // State transition metrics
    Counter mHerderStateCurrent;
    Timer mHerderStateChanges;

    // Pending tx buffer sizes
    Counter mHerderPendingTxs0;
    Counter mHerderPendingTxs1;
    Counter mHerderPendingTxs2;
    Counter mHerderPendingTxs3;

    public SCPMetrics(Metrics metrics) {
    	mValueValid = metrics.newMeter("scp", "value", "valid", "value");
    	mValueInvalid = 
    			metrics.newMeter("scp", "value", "invalid", "value");
    	mNominatingValue = 
    			metrics.newMeter("scp", "value", "nominating", "value");
    	mValueExternalize = 
    			metrics.newMeter("scp", "value", "externalize", "value");
    	mUpdatedCandidate = 
    			metrics.newMeter("scp", "value", "candidate", "value");
    	mStartBallotProtocol = 
    			metrics.newMeter("scp", "ballot", "started", "ballot");
    	mAcceptedBallotPrepared = metrics.newMeter(
    	    "scp", "ballot", "accepted-prepared", "ballot");
    	mConfirmedBallotPrepared = metrics.newMeter(
    	    "scp", "ballot", "confirmed-prepared", "ballot");
    	mAcceptedCommit = metrics.newMeter(
    	    "scp", "ballot", "accepted-commit", "ballot");
    	mBallotExpire = 
    	    metrics.newMeter("scp", "ballot", "expire", "ballot");

    	mQuorumHeard = 
    	    metrics.newMeter("scp", "quorum", "heard", "quorum");

    	mLostSync = metrics.newMeter("scp", "sync", "lost", "sync");

    	mEnvelopeEmit = 
    	    metrics.newMeter("scp", "envelope", "emit", "envelope");
    	mEnvelopeReceive = 
    	    metrics.newMeter("scp", "envelope", "receive", "envelope");
    	mEnvelopeSign = 
    	    metrics.newMeter("scp", "envelope", "sign", "envelope");
    	mEnvelopeValidSig = metrics.newMeter(
    	    "scp", "envelope", "validsig", "envelope");
    	mEnvelopeInvalidSig = metrics.newMeter(
    	    "scp", "envelope", "invalidsig", "envelope");

    	mKnownSlotsSize = 
    	    metrics.newCounter("scp", "memory", "known-slots");
    	mCumulativeStatements = metrics.newCounter(
    	    "scp", "memory", "cumulative-statements");

    	mHerderStateCurrent = 
    	    metrics.newCounter("herder", "state", "current");
    	mHerderStateChanges = 
    	    metrics.newTimer("herder", "state", "changes");

    	mHerderPendingTxs0 = 
    	    metrics.newCounter("herder", "pending-txs", "age0");
    	mHerderPendingTxs1 = 
    	    metrics.newCounter("herder", "pending-txs", "age1");
    	mHerderPendingTxs2 = 
    	    metrics.newCounter("herder", "pending-txs", "age2");
    	mHerderPendingTxs3 = 
    	    metrics.newCounter("herder", "pending-txs", "age3");
    }
}