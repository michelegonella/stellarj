package com.consumimurigni.stellarj.herder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.codahale.metrics.Counter;
import com.consumimurigni.stellarj.herder.Herder.EnvelopeStatus;
import com.consumimurigni.stellarj.main.Application;
import com.consumimurigni.stellarj.scp.QuorumSetUtils;
import com.consumimurigni.stellarj.scp.SCP;
import com.consumimurigni.stellarj.scp.Slot;
import com.consuminurigni.stellarj.common.Tuple2;
import com.consuminurigni.stellarj.overlay.ItemFetcher;
import com.consuminurigni.stellarj.overlay.Peer;
import com.consuminurigni.stellarj.overlay.xdr.MessageType;
import com.consuminurigni.stellarj.overlay.xdr.StellarMessage;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.NodeID;
import com.consuminurigni.stellarj.xdr.Uint256;
import com.consuminurigni.stellarj.xdr.Uint32;
import com.consuminurigni.stellarj.xdr.Uint64;

public class PendingEnvelopes {
	private static final int QSET_CACHE_SIZE = 10000;
	private static final int TXSET_CACHE_SIZE = 10000;
	private static final int NODES_QUORUM_CACHE_SIZE = 1000;

	private class SlotEnvelopes
	{
	    // list of envelopes we have processed already
		LinkedList<SCPEnvelope> mProcessedEnvelopes = new LinkedList<>();
	    // list of envelopes we have discarded already
		TreeSet<SCPEnvelope> mDiscardedEnvelopes = new TreeSet<>();
	    // list of envelopes we are fetching right now
		TreeSet<SCPEnvelope> mFetchingEnvelopes = new TreeSet<>();
	    // list of ready envelopes that haven't been sent to SCP yet
	    LinkedList<SCPEnvelope> mReadyEnvelopes = new LinkedList<>();
	};

	private static final Logger log = LogManager.getLogger();
    private final Application mApp;
    private final Herder mHerder;

    
    // ledger# and list of envelopes in various states
    private final TreeMap<Uint64, SlotEnvelopes> mEnvelopes = new TreeMap<>();

    //using SCPQuorumSetCacheItem = std::pair<uint64, SCPQuorumSetPtr>;
    // all the quorum sets we have learned about
    private final HashMap<Hash, Tuple2<Uint64, SCPQuorumSet>> mQsetCache;//TODO cache::lru_cache<Hash, SCPQuorumSetCacheItem> mQsetCache;

    private final ItemFetcher mTxSetFetcher;
    private final ItemFetcher mQuorumSetFetcher;

    //using TxSetFramCacheItem = std::pair<uint64, TxSetFramePtr>;
    // all the txsets we have learned about per ledger#
    private final HashMap<Hash, Tuple2<Uint64, TxSetFrame>> mTxSetCache;//TODO cache::lru_cache<Hash, TxSetFramCacheItem> mTxSetCache;

    // NodeIDs that are in quorum
    //TODO cache::lru_cache<NodeID, bool> mNodesInQuorum;
    private final HashMap<NodeID, Boolean> mNodesInQuorum;

    private final Counter mReadyEnvelopesSize;

	public PendingEnvelopes(Application app, Herder /*TODO*/ herder) {
		this.mApp = app;
		this.mHerder = herder;
	    this.mQsetCache = new HashMap<>(QSET_CACHE_SIZE);
	    this.mTxSetFetcher = new ItemFetcher(app, (peer, hash)-> peer.sendGetTxSet(hash));
	    this.mQuorumSetFetcher = new ItemFetcher(app, (peer, hash)-> peer.sendGetQuorumSet(hash));
	    this.mTxSetCache = new HashMap<>(TXSET_CACHE_SIZE);
	    this.mNodesInQuorum = new HashMap<>(NODES_QUORUM_CACHE_SIZE);
	    this.mReadyEnvelopesSize = 
	          app.newCounter("scp", "memory", "pending-envelopes");

	}

