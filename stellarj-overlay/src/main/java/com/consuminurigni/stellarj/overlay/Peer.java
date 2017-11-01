package com.consuminurigni.stellarj.overlay;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.consumimurigni.stellarj.crypto.CryptoUtils;
import com.consumimurigni.stellarj.crypto.SHA256;
import com.consumimurigni.stellarj.scp.Herder;
import com.consuminurigni.stellarj.common.AbstractPeer;
import com.consuminurigni.stellarj.common.BufUtils;
import com.consuminurigni.stellarj.common.Config;
import com.consuminurigni.stellarj.common.Database;
import com.consuminurigni.stellarj.common.VirtualClock;
import com.consuminurigni.stellarj.common.VirtualTimer;
import com.consuminurigni.stellarj.common.VirtualTimerErrorCode;
import com.consuminurigni.stellarj.metering.Meter;
import com.consuminurigni.stellarj.metering.Metrics;
import com.consuminurigni.stellarj.metering.Timer;
import com.consuminurigni.stellarj.overlay.LoadManager.PeerContext;
import com.consuminurigni.stellarj.overlay.xdr.AuthCert;
import com.consuminurigni.stellarj.overlay.xdr.AuthenticatedMessage;
import com.consuminurigni.stellarj.overlay.xdr.AuthenticatedMessage.AuthenticatedMessageV0;
import com.consuminurigni.stellarj.overlay.xdr.DontHave;
import com.consuminurigni.stellarj.overlay.xdr.ErrorCode;
import com.consuminurigni.stellarj.overlay.xdr.Hello;
import com.consuminurigni.stellarj.overlay.xdr.PeerAddress;
import com.consuminurigni.stellarj.overlay.xdr.StellarMessage;
import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.HmacSha256Key;
import com.consuminurigni.stellarj.xdr.MessageType;
import com.consuminurigni.stellarj.xdr.NodeID;
import com.consuminurigni.stellarj.xdr.Uint256;
import com.consuminurigni.stellarj.xdr.Uint32;
import com.consuminurigni.stellarj.xdr.Uint64;

//TODO implement eq and hashcoddde

//LATER: need to add some way of docking peers that are misbehaving by sending
//you bad data

public abstract class Peer extends AbstractPeer {
	private static final Logger log = LogManager.getLogger();

	enum PeerState
    {
        CONNECTING,// = 0,
        CONNECTED,// = 1,
        GOT_HELLO,// = 2,
        GOT_AUTH,// = 3,
        CLOSING// = 4
    };

    enum PeerRole
    {
        REMOTE_CALLED_US,
        WE_CALLED_REMOTE
    };
    private final PeerRole mRole;
    private final PeerState mState;
    private final NodeID mPeerID;
    private final Uint256 mSendNonce;
    private final Uint256 mRecvNonce;

    private final HmacSha256Key mSendMacKey;
    private final HmacSha256Key mRecvMacKey;
    private Uint64 mSendMacSeq = Uint64.ZERO;
    private Uint64 mRecvMacSeq = Uint64.ZERO;

    private final String mRemoteVersion;
    private final Uint32 mRemoteOverlayMinVersion;
    private final Uint32 mRemoteOverlayVersion;
    private final int mRemoteListeningPort;

    private final VirtualTimer mIdleTimer;
    private Instant mLastRead;
    private final Instant mLastWrite;

    private final Meter mMessageRead;
    private final Meter mMessageWrite;
    private final Meter mByteRead;
    private final Meter mByteWrite;
    private final Meter mErrorRead;
    private final Meter mErrorWrite;
    private final Meter mTimeoutIdle;

    private final Timer mRecvErrorTimer;
    private final Timer mRecvHelloTimer;
    private final Timer mRecvAuthTimer;
    private final Timer mRecvDontHaveTimer;
    private final Timer mRecvGetPeersTimer;
    private final Timer mRecvPeersTimer;
    private final Timer mRecvGetTxSetTimer;
    private final Timer mRecvTxSetTimer;
    private final Timer mRecvTransactionTimer;
    private final Timer mRecvGetSCPQuorumSetTimer;
    private final Timer mRecvSCPQuorumSetTimer;
    private final Timer mRecvSCPMessageTimer;
    private final Timer mRecvGetSCPStateTimer;

