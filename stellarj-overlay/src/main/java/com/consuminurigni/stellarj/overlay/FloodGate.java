package com.consuminurigni.stellarj.overlay;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.consumimurigni.stellarj.crypto.CryptoUtils;
import com.consumimurigni.stellarj.scp.Herder;
import com.consuminurigni.stellarj.metering.Counter;
import com.consuminurigni.stellarj.metering.Meter;
import com.consuminurigni.stellarj.metering.Metrics;
import com.consuminurigni.stellarj.overlay.xdr.StellarMessage;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.Uint32;

public class FloodGate {
	private static final Logger log = LogManager.getLogger();
	
	
	private class FloodRecord
    {
        Uint32 mLedgerSeq;
        StellarMessage mMessage;
        SortedSet<Peer> mPeersTold = new TreeSet<>();

        FloodRecord(StellarMessage msg, Uint32 ledger,
                    @Nullable Peer peer) {
        	this.mLedgerSeq = ledger;
        	this.mMessage = msg;
        	if(peer != null) { 
            	this.mPeersTold.add(peer);
        	}
        }
        Uint32 getLedgerSeq() {
        	return mLedgerSeq;
        }
		public void addPeerTold(Peer peer) {
			this.mPeersTold.add(peer);
		}
		public SortedSet<Peer> getPeersTold() {
			return mPeersTold;
		}
    };

    private final TreeMap<Hash, FloodRecord> mFloodMap = new TreeMap<>();
    private final Counter mFloodMapSize;
    private final Meter mSendFromBroadcast;
    private volatile boolean mShuttingDown = false;
    private final Herder herder;
    private final OverlayManager overlayManager;

    public FloodGate(OverlayManager overlayManager, Herder herder, Metrics metrics) {
    	this.overlayManager = overlayManager;
    	this.herder = herder;
    	this.mFloodMapSize = metrics.newCounter("overlay", "memory", "flood-map");
    	this.mSendFromBroadcast = metrics.newMeter("overlay", "message", "send-from-broadcast", "message");
    }
    
 // remove old flood records
    void clearBelow(Uint32 currentLedger)
    {
    	Iterator<Entry<Hash, FloodRecord>> itr = mFloodMap.entrySet().iterator();
    	while(itr.hasNext()) {
    		if(itr.next().getValue().getLedgerSeq().plus(10/*TODO parametrize ??*/).lt(currentLedger)) {
    			itr.remove();
    		}
    	}
    }

    //TODO true if we have never seen this message
    boolean addRecord(StellarMessage msg, Peer peer)
    {
        if (mShuttingDown)
        {
            return false;
        }
        Hash index = Hash.of(CryptoUtils.sha256(msg.encode()));
        if(! mFloodMap.containsKey(index)) {
        	mFloodMap.put(index, new FloodRecord(msg, herder.getCurrentLedgerSeq(), peer));
        	mFloodMapSize.set_count(mFloodMap.size());
        	return true;
        } else {
        	mFloodMap.get(index).addPeerTold(peer);
        	return false;
        }
    }

 // send message to anyone you haven't gotten it from
    void broadcast(StellarMessage msg, boolean force)
    {
        if (mShuttingDown)
        {
            return;
        }
        Hash index = Hash.of(CryptoUtils.sha256(msg.encode()));
        log.trace("Overlay broadcast {}",  () -> index.hexAbbrev());

        FloodRecord record = mFloodMap.get(index);
        if (record == null || force)
        { // no one has sent us this message
        	record = new FloodRecord(msg, herder.getCurrentLedgerSeq(), null);
        	mFloodMap.put(index, record);
        	mFloodMapSize.set_count(mFloodMap.size());
        }
        // send it to people that haven't sent it to us

        // make a copy, in case peers gets modified
        Set<Peer> peersTold = new LinkedHashSet<>(record.getPeersTold());

        List<Peer> peers = overlayManager.getPeers();

        for (Peer peer : peers)
        {
            if (! peersTold.contains(peer) && peer.isAuthenticated())
            {
                mSendFromBroadcast.mark();
                peer.sendMessage(msg);
                peersTold.add(peer);
            }
        }
        log.trace("Overlay broadcast {} told {}", () -> index.hexAbbrev(), () -> peersTold.size());
    }

    SortedSet<Peer> getPeersKnows(Hash h)
    {
    	SortedSet<Peer> res = new TreeSet<>();
    	FloodRecord record = mFloodMap.get(h);
        if (record != null)
        {
            res = record.getPeersTold();
        }
        return Collections.unmodifiableSortedSet(res);
    }

    void shutdown()
    {
        mShuttingDown = true;
        mFloodMap.clear();
    }

}