	public void addSCPQuorumSet(Hash hash, Uint64 lastSeenSlotIndex, SCPQuorumSet q) {
	    assert(QuorumSetUtils.isQuorumSetSane(q, false));

	    log.trace("Herder Add SCPQSet {}",hash.hexAbbrev());

	    SCPQuorumSet qset = new SCPQuorumSet(q);
	    mQsetCache.put(hash, new Tuple2<>(lastSeenSlotIndex, qset));
	    mQuorumSetFetcher.recv(hash);
	}

	public @Nullable TxSetFrame getTxSet(Hash hash) {
	    if (mTxSetCache.containsKey(hash))
	    {
	        return mTxSetCache.get(hash).get1();
	    }

	    return null;//TODO cpp TxSetFramePtr();
	}

	public boolean recvTxSet(Hash hash, TxSetFrame txset) {
	    log.trace("Herder Got TxSet {}", hash.hexAbbrev());

	    Uint64 lastSeenSlotIndex = mTxSetFetcher.getLastSeenSlotIndex(hash);
	    if (lastSeenSlotIndex.eq(0))
	    {
	        return false;
	    }

	    addTxSet(hash, lastSeenSlotIndex.toUint32(), txset);//TODO overflow risk ??
	    return true;
	}

	boolean isNodeInQuorum(NodeID node)
	{
	    boolean res = mNodesInQuorum.containsKey(node);
	    if (res)
	    {
	        res = mNodesInQuorum.get(node);
	    }

	    if (!res)
	    {
	        // search through the known slots
	        SCP.TriBool r = mHerder.getSCP().isNodeInQuorum(node);
	        if (r == SCP.TriBool.TB_TRUE)
	        {
	            // only cache positive answers
	            // so that nodes can be added during rounds
	            mNodesInQuorum.put(node, true);
	            res = true;
	        }
	        else if (r == SCP.TriBool.TB_FALSE)
	        {
	            res = false;
	        }
	        else
	        {
	            // MAYBE -> return true, but don't cache
	            res = true;
	        }
	    }

	    return res;
	}

	// called from Peer and when an Item tracker completes
	public EnvelopeStatus recvSCPEnvelope(SCPEnvelope envelope) {
		    NodeID nodeID = envelope.getStatement().getNodeID();
		    if (!isNodeInQuorum(nodeID))
		    {
		        log.debug("Herder Dropping envelope from {} (not in quorum)", mApp.getConfig().toShortString(nodeID));
		        return Herder.EnvelopeStatus.ENVELOPE_STATUS_DISCARDED;
		    }

		    // did we discard this envelope?
		    // do we already have this envelope?
		    // do we have the qset
		    // do we have the txset

		    try
		    {
		        if (isDiscarded(envelope))
		        {
		            return Herder.EnvelopeStatus.ENVELOPE_STATUS_DISCARDED;
		        }

		        touchFetchCache(envelope);

		        Set<SCPEnvelope> set = mEnvelopes.get(envelope.getStatement().getSlotIndex()).mFetchingEnvelopes;
		        List<SCPEnvelope> processedList =
		            mEnvelopes.get(envelope.getStatement().getSlotIndex()).mProcessedEnvelopes;

		        boolean fetching = set.contains(envelope);

		        if (! fetching)
		        { // we aren't fetching this envelope
		            if (! processedList.contains(envelope))
		            { // we haven't seen this envelope before
		                // insert it into the fetching set
		                fetching = set.add(envelope);//always true...
		                startFetch(envelope);
		            }
		            else
		            {
		                // we already have this one
		                return Herder.EnvelopeStatus.ENVELOPE_STATUS_PROCESSED;
		            }
		        }

		        // we are fetching this envelope
		        // check if we are done fetching it
		        if (isFullyFetched(envelope))
		        {
		            // move the item from fetching to processed
		            processedList.add(envelope);
		            set.remove(envelope);//TODO asert true... ??
		            envelopeReady(envelope);
		            return Herder.EnvelopeStatus.ENVELOPE_STATUS_READY;
		        } // else just keep waiting for it to come in

		        return Herder.EnvelopeStatus.ENVELOPE_STATUS_FETCHING;
		    }
		    catch (RuntimeException e)
		    {
		        log.trace("Herder PendingEnvelopes::recvSCPEnvelope got corrupt message: {}", e.getMessage());
		        return Herder.EnvelopeStatus.ENVELOPE_STATUS_DISCARDED;
		    }
		}