    private final Timer mRecvSCPPrepareTimer;
    private final Timer mRecvSCPConfirmTimer;
    private final Timer mRecvSCPNominateTimer;
    private final Timer mRecvSCPExternalizeTimer;

    private final Meter mSendErrorMeter;
    private final Meter mSendHelloMeter;
    private final Meter mSendAuthMeter;
    private final Meter mSendDontHaveMeter;
    private final Meter mSendGetPeersMeter;
    private final Meter mSendPeersMeter;
    private final Meter mSendGetTxSetMeter;
    private final Meter mSendTransactionMeter;
    private final Meter mSendTxSetMeter;
    private final Meter mSendGetSCPQuorumSetMeter;
    private final Meter mSendSCPQuorumSetMeter;
    private final Meter mSendSCPMessageSetMeter;
    private final Meter mSendGetSCPStateMeter;

    private final Meter mDropInConnectHandlerMeter;
    private final Meter mDropInRecvMessageDecodeMeter;
    private final Meter mDropInRecvMessageSeqMeter;
    private final Meter mDropInRecvMessageMacMeter;
    private final Meter mDropInRecvMessageUnauthMeter;
    private final Meter mDropInRecvHelloUnexpectedMeter;
    private final Meter mDropInRecvHelloVersionMeter;
    private final Meter mDropInRecvHelloSelfMeter;
    private final Meter mDropInRecvHelloPeerIDMeter;
    private final Meter mDropInRecvHelloCertMeter;
    private final Meter mDropInRecvHelloBanMeter;
    private final Meter mDropInRecvHelloNetMeter;
    private final Meter mDropInRecvHelloPortMeter;
    private final Meter mDropInRecvAuthUnexpectedMeter;
    private final Meter mDropInRecvAuthRejectMeter;
    private final Meter mDropInRecvAuthInvalidPeerMeter;
    private final Meter mDropInRecvErrorMeter;

    private final Hash networkID;
    private final Herder herder;
    private final Config config;
    private final VirtualClock virtualClock;
    private final Metrics metrics;
    private final Database database;
    private final BanManager banManager;
    private final OverlayManager overlayManager;

