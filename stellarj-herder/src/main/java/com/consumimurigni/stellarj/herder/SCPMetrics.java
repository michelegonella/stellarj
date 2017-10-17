package com.consumimurigni.stellarj.herder;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.consumimurigni.stellarj.main.Application;

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

    public SCPMetrics(Application app) {
    	mValueValid = app.newMeter("scp", "value", "valid", "value");
    	mValueInvalid = 
    	    app.newMeter("scp", "value", "invalid", "value");
    	mNominatingValue = 
    	    app.newMeter("scp", "value", "nominating", "value");
    	mValueExternalize = 
    	    app.newMeter("scp", "value", "externalize", "value");
    	mUpdatedCandidate = 
    	    app.newMeter("scp", "value", "candidate", "value");
    	mStartBallotProtocol = 
    	    app.newMeter("scp", "ballot", "started", "ballot");
    	mAcceptedBallotPrepared = app.newMeter(
    	    "scp", "ballot", "accepted-prepared", "ballot");
    	mConfirmedBallotPrepared = app.newMeter(
    	    "scp", "ballot", "confirmed-prepared", "ballot");
    	mAcceptedCommit = app.newMeter(
    	    "scp", "ballot", "accepted-commit", "ballot");
    	mBallotExpire = 
    	    app.newMeter("scp", "ballot", "expire", "ballot");

    	mQuorumHeard = 
    	    app.newMeter("scp", "quorum", "heard", "quorum");

    	mLostSync = app.newMeter("scp", "sync", "lost", "sync");

    	mEnvelopeEmit = 
    	    app.newMeter("scp", "envelope", "emit", "envelope");
    	mEnvelopeReceive = 
    	    app.newMeter("scp", "envelope", "receive", "envelope");
    	mEnvelopeSign = 
    	    app.newMeter("scp", "envelope", "sign", "envelope");
    	mEnvelopeValidSig = app.newMeter(
    	    "scp", "envelope", "validsig", "envelope");
    	mEnvelopeInvalidSig = app.newMeter(
    	    "scp", "envelope", "invalidsig", "envelope");

    	mKnownSlotsSize = 
    	    app.newCounter("scp", "memory", "known-slots");
    	mCumulativeStatements = app.newCounter(
    	    "scp", "memory", "cumulative-statements");

    	mHerderStateCurrent = 
    	    app.newCounter("herder", "state", "current");
    	mHerderStateChanges = 
    	    app.newTimer("herder", "state", "changes");

    	mHerderPendingTxs0 = 
    	    app.newCounter("herder", "pending-txs", "age0");
    	mHerderPendingTxs1 = 
    	    app.newCounter("herder", "pending-txs", "age1");
    	mHerderPendingTxs2 = 
    	    app.newCounter("herder", "pending-txs", "age2");
    	mHerderPendingTxs3 = 
    	    app.newCounter("herder", "pending-txs", "age3");
    }
}