	public void eraseBelow(Uint64 slotIndex) {
		Iterator<Entry<Uint64, SlotEnvelopes>> itr = mEnvelopes.entrySet().iterator();
	    while (itr.hasNext())
	    {
	        if (itr.next().getKey().lt(slotIndex))
	        {
	            itr.remove();
	        }
	        else {
	            break;
	        }
	    }

	    //TODO works On LinkedHashMAps replace apropriately with the selected cache
	    // 0 is special mark for data that we do not know the slot index
	    // it is used for state loaded from database
	    Iterator<Entry<Hash, Tuple2<Uint64, SCPQuorumSet>>> qsetCacheItr = mQsetCache.entrySet().iterator();
	    while(qsetCacheItr.hasNext()) {
	    	Uint64 i = qsetCacheItr.next().getValue().get0();
	    	if(i.ne(0) && i.lt(slotIndex)) {
	    		qsetCacheItr.remove();
	    	}
	    }
	    Iterator<Entry<Hash, Tuple2<Uint64, TxSetFrame>>> txSetCacheItr = mTxSetCache.entrySet().iterator();
	    while(txSetCacheItr.hasNext()) {
	    	Uint64 i = txSetCacheItr.next().getValue().get0();
	    	if(i.ne(0) && i.lt(slotIndex)) {
	    		txSetCacheItr.remove();
	    	}
	    }
	}

	public List<Uint64> readySlots() {
		List<Uint64> result = new LinkedList<>();
		    for (Entry<Uint64, SlotEnvelopes> entry : mEnvelopes.entrySet())
		    {
		        if (!entry.getValue().mReadyEnvelopes.isEmpty())
		            result.add(entry.getKey());
		    }
		    return result;
		}

	public @Nullable SCPEnvelope pop(Uint64 slotIndex) {
		for(Entry<Uint64, SlotEnvelopes> e : mEnvelopes.entrySet()) {
			if(e.getKey().gt(slotIndex)) {
				break;
			}
			if(e.getValue().mReadyEnvelopes.size() > 0) {
				return e.getValue().mReadyEnvelopes.removeLast();
			}
		}
	    return null;
	}

	public void slotClosed(Uint64 slotIndex) {
	    // stop processing envelopes & downloads for the slot falling off the
	    // window
	    if (slotIndex.gt(Herder.MAX_SLOTS_TO_REMEMBER))
	    {
	        slotIndex = slotIndex.minus(Herder.MAX_SLOTS_TO_REMEMBER.toUint64());

	        mEnvelopes.remove(slotIndex);

	        mTxSetFetcher.stopFetchingBelow(slotIndex.plus(1));
	        mQuorumSetFetcher.stopFetchingBelow(slotIndex.plus(1));

		    //TODO works On LinkedHashMAps replace apropriately with the selected cache
		    Iterator<Entry<Hash, Tuple2<Uint64, SCPQuorumSet>>> qsetCacheItr = mQsetCache.entrySet().iterator();
		    while(qsetCacheItr.hasNext()) {
		    	Uint64 i = qsetCacheItr.next().getValue().get0();
		    	if(i.eq(slotIndex)) {
		    		qsetCacheItr.remove();
		    	}
		    }
		    Iterator<Entry<Hash, Tuple2<Uint64, TxSetFrame>>> txSetCacheItr = mTxSetCache.entrySet().iterator();
		    while(txSetCacheItr.hasNext()) {
		    	Uint64 i = txSetCacheItr.next().getValue().get0();
		    	if(i.eq(slotIndex)) {
		    		txSetCacheItr.remove();
		    	}
		    }
	    }

		
	}