    protected Peer(PeerRole role, Hash networkID, Herder herder, OverlayManager overlayManager, Config config, VirtualClock virtualClock, Metrics metrics, Database database, BanManager banManager) {
    	this.mRole = role;
    	this.networkID = networkID;
    	this.herder = herder;
    	this.overlayManager = overlayManager;
    	this.config = config;
    	this.virtualClock = virtualClock;
    	this.metrics = metrics;
    	this.database = database;
    	this.banManager = banManager;
    	this.mSendNonce = Uint256.of(CryptoUtils.randomBytes(32));
    	this.mState = role == PeerRole.WE_CALLED_REMOTE ? PeerState.CONNECTING : PeerState.CONNECTED;
    	this.mRemoteOverlayVersion = Uint32.ZERO;
    	this.mRemoteListeningPort = 0;
    	this.mIdleTimer = new VirtualTimer(virtualClock);
    	this.mLastRead = virtualClock.now();
    	this.mLastWrite = virtualClock.now();

    	this.mMessageRead = 
    	    metrics.newMeter("overlay", "message", "read", "message");
    	this.mMessageWrite = 
    	    metrics.newMeter("overlay", "message", "write", "message");
    	this.mByteRead = getByteReadMeter(metrics);
    	this.mByteWrite = getByteWriteMeter(metrics);
    	this.mErrorRead = 
    	    metrics.newMeter("overlay", "error", "read", "error");
    	this.mErrorWrite = 
    	    metrics.newMeter("overlay", "error", "write", "error");
    	this.mTimeoutIdle = 
    	    metrics.newMeter("overlay", "timeout", "idle", "timeout");

    	this.mRecvErrorTimer = metrics.newTimer("overlay", "recv", "error");
    	this.mRecvHelloTimer = metrics.newTimer("overlay", "recv", "hello");
    	this.mRecvAuthTimer = metrics.newTimer("overlay", "recv", "auth");
    	this.mRecvDontHaveTimer = 
    	    metrics.newTimer("overlay", "recv", "dont-have");
    	this.mRecvGetPeersTimer = 
    	    metrics.newTimer("overlay", "recv", "get-peers");
    	this.mRecvPeersTimer = metrics.newTimer("overlay", "recv", "peers");
    	this.mRecvGetTxSetTimer = 
    	    metrics.newTimer("overlay", "recv", "get-txset");
    	this.mRecvTxSetTimer = metrics.newTimer("overlay", "recv", "txset");
    	this.mRecvTransactionTimer = 
    	    metrics.newTimer("overlay", "recv", "transaction");
    	this.mRecvGetSCPQuorumSetTimer = 
    	    metrics.newTimer("overlay", "recv", "get-scp-qset");
    	this.mRecvSCPQuorumSetTimer = 
    	    metrics.newTimer("overlay", "recv", "scp-qset");
    	this.mRecvSCPMessageTimer = 
    	    metrics.newTimer("overlay", "recv", "scp-message");
    	this.mRecvGetSCPStateTimer = 
    	    metrics.newTimer("overlay", "recv", "get-scp-state");

    	this.mRecvSCPPrepareTimer = 
    	    metrics.newTimer("overlay", "recv", "scp-prepare");
    	this.mRecvSCPConfirmTimer = 
    	    metrics.newTimer("overlay", "recv", "scp-confirm");
    	this.mRecvSCPNominateTimer = 
    	    metrics.newTimer("overlay", "recv", "scp-nominate");
    	this.mRecvSCPExternalizeTimer = 
    	    metrics.newTimer("overlay", "recv", "scp-externalize");

    	this.mSendErrorMeter = 
    	    metrics.newMeter("overlay", "send", "error", "message");
    	this.mSendHelloMeter = 
    	    metrics.newMeter("overlay", "send", "hello", "message");
    	this.mSendAuthMeter = 
    	    metrics.newMeter("overlay", "send", "auth", "message");
    	this.mSendDontHaveMeter = metrics.newMeter(
    	    "overlay", "send", "dont-have", "message");
    	this.mSendGetPeersMeter = metrics.newMeter(
    	    "overlay", "send", "get-peers", "message");
    	this.mSendPeersMeter = 
    	    metrics.newMeter("overlay", "send", "peers", "message");
    	this.mSendGetTxSetMeter = metrics.newMeter(
    	    "overlay", "send", "get-txset", "message");
    	this.mSendTransactionMeter = metrics.newMeter(
    	    "overlay", "send", "transaction", "message");
    	this.mSendTxSetMeter = 
    	    metrics.newMeter("overlay", "send", "txset", "message");
    	this.mSendGetSCPQuorumSetMeter = metrics.newMeter(
    	    "overlay", "send", "get-scp-qset", "message");
    	this.mSendSCPQuorumSetMeter = 
    	    metrics.newMeter("overlay", "send", "scp-qset", "message");
    	this.mSendSCPMessageSetMeter = metrics.newMeter(
    	    "overlay", "send", "scp-message", "message");
    	this.mSendGetSCPStateMeter = metrics.newMeter(
    	    "overlay", "send", "get-scp-state", "message");
    	this.mDropInConnectHandlerMeter = metrics.newMeter(
    	    "overlay", "drop", "connect-handler", "drop");
    	this.mDropInRecvMessageDecodeMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-message-decode", "drop");
    	this.mDropInRecvMessageSeqMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-message-seq", "drop");
    	this.mDropInRecvMessageMacMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-message-mac", "drop");
    	this.mDropInRecvMessageUnauthMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-message-unauth", "drop");
    	this.mDropInRecvHelloUnexpectedMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-hello-unexpected", "drop");
    	this.mDropInRecvHelloVersionMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-hello-version", "drop");
    	this.mDropInRecvHelloSelfMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-hello-self", "drop");
    	this.mDropInRecvHelloPeerIDMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-hello-peerid", "drop");
    	this.mDropInRecvHelloCertMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-hello-cert", "drop");
    	this.mDropInRecvHelloBanMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-hello-ban", "drop");
    	this.mDropInRecvHelloNetMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-hello-net", "drop");
    	this.mDropInRecvHelloPortMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-hello-port", "drop");
    	this.mDropInRecvAuthUnexpectedMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-auth-unexpected", "drop");
    	this.mDropInRecvAuthRejectMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-auth-reject", "drop");
    	this.mDropInRecvAuthInvalidPeerMeter = metrics.newMeter(
    	    "overlay", "drop", "recv-auth-invalid-peer", "drop");
    	this.mDropInRecvErrorMeter = 
    	          metrics.newMeter("overlay", "drop", "recv-error", "drop");
    }
    Meter getByteReadMeter(Metrics metrics)
    {
        return metrics.newMeter("overlay", "byte", "read", "byte");
    }

