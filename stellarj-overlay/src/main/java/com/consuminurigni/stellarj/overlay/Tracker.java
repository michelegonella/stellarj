package com.consuminurigni.stellarj.overlay;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.consumimurigni.stellarj.crypto.CryptoUtils;
import com.consumimurigni.stellarj.role.IPeer;
import com.consuminurigni.stellarj.common.Assert;
import com.consuminurigni.stellarj.common.Tuple2;
import com.consuminurigni.stellarj.common.VirtualClock;
import com.consuminurigni.stellarj.common.VirtualTimer;
import com.consuminurigni.stellarj.metering.Meter;
import com.consuminurigni.stellarj.metering.Metrics;
import com.consuminurigni.stellarj.overlay.xdr.StellarMessage;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.MessageType;
import com.consuminurigni.stellarj.xdr.Uint64;

/**
 * @class Tracker
 *
 *        Asks peers for given data set. If a peer does not have given data set,
 *        asks another one. If no peer does have given data set, it starts again
 *        with new set of peers (possibly overlapping, as peers may learned
 *        about this data set in meantime).
 *
 *        For asking a AskPeer delegate is used.
 *
 *        Tracker keeps list of envelopes that requires given data set to be
 *        fully resolved. When data is received each envelope is resend to
 *        Herder so it can check if it has all required data and then process
 *        envelope.
 * @see listen(Peer::pointer) is used to add envelopes to that list.
 */
public class Tracker {
	private static final Logger log = LogManager.getLogger();
	private static final Duration MS_TO_WAIT_FOR_FETCH_REPLY = Duration.ofMillis(1500);
	private static final int MAX_REBUILD_FETCH_LIST = 1000;

	private final AskPeer mAskPeer;
	private Peer mLastAskedPeer = null;
	private int mNumListRebuild = 0;
	private final LinkedList<Peer> mPeersToAsk = new LinkedList<>();
	private final VirtualTimer mTimer;
	private final LinkedList<Tuple2<Hash, SCPEnvelope>> mWaitingEnvelopes = new LinkedList<>();
	private final Hash mItemHash;
	private final Meter mTryNextPeerReset;
	private final Meter mTryNextPeer;
	private Uint64 mLastSeenSlotIndex = Uint64.ZERO;
	private final OverlayManager overlayManager;

	// TODO ?? needed ?
	@Override
	protected void finalize() throws Throwable {
		cancel();
	}

	void cancel() {
		mTimer.cancel();
		mLastSeenSlotIndex = Uint64.ZERO;
	}

	/**
	 * Create Tracker that tracks data identified by @p hash. @p askPeer delegate is
	 * used to fetch the data.
	 */
	public Tracker(OverlayManager overlayManager, VirtualClock virtualClock, Metrics metrics, Hash hash,
			AskPeer askPeer) {
		this.overlayManager = overlayManager;
		this.mNumListRebuild = 0;
		this.mTimer = new VirtualTimer(virtualClock);
		this.mItemHash = hash;
		this.mTryNextPeerReset = metrics.newMeter("overlay", "item-fetcher", "reset-fetcher", "item-fetcher");
		this.mTryNextPeer = metrics.newMeter("overlay", "item-fetcher", "next-peer", "item-fetcher");
		this.mAskPeer = Assert.assertNotNull(askPeer);
	}

	@Nullable
	/* ??? */ SCPEnvelope pop() {
		Tuple2<Hash, SCPEnvelope> pair = mWaitingEnvelopes.pop();
		return pair.get1();// TODO npe ??
	}

	// returns false if no one cares about this guy anymore
	boolean clearEnvelopesBelow(Uint64 slotIndex) {
		Iterator<Tuple2<Hash, SCPEnvelope>> itr = mWaitingEnvelopes.iterator();
		while (itr.hasNext()) {
			if (itr.next().get1().getStatement().getSlotIndex().lt(slotIndex)) {
				itr.remove();
			}
		}
		if (!mWaitingEnvelopes.isEmpty()) {
			return true;
		}

		mTimer.cancel();
		mLastAskedPeer = null;

		return false;
	}

	void doesntHave(IPeer peer) {
		if (mLastAskedPeer.equals(peer)) {
			log.trace("Overlay Does not have {}", () -> mItemHash.hexAbbrev());
			tryNextPeer();
		}
	}