	public boolean recvSCPQuorumSet(Hash hash, SCPQuorumSet q) {
	    log.trace("Herder Got SCPQSet {}", hash.hexAbbrev());

	    Uint64 lastSeenSlotIndex = mQuorumSetFetcher.getLastSeenSlotIndex(hash);
	    if (lastSeenSlotIndex.lte(Uint64.ZERO))
	    {
	        return false;
	    }

	    if (QuorumSetUtils.isQuorumSetSane(q, false))
	    {
	        addSCPQuorumSet(hash, lastSeenSlotIndex, q);
	        return true;
	    }
	    else
	    {
	        discardSCPEnvelopesWithQSet(hash);
	        return false;
	    }
	}

	void discardSCPEnvelopesWithQSet(Hash hash)
	{
	    log.trace("Herder Discarding SCP Envelopes with SCPQSet {}", hash.hexAbbrev());

	    List<SCPEnvelope> envelopes = mQuorumSetFetcher.fetchingFor(hash);
	    for (SCPEnvelope envelope : envelopes) {
	        discardSCPEnvelope(envelope);
	    }
	}

	void discardSCPEnvelope(SCPEnvelope envelope)
	{
	    try
	    {
	        if (isDiscarded(envelope))
	        {
	            return;
	        }

	        Set<SCPEnvelope> discardedSet = mEnvelopes.get(envelope.getStatement().getSlotIndex()).mDiscardedEnvelopes;
	        discardedSet.add(envelope);

	        Set<SCPEnvelope> fetchingSet =
	            mEnvelopes.get(envelope.getStatement().getSlotIndex()).mFetchingEnvelopes;
	        fetchingSet.remove(envelope);

	        stopFetch(envelope);
	    }
	    catch (RuntimeException e)
	    {
	        log.trace("Herder PendingEnvelopes::discardSCPEnvelope got corrupt message: {}", e.getMessage());
	    }
	}

	boolean isDiscarded(SCPEnvelope envelope)
	{
	    SlotEnvelopes envelopes = mEnvelopes.get(envelope.getStatement().getSlotIndex());
	    if (envelopes == null)
	    {
	        return false;
	    }

	    Set<SCPEnvelope> discardedSet = envelopes.mDiscardedEnvelopes;
	    boolean discarded = discardedSet.contains(envelope);
	    return discarded;
	}

	void envelopeReady(SCPEnvelope envelope)
	{
	    StellarMessage msg = new StellarMessage();
	    msg.setDiscriminant(MessageType.SCP_MESSAGE);
	    msg.setEnvelope(envelope);
	    mApp.getOverlayManager().broadcastMessage(msg);

	    mEnvelopes.get(envelope.getStatement().getSlotIndex()).mReadyEnvelopes.add(envelope);

	    log.trace("Herder Envelope ready i:{} t:{}", envelope.getStatement().getSlotIndex().toString(), envelope.getStatement().getPledges().getDiscriminant().name());
	}

	boolean isFullyFetched(SCPEnvelope envelope)
	{
	    if (!mQsetCache.containsKey(
	            Slot.getCompanionQuorumSetHashFromStatement(envelope.getStatement())))
	        return false;

	    List<Hash> txSetHashes = HerderUtils.getTxSetHashes(envelope);
	    return txSetHashes.stream().allMatch((txSetHash) ->  mTxSetCache.containsKey(txSetHash));
	}

	void startFetch(SCPEnvelope envelope)
	{
	    Hash h = Slot.getCompanionQuorumSetHashFromStatement(envelope.getStatement());

	    if (!mQsetCache.containsKey(h))
	    {
	        mQuorumSetFetcher.fetch(h, envelope);
	    }

	    for (Hash h2 : HerderUtils.getTxSetHashes(envelope))
	    {
	        if (!mTxSetCache.containsKey(h2))
	        {
	            mTxSetFetcher.fetch(h2, envelope);
	        }
	    }

	    log.trace("Herder StartFetch i:{} t:{}", envelope.getStatement().getSlotIndex().toString(), envelope.getStatement().getPledges().getDiscriminant().name());
	}