    Meter getByteWriteMeter(Metrics metrics)
    {
        return metrics.newMeter("overlay", "byte", "write", "byte");
    }

    
    PeerRole getRole()
    {
        return mRole;
    }
    

    PeerState getState() 
    {
        return mState;
    }

    String getRemoteVersion() 
    {
        return mRemoteVersion;
    }

    Uint32 getRemoteOverlayMinVersion()
    {
        return mRemoteOverlayMinVersion;
    }

    Uint32 getRemoteOverlayVersion() 
    {
        return mRemoteOverlayVersion;
    }

    int getRemoteListeningPort()
    {
        return mRemoteListeningPort;
    }
    
    NodeID getPeerID()
    {
        return mPeerID;
    }

    void
    sendHello()
    {
        log.debug("Overlay sendHello to {}", () -> toString());
        StellarMessage msg = new StellarMessage();
        msg.setDiscriminant(MessageType.HELLO);
        Hello elo = new Hello();
        elo.setLedgerVersion(config.LEDGER_PROTOCOL_VERSION);
        elo.setOverlayMinVersion(config.OVERLAY_PROTOCOL_MIN_VERSION);
        elo.setOverlayVersion(config.OVERLAY_PROTOCOL_VERSION);
        elo.setVersionStr(config.VERSION_STR);
        elo.setNetworkID(networkID);
        elo.setListeningPort(config.PEER_PORT);
        elo.setPeerID(config.NODE_SEED.getPublicKey().toNodeID());
        elo.setCert(this.getAuthCert());
        elo.setNonce(mSendNonce);
        msg.setHello(elo);
        sendMessage(msg);
    }

    private AuthCert getAuthCert() {
	    return overlayManager.getPeerAuth().getAuthCert();
	}

    int
    getIOTimeoutSeconds()
    {
        if (isAuthenticated())
        {
            // Normally willing to wait 30s to hear anything
            // from an authenticated peer.
            return 30;
        }
        else
        {
            // We give peers much less timing leeway while
            // performing handshake.
            return 5;
        }
    }

    // helper method to acknownledge that some bytes were received
    void
    receivedBytes(int byteCount, boolean gotFullMessage)
    {
        PeerContext loadCtx = new PeerContext();//TODO (mApp, mPeerID);
        mLastRead = virtualClock.now();
        if (gotFullMessage)
            mMessageRead.mark();
        mByteRead.mark(byteCount);
    }

    void
    startIdleTimer()
    {
        if (shouldAbort())
        {
            return;
        }

//        auto self = shared_from_this();
        mIdleTimer.expires_from_now(Duration.ofSeconds(getIOTimeoutSeconds()));
        mIdleTimer.async_wait((error) -> {//asio::error_code error
            idleTimerExpired(error);
        });
    }

