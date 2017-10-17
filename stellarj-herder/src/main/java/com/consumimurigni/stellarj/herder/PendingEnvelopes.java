package com.consumimurigni.stellarj.herder;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.consumimurigni.stellarj.herder.Herder.EnvelopeStatus;
import com.consumimurigni.stellarj.ledger.TxSetFrame;
import com.consumimurigni.stellarj.main.Application;
import com.consuminurigni.stellarj.overlay.Peer;
import com.consuminurigni.stellarj.overlay.xdr.MessageType;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.Uint256;
import com.consuminurigni.stellarj.xdr.Uint32;
import com.consuminurigni.stellarj.xdr.Uint64;

public class PendingEnvelopes {

	public PendingEnvelopes(Application app, Herder /*TODO*/ herder) {
		// TODO Auto-generated constructor stub
	}

	public void addSCPQuorumSet(Hash hash, Uint64 lastSeenSlotIndex, SCPQuorumSet quorumSet) {
		// TODO Auto-generated method stub
		
	}

	public @Nullable TxSetFrame getTxSet(Hash txSetHash) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean recvTxSet(Hash contentsHash, TxSetFrame bestTxSet) {
		// TODO Auto-generated method stub
		return false;
	}

	public EnvelopeStatus recvSCPEnvelope(SCPEnvelope envelope) {
		// TODO Auto-generated method stub
		return null;
	}

	public void eraseBelow(Uint32 minus) {
		// TODO Auto-generated method stub
		
	}

	public List<Uint64> readySlots() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean pop(Uint64 slotIndex, SCPEnvelope env) {
		// TODO Auto-generated method stub
		return false;
	}

	public @Nullable SCPEnvelope pop(Uint64 slotIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	public void slotClosed(Uint32 lastConsensusLedgerIndex) {
		// TODO Auto-generated method stub
		
	}

	public boolean recvSCPQuorumSet(Hash hash, SCPQuorumSet qset) {
		// TODO Auto-generated method stub
		return false;
	}

	public void peerDoesntHave(MessageType type, Uint256 itemID, Peer peer) {
		// TODO Auto-generated method stub
		
	}

	public SCPQuorumSet getQSet(Hash qSetHash) {
		// TODO Auto-generated method stub
		return null;
	}

	public void addTxSet(Hash txSetHash, Uint32 slotIndex, TxSetFrame proposedSet) {
		// TODO Auto-generated method stub
		
	}

	public void dumpInfo(Map<String, Object> ret, int limit) {
		// TODO Auto-generated method stub
		
	}

}
