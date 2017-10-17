package com.consumimurigni.stellarj.scp;

import java.util.Set;
import java.util.function.Consumer;

import com.consumimurigni.stellarj.crypto.CryptoUtils;
import com.consumimurigni.stellarj.crypto.HashingFunction;
import com.consumimurigni.stellarj.crypto.SHA256;
import com.consuminurigni.stellarj.common.Hex;
import com.consuminurigni.stellarj.scp.xdr.SCPBallot;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.scp.xdr.ValueSet;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.Int32;
import com.consuminurigni.stellarj.xdr.NodeID;
import com.consuminurigni.stellarj.xdr.PublicKey;
import com.consuminurigni.stellarj.xdr.Uint256;
import com.consuminurigni.stellarj.xdr.Uint32;
import com.consuminurigni.stellarj.xdr.Uint64;
import com.consuminurigni.stellarj.xdr.Value;

public abstract class SCPDriver {
	public enum ValidationLevel
	    {
	        kInvalidValue,        // value is invalid for sure
	        kFullyValidatedValue, // value is valid for sure
	        kMaybeValidValue      // value may be valid
	    }

	 public SCPDriver() {
		 
	 }
	private final HashingFunction hasher = CryptoUtils.sha256();
	 public HashingFunction getHashingFunction() {
		 return hasher;
	 }
	    // Envelope signature/verification
	    public abstract void signEnvelope(SCPEnvelope envelope);
	    public abstract boolean verifyEnvelope(SCPEnvelope envelope);

	    // Delegates the retrieval of the quorum set designated by `qSetHash` to
	    // the user of SCP.
	    public abstract SCPQuorumSet getQSet(Hash qSetHash);

	    // Users of the SCP library should inherit from SCPDriver and implement the
	    // abstract methods which are called by the SCP implementation to
	    // abstract the transport layer used from the implementation of the SCP
	    // protocol.

	    // Delegates the emission of an SCPEnvelope to the user of SCP. Envelopes
	    // should be flooded to the network.
	    public abstract void emitEnvelope(SCPEnvelope envelope);

	    // methods to hand over the validation and ordering of values and ballots.

	    // `validateValue` is called on each message received before any processing
	    // is done. It should be used to filter out values that are not compatible
	    // with the current state of that node. Unvalidated values can never
	    // externalize.
	    // If the value cannot be validated (node is missing some context) but
	    // passes
	    // the validity checks, kMaybeValidValue can be returned. This will cause
	    // the current slot to be marked as a non validating slot: the local node
	    // will abstain from emiting its position.

	    
	    
	    //abstract 
	    ValidationLevel
	    validateValue(Uint64 slotIndex, Value value)
	    {
	        return ValidationLevel.kMaybeValidValue;
	    }

	    //abstract 
	    Value
	    extractValidValue(Uint64 slotIndex, Value value)
	    {
	        return new Value();//TODO ??
	    }
	    

	    // `computeHashNode` is used by the nomination protocol to
	    // randomize the order of messages between nodes.
	    public abstract Uint64 computeHashNode(Uint64 slotIndex, Value prev,
	                                   boolean isPriority, int roundNumber,
	                                   NodeID nodeID);

	    // `computeValueHash` is used by the nomination protocol to
	    // randomize the relative order between values.
	    public abstract Uint64 computeValueHash(Uint64 slotIndex, Value prev,
	                                    int roundNumber, Value value);

	    // `combineCandidates` computes the composite value based off a list
	    // of candidate values.
	    public abstract Value combineCandidates(Uint64 slotIndex,
	                                    ValueSet candidates);

	    // `setupTimer`: requests to trigger 'cb' after timeout
	    public abstract void setupTimer(Uint64 slotIndex, Slot.timerIDs timerID,
	                            long timeout,
	                            Runnable cb);


	    // Inform about events happening within the consensus algorithm.

	    // `valueExternalized` is called at most once per slot when the slot
	    // externalize its value.
	    public abstract void
	    valueExternalized(Uint64 slotIndex, Value value);
//	    {
//	    }

	    // ``nominatingValue`` is called every time the local instance nominates
	    // a new value.
	    public abstract void
	    nominatingValue(Uint64 slotIndex, Value value);
//	    {
//	    }

	    // the following methods are used for monitoring of the SCP subsystem
	    // most implementation don't really need to do anything with these