    void
    idleTimerExpired(@Nullable VirtualTimerErrorCode error)
    {
        if (error == null)
        {
            Instant now = virtualClock.now();
            Duration timeout = Duration.ofSeconds(getIOTimeoutSeconds());
            if ((Duration.between(mLastRead, now)).compareTo(timeout) >= 0 && Duration.between(mLastWrite, now).compareTo(timeout) >= 0)
            {
                log.warn("Overlay idle timeout");
                mTimeoutIdle.mark();
                drop();
            }
            else
            {
                startIdleTimer();
            }
        }
    }

    void
    sendAuth()
    {
        StellarMessage msg = new StellarMessage();
        msg.setDiscriminant(MessageType.AUTH);
        sendMessage(msg);
    }

    void
    drop(ErrorCode err, String msg)
    {
        StellarMessage m = new StellarMessage();
        m.setDiscriminant(MessageType.ERROR_MSG);
        com.consuminurigni.stellarj.overlay.xdr.Error xdrErr = new com.consuminurigni.stellarj.overlay.xdr.Error();
        xdrErr.setCode(err);
        xdrErr.setMsg(msg);
        m.setError(xdrErr);
        sendMessage(m);
        // note: this used to be a post which caused delays in stopping
        // to process read messages.
        // this has no effect wrt the sending queue.
        drop();
    }

    void
    connectHandler(@Nullable VirtualTimerErrorCode error)
    {
        if (error != null)
        {
            log.warn("Overlay  connectHandler error: {}", error.getMessage());
            mDropInConnectHandlerMeter.mark();
            drop();
        }
        else
        {
            log.debug("Overlay connected ", () -> toString());
            connected();
            mState = PeerState.CONNECTED;
            sendHello();
        }
    }

    void
    sendDontHave(MessageType type, Uint256 itemID)
    {
        StellarMessage msg = new StellarMessage();
        msg.setDiscriminant(MessageType.DONT_HAVE);
        DontHave dontHave = new DontHave();
        dontHave.setReqHash(itemID);
        dontHave.setType(type);
        msg.setDontHave(dontHave);
        sendMessage(msg);
    }

    void
    sendSCPQuorumSet(SCPQuorumSet qSet)
    {
        StellarMessage msg = new StellarMessage();
        msg.setDiscriminant(MessageType.SCP_QUORUMSET);
        msg.setQSet(qSet);;

        sendMessage(msg);
    }
    void
    sendGetTxSet(Uint256 setID)
    {
        StellarMessage newMsg = new StellarMessage();
        newMsg.setDiscriminant(MessageType.GET_TX_SET);
        newMsg.setTxSetHash(setID);

        sendMessage(newMsg);
    }
    void
    sendGetQuorumSet(Uint256 setID)
    {
            log.trace("Overlay Get quorum set: {}", () -> setID.hexAbbrev());

        StellarMessage newMsg = new StellarMessage();
        newMsg.setDiscriminant(MessageType.GET_SCP_QUORUMSET);
        newMsg.setQSetHash(setID);

        sendMessage(newMsg);
    }

    void
    sendGetPeers()
    {
        log.trace("Overlay Get peers");

        StellarMessage newMsg = new StellarMessage();
        newMsg.setDiscriminant(MessageType.GET_PEERS);

        sendMessage(newMsg);
    }

    void
    sendGetScpState(Uint32 ledgerSeq)
    {
        log.trace("Overlay Get SCP State for {}", () -> ledgerSeq);

        StellarMessage newMsg = new StellarMessage();
        newMsg.setDiscriminant(MessageType.GET_SCP_STATE);
        newMsg.setGetSCPLedgerSeq(ledgerSeq);
        sendMessage(newMsg);
    }