	void stopFetch(SCPEnvelope envelope)
	{
	    Hash h = Slot.getCompanionQuorumSetHashFromStatement(envelope.getStatement());
	    mQuorumSetFetcher.stopFetch(h, envelope);

	    for (Hash h2 : HerderUtils.getTxSetHashes(envelope))
	    {
	        mTxSetFetcher.stopFetch(h2, envelope);
	    }

	    log.trace("Herder StopFetch i:{} t:{}", envelope.getStatement().getSlotIndex().toString(), envelope.getStatement().getPledges().getDiscriminant().name());
	}

	void touchFetchCache(SCPEnvelope envelope)
	{
	    Hash qsetHash =
	        Slot.getCompanionQuorumSetHashFromStatement(envelope.getStatement());
	    if (mQsetCache.containsKey(qsetHash))
	    {
	        Tuple2<Uint64, SCPQuorumSet> item = mQsetCache.get(qsetHash);
	        if(envelope.getStatement().getSlotIndex().gt(item.get0())) {
	        	mQsetCache.put(qsetHash, new Tuple2<Uint64, SCPQuorumSet>(envelope.getStatement().getSlotIndex(), item.get1()));
	        }
	    }

	    for (Hash h : HerderUtils.getTxSetHashes(envelope))
	    {
	        if (mTxSetCache.containsKey(h))
	        {
	        	Tuple2<Uint64, TxSetFrame> item = mTxSetCache.get(qsetHash);
		        if(envelope.getStatement().getSlotIndex().gt(item.get0())) {
		        	mTxSetCache.put(qsetHash, new Tuple2<Uint64, TxSetFrame>(envelope.getStatement().getSlotIndex(), item.get1()));
		        }
	        }
	    }
	}

	public void peerDoesntHave(MessageType type, Uint256 itemID, Peer peer) {
	    switch (type)
	    {
	    case TX_SET:
	        mTxSetFetcher.doesntHave(itemID, peer);
	        break;
	    case SCP_QUORUMSET:
	        mQuorumSetFetcher.doesntHave(itemID, peer);
	        break;
	    default:
	        log.info("Herder Unknown Type in peerDoesntHave: {}", type);
	        break;
	    }
	}

	public @Nullable SCPQuorumSet getQSet(Hash hash) {
	    if (mQsetCache.containsKey(hash))
	    {
	        return mQsetCache.get(hash).get1();
	    }

	    return null;
	}

	public void addTxSet(Hash hash, Uint32 slotIndex, TxSetFrame txset) {
	    log.trace("Herder Add TxSet {}", hash.hexAbbrev());

	    mTxSetCache.put(hash, new Tuple2<>(slotIndex.toUint64(), txset));
	    mTxSetFetcher.recv(hash);
	}

	public void dumpInfo(LinkedHashMap<String, Object> ret, int limit) {
		LinkedHashMap<String, Object> q = new LinkedHashMap<>();
		ret.put("queue", q);

	    {
	        Iterator<Entry<Uint64,SlotEnvelopes>> it = mEnvelopes.entrySet().iterator();
	        int l = limit;
	        while (it.hasNext() && l-- != 0)
	        {
	        	Entry<Uint64,SlotEnvelopes> entry = it.next();
	        	Uint64 k = entry.getKey();
	        	SlotEnvelopes v = entry.getValue();
        		LinkedHashMap<String, Object> f = new LinkedHashMap<>();
        		q.put(k.toString(), f);

	            if (v.mFetchingEnvelopes.size() != 0)
	            {
	        		List<Object> fl = new LinkedList<>();
	        		f.put("fetching", fl);
	                for (SCPEnvelope e : v.mFetchingEnvelopes)
	                {
	                    fl.add(mHerder.getSCP().envToStr(e));
	                }
	            }
	            if (v.mReadyEnvelopes.size() != 0)
	            {
	        		List<Object> fl = new LinkedList<>();
	        		f.put("pending", fl);
	                for (SCPEnvelope e : v.mReadyEnvelopes)
	                {
	                    fl.add(mHerder.getSCP().envToStr(e));
	                }
	            }
	        }
	    }
	}

}