	    // `updatedCandidateValue` is called every time a new candidate value
	    // is included in the candidate set, the value passed in is
	    // a composite value
	    public abstract void
	    updatedCandidateValue(Uint64 slotIndex, Value value);
//	    {
//	    }

	    // `startedBallotProtocol` is called when the ballot protocol is started
	    // (ie attempts to prepare a new ballot)
	    public abstract void
	    startedBallotProtocol(Uint64 slotIndex, SCPBallot ballot);
//	    {
//	    }

	    // `acceptedBallotPrepared` every time a ballot is accepted as prepared
	    public abstract void
	    acceptedBallotPrepared(Uint64 slotIndex, SCPBallot ballot);
//	    {
//	    }

	    // `confirmedBallotPrepared` every time a ballot is confirmed prepared
	    public abstract void
	    confirmedBallotPrepared(Uint64 slotIndex, SCPBallot ballot);
//	    {
//	    }

	    // `acceptedCommit` every time a ballot is accepted commit
	    public abstract void acceptedCommit(Uint64 slotIndex, SCPBallot ballot);
//	    {
//	    }

	    // `ballotDidHearFromQuorum` is called when we received messages related to
	    // the current `mBallot` from a set of node that is a transitive quorum for
	    // the local node.
	    public abstract void
	    ballotDidHearFromQuorum(Uint64 slotIndex, SCPBallot ballot);
//	    {
//	    }


	    String getValueString(Value v)
	    {
	    	return Hex.hexAbbrev(hasher.apply(v.getValue()));
//	        Uint256 valueHash = new Uint256(hasher.apply(v.getValue()));
//	        return CryptoUtils.hexAbbrev(valueHash.getUint256());
	    }

	    // `toShortString` converts to the common name of a key if found
	    String toShortString(PublicKey pk)
	    {
	        return pk.toShortString();
	    }

	    // values used to switch hash function between priority and neighborhood checks
	    static final Uint32 hash_N = Uint32.ofPositiveInt(1);
	    static final Uint32 hash_P = Uint32.ofPositiveInt(2);
	    static final Uint32 hash_K = Uint32.ofPositiveInt(3);

	    static Uint64  hashHelper(Uint64 slotIndex, Value prev,
	               Consumer<SHA256> extra)
	    {
	        SHA256 h = SHA256.create();
	        h.add(slotIndex.toOpaque());
	        h.add(prev.getValue());
	        extra.accept(h);
	        Uint256 t = h.finish();
	        Uint64 res = new Uint64();
	        res.setUint64(0L);
	        for (int i = 0; i < 8/* sizeof(res)*/; i++)
	        {
	            //TODO res = (res << 8) | t[i];
	        }
	        return res;
	    }

	    Uint64 computeHashNode(Uint64 slotIndex, Value prev, boolean isPriority,
	                               Int32 roundNumber, NodeID nodeID)
	    {
	    	//TODO 
	    	return null;
//	        return hashHelper(slotIndex, prev, [&](SHA256* h) {
//	            h->add(xdr::xdr_to_opaque(isPriority ? hash_P : hash_N));
//	            h->add(xdr::xdr_to_opaque(roundNumber));
//	            h->add(xdr::xdr_to_opaque(nodeID));
//	        });
	    }

	    Uint64 computeValueHash(Uint64 slotIndex, Value prev,
	                                Int32 roundNumber, Value value)
	    {
	        return hashHelper(slotIndex, prev, (SHA256 h) -> {
	            h.add(hash_K.encode());
	            h.add(roundNumber.encode());
	            h.add(value.getValue());
	        });
	    }

	    static final int MAX_TIMEOUT_SECONDS = (30 * 60);

	    // `computeTimeout` computes a timeout given a round number
	    // it should be sufficiently large such that nodes in a
	    // quorum can exchange 4 messages

	    long
	    computeTimeout(Int32 i) {
	    	return computeTimeout(i.toUint());
	    }
	    long
	    computeTimeout(Uint32 roundNumber)
	    {
	        // straight linear timeout
	        // starting at 1 second and capping at MAX_TIMEOUT_SECONDS

	        int timeoutInSeconds;
	        if (roundNumber.gt(MAX_TIMEOUT_SECONDS))
	        {
	            timeoutInSeconds = MAX_TIMEOUT_SECONDS;
	        }
	        else
	        {
	            timeoutInSeconds = roundNumber.intValue();
	        }
	        return (timeoutInSeconds) * 1000L;
	    }

}