    void
    sendPeers()
    {
        // send top 50 peers we know about
        List<PeerRecord> peerList = PeerRecord.loadPeerRecords(database, 50, virtualClock.now());
        StellarMessage newMsg = new StellarMessage();
        newMsg.setDiscriminant(MessageType.PEERS);
        List<PeerAddress> peerAddrList = new ArrayList<>(peerList.size());
        for (PeerRecord pr : peerList)
        {
            if (pr.isPrivateAddress() ||
                pr.isSelfAddressAndPort(getIP(), mRemoteListeningPort))
            {
                continue;
            }
            PeerAddress pa =  pr.toXdr();
            peerAddrList.add(pa);
        }
        newMsg.setPeers(peerAddrList.toArray(new PeerAddress[peerAddrList.size()]));
        sendMessage(newMsg);
    }

    static String
    msgSummary(StellarMessage msg)
    {
        switch (msg.getDiscriminant())
        {
        case ERROR_MSG:
            return "ERROR";
        case HELLO:
            return "HELLO";
        case AUTH:
            return "AUTH";
        case DONT_HAVE:
            return "DONTHAVE";
        case GET_PEERS:
            return "GETPEERS";
        case PEERS:
            return "PEERS";

        case GET_TX_SET:
            return "GETTXSET";
        case TX_SET:
            return "TXSET";

        case TRANSACTION:
            return "TRANSACTION";

        case GET_SCP_QUORUMSET:
            return "GET_SCP_QSET";
        case SCP_QUORUMSET:
            return "SCP_QSET";
        case SCP_MESSAGE:
            switch (msg.getEnvelope().getStatement().getPledges().getDiscriminant())
            {
            case SCP_ST_PREPARE:
                return "SCP::PREPARE";
            case SCP_ST_CONFIRM:
                return "SCP::CONFIRM";
            case SCP_ST_EXTERNALIZE:
                return "SCP::EXTERNALIZE";
            case SCP_ST_NOMINATE:
                return "SCP::NOMINATE";
            }
        case GET_SCP_STATE:
            return "GET_SCP_STATE";
        }
        return "UNKNOWN";
    }

    // NB: This is a move-argument because the write-buffer has to travel
    // with the write-request through the async IO system, and we might have
    // several queued at once. We have carefully arranged this to not copy
    // data more than the once necessary into this buffer, but it can't be
    // put in a reused/non-owned buffer without having to buffer/queue
    // messages somewhere else. The async write request will point _into_
    // this owned buffer. This is really the best we can do.
    void
    sendMessage(StellarMessage msg)
    {
            log.trace("Overlay ({}) send: {} to : {}", 
            	() -> config.toShortString(config.NODE_SEED.getPublicKey()),
            	() -> msgSummary(msg),
            	() -> config.toShortString(mPeerID));

        switch (msg.getDiscriminant())
        {
        case ERROR_MSG:
            mSendErrorMeter.mark();
            break;
        case HELLO:
            mSendHelloMeter.mark();
            break;
        case AUTH:
            mSendAuthMeter.mark();
            break;
        case DONT_HAVE:
            mSendDontHaveMeter.mark();
            break;
        case GET_PEERS:
            mSendGetPeersMeter.mark();
            break;
        case PEERS:
            mSendPeersMeter.mark();
            break;
        case GET_TX_SET:
            mSendGetTxSetMeter.mark();
            break;
        case TX_SET:
            mSendTxSetMeter.mark();
            break;
        case TRANSACTION:
            mSendTransactionMeter.mark();
            break;
        case GET_SCP_QUORUMSET:
            mSendGetSCPQuorumSetMeter.mark();
            break;
        case SCP_QUORUMSET:
            mSendSCPQuorumSetMeter.mark();
            break;
        case SCP_MESSAGE:
            mSendSCPMessageSetMeter.mark();
            break;
        case GET_SCP_STATE:
            mSendGetSCPStateMeter.mark();
            break;
        };

        AuthenticatedMessage amsg = new AuthenticatedMessage();
        AuthenticatedMessageV0 msgv0 = new AuthenticatedMessageV0();
        msgv0.setMessage(msg);
        if (msg.getDiscriminant() != MessageType.HELLO && msg.getDiscriminant() != MessageType.ERROR_MSG)
        {
        	msgv0.setSequence(mSendMacSeq);
        	msgv0.setMac(
                CryptoUtils.hmacSHA256(mSendMacKey, BufUtils.concat(mSendMacSeq.encode(), msg.encode())));
            mSendMacSeq = mSendMacSeq.incr();
        }
        amsg.setV0(msgv0);;
        //TODO correct ??
        //xdr::msg_ptr xdrBytes(xdr::xdr_to_msg(amsg));
        //sendMessage(std::move(xdrBytes));
        sendMessage(amsg.encode());
    }
    //TODO ????
    // NB: This is a move-argument because the write-buffer has to travel
    // with the write-request through the async IO system, and we might have
    // several queued at once. We have carefully arranged this to not copy
    // data more than the once necessary into this buffer, but it can't be
    // put in a reused/non-owned buffer without having to buffer/queue
    // messages somewhere else. The async write request will point _into_
    // this owned buffer. This is really the best we can do.
    protected abstract void sendMessage(byte[] msg);

