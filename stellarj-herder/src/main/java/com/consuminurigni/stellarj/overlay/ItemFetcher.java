package com.consuminurigni.stellarj.overlay;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.consumimurigni.stellarj.main.Application;
import com.consuminurigni.stellarj.common.Tuple2;
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
	Map<Hash, Tracker> mTrackers;
	public ItemFetcher(Application app, /*aka typedef AskPeer*/BiConsumer<Peer, Hash> askPeer) {
		
	}

	public void doesntHave(Uint256 itemID, Peer peer) {
		// TODO Auto-generated method stub
		
	}

	public void recv(Hash hash) {
		// TODO Auto-generated method stub
		
	}

	public Uint64 getLastSeenSlotIndex(Hash hash) {
		// TODO Auto-generated method stub
		return null;
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

	public void fetch(Hash h, SCPEnvelope envelope) {
		// TODO Auto-generated method stub
		
	}

	public void stopFetch(Hash h, SCPEnvelope envelope) {
		// TODO Auto-generated method stub
		
	}

	public void stopFetchingBelow(Uint64 plus) {
		// TODO Auto-generated method stub
		
	}

}
