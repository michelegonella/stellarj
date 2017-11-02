package com.consumimurigni.stellarj.scp;

import com.consumimurigni.stellarj.role.IPeer;
import com.consumimurigni.stellarj.role.ITxSetFrame;
import com.consumimurigni.stellarj.transactions.TransactionFrame;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.MessageType;
import com.consuminurigni.stellarj.xdr.Uint256;
import com.consuminurigni.stellarj.xdr.Uint32;

public abstract class Herder extends SCPDriver {
	public enum EnvelopeStatus {
		// for some reason this envelope was discarded - either is was invalid,
		// used unsane qset or was coming from node that is not in quorum
		ENVELOPE_STATUS_DISCARDED,
		// envelope data is currently being fetched
		ENVELOPE_STATUS_FETCHING,
		// current call to recvSCPEnvelope() was the first when the envelope
		// was fully fetched so it is ready for processing
		ENVELOPE_STATUS_READY,
		// envelope was already processed
		ENVELOPE_STATUS_PROCESSED,
	};

	public enum State {
		HERDER_SYNCING_STATE, HERDER_TRACKING_STATE
	};

	public enum TransactionSubmitStatus {
		TX_STATUS_PENDING, TX_STATUS_DUPLICATE, TX_STATUS_ERROR, TX_STATUS_COUNT
	};

	public abstract Uint32 getCurrentLedgerSeq();

	public abstract Herder.EnvelopeStatus recvSCPEnvelope(SCPEnvelope envelope);

	public abstract SCP getSCP();

	public abstract void peerDoesntHave(MessageType type, Uint256 reqHash, IPeer peer);
	
	public abstract ITxSetFrame getTxSet(Hash hash);

	public abstract boolean recvTxSet(Hash contentsHash, ITxSetFrame frame);

	public abstract TransactionSubmitStatus recvTransaction(TransactionFrame tx);

	public abstract boolean recvSCPQuorumSet(Hash hash, SCPQuorumSet qset);
	
	public abstract void sendSCPStateToPeer(Uint32 ledgerSeq, IPeer ipeer);
}