    void
    recvMessage(byte[] msg)
    {
        if (shouldAbort())
        {
            return;
        }

        LoadManager.PeerContext loadCtx = new PeerContext();//TODO (mApp, mPeerID);

        log.trace("Overlay received xdr::msg_ptr");
        try
        {
            AuthenticatedMessage am = AuthenticatedMessage.decode(msg);
            recvMessage(am);
        }
        catch (Exception e)
        {
            log.error("Overlay received corrupt xdr::msg_ptr {}", e.getMessage());
            mDropInRecvMessageDecodeMeter.mark();
            drop();
            return;
        }
    }

    boolean
    isConnected()
    {
        return mState != PeerState.CONNECTING && mState != PeerState.CLOSING;
    }

    boolean
    isAuthenticated()
    {
        return mState == PeerState.GOT_AUTH;
    }

    boolean
    shouldAbort()
    {
        return (mState == PeerState.CLOSING) || overlayManager.isShuttingDown();
    }

    void
    recvMessage(AuthenticatedMessage msg)
    {
        if (shouldAbort())
        {
            return;
        }

		AuthenticatedMessageV0 msgv0 = msg.getV0();
		if (mState.ordinal() >= PeerState.GOT_HELLO.ordinal() && msgv0.getMessage().getDiscriminant() != MessageType.ERROR_MSG)
        {
            if (msgv0.getSequence().ne(mRecvMacSeq))
            {
                log.error("Overlay Unexpected message-auth sequence");
                mDropInRecvMessageSeqMeter.mark();
                mRecvMacSeq = mRecvMacSeq.incr();
                drop(ErrorCode.ERR_AUTH, "unexpected auth sequence");
                return;
            }

            byte[] signedBuf = BufUtils.concat(msgv0.getSequence().encode(), msgv0.getMessage().encode());
            if (! CryptoUtils.hmacSHA256Verify(mRecvMacKey, signedBuf, msgv0.getMac()))
            {
                log.error("Overlay Message-auth check failed");
                mDropInRecvMessageMacMeter.mark();
                mRecvMacSeq = mRecvMacSeq.incr();
                drop(ErrorCode.ERR_AUTH, "unexpected MAC");
                return;
            }
            mRecvMacSeq = mRecvMacSeq.incr();
        }
        recvMessage(msgv0.getMessage());
    }

    
    
    
    void
    recvDontHave(StellarMessage msg)
    {
        herder.peerDoesntHave(msg.getDontHave().getType(), msg.getDontHave().getReqHash(),
                                        this);
    }

    void
    recvGetTxSet(StellarMessage msg)
    {
//        auto self = shared_from_this();
        if (TxSetFrame txSet = herder.getTxSet(msg.txSetHash()))
        {
            StellarMessage newMsg;
            newMsg.type(TX_SET);
            txSet->toXDR(newMsg.txSet());

            self->sendMessage(newMsg);
        }
        else
        {
            sendDontHave(TX_SET, msg.txSetHash());
        }
    }

    void
    recvTxSet(StellarMessage msg)
    {
        TxSetFrame frame(networkID, msg.txSet());
        herder.recvTxSet(frame.getContentsHash(), frame);
    }

