package com.consumimurigni.stellarj.herder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.codahale.metrics.Gauge;
import com.consumimurigni.stellarj.crypto.CryptoUtils;
import com.consumimurigni.stellarj.crypto.KeyUtils;
import com.consumimurigni.stellarj.crypto.PubKeyUtils;
import com.consumimurigni.stellarj.crypto.SecretKey;
import com.consumimurigni.stellarj.ledger.LedgerCloseData;
import com.consumimurigni.stellarj.ledger.LedgerManager;
import com.consumimurigni.stellarj.ledger.TxSetFrame;
import com.consumimurigni.stellarj.ledger.xdr.AccountID;
import com.consumimurigni.stellarj.ledger.xdr.LedgerHeader;
import com.consumimurigni.stellarj.ledger.xdr.LedgerHeaderHistoryEntry;
import com.consumimurigni.stellarj.ledger.xdr.LedgerSCPMessages;
import com.consumimurigni.stellarj.ledger.xdr.LedgerUpgrade;
import com.consumimurigni.stellarj.ledger.xdr.LedgerUpgradeType;
import com.consumimurigni.stellarj.ledger.xdr.SCPHistoryEntry;
import com.consumimurigni.stellarj.ledger.xdr.SCPHistoryEntryV0;
import com.consumimurigni.stellarj.ledger.xdr.SequenceNumber;
import com.consumimurigni.stellarj.ledger.xdr.StellarValue;
import com.consumimurigni.stellarj.ledger.xdr.TransactionResultCode;
import com.consumimurigni.stellarj.ledger.xdr.TransactionSet;
import com.consumimurigni.stellarj.ledger.xdr.UpgradeType;
import com.consumimurigni.stellarj.main.Application;
import com.consumimurigni.stellarj.main.Database;
import com.consumimurigni.stellarj.main.PersistentState;
import com.consumimurigni.stellarj.scp.SCP;
import com.consumimurigni.stellarj.scp.SCPDriver;
import com.consumimurigni.stellarj.scp.Slot;
import com.consumimurigni.stellarj.scp.Slot.timerIDs;
import com.consumimurigni.stellarj.transactions.TransactionFrame;
import com.consuminurigni.stellarj.common.Assert;
import com.consuminurigni.stellarj.common.Base64Codec;
import com.consuminurigni.stellarj.common.Tuple2;
import com.consuminurigni.stellarj.common.Vectors;
import com.consuminurigni.stellarj.common.VirtualClock;
import com.consuminurigni.stellarj.common.VirtualTimer;
import com.consuminurigni.stellarj.overlay.Peer;
import com.consuminurigni.stellarj.overlay.xdr.MessageType;
import com.consuminurigni.stellarj.overlay.xdr.StellarMessage;
import com.consuminurigni.stellarj.scp.xdr.SCPBallot;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.scp.xdr.ValueSet;
import com.consuminurigni.stellarj.xdr.EnvelopeType;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.Int64;
import com.consuminurigni.stellarj.xdr.NodeID;
import com.consuminurigni.stellarj.xdr.PublicKey;
import com.consuminurigni.stellarj.xdr.Uint256;
import com.consuminurigni.stellarj.xdr.Uint32;
import com.consuminurigni.stellarj.xdr.Uint64;
import com.consuminurigni.stellarj.xdr.Value;
import com.consuminurigni.stellarj.xdr.XDROutputFileStream;
import com.consuminurigni.stellarj.xdr.Xdr;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Herder extends SCPDriver {
	private static final Logger log = LogManager.getLogger();

	// the value of LEDGER_VALIDITY_BRACKET should be in the order of
	// how many ledgers can close ahead given CONSENSUS_STUCK_TIMEOUT_SECONDS

	// How many ledger in the future we consider an envelope viable.
	private static final Uint32 LEDGER_VALIDITY_BRACKET = Uint32.ofPositiveInt(100);
	// How many ledgers in the past we keep track of
	private static final Uint32 MAX_SLOTS_TO_REMEMBER = Uint32.ofPositiveInt(4);

	// Expected time between two ledger close.
	private static final Duration EXP_LEDGER_TIMESPAN_SECONDS = Duration.ofSeconds(5);

	// Maximum timeout for SCP consensus.
	private static final Duration MAX_SCP_TIMEOUT_SECONDS = Duration.ofSeconds(240);

	// timeout before considering the node out of sync
	private static final Duration CONSENSUS_STUCK_TIMEOUT_SECONDS = Duration.ofSeconds(35);

	// Maximum time slip between nodes.
	private static final Duration MAX_TIME_SLIP_SECONDS = Duration.ofSeconds(60);

	// How many seconds of inactivity before evicting a node.
	private static final Duration NODE_EXPIRATION_SECONDS = Duration.ofSeconds(240);

	enum State {
		HERDER_SYNCING_STATE, HERDER_TRACKING_STATE
	};

	enum TransactionSubmitStatus {
		TX_STATUS_PENDING, TX_STATUS_DUPLICATE, TX_STATUS_ERROR, TX_STATUS_COUNT
	};

	enum EnvelopeStatus {
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

	////////////
	private final SCP mSCP;

	private static class TxMap {
		public SequenceNumber getMaxSeq() {
			return mMaxSeq;
		}

		public Int64 getTotalFees() {
			return mTotalFees;
		}

		public HashMap<Hash, TransactionFrame> getTransactions() {
			return mTransactions;
		}

		private SequenceNumber mMaxSeq = SequenceNumber.of(0);
		private Int64 mTotalFees = Int64.of(0);
		private HashMap<Hash, TransactionFrame> mTransactions;

		void addTx(TransactionFrame tx)
		{
		    Hash h = tx.getFullHash();
		    if (mTransactions.containsKey(h))
		    {
		        return;
		    }
		    mTransactions.put(h, tx);
		    mMaxSeq = SequenceNumber.max(tx.getSeqNum(), mMaxSeq);
		    mTotalFees = mTotalFees.plus(tx.getFee());
		}

		void recalculate()
		{
		    mMaxSeq = SequenceNumber.of(0);
		    mTotalFees = Int64.of(0);
		    for (TransactionFrame txf : mTransactions.values())
		    {
		        mMaxSeq = SequenceNumber.max(txf.getSeqNum(), mMaxSeq);
		        mTotalFees = mTotalFees.plus(txf.getFee());
		    }
		}
	};

	//TODO promote to type AccountTxMap
	private final HashMap<AccountID, TxMap> accountTxMap = new HashMap<>();

	// 0- tx we got during ledger close
	// 1- one ledger ago. rebroadcast
	// 2- two ledgers ago. rebroadcast
	// ...
	private final Deque<HashMap<AccountID, TxMap>> mPendingTransactions = new ArrayDeque<>(4);

	private final PendingEnvelopes mPendingEnvelopes;

	private static class ConsensusData {
		private final Uint64 consensusIndex;
		private final StellarValue consensusValue;

		public ConsensusData(Uint64 mConsensusIndex, StellarValue mConsensusValue) {
			this.consensusIndex = mConsensusIndex;
			this.consensusValue = mConsensusValue;
		}

		public ConsensusData(ConsensusData mTrackingSCP) {
			// TODO Auto-generated constructor stub
		}

		public Uint64 getConsensusIndex() {
			return consensusIndex;
		}

		public StellarValue getConsensusValue() {
			return consensusValue;
		}

		public Object release() {
			// TODO Auto-generated method stub
			return null;
		}

	};

	// if the local instance is tracking the current state of SCP
	// herder keeps track of the consensus index and ballot
	// when not set, it just means that herder will try to snap to any slot that
	// reached consensus
	private @Nullable ConsensusData mTrackingSCP = null;

	// when losing track of consensus, records where we left off so that we
	// ignore older ledgers (as we potentially receive old messages)
	private @Nullable ConsensusData mLastTrackingSCP = null;

	// last slot that was persisted into the database
	// only keep track of the most recent slot
	private final Uint64 mLastSlotSaved;

	// actually vistual-instant
	private final /* VirtualClock::time_point */ Instant mLastStateChange;

	// timer that detects that we're stuck on an SCP slot
	private final VirtualTimer mTrackingTimer;

	private final /* VirtualClock::time_point */Instant mLastTrigger;
	private final VirtualTimer mTriggerTimer;

	private final VirtualTimer mRebroadcastTimer;

	private final Uint32 mLedgerSeqNominating;
	private final Value mCurrentValue;

	private final LinkedHashMap<Uint64, Map<Integer, VirtualTimer>> mSCPTimers = new LinkedHashMap<>();

	private final Application mApp;
	private final LedgerManager mLedgerManager;

	private final SCPMetrics mSCPMetrics;

	////////////
	public static Herder create(Application app) {
		return new Herder(app);
	}
	private final Application app;
	private Herder(Application app) {
		this.app = app;
		mSCP = new SCP(this, app.getConfig().NODE_SEED, app.getConfig().NODE_IS_VALIDATOR, app.getConfig().QUORUM_SET);
		mPendingEnvelopes = new PendingEnvelopes(app, this);
		mLastSlotSaved = Uint64.ZERO;
		Instant now = app.getClock().now();
		mLastStateChange = now;
		mTrackingTimer = new VirtualTimer(app);
		mLastTrigger = now;
		mTriggerTimer = new VirtualTimer(app);
		mRebroadcastTimer = new VirtualTimer(app);
		mApp = app;
		mLedgerManager = app.getLedgerManager();
		mSCPMetrics = new SCPMetrics(app);
		Hash hash = mSCP.getLocalNode().getQuorumSetHash();
		mPendingEnvelopes.addSCPQuorumSet(hash, Uint64.ZERO, mSCP.getLocalNode().getQuorumSet());
	}

	public SCP getSCP() {
		return mSCP;
	}

	State getState() {
	    return (mTrackingSCP != null && mLastTrackingSCP != null) ? State.HERDER_TRACKING_STATE
	                                              : State.HERDER_SYNCING_STATE;
	}

	void syncMetrics()
	{
	    Int64 c = Int64.of(mSCPMetrics.mHerderStateCurrent.getCount());
	    Int64 n = Int64.of(getState().ordinal());
	    if (c.ne(n))
	    {
	        //TODO mSCPMetrics.mHerderStateCurrent.set_count(n);
	    }
	}

	String getStateHuman() {
	    return getState().name();
	}

	void stateChanged()
	{
	    //TODO mSCPMetrics.mHerderStateCurrent.set_count(static_cast<int64_t>(getState()));
	    Instant now = mApp.getClock().now();
	    mSCPMetrics.mHerderStateChanges.update(now.toEpochMilli() - mLastStateChange.toEpochMilli(), TimeUnit.MILLISECONDS);//TODO ?? npe mLastStateChange
	    mLastStateChange = now;
	    mApp.syncOwnMetrics();
	}

	void bootstrap()
	{
	    log.info("Herder Force joining SCP with local state");
	    assert(mSCP.isValidator());
	    assert(mApp.getConfig().FORCE_SCP);

	    mLedgerManager.setState(LedgerManager.State.LM_SYNCED_STATE);
	    stateChanged();

	    mLastTrigger = mApp.getClock().now().minus(EXP_LEDGER_TIMESPAN_SECONDS);
	    ledgerClosed();
	}

	boolean isSlotCompatibleWithCurrentState(Uint64 slotIndex)
	{
	    boolean res = false;
	    if (mLedgerManager.isSynced())
	    {
	    	LedgerHeaderHistoryEntry lcl = mLedgerManager.getLastClosedLedgerHeader();
	        res = (slotIndex.eq(lcl.getHeader().getLedgerSeq().plus(1)));
	    }

	    return res;
	}

	SCPDriver.ValidationLevel validateValueHelper(Uint64 slotIndex, StellarValue b)
	{
	    Uint64 lastCloseTime;

	    boolean compat = isSlotCompatibleWithCurrentState(slotIndex);

	    if (compat)
	    {
	        lastCloseTime = mLedgerManager.getLastClosedLedgerHeader()
	                            .getHeader().getScpValue().getCloseTime();
	    }
	    else
	    {
	        if (mTrackingSCP == null)
	        {
	            // if we're not tracking, there is not much more we can do to
	            // validate
	            return SCPDriver.ValidationLevel.kMaybeValidValue;
	        }

	        // Check slotIndex.
	        if (nextConsensusLedgerIndex().gt(slotIndex))
	        {
	            // we already moved on from this slot
	            // still send it through for emitting the final messages
	            return SCPDriver.ValidationLevel.kMaybeValidValue;
	        }
	        if (nextConsensusLedgerIndex().lt(slotIndex))
	        {
	            // this is probably a bug as "tracking" means we're processing
	            // messages only for smaller slots
	            log.error("Herder HerderImpl::validateValue  i: {} processing a future message while tracking", slotIndex.toString());

	            return SCPDriver.ValidationLevel.kInvalidValue;
	        }
	        lastCloseTime = mTrackingSCP.getConsensusValue().getCloseTime();
	    }

	    // Check closeTime (not too old)
	    if (b.getCloseTime().lte(lastCloseTime))
	    {
	        return SCPDriver.ValidationLevel.kInvalidValue;
	    }

	    // Check closeTime (not too far in future)
	    long timeNow = mApp.timeNow();
	    if (b.getCloseTime().gt(timeNow + MAX_TIME_SLIP_SECONDS.getSeconds()))
	    {
	        return SCPDriver.ValidationLevel.kInvalidValue;
	    }

	    if (!compat)
	    {
	        // this is as far as we can go if we don't have the state
	        return SCPDriver.ValidationLevel.kMaybeValidValue;
	    }

	    Hash txSetHash = b.getTxSetHash();

	    // we are fully synced up

	    TxSetFrame txSet = mPendingEnvelopes.getTxSet(txSetHash);

	    SCPDriver.ValidationLevel res;

	    if (txSet == null)
	    {
	        log.error("Herder HerderImpl::validateValue i: {} txSet not found?", slotIndex.toString());

	        res = SCPDriver.ValidationLevel.kInvalidValue;
	    }
	    else if (!txSet.checkValid(mApp))
	    {
	            log.debug("Herder HerderImpl::validateValue i: {} Invalid txSet: {}"
	            	, slotIndex.toString(),txSet.getContentsHash().hexAbbrev());
	        res = SCPDriver.ValidationLevel.kInvalidValue;
	    }
	    else
	    {
	            log.debug("Herder HerderImpl::validateValue i: {} txSet: {} OK", slotIndex.toString(),txSet.getContentsHash().hexAbbrev());
	        res = SCPDriver.ValidationLevel.kFullyValidatedValue;
	    }
	    return res;
	}

	Tuple2<Boolean, LedgerUpgradeType> validateUpgradeStep(Uint64 slotIndex, UpgradeType upgrade
			//,LedgerUpgradeType upgradeType
	                                )
	{
	    LedgerUpgrade lupgrade;

	    try
	    {
	    	lupgrade = LedgerUpgrade.decode(upgrade.encode());//TODO ???
	    }
	    catch (RuntimeException e)
	    {
	        return new Tuple2<Boolean, LedgerUpgradeType>(false, null);
	    }

	    boolean res;
	    switch (lupgrade.getDiscriminant())
	    {
	    case LEDGER_UPGRADE_VERSION:
	    {
	        Uint32 newVersion = lupgrade.getNewLedgerVersion();
	        res = (newVersion == mApp.getConfig().LEDGER_PROTOCOL_VERSION);
	    }
	    break;
	    case LEDGER_UPGRADE_BASE_FEE:
	    {
	        Uint32 newFee = lupgrade.getNewBaseFee();
	        // allow fee to move within a 2x distance from the one we have in our
	        // config
	        res = (newFee.gte(mApp.getConfig().DESIRED_BASE_FEE.mul(.5)) &&
	              (newFee.lte(mApp.getConfig().DESIRED_BASE_FEE.mul(2))));
	    }
	    break;
	    case LEDGER_UPGRADE_MAX_TX_SET_SIZE:
	    {
	        // allow max to be within 30% of the config value
	        Uint32 newMax = lupgrade.getNewMaxTxSetSize();
	        res = (newMax.gte(mApp.getConfig().DESIRED_MAX_TX_PER_LEDGER.mul(0.7))) &&
	              (newMax.lte(mApp.getConfig().DESIRED_MAX_TX_PER_LEDGER.mul(1.3)));
	    }
	    break;
	    default:
	        res = false;
	    }
	    if (res)
	    {
	        return new Tuple2<Boolean, LedgerUpgradeType>(res, lupgrade.getDiscriminant());
	    } else {
	        return new Tuple2<Boolean, LedgerUpgradeType>(res, LedgerUpgradeType.LEDGER_UPGRADE_VERSION/*TODO ?? default the lowest*/);
	    }
	}

	@Override
	public void signEnvelope(SCPEnvelope envelope) {
	    mSCPMetrics.mEnvelopeSign.mark();
	    envelope.setSignature(mSCP.getSecretKey().sign(Xdr.pack(
	        mApp.getNetworkID().encode(), EnvelopeType.ENVELOPE_TYPE_SCP.encode(), envelope.getStatement().encode())));
	}

	@Override
	public boolean verifyEnvelope(SCPEnvelope envelope) {
	    boolean b = PubKeyUtils.verifySig(
	        envelope.getStatement().getNodeID(), envelope.getSignature(),
	        Xdr.pack(
	    	        mApp.getNetworkID().encode(), EnvelopeType.ENVELOPE_TYPE_SCP.encode(), envelope.getStatement().encode()));
	    if (b)
	    {
	        mSCPMetrics.mEnvelopeValidSig.mark();
	    }
	    else
	    {
	        mSCPMetrics.mEnvelopeInvalidSig.mark();
	    }

	    return b;
	}

	SCPDriver.ValidationLevel validateValue(Uint64 slotIndex, Value value)
	{
	    StellarValue b;
	    try
	    {
	    	b = StellarValue.decode(value.getValue());
	    }
	    catch (Exception e)
	    {
	        mSCPMetrics.mValueInvalid.mark();
	        return SCPDriver.ValidationLevel.kInvalidValue;
	    }

	    SCPDriver.ValidationLevel res = validateValueHelper(slotIndex, b);
	    if (res != SCPDriver.ValidationLevel.kInvalidValue)
	    {
	        LedgerUpgradeType lastUpgradeType = LedgerUpgradeType.LEDGER_UPGRADE_VERSION;
	        // check upgrades
	        for (int i = 0; i < b.getUpgrades().length; i++)
	        {
	            Tuple2<Boolean, LedgerUpgradeType> t2 = validateUpgradeStep(slotIndex, b.getUpgrades()[i]);
	            boolean validateUpgradeStepRes = t2.get0();
	            @Nullable LedgerUpgradeType thisUpgradeType = t2.get1();
	            if (!validateUpgradeStepRes)
	            {
	                log.trace("Herder HerderImpl::validateValue invalid step at index {}", i);
	                res = SCPDriver.ValidationLevel.kInvalidValue;
	            }
	            if (i != 0 && (lastUpgradeType.compareTo(thisUpgradeType) >= 0))
	            {
	            	log.trace("Herder HerderImpl::validateValue out of order upgrade step at index {}", i);
	                res = SCPDriver.ValidationLevel.kInvalidValue;
	            }

	            lastUpgradeType = thisUpgradeType;
	        }
	    }

	    if (res != SCPDriver.ValidationLevel.kInvalidValue)//TODO ?? if(res) meaning not null or not zero ? validateValueHelper never returns null thus this interpretation of c++ based on kInvalidValue.ord == 0
	    {
	        mSCPMetrics.mValueValid.mark();
	    }
	    else
	    {
	        mSCPMetrics.mValueInvalid.mark();
	    }
	    return res;
	}

	Value extractValidValue(Uint64 slotIndex, Value value)
	{
	    StellarValue b;
	    try
	    {
	        b = StellarValue.decode(value.getValue());
	    }
	    catch (Exception e)
	    {
	        return Value.NO_VALUE;
	    }
	    Value res = new Value();
	    if (validateValueHelper(slotIndex, b) == SCPDriver.ValidationLevel.kFullyValidatedValue)
	    {
	        // remove the upgrade steps we don't like
	        //TODO ?? LedgerUpgradeType thisUpgradeType;
	        UpgradeType[] upgrades = b.getUpgrades();
	        List<UpgradeType> l = new ArrayList<UpgradeType>(upgrades.length);
	        for (int i = 0; i < upgrades.length;i++)
	        {
	            if (validateUpgradeStep(slotIndex, upgrades[i]).get0())
	            {
	            	l.add(upgrades[i]);
	            }
	        }
	        b.setUpgrades(l.toArray(new UpgradeType[l.size()]));
	        res.setValue(b.encode());
	    }

	    return res;
	}

	String toShortString(PublicKey pk)
	{
	    return mApp.getConfig().toShortString(pk);
	}

	String getValueString(Value v)
	{
	    StellarValue b;
	    if (v.isEmpty())
	    {
	        return "[:empty:]";
	    }

	    try
	    {
	    	b = StellarValue.decode(v.getValue());
	        return stellarValueToString(b);
	    }
	    catch (Exception e)
	    {
	        return "[:invalid:]";
	    }
	}

	void updateSCPCounters()
	{
		//TODO
	    //mSCPMetrics.mKnownSlotsSize.set_count(mSCP.getKnownSlotsCount());
	    //mSCPMetrics.mCumulativeStatements.set_count(
	    //    mSCP.getCumulativeStatemtCount());
	}

	static Uint64 countTxs(Map<AccountID, TxMap> acc)
	{
	    long sz = 0;
	    for (TxMap a : acc.values())
	    {
	        sz += a.getTransactions().size();
	    }
	    return Uint64.of(sz);
	}

	//TODO !!!!!! modifies acc
	static @Nullable TxMap findOrAdd(Map<AccountID, TxMap> acc, AccountID aid)
	{
	    TxMap txmap = null;
	    TxMap i = acc.get(aid);
	    if (i == null)
	    {
	        txmap = new TxMap();
	        acc.put(aid, txmap);
	    }
	    else
	    {
	        txmap = i;
	    }
	    return txmap;
	}

	void logQuorumInformation(Uint64 index)
	{
	    String res;
	    LinkedHashMap<String, Object> v = new LinkedHashMap<>();
	    dumpQuorumInfo(v, mSCP.getLocalNodeID(), true, index);
	    Map<String, Object> slots = (Map<String, Object>)v.get("slots");
	    if (slots != null && ! slots.isEmpty())
	    {
	        String indexs = index.toString();
	        Object i = slots.get(indexs);
	        if (i != null)
	        {
	            log.info("Herder Quorum information for {} : {}",index.toString(), app.toJsonString(i));
	        }
	    }
	}

	Value combineCandidates(Uint64 slotIndex,
	                              Set<Value> candidates)
	{
	    Hash h;

  	    StellarValue comp;//TODO(h, 0, emptyUpgradeSteps, 0);

	    LinkedHashMap<LedgerUpgradeType, LedgerUpgrade> upgrades = new LinkedHashMap<>();

	    LinkedHashSet<TransactionFrame> aggSet = new LinkedHashSet<>();

	    LedgerHeaderHistoryEntry lcl = mLedgerManager.getLastClosedLedgerHeader();

	    Hash candidatesHash;//TODO ?? init ??

	    List<StellarValue> candidateValues = new LinkedList<>();

	    for (Value c : candidates)
	    {
	    	StellarValue sv = StellarValue.fromOpaque(c);
	    	candidateValues.add(sv);
	        candidatesHash ^= sha256(c);

	        // max closeTime
	        if (comp.getCloseTime().lt(sv.getCloseTime()))
	        {
	            comp.setCloseTime(sv.getCloseTime());
	        }
	        for (UpgradeType upgrade : sv.getUpgrades())
	        {
	            LedgerUpgrade lupgrade = LedgerUpgrade.decode(upgrade.encode());
	            LedgerUpgrade it = upgrades.get(lupgrade.getDiscriminant());
	            if (! upgrades.containsKey(lupgrade.getDiscriminant()))
	            {
	                upgrades.put(lupgrade.getDiscriminant(), lupgrade);
	            }
	            else
	            {
	                LedgerUpgrade clUpgrade = upgrades.get(lupgrade.getDiscriminant());
	                switch (lupgrade.getDiscriminant())
	                {
	                case LEDGER_UPGRADE_VERSION:
	                    // pick the highest version
	                    if (clUpgrade.getNewLedgerVersion().lt(
	                        lupgrade.getNewLedgerVersion()))
	                    {
	                        clUpgrade.setNewLedgerVersion(
	                            lupgrade.getNewLedgerVersion());
	                    }
	                    break;
	                case LEDGER_UPGRADE_BASE_FEE:
	                    // take the max fee
	                    if (clUpgrade.getNewBaseFee().lt(lupgrade.getNewBaseFee()))
	                    {
	                        clUpgrade.setNewBaseFee(lupgrade.getNewBaseFee());
	                    }
	                    break;
	                case LEDGER_UPGRADE_MAX_TX_SET_SIZE:
	                    // take the max tx set size
	                    if (clUpgrade.getNewMaxTxSetSize().lt(
	                        lupgrade.getNewMaxTxSetSize()))
	                    {
	                        clUpgrade.setNewMaxTxSetSize(
	                            lupgrade.getNewMaxTxSetSize());
	                    }
	                    break;
	                default:
	                    // should never get there with values that are not valid
	                    throw new RuntimeException("invalid upgrade step");
	                }
	            }
	        }
	    }

	    // take the txSet with the highest number of transactions,
	    // highest xored hash that we have
	    TxSetFrame bestTxSet;//TODO init
	    {
	        Hash highest;//TODO init
	        TxSetFrame highestTxSet;//TODO init
	        for (StellarValue sv : candidateValues)
	        {
	            @Nullable TxSetFrame cTxSet = getTxSet(sv.getTxSetHash());

	            if (cTxSet != null && cTxSet.previousLedgerHash().equals(lcl.getHash()))
	            {
	                if (highestTxSet == null || (cTxSet.getTransactions().size() >
	                                      highestTxSet.getTransactions().size()) ||
	                    ((cTxSet.getTransactions().size() ==
	                      highestTxSet.getTransactions().size()) &&
	                     lessThanXored(highest, sv.getTxSetHash(), candidatesHash)))
	                {
	                    highestTxSet = cTxSet;
	                    highest = sv.getTxSetHash();
	                }
	            }
	        }
	        // make a copy as we're about to modify it and we don't want to mess
	        // with the txSet cache
	        bestTxSet = new TxSetFrame(highestTxSet);
	    }

	    List<UpgradeType> l = new LinkedList<>();
	    for (LedgerUpgrade upgrade : upgrades.values())
	    {
//TODO ???
//	        Value v = xdr::xdr_to_opaque(upgrade.encode());
//	        comp.upgrades.emplace_back(v.begin(), v.end());
	        l.add(UpgradeType.decode(upgrade.encode()));
	    }
	    comp.setUpgrades(l.toArray(new UpgradeType[l.size()]));

	    // just to be sure
	    List<TransactionFrame> removed =  bestTxSet.trimInvalid(mApp);
	    comp.setTxSetHash(bestTxSet.getContentsHash());

	    if (removed.size() != 0)
	    {
	        log.warn("Herder Candidate set had {} invalid transactions", removed.size());

	        // post to avoid triggering SCP handling code recursively
	        mApp.getClock().getIOService().post(() -> {
	            mPendingEnvelopes.recvTxSet(bestTxSet.getContentsHash(),
	                                        bestTxSet);
	        });
	    }

	    return comp.toOpaqueValue();
	}

	void setupTimer(Uint64 slotIndex, int timerID,
	                       Duration timeout,
	                       Runnable cb)
	{
	    // don't setup timers for old slots
	    if (slotIndex.lte(getCurrentLedgerSeq()))
	    {
	        mSCPTimers.remove(slotIndex);
	        return;
	    }

	    Map<Integer, VirtualTimer> slotTimers = mSCPTimers.get(slotIndex);

	    VirtualTimer timer = slotTimers.get(timerID);
	    if (timer == null)
	    {
	        timer = new VirtualTimer(mApp);
	        slotTimers.put(timerID,  timer);
	    }
	    timer.cancel();
	    timer.expires_from_now(timeout);
	    timer.async_wait(cb, VirtualTimer::onFailureNoop);
	}

	void rebroadcast()
	{
	    for (SCPEnvelope e :
	         mSCP.getLatestMessagesSend(mLedgerManager.getLedgerNum().toUint64()))
	    {
	        broadcast(e);
	    }
	    startRebroadcastTimer();
	}

	void broadcast(SCPEnvelope e)
	{
	    if (!mApp.getConfig().MANUAL_CLOSE)
	    {
	        StellarMessage m = new StellarMessage();
	        m.setDiscriminant(MessageType.SCP_MESSAGE);
	        m.setEnvelope(e);

	        log.debug("Herder broadcast  s:{}  i:{}", e.getStatement().getPledges().getDiscriminant().name(), e.getStatement().getSlotIndex().toString());

	        mSCPMetrics.mEnvelopeEmit.mark();
	        mApp.getOverlayManager().broadcastMessage(m, true);
	    }
	}

	void startRebroadcastTimer()
	{
	    mRebroadcastTimer.expires_from_now(Duration.ofSeconds(2));

	    mRebroadcastTimer.async_wait(this::rebroadcast,
	                                 VirtualTimer::onFailureNoop);
	}

	TransactionSubmitStatus recvTransaction(TransactionFrame tx)
	{
//TODO	    soci::transaction sqltx(mApp.getDatabase().getSession());
//	    mApp.getDatabase().setCurrentTransactionReadOnly();

	    AccountID acc = tx.getSourceID();
	    Hash txID = tx.getFullHash();

	    // determine if we have seen this tx before and if not if it has the right
	    // seq num
	    Int64 totFee = tx.getFee().toInt64();
	    SequenceNumber highSeq = SequenceNumber.of(0);

	    for (HashMap<AccountID,TxMap> map : mPendingTransactions)
	    {
	    	TxMap txmap = map.get(acc);
	        if (txmap != null)
	        {
	            TransactionFrame j = txmap.getTransactions().get(txID);
	            if (j != null)
	            {
	                return TransactionSubmitStatus.TX_STATUS_DUPLICATE;
	            }
	            totFee = totFee.plus(txmap.getTotalFees());
	            highSeq = SequenceNumber.max(highSeq, txmap.getMaxSeq());
	        }
	    }

	    if (!tx.checkValid(mApp, highSeq))
	    {
	        return TransactionSubmitStatus.TX_STATUS_ERROR;
	    }

	    if (tx.getSourceAccount().getBalanceAboveReserve(mLedgerManager).lt(totFee))
	    {
	        tx.getResult().getResult().setDiscriminant(TransactionResultCode.txINSUFFICIENT_BALANCE);
	        return TransactionSubmitStatus.TX_STATUS_ERROR;
	    }

	    log.trace("Herder recv transaction {}  for {}",txID.hexAbbrev(), acc.getAccountID().toShortString());

	    TxMap txmap = findOrAdd(mPendingTransactions.getFirst()/*[0] ?? correct getFirst or getLast ??*/, acc);
	    txmap.addTx(tx);

	    return TransactionSubmitStatus.TX_STATUS_PENDING;
	}

	Herder.EnvelopeStatus recvSCPEnvelope(SCPEnvelope envelope)
	{
	    if (mApp.getConfig().MANUAL_CLOSE)
	    {
	        return Herder.EnvelopeStatus.ENVELOPE_STATUS_DISCARDED;
	    }

	        log.debug("Herder recvSCPEnvelope from: {} s:{} i:{} a:{}",
	        	envelope.getStatement().getNodeID().toShortString(),
	        	envelope.getStatement().getPledges().getDiscriminant().name(),
	        	envelope.getStatement().getSlotIndex(),
	        	mApp.getStateHuman());

	    if (envelope.getStatement().getNodeID().eq(mSCP.getLocalNode().getNodeID()))
	    {
	        log.debug("Herder recvSCPEnvelope: skipping own message");
	        return Herder.EnvelopeStatus.ENVELOPE_STATUS_DISCARDED;
	    }

	    mSCPMetrics.mEnvelopeReceive.mark();
	    Uint32 minLedgerSeq = getCurrentLedgerSeq();
	    if (minLedgerSeq.gt(MAX_SLOTS_TO_REMEMBER))
	    {
	        minLedgerSeq = minLedgerSeq.minus(MAX_SLOTS_TO_REMEMBER);
	    }

	    Uint32 maxLedgerSeq = Uint32.UINT32_MAX;

	    if (mTrackingSCP != null)
	    {
	        // when tracking, we can filter messages based on the information we got
	        // from consensus for the max ledger

	        // note that this filtering will cause a node on startup
	        // to potentially drop messages outside of the bracket
	        // causing it to discard CONSENSUS_STUCK_TIMEOUT_SECONDS worth of
	        // ledger closing
	        maxLedgerSeq = nextConsensusLedgerIndex().plus(LEDGER_VALIDITY_BRACKET);
	    }

	    // If envelopes are out of our validity brackets, we just ignore them.
	    if (envelope.getStatement().getSlotIndex().gt(maxLedgerSeq) ||
	        envelope.getStatement().getSlotIndex().lt(minLedgerSeq))
	    {
	        log.debug("Herder Ignoring SCPEnvelope outside of range: {} ( {}, {} )",
	        		envelope.getStatement().getSlotIndex().toString(),minLedgerSeq.toString(),maxLedgerSeq.toString());
	        return Herder.EnvelopeStatus.ENVELOPE_STATUS_DISCARDED;
	    }

	    Herder.EnvelopeStatus status = mPendingEnvelopes.recvSCPEnvelope(envelope);
	    if (status == Herder.EnvelopeStatus.ENVELOPE_STATUS_READY)
	    {
	        processSCPQueue();
	    }
	    return status;
	}

	void sendSCPStateToPeer(Uint32 ledgerSeq, Peer peer)
	{
	    Uint32 minSeq;
	    Uint32 maxSeq;

	    if (ledgerSeq.eqZero())
	    {
	        Uint32 nbLedgers = Uint32.ofPositiveInt(3);
	        Uint32 minLedger = Uint32.ofPositiveInt(2);

	        // include the most recent slot
	        maxSeq = getCurrentLedgerSeq().plus(1);

	        if (maxSeq.gte(minLedger.plus(nbLedgers)))
	        {
	            minSeq = maxSeq.minus(nbLedgers);
	        }
	        else
	        {
	            minSeq = minLedger;
	        }
	    }
	    else
	    {
	        minSeq = ledgerSeq;
	        maxSeq = ledgerSeq;
	    }

	    // use uint64_t for seq to prevent overflows
	    for (Uint64 seq = minSeq.toUint64(); seq.lte(maxSeq.toUint64()); seq = seq.plus(1))
	    {
	        List<SCPEnvelope> envelopes = mSCP.getCurrentState(seq);

	        if (envelopes.size() != 0)
	        {
	            log.debug("Herder Send state {} for ledger {}",envelopes.size(), seq);

	            for (SCPEnvelope e : envelopes)
	            {
	                StellarMessage m = new StellarMessage();
	                m.setDiscriminant(MessageType.SCP_MESSAGE);
	                m.setEnvelope(e);
	                peer.sendMessage(m);
	            }
	        }
	    }
	}

	void processSCPQueue()
	{
	    if (mTrackingSCP != null)
	    {
	        // drop obsolete slots
	        if (nextConsensusLedgerIndex().gt(MAX_SLOTS_TO_REMEMBER))
	        {
	            mPendingEnvelopes.eraseBelow(nextConsensusLedgerIndex().minus(MAX_SLOTS_TO_REMEMBER));
	        }

	        processSCPQueueUpToIndex(nextConsensusLedgerIndex());
	    }
	    else
	    {
	        // we don't know which ledger we're in
	        // try to consume the messages from the queue
	        // starting from the smallest slot
	        for (Uint64 slot : mPendingEnvelopes.readySlots())
	        {
	            processSCPQueueUpToIndex(slot);
	            if (mTrackingSCP != null)
	            {
	                // one of the slots externalized
	                // we go back to regular flow
	                break;
	            }
	        }
	    }
	}

	void processSCPQueueUpToIndex(Uint64 slotIndex)
	{
	    while (true)
	    {
	        SCPEnvelope env = mPendingEnvelopes.pop(slotIndex);
	        if (env != null)
	        {
	            mSCP.receiveEnvelope(env);
	        }
	        else
	        {
	            return;
	        }
	    }
	}

	void ledgerClosed()
	{
	    mTriggerTimer.cancel();

	    updateSCPCounters();
	    log.trace("Herder HerderImpl::ledgerClosed");

	    mPendingEnvelopes.slotClosed(lastConsensusLedgerIndex());

	    mApp.getOverlayManager().ledgerClosed(lastConsensusLedgerIndex());

	    Uint64 nextIndex = nextConsensusLedgerIndex();

	    // process any statements up to this slot (this may trigger externalize)
	    processSCPQueueUpToIndex(nextIndex);

	    // if externalize got called for a future slot, we don't
	    // need to trigger (the now obsolete) next round
	    if (nextIndex.neq(nextConsensusLedgerIndex()))
	    {
	        return;
	    }

	    // If we are not a validating node and just watching SCP we don't call
	    // triggerNextLedger. Likewise if we are not in synced state.
	    if (!mSCP.isValidator())
	    {
	        log.debug("Herder Non-validating node, not triggering ledger-close.");
	        return;
	    }

	    if (!mLedgerManager.isSynced())
	    {
	    	log.debug("Herder Not presently synced, not triggering ledger-close.");
	        return;
	    }

	    Duration seconds = EXP_LEDGER_TIMESPAN_SECONDS;
	    if (mApp.getConfig().ARTIFICIALLY_ACCELERATE_TIME_FOR_TESTING)
	    {
	        seconds = Duration.ofSeconds(1);
	    }
	    if (mApp.getConfig().ARTIFICIALLY_SET_CLOSE_TIME_FOR_TESTING.gt(0))
	    {
	        seconds = Duration.ofSeconds(
	            mApp.getConfig().ARTIFICIALLY_SET_CLOSE_TIME_FOR_TESTING.longValue());
	    }

	    Instant now = mApp.getClock().now();
	    Duration dur = Duration.between(mLastTrigger, now);
	    if (dur.compareTo(seconds) < 0)//cpp (now - mLastTrigger) < seconds
	    {
	    	Duration timeout = seconds.minus(dur);
	        mTriggerTimer.expires_from_now(timeout);
	    }
	    else
	    {
	        mTriggerTimer.expires_from_now(Duration.ofNanos(0));
	    }

	    if (!mApp.getConfig().MANUAL_CLOSE)
	        mTriggerTimer.async_wait(()-> triggerNextLedger(nextIndex),
	                                 VirtualTimer::onFailureNoop);
	}

	void removeReceivedTxs(List<TransactionFrame> dropTxs)
	{
	    for (HashMap<AccountID, TxMap> m : mPendingTransactions)
	    {
	        if (m.isEmpty())
	        {
	            continue;
	        }

	        Set<TxMap> toRecalculate = new HashSet<>();

	        for (TransactionFrame tx : dropTxs)
	        {
	            AccountID acc = tx.getSourceID();
	            Hash txID = tx.getFullHash();
	            TxMap txm = m.get(acc);
	            if (txm != null)
	            {
	                HashMap<Hash,TransactionFrame> txs = txm.getTransactions();
	                TransactionFrame j = txs.get(txID);
	                if (j != null)
	                {
	                    txs.remove(txID);
	                    if (txs.isEmpty())
	                    {
	                        m.remove(acc);
	                    }
	                    else
	                    {
	                        toRecalculate.add(txm);
	                    }
	                }
	            }
	        }

	        for (TxMap txm : toRecalculate)
	        {
	            txm.recalculate();
	        }
	    }
	}

	boolean recvSCPQuorumSet(Hash hash, SCPQuorumSet qset)
	{
	    return mPendingEnvelopes.recvSCPQuorumSet(hash, qset);
	}

	boolean recvTxSet(Hash hash, TxSetFrame t)
	{
	    TxSetFrame txset = new TxSetFrame(t);
	    return mPendingEnvelopes.recvTxSet(hash, txset);
	}

	void peerDoesntHave(MessageType type, Uint256 itemID,
	                           Peer peer)
	{
	    mPendingEnvelopes.peerDoesntHave(type, itemID, peer);
	}

	TxSetFrame getTxSet(Hash hash)
	{
	    return mPendingEnvelopes.getTxSet(hash);
	}


	Uint32 getCurrentLedgerSeq()
	{
	    Uint32 res = mLedgerManager.getLastClosedLedgerNum();

	    if (mTrackingSCP != null && res.lt(mTrackingSCP.getConsensusIndex()))
	    {
	        res = mTrackingSCP.getConsensusIndex().toUint32();
	    }
	    if (mLastTrackingSCP != null && res.lt(mLastTrackingSCP.getConsensusIndex()))
	    {
	        res = mLastTrackingSCP.getConsensusIndex().toUint32();
	    }
	    return res;
	}

	SequenceNumber getMaxSeqInPendingTxs(AccountID acc)
	{
	    SequenceNumber highSeq = SequenceNumber.of(0);
	    for (HashMap<AccountID,TxMap> m : mPendingTransactions)
	    {
	    	TxMap txm = m.get(acc);
	        if (txm == null)
	        {
	            continue;
	        }
	        highSeq = SequenceNumber.max(txm.getMaxSeq(), highSeq);
	    }
	    return highSeq;
	}

	// called to take a position during the next round
	// uses the state in LedgerManager to derive a starting position
	void triggerNextLedger(Uint32 ledgerSeqToTrigger)
	{
	    if (mTrackingSCP != null || ! mLedgerManager.isSynced())
	    {
	        log.debug("Herder triggerNextLedger: skipping (out of sync) : {}",mApp.getStateHuman());
	        return;
	    }
	    updateSCPCounters();

	    // our first choice for this round's set is all the tx we have collected
	    // during last ledger close
	    LedgerHeaderHistoryEntry lcl = mLedgerManager.getLastClosedLedgerHeader();
	    TxSetFrame proposedSet = new TxSetFrame(lcl.getHash());

	    for (HashMap<AccountID,TxMap> m : mPendingTransactions)
	    {
	        for (TxMap txm : m.values())
	        {
	            for (TransactionFrame tx : txm.getTransactions().values())
	            {
	                proposedSet.add(tx);
	            }
	        }
	    }

	    List<TransactionFrame> removed = new LinkedList<>();
	    proposedSet.trimInvalid(mApp, removed);
	    removeReceivedTxs(removed);

	    proposedSet.surgePricingFilter(mLedgerManager);

	    if (!proposedSet.checkValid(mApp))
	    {
	        throw new RuntimeException("wanting to emit an invalid txSet");
	    }

	    Hash txSetHash = proposedSet.getContentsHash();

	    // use the slot index from ledger manager here as our vote is based off
	    // the last closed ledger stored in ledger manager
	    Uint32 slotIndex = lcl.getHeader().getLedgerSeq().plus(1);

	    // Inform the item fetcher so queries from other peers about his txSet
	    // can be answered. Note this can trigger SCP callbacks, externalize, etc
	    // if we happen to build a txset that we were trying to download.
	    mPendingEnvelopes.addTxSet(txSetHash, slotIndex, proposedSet);

	    // no point in sending out a prepare:
	    // externalize was triggered on a more recent ledger
	    if (ledgerSeqToTrigger.ne(slotIndex))
	    {
	        return;
	    }

	    // We store at which time we triggered consensus
	    mLastTrigger = mApp.getClock().now();

	    // We pick as next close time the current time unless it's before the last
	    // close time. We don't know how much time it will take to reach consensus
	    // so this is the most appropriate value to use as closeTime.
	    Uint64 nextCloseTime = VirtualClock.to_time_t(mLastTrigger);
	    if (nextCloseTime.lte(lcl.getHeader().getScpValue().getCloseTime()))
	    {
	        nextCloseTime = lcl.getHeader().getScpValue().getCloseTime().plus(1);
	    }

	    StellarValue newProposedValue = new StellarValue(txSetHash, nextCloseTime, LedgerCloseData.emptyUpgradeSteps,
	                                  0);

	    // see if we need to include some upgrades
	    List<LedgerUpgrade> upgrades = prepareUpgrades(lcl.getHeader());

	    for (LedgerUpgrade upgrade : upgrades)
	    {
	        Value v = Value.decode(upgrade.encode());
	        if (UpgradeType.max_size().lt(v.size()))
	        {
	            log.error("Herder HerderImpl::triggerNextLedger exceeded size for upgrade step (got {}) for upgrade type {}",
	                                  v.size(), upgrade.getDiscriminant().name());
	        }
	        else
	        {
	            newProposedValue.setUpgrades(Vectors.emplace_back(newProposedValue.getUpgrades(), UpgradeType.decode(v.getValue())));//TODO check correctness
	        }
	    }

	    mCurrentValue = newProposedValue.toOpaqueValue();
	    mLedgerSeqNominating = slotIndex;

	    Uint256 valueHash = Uint256.of(CryptoUtils.sha256().apply(mCurrentValue.getValue()));
	    log.debug("Herder HerderImpl::triggerNextLedger txSet.size: {} previousLedgerHash: {} value: {} slot: {}", 
	    	proposedSet.getTransactions().size(), proposedSet.previousLedgerHash().hexAbbrev(), valueHash.hexAbbrev(),slotIndex.toString());

	    Value prevValue = lcl.getHeader().getScpValue().toOpaqueValue();

	    mSCP.nominate(slotIndex.toUint64(), mCurrentValue, prevValue);
	}

	List<LedgerUpgrade> prepareUpgrades(LedgerHeader header)
	{
		List<LedgerUpgrade> result = new LinkedList<>();

	    if (header.getLedgerVersion().ne(mApp.getConfig().LEDGER_PROTOCOL_VERSION))
	    {
	        boolean timeForUpgrade =
	            !mApp.getConfig().PREFERRED_UPGRADE_DATETIME.isPresent() ||
	            mApp.getConfig().PREFERRED_UPGRADE_DATETIME.get().isBefore(
	                mApp.getClock().now());//TODO or same
	        if (timeForUpgrade)
	        {
	        	LedgerUpgrade lu = new LedgerUpgrade();
	        	lu.setDiscriminant(LedgerUpgradeType.LEDGER_UPGRADE_VERSION);
	        	lu.setNewLedgerVersion(mApp.getConfig().LEDGER_PROTOCOL_VERSION);
	            result.add(lu);
	        }
	    }
	    if (header.getBaseFee().ne(mApp.getConfig().DESIRED_BASE_FEE))
	    {
        	LedgerUpgrade lu = new LedgerUpgrade();
        	lu.setDiscriminant(LedgerUpgradeType.LEDGER_UPGRADE_BASE_FEE);
        	lu.setNewLedgerVersion(mApp.getConfig().DESIRED_BASE_FEE);
            result.add(lu);
	    }
	    if (header.getMaxTxSetSize().ne(mApp.getConfig().DESIRED_MAX_TX_PER_LEDGER))
	    {
        	LedgerUpgrade lu = new LedgerUpgrade();
        	lu.setDiscriminant(LedgerUpgradeType.LEDGER_UPGRADE_MAX_TX_SET_SIZE);
        	lu.setNewLedgerVersion(mApp.getConfig().DESIRED_MAX_TX_PER_LEDGER);
            result.add(lu);
	    }

	    return result;
	}

	@Nullable PublicKey resolveNodeID(String s)
	{
		@Nullable PublicKey retKey = mApp.getConfig().resolveNodeID(s);
	    if (retKey == null)
	    {
	        if (s.length() > 1 && s.charAt(0) == '@')
	        {
	            String arg = s.substring(1);
	            // go through SCP messages of the previous ledger
	            // (to increase the chances of finding the node)
	            Uint32 seq = getCurrentLedgerSeq();
	            if (seq.gt(2))
	            {
	                seq = seq.minus(1);
	            }
	            List<SCPEnvelope> envelopes = mSCP.getCurrentState(seq.toUint64());
	            for (SCPEnvelope e : envelopes)
	            {
	                String curK = KeyUtils.toStrKey(e.getStatement().getNodeID());
	                
	                if (curK.startsWith(arg))//TODO cpp curK.length()  curK.compare(0, arg.size(), arg)
	                {
	                    retKey = e.getStatement().getNodeID().getNodeID();
	                    break;
	                }
	            }
	        }
	    }
	    return retKey;
	}

	
	
	@Override
	public SCPQuorumSet getQSet(Hash qSetHash) {
	    return mPendingEnvelopes.getQSet(qSetHash);
	}

	@Override
	public void emitEnvelope(SCPEnvelope envelope) {
	    Uint64 slotIndex = envelope.getStatement().getSlotIndex();

        log.debug("Herder emitEnvelope s:{} i:{} a:{}", 
        	envelope.getStatement().getPledges().getDiscriminant().name(), slotIndex.toString(), mApp.getStateHuman());

    persistSCPState(slotIndex);

    broadcast(envelope);

    // this resets the re-broadcast timer
    startRebroadcastTimer();
	}

	
//	@Override
//	public Uint64 computeHashNode(Uint64 slotIndex, Value prev, boolean isPriority, int roundNumber, NodeID nodeID) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Uint64 computeValueHash(Uint64 slotIndex, Value prev, int roundNumber, Value value) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Value combineCandidates(Uint64 slotIndex, ValueSet candidates) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Override
	public void valueExternalized(Uint64 slotIndex, Value value) {
	    updateSCPCounters();
	    mSCPMetrics.mValueExternalize.mark();
	    Iterator<Entry<Uint64, Map<Integer,VirtualTimer>>> itr = mSCPTimers.entrySet().iterator();
	    while(itr.hasNext()) {
	    	if(itr.next().getKey().lte(slotIndex)) {
	    		itr.remove();
	    	} else {
	    		break;
	    	}
	    }

	    if (slotIndex.lte(getCurrentLedgerSeq()))
	    {
	        // externalize may trigger on older slots:
	        //  * when the current instance starts up
	        //  * when getting back in sync (a gap potentially opened)
	        // in both cases it's safe to just ignore those as we're already
	        // tracking a more recent state
	        log.debug("Herder Ignoring old ledger externalize {}", slotIndex);
	        return;
	    }

	    StellarValue b;
	    try
	    {
	        b = StellarValue.decode(value.getValue());
	    }
	    catch (Exception e)
	    {
	        // This may not be possible as all messages are validated and should
	        // therefore contain a valid StellarValue.
	        log.fatal("Herder HerderImpl::valueExternalized Externalized StellarValue malformed");
	        // no point in continuing as 'b' contains garbage at this point
	        Assert.abort();
	    }

	    Hash txSetHash = b.getTxSetHash();

	    log.debug("Herder HerderImpl::valueExternalized txSet: {}", txSetHash.hexAbbrev());

	    // log information from older ledger to increase the chances that
	    // all messages made it
	    if (slotIndex.gt(2))
	    {
	        logQuorumInformation(slotIndex.minus(2));
	    }

	    if (!mCurrentValue.isEmpty())
	    {
	        // stop nomination
	        // this may or may not be the ledger that is currently externalizing
	        // in both cases, we want to stop nomination as:
	        // either we're closing the current ledger (typical case)
	        // or we're going to trigger catchup from history
	        mSCP.stopNomination(mLedgerSeqNominating.toUint64());
	        mCurrentValue.clear();
	    }

	    if (mTrackingSCP == null)
	    {
	        stateChanged();
	    }

	    mTrackingSCP = new ConsensusData(slotIndex, b);

	    if (mLastTrackingSCP == null)
	    {
	        mLastTrackingSCP = new ConsensusData(mTrackingSCP);
	    }

	    trackingHeartBeat();

	    TxSetFrame externalizedSet = mPendingEnvelopes.getTxSet(txSetHash);

	    // trigger will be recreated when the ledger is closed
	    // we do not want it to trigger while downloading the current set
	    // and there is no point in taking a position after the round is over
	    mTriggerTimer.cancel();

	    // save the SCP messages in the database
	    saveSCPHistory(slotIndex);

	    // tell the LedgerManager that this value got externalized
	    // LedgerManager will perform the proper action based on its internal
	    // state: apply, trigger catchup, etc
	    LedgerCloseData ledgerData = new LedgerCloseData(lastConsensusLedgerIndex(), externalizedSet, b);
	    mLedgerManager.valueExternalized(ledgerData);

	    // perform cleanups
	    updatePendingTransactions(externalizedSet.getTransactions());//TODO ?? semantics ?? check

	    // Evict slots that are outside of our ledger validity bracket
	    if (slotIndex.gt(MAX_SLOTS_TO_REMEMBER.toUint64()))
	    {
	        mSCP.purgeSlots(slotIndex.minus(MAX_SLOTS_TO_REMEMBER.longValue()));
	    }

	    ledgerClosed();
	}

	@Override
	public void nominatingValue(Uint64 slotIndex, Value value) {
		        log.debug("Herder nominatingValue i: {} v: {}", slotIndex.toString(),getValueString(value));

		    if (!value.isEmpty())
		    {
		        mSCPMetrics.mNominatingValue.mark();
		    }
		}

	// Extra SCP methods overridden solely to increment metrics.
	@Override
	public void updatedCandidateValue(Uint64 slotIndex, Value value) {
		mSCPMetrics.mUpdatedCandidate.mark();

	}

	@Override
	public void startedBallotProtocol(Uint64 slotIndex, SCPBallot ballot) {
		mSCPMetrics.mStartBallotProtocol.mark();
	}

	@Override
	public void acceptedBallotPrepared(Uint64 slotIndex, SCPBallot ballot) {
		mSCPMetrics.mAcceptedBallotPrepared.mark();

	}

	@Override
	public void confirmedBallotPrepared(Uint64 slotIndex, SCPBallot ballot) {
		mSCPMetrics.mConfirmedBallotPrepared.mark();
	}

	@Override
	public void acceptedCommit(Uint64 slotIndex, SCPBallot ballot) {
		mSCPMetrics.mAcceptedCommit.mark();
	}

	@Override
	public void ballotDidHearFromQuorum(Uint64 slotIndex, SCPBallot ballot) {
		mSCPMetrics.mQuorumHeard.mark();

	}

//	@Override
//	public void setupTimer(Uint64 slotIndex, timerIDs timerID, long timeout, Runnable cb) {
//		// TODO Auto-generated method stub
//
//	}

	void dumpInfo(Map<String,Object> ret, int limit)
	{
	    ret.put("you", mApp.getConfig().toStrKey(mSCP.getSecretKey().getPublicKey()));

	    mSCP.dumpInfo(ret, limit);

	    mPendingEnvelopes.dumpInfo(ret, limit);
	}

	void dumpQuorumInfo(LinkedHashMap<String,Object> ret, NodeID id, boolean summary,
	                           Uint64 index)
	{
	    ret.put("node", mApp.getConfig().toStrKey(id));
	    LinkedHashMap<String,Object> slots = (LinkedHashMap<String,Object>)ret.get("slots");
	    if(slots == null) {
	    	slots = new LinkedHashMap<>();
	    }
	    mSCP.dumpQuorumInfo(slots, id, summary, index);
	}

	
	void persistSCPState(Uint64 slot)
	{
	    if (slot.lt(mLastSlotSaved))
	    {
	        return;
	    }

	    mLastSlotSaved = slot;

	    // saves SCP messages and related data (transaction sets, quorum sets)
	    List<SCPEnvelope> latestEnvs = new LinkedList<>();
	    LinkedHashMap<Hash, TxSetFrame> txSets = new LinkedHashMap<>();
	    LinkedHashMap<Hash, SCPQuorumSet> quorumSets = new LinkedHashMap<>();

	    for (SCPEnvelope e : mSCP.getLatestMessagesSend(slot))
	    {
	        latestEnvs.add(e);

	        // saves transaction sets referred by the statement
	        for (Hash h : getTxSetHashes(e))
	        {
	        	TxSetFrame txSet = mPendingEnvelopes.getTxSet(h);
	            if (txSet != null)
	            {
	                txSets.put(h, txSet);
	            }
	        }
	        Hash qsHash = Slot.getCompanionQuorumSetHashFromStatement(e.getStatement());
	        SCPQuorumSet qSet = mPendingEnvelopes.getQSet(qsHash);
	        if (qSet != null)
	        {
	            quorumSets.put(qsHash, qSet);
	        }
	    }

	    List<TransactionSet> latestTxSets = new LinkedList<>();
	    for (TxSetFrame it : txSets.values())
	    {
	    	TransactionSet ts = new TransactionSet();
	    	it.toXDR(ts);
	        latestTxSets.add(ts);
	    }

	    List<SCPQuorumSet> latestQSets = new LinkedList<>();
	    for (SCPQuorumSet it : quorumSets.values())
	    {
	        latestQSets.add(it);
	    }

	    byte[] latestSCPData =
	        Xdr.packObjects(latestEnvs, latestTxSets, latestQSets);
	    String scpState = Base64.getEncoder().encodeToString(latestSCPData);

	    mApp.getPersistentState().setState(PersistentState.Entry.kLastSCPData, scpState);
	}

	void restoreSCPState()
	{
	    // setup a sufficient state that we can participate in consensus
	    LedgerHeaderHistoryEntry lcl = mLedgerManager.getLastClosedLedgerHeader();
	    mTrackingSCP =
	        new ConsensusData(lcl.getHeader().getLedgerSeq().toUint64(), lcl.getHeader().getScpValue());

	    trackingHeartBeat();

	    // load saved state from database
	    String latest64 =
	        mApp.getPersistentState().getState(PersistentState.Entry.kLastSCPData);

	    if (latest64.isEmpty())
	    {
	        return;
	    }

	    byte[] buffer = Base64.getDecoder().decode(latest64);

	    List<SCPEnvelope> latestEnvs = new LinkedList<>();
	    List<TransactionSet> latestTxSets = new LinkedList<>();
	    List<SCPQuorumSet> latestQSets = new LinkedList<>();

	    try
	    {
	        Xdr.xdr_from_opaque(buffer, latestEnvs, latestTxSets, latestQSets);

	        for (TransactionSet txset : latestTxSets)
	        {
	            TxSetFrame cur =
	                new TxSetFrame(mApp.getNetworkID(), txset);
	            Hash h = cur.getContentsHash();
	            mPendingEnvelopes.addTxSet(h, Uint32.ZERO, cur);
	        }
	        for (SCPQuorumSet qset : latestQSets)
	        {
	            Hash hash = Hash.decode(CryptoUtils.sha256().apply(qset.encode()));
	            mPendingEnvelopes.addSCPQuorumSet(hash, Uint64.ZERO, qset);
	        }
	        for (SCPEnvelope e : latestEnvs)
	        {
	            mSCP.setStateFromEnvelope(e.getStatement().getSlotIndex(), e);
	        }

	        if (latestEnvs.size() != 0)
	        {
	            mLastSlotSaved = latestEnvs.get(latestEnvs.size() - 1).getStatement().getSlotIndex();
	            startRebroadcastTimer();
	        }
	    }
	    catch (Exception e)
	    {
	        // we may have exceptions when upgrading the protocol
	        // this should be the only time we get exceptions decoding old messages.
	        log.info("Herder Error while restoring old scp messages, proceeding without them :{} ", e.getMessage());
	    }
	}

	void trackingHeartBeat()
	{
	    if (mApp.getConfig().MANUAL_CLOSE)
	    {
	        return;
	    }

	    assert(mTrackingSCP != null);
	    mTrackingTimer.expires_from_now(
	        CONSENSUS_STUCK_TIMEOUT_SECONDS);
	    mTrackingTimer.async_wait(this::herderOutOfSync,
	                              VirtualTimer::onFailureNoop);
	}

	void updatePendingTransactions(
	    List<TransactionFrame> applied)
	{
	    // remove all these tx from mPendingTransactions
	    removeReceivedTxs(applied);

	    // drop the highest level
	    mPendingTransactions.remove(mPendingTransactions.size() - 1);

	    // shift entries up
	    mPendingTransactions.addFirst(new HashMap<AccountID,TxMap>());

	    // rebroadcast entries, sorted in apply-order to maximize chances of
	    // propagation
	    {
	        Hash h = Hash.createEmpty();
	        TxSetFrame toBroadcast = new TxSetFrame(h);
	        for (HashMap<AccountID,TxMap> l : mPendingTransactions)
	        {
	            for (TxMap txm : l.values())
	            {
	                for (TransactionFrame tx : txm.getTransactions().values())
	                {
	                    toBroadcast.add(tx);
	                }
	            }
	        }
	        for (TransactionFrame tx : toBroadcast.sortForApply())
	        {
	        	StellarMessage msg = tx.toStellarMessage();
	            mApp.getOverlayManager().broadcastMessage(msg);
	        }
	    }

	    //TODO check npes
//	    mSCPMetrics.mHerderPendingTxs0.set_count(countTxs(mPendingTransactions[0]));
//	    mSCPMetrics.mHerderPendingTxs1.set_count(countTxs(mPendingTransactions[1]));
//	    mSCPMetrics.mHerderPendingTxs2.set_count(countTxs(mPendingTransactions[2]));
//	    mSCPMetrics.mHerderPendingTxs3.set_count(countTxs(mPendingTransactions[3]));
	}
	void herderOutOfSync()
	{
	    log.info("Herder Lost track of consensus");

	    LinkedHashMap<String, Object> v = new LinkedHashMap<>();
	    dumpInfo(v, 20);
	    String s = mApp.toJsonString(v);
	    log.info("Herder Out of sync context: {}", s);

	    mSCPMetrics.mLostSync.mark();
	    stateChanged();

	    // transfer ownership to mLastTrackingSCP
	    mLastTrackingSCP = mTrackingSCP;
	    mTrackingSCP = null;

	    processSCPQueue();

	}

	//TODO longValue() unsigned 64
	void saveSCPHistory(Uint64 index)
	{
	    Uint32 seq = index.toUint32();
	    //TODO ???
	    List<SCPEnvelope> envs = mSCP.getExternalizingState(seq.toUint64());
	    if (! envs.isEmpty())
	    {
	        HashMap<Hash, SCPQuorumSet> usedQSets = new HashMap<>();

	        JdbcTemplate db = mApp.getDatabase();
	        TransactionTemplate tt = mApp.getTransactionTemplate();
	        tt.execute((TransactionStatus status) -> {
		        //cpp soci::transaction txscope(db.getSession());
	                //TODO auto timer = db.getDeleteTimer("scphistory");
		        	db.update("DELETE FROM scphistory WHERE ledgerseq = ?", seq.longValue());
		        for (SCPEnvelope e : envs)
		        {
		            Hash qHash =
		                Slot.getCompanionQuorumSetHashFromStatement(e.getStatement());
		            usedQSets.put(qHash, getQSet(qHash));

		            String nodeIDStrKey = KeyUtils.toStrKey(e.getStatement().getNodeID());

		            byte[] envelopeBytes = e.encode();

		            String envelopeEncoded = Base64.getEncoder().encodeToString(envelopeBytes);
	            	//TODO auto timer = db.getInsertTimer("scphistory");
		            int affectedRows = db.update("INSERT INTO scphistory (nodeid, ledgerseq, envelope) VALUES (?, ?, ?)", nodeIDStrKey, seq.longValue(),envelopeEncoded);
		            if (affectedRows != 1)
		            {
		                throw new RuntimeException("Could not update data in SQL");//TODO not needed
		            }
		        }

		        for (Entry<Hash, SCPQuorumSet> p : usedQSets.entrySet())
		        {
		            String qSetH = p.getKey().toHex();
	            	//TODO auto timer = db.getInsertTimer("scpquorums");
		            int affectedRows = db.update("UPDATE scpquorums SET lastledgerseq = ? WHERE qsethash = ?", seq.longValue(), qSetH);
		            if (affectedRows != 1)
		            {
		                byte[]  qSetBytes = p.getValue().encode();

		                String qSetEncoded = Base64.getEncoder().encodeToString(qSetBytes);
		              //TODO auto timer = db.getInsertTimer("scpquorums");
		                int affectedRows2 = db.update("INSERT INTO scpquorums (qsethash, lastledgerseq, qset) VALUES (?,?,?)",qSetH,seq.longValue(),qSetEncoded);
		                if (affectedRows2 != 1)
		                {
		                    throw new RuntimeException("Could not update data in SQL");
		                }
		            }
		        }

		        //txscope.commit();
	        	return null;
	        });
	    }
	}

	//TODO longValue() unsigned 64
	int copySCPHistoryToStream(Database db, TransactionTemplate sess,
	                               Uint32 ledgerSeq, Uint32 ledgerCount,
	                               XDROutputFileStream scpHistory)
	{
	    Uint32 begin = ledgerSeq;
	    Uint32 end = ledgerSeq.plus(ledgerCount);
	    int n = 0;

	    // all known quorum sets
	    HashMap<Hash, SCPQuorumSet> qSets = new HashMap<>();

	    for (Uint32 curLedgerSeq = begin; curLedgerSeq.lt(end); curLedgerSeq = curLedgerSeq.plus(1))
	    {
	        // SCP envelopes for this ledger
	        // quorum sets missing in this batch of envelopes
	        Set<Hash> missingQSets = new HashSet<>();
//TODO init

	        // fetch SCP messages from history
	        SCPEnvelope[] curEnvs = new SCPEnvelope[0];
	        {

	            //TODO auto timer = db.getSelectTimer("scphistory");
	        	JdbcTemplate tpl = db.getJdbcTemplate();
	            List<String> envB64s = tpl.queryForList("SELECT envelope FROM scphistory WHERE ledgerseq = ? ORDER BY nodeid", String.class, curLedgerSeq.longValue());
	            for(String envB64 : envB64s)
	            {
	            	byte[] barr = Base64.getDecoder().decode(envB64);
	            	
	                SCPEnvelope env = SCPEnvelope.decode(barr);
	                curEnvs = Vectors.emplace_back(curEnvs, env);


	                // record new quorum sets encountered
	                Hash qSetHash =
	                    Slot.getCompanionQuorumSetHashFromStatement(env.getStatement());
	                if (! qSets.containsKey(qSetHash))
	                {
	                    missingQSets.add(qSetHash);
	                }
	            }
	        }
	        LedgerSCPMessages lm = new LedgerSCPMessages();
	        lm.setLedgerSeq(curLedgerSeq);
	        lm.setMessages(curEnvs);
	        
	        SCPQuorumSet[] quorumSets = new SCPQuorumSet[0];
	        // fetch the quorum sets from the db
	        for (Hash q : missingQSets)
	        {
	            String qset64;
	            String qSetHashHex;


	            qSetHashHex = q.toHex();

	            //TODO auto timer = db.getSelectTimer("scpquorums");
	            
	            soci::statement st = (sess.prepare << "SELECT qset FROM scpquorums "
	                                                  "WHERE qsethash = :h",
	                                  into(qset64), use(qSetHashHex));

	            st.execute(true);

	            if (!st.got_data())
	            {
	                throw std::runtime_error(
	                    "corrupt database state: missing quorum set");
	            }

	            std::vector<uint8_t> qSetBytes;
	            bn::decode_b64(qset64, qSetBytes);

	            xdr::xdr_get g1(&qSetBytes.front(), &qSetBytes.back() + 1);
	            xdr_argpack_archive(g1, qset);
	            Vectors.emplace_back(quorumSets, qset);
	        }

	        if (curEnvs.size() != 0)
	        {
	            scpHistory.writeOne(hEntryV);
	        }
	    }
        SCPHistoryEntryV0 hEntry = new SCPHistoryEntryV0();
        hEntry.setLedgerMessages(lm);
        hEntry.setQuorumSets(quorumSets);
        SCPHistoryEntry hEntryV = new SCPHistoryEntry();
        hEntryV.setDiscriminant(0);;
        hEntryV.setV0(hEntry);

	    return n;
	}

}