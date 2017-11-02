package com.consuminurigni.stellarj.overlay;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.consumimurigni.stellarj.role.IPeer;
import com.consumimurigni.stellarj.scp.Herder;
import com.consuminurigni.stellarj.common.Tuple2;
import com.consuminurigni.stellarj.common.VirtualClock;
import com.consuminurigni.stellarj.metering.Counter;
import com.consuminurigni.stellarj.metering.Metrics;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.Uint256;
import com.consuminurigni.stellarj.xdr.Uint64;

/**
 * @class ItemFetcher
 *
 * Manages asking for Transaction or Quorum sets from Peers
 *
 * The ItemFetcher keeps instances of the Tracker class. There exists exactly
 * one Tracker per item. The tracker is used to maintain the state of the
 * search.
 */
public class ItemFetcher {
	private static final Logger log = LogManager.getLogger();
	private final SortedMap<Hash, Tracker> mTrackers = new TreeMap<>();
    // NB: There are many ItemFetchers in the system at once, but we are sharing
    // a single counter for all the items being fetched by all of them. Be
    // careful, therefore, to only increment and decrement this counter, not set
    // it absolutely.
    private final Counter mItemMapSize;
    private final AskPeer mAskPeer;
	private final OverlayManager overlayManager;
	private final VirtualClock virtualClock;
	private final Metrics metrics;
	private final Herder herder;
	
	public ItemFetcher(Herder herder, OverlayManager overlayManager, VirtualClock virtualClock, Metrics metrics, AskPeer askPeer) {
		this.herder = herder;
		this.overlayManager = overlayManager;
		this.virtualClock = virtualClock;
		this.metrics = metrics;
		this.mItemMapSize = metrics.newCounter("overlay", "memory", "item-fetch-map");
		this.mAskPeer = askPeer;
	}

	public void fetch(Hash itemHash, SCPEnvelope envelope) {
	    log.trace("Overlay fetch {} ", () -> itemHash.hexAbbrev());
	    if (!mTrackers.containsKey(itemHash))
	    { // not being tracked
	        Tracker tracker = new Tracker(overlayManager, virtualClock, metrics, itemHash, mAskPeer);
	        mTrackers.put(itemHash, tracker);
	        mItemMapSize.inc();
	        tracker.listen(envelope);
	        tracker.tryNextPeer();
	    }
	    else
	    {
	    	mTrackers.get(itemHash).listen(envelope);
	    }
	}

	public void stopFetch(Hash itemHash, SCPEnvelope envelope) {
	    log.trace("Overlay stopFetch {}", () -> itemHash.hexAbbrev());
	    Tracker tracker = mTrackers.get(itemHash);
	    if (tracker != null)
	    {

	        log.trace("Overlay stopFetch {} : {}", ()-> itemHash.hexAbbrev(), () -> tracker.size());
	        tracker.discard(envelope);
	        if (tracker.empty())
	        {
	            // stop the timer, stop requesting the item as no one is waiting for
	            // it
	            tracker.cancel();
	        }
	    }
	}

	public Uint64 getLastSeenSlotIndex(Hash itemHash) {
		Tracker tracker = mTrackers.get(itemHash);
		if (tracker == null)
	    {
	        return Uint64.ZERO;
	    } else {
		    return tracker.getLastSeenSlotIndex();
	    }

	}

	public List<SCPEnvelope> fetchingFor(Hash itemHash)
	{
		Tracker iter = mTrackers.get(itemHash);
	    if (iter == null)
	    {
	        return Collections.emptyList();
	    }

	    
	    return iter.waitingEnvelopes().stream().map((t2) -> t2.get1()).collect(Collectors.toList());
	}

	public void stopFetchingBelow(Uint64 slotIndex) {
	    // only perform this cleanup from the top of the stack as it causes
	    // all sorts of evil side effects
		virtualClock.getIOService().post(
	        () -> { stopFetchingBelowInternal(slotIndex); });
	}

	void stopFetchingBelowInternal(Uint64 slotIndex)
	{
		Iterator<Entry<Hash, Tracker>> itr = mTrackers.entrySet().iterator();
	    while(itr.hasNext())
	    {
	        Tracker tracker = itr.next().getValue();
			if (!tracker.clearEnvelopesBelow(slotIndex))
	        {
	            itr.remove();
	            mItemMapSize.dec();
	        }
	    }
	}

	public void doesntHave(Hash itemHash, IPeer peer) {
	    Tracker tracker = mTrackers.get(itemHash);
	    if (tracker != null)
	    {
	        tracker.doesntHave(peer);
	    }
	}

	public void recv(Hash itemHash) {
	    log.trace("Overlay Recv {}", () -> itemHash.hexAbbrev());
	    Tracker tracker = mTrackers.get(itemHash);

	    if (tracker != null)
	    {
	        // this code can safely be called even if recvSCPEnvelope ends up
	        // calling recv on the same itemHash

	        log.trace("Overlay Recv {} : {}", itemHash.hexAbbrev(), tracker.size());

	        while (!tracker.empty())
	        {
	            this.herder.recvSCPEnvelope(tracker.pop());
	        }
	        // stop the timer, stop requesting the item as we have it
	        tracker.resetLastSeenSlotIndex();
	        tracker.cancel();
	    }
	}

}