    void
    recvTransaction(StellarMessage msg)
    {
        TransactionFramePtr transaction = TransactionFrame::makeTransactionFromWire(
            networkID, msg.transaction());
        if (transaction)
        {
            // add it to our current set
            // and make sure it is valid
            auto recvRes = herder.recvTransaction(transaction);

            if (recvRes == Herder::TX_STATUS_PENDING ||
                recvRes == Herder::TX_STATUS_DUPLICATE)
            {
                // record that this peer sent us this transaction
                overlayManager.recvFloodedMsg(msg, shared_from_this());

                if (recvRes == Herder::TX_STATUS_PENDING)
                {
                    // if it's a new transaction, broadcast it
                    overlayManager.broadcastMessage(msg);
                }
            }
        }
    }

    void
    recvGetSCPQuorumSet(StellarMessage msg)
    {
        SCPQuorumSetPtr qset = herder.getQSet(msg.qSetHash());

        if (qset)
        {
            sendSCPQuorumSet(qset);
        }
        else
        {
            if (Logging::logTrace("Overlay"))
                log.trace("Overlay No quorum set: "
                                       << hexAbbrev(msg.qSetHash());
            sendDontHave(SCP_QUORUMSET, msg.qSetHash());
            // do we want to ask other people for it?
        }
    }
    void
    recvSCPQuorumSet(StellarMessage msg)
    {
        Hash hash = sha256(xdr::xdr_to_opaque(msg.qSet()));
        herder.recvSCPQuorumSet(hash, msg.qSet());
    }

    void
    recvSCPMessage(StellarMessage msg)
    {
        SCPEnvelope envelope = msg.envelope();
        if (Logging::logTrace("Overlay"))
            CLOG(TRACE, "Overlay")
                << "recvSCPMessage node: "
                << config.toShortString(msg.envelope().statement.nodeID);

        overlayManager.recvFloodedMsg(msg, shared_from_this());

        auto type = msg.envelope().statement.pledges.type();
        auto t = (type == SCP_ST_PREPARE
                      ? mRecvSCPPrepareTimer.TimeScope()
                      : (type == SCP_ST_CONFIRM
                             ? mRecvSCPConfirmTimer.TimeScope()
                             : (type == SCP_ST_EXTERNALIZE
                                    ? mRecvSCPExternalizeTimer.TimeScope()
                                    : (mRecvSCPNominateTimer.TimeScope()))));

        herder.recvSCPEnvelope(envelope);
    }

    void
    recvGetSCPState(StellarMessage msg)
    {
        uint32 seq = msg.getSCPLedgerSeq();
        log.trace("Overlay get SCP State " << seq;
        herder.sendSCPStateToPeer(seq, shared_from_this());
    }

    void
    recvError(StellarMessage msg)
    {
        String codeStr = "UNKNOWN";
        switch (msg.error().code)
        {
        case ERR_MISC:
            codeStr = "ERR_MISC";
            break;
        case ERR_DATA:
            codeStr = "ERR_DATA";
            break;
        case ERR_CONF:
            codeStr = "ERR_CONF";
            break;
        case ERR_AUTH:
            codeStr = "ERR_AUTH";
            break;
        case ERR_LOAD:
            codeStr = "ERR_LOAD";
            break;
        default:
            break;
        }
        log.warn("Overlay Received error (" << codeStr
                                 << "): " << msg.error().msg;
        mDropInRecvErrorMeter.mark();
        drop();
    }

    private void idleTimerExpired(Exception error) {
		// TODO Auto-generated method stub
		
	}
	// returns false if we should drop this peer
    void noteHandshakeSuccessInPeerRecord() {}



    
    public void sendGetTxSet(Hash hash) {
		// TODO Auto-generated method stub
	}

	public void sendGetQuorumSet(Hash hash) {
		// TODO Auto-generated method stub
	}

	@Override
	public String toString() {
	    return getIP() + ":" + mRemoteListeningPort;

	}

	public void reset() {
		// TODO Auto-generated method stub
		
	}
}