	void tryNextPeer() {

		log.trace("Overlay tryNextPeer {} last: {}", () -> mItemHash.hexAbbrev(),
				() -> (mLastAskedPeer == null ? "<none>" : mLastAskedPeer.toString()));

		// if we don't have a list of peers to ask and we're not
		// currently asking peers, build a new list
		if (mPeersToAsk.isEmpty() && mLastAskedPeer == null) {
			SortedSet<Peer> peersWithEnvelope = new TreeSet<>();
			for (Tuple2<Hash, SCPEnvelope> e : mWaitingEnvelopes) {
				SortedSet<Peer> s = overlayManager.getPeersKnows(e.get0());
				peersWithEnvelope.addAll(s);
			}

			// move the peers that have the envelope to the back,
			// to be processed first
			for (Peer p : overlayManager.getRandomPeers()) {
				if (peersWithEnvelope.contains(p)) {
					mPeersToAsk.addLast(p);
				} else {
					mPeersToAsk.addFirst(p);
				}
			}

			mNumListRebuild++;

			log.trace("Overlay tryNextPeer {} attempt {} reset to #{}", () -> mItemHash.hexAbbrev(),
					() -> mNumListRebuild, () -> mPeersToAsk.size());
			mTryNextPeerReset.mark();
		}

		// will be called by some timer or when we get a
		// response saying they don't have it
		Peer peer = null;

		while (peer == null && !mPeersToAsk.isEmpty()) {
			peer = mPeersToAsk.pop();
			if (!peer.isAuthenticated()) {
				peer = null;
			}
		}

		Duration nextTry;
		if (peer == null) { // we have asked all our peers
							// clear mLastAskedPeer so that we rebuild a new list
			mLastAskedPeer = null;
			if (mNumListRebuild > MAX_REBUILD_FETCH_LIST) {
				nextTry = MS_TO_WAIT_FOR_FETCH_REPLY.multipliedBy(MAX_REBUILD_FETCH_LIST);
			} else {
				nextTry = MS_TO_WAIT_FOR_FETCH_REPLY.multipliedBy(mNumListRebuild);
			}
		} else {
			mLastAskedPeer = peer;
			log.trace("Overlay Asking for {} to {}", () -> mItemHash.hexAbbrev(),
					() -> /* c++ peer */mLastAskedPeer.toString());
			mTryNextPeer.mark();
			mAskPeer.accept(peer, mItemHash);
			nextTry = MS_TO_WAIT_FOR_FETCH_REPLY;
		}

		mTimer.expires_from_now(nextTry);
		mTimer.async_wait(() -> {
			this.tryNextPeer();
		}, VirtualTimer::onFailureNoop);
	}

	/**
	 * Return true if does not wait for any envelope.
	 */
	boolean empty() {
		return mWaitingEnvelopes.isEmpty();
	}

	/**
	 * Return count of envelopes it is waiting for.
	 */
	int size() {
		return mWaitingEnvelopes.size();
	}

	/**
	 * Return list of envelopes this tracker is waiting for.
	 */
	public List<Tuple2<Hash, SCPEnvelope>> waitingEnvelopes() {
		return Collections.unmodifiableList(mWaitingEnvelopes);
	}

	/**
	 * Return biggest slot index seen since last reset.
	 */
	Uint64 getLastSeenSlotIndex() {
		return mLastSeenSlotIndex;
	}

	/**
	 * Reset value of biggest slot index seen.
	 */
	void resetLastSeenSlotIndex() {
		mLastSeenSlotIndex = Uint64.ZERO;
	}

	void listen(SCPEnvelope env) {
		mLastSeenSlotIndex = Uint64.max(env.getStatement().getSlotIndex(), mLastSeenSlotIndex);

		StellarMessage m = new StellarMessage();
		m.setDiscriminant(MessageType.SCP_MESSAGE);
		m.setEnvelope(env);
		mWaitingEnvelopes.add(new Tuple2<>(Hash.of(CryptoUtils.sha256(m.encode())), env));
	}

	void discard(SCPEnvelope env) {
		Iterator<Tuple2<Hash, SCPEnvelope>> itr = mWaitingEnvelopes.iterator();
		while (itr.hasNext()) {
			if (itr.next().get1().equals(env)) {
				itr.remove();
			}
		}
	}

}
