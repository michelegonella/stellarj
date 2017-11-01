package com.consuminurigni.stellarj.overlay;

import java.time.Instant;
import java.util.List;

import com.consuminurigni.stellarj.common.LRUCache;
import com.consuminurigni.stellarj.metering.Meter;
import com.consuminurigni.stellarj.xdr.NodeID;
import com.consuminurigni.stellarj.xdr.Uint64;

// This class monitors system load, and attempts to assign blame for
// the origin of load to particular peers, including the transactions
// we inject ourselves.
//
// The purpose is ultimately to offer a diagnostic view of the peer
// when and if it's overloaded, as well as to support an automatic
// load-shedding action of disconnecting the "worst" peers and/or
// rejecting traffic due to load.
//
// This is all very heuristic and speculative; if it turns out not to
// work, or to do more harm than good, it ought to be disabled/removed.

public class LoadManager {

//    // We track the costs incurred by each peer in a PeerCosts structure,
//    // and keep these in an LRU cache to avoid overfilling the LoadManager
//    // should we have ongoing churn in low-cost peers.
    public static class PeerCosts implements Comparable<PeerCosts>
    {
        Meter mTimeSpent;
        Meter mBytesSend;
        Meter mBytesRecv;
        Meter mSQLQueries;
		@Override
		public int compareTo(PeerCosts o) {
			// TODO Auto-generated method stub
			return 0;
		}
    };
// // Context manager for doing work on behalf of a node, we push
//    // one of these on the stack. When destroyed it will debit the
//    // peer in question with the cost.
    public static class PeerContext
    {
        NodeID mNode;

        Instant mWorkStart;
        Instant mBytesSendStart;
        Instant mBytesRecvStart;
        Instant mSQLQueriesStart;
    }
//    private final LRUCache<NodeID, PeerCosts> mPeerCosts;
//
//
//   private static final String[] bsz = {"B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB"};
//String byteMag(long bytes)
//{
//    long i = 1024;
//    int j = 0;
//    while(bytes / i > 1) {
//    	j++;
//    	i *= 1024;
//    }
//    return bsz[j];
//}
//
//private static final String[] tsz = {"ns", "us", "ms", "s"};
//String timeMag(long nanos)
//{
//
//    uint64_t mag = 1000000000;
//    for (int i = 3; i >= 0; --i)
//    {
//        if (nanos >= mag)
//        {
//            return fmt::format("{:>d}{:s}", nanos / mag, sz[i]);
//        }
//        mag /= 1000;
//    }
//    return "0";
//}

//void reportLoads(List<Peer> peers,
//                         Config config)
//{
//    CLOG(INFO, "Overlay") << "";
//    CLOG(INFO, "Overlay") << "Cumulative peer-load costs:";
//    CLOG(INFO, "Overlay")
//        << "------------------------------------------------------";
//    CLOG(INFO, "Overlay") << fmt::format(
//        "{:>10s} {:>10s} {:>10s} {:>10s} {:>10s}", "peer", "time", "send",
//        "recv", "query");
//    for (auto const& peer : peers)
//    {
//        auto cost = getPeerCosts(peer->getPeerID());
//        CLOG(INFO, "Overlay") << fmt::format(
//            "{:>10s} {:>10s} {:>10s} {:>10s} {:>10d}",
//            app.getConfig().toShortString(peer->getPeerID()),
//            timeMag(static_cast<uint64_t>(cost->mTimeSpent.one_minute_rate())),
//            byteMag(static_cast<uint64_t>(cost->mBytesSend.one_minute_rate())),
//            byteMag(static_cast<uint64_t>(cost->mBytesRecv.one_minute_rate())),
//            cost->mSQLQueries.count());
//    }
//    CLOG(INFO, "Overlay") << "";
//}
//
//LoadManager::~LoadManager()
//{
//}
//
//void
//LoadManager::maybeShedExcessLoad(Application& app)
//{
//    uint32_t minIdle = app.getConfig().MINIMUM_IDLE_PERCENT;
//    uint32_t idleClock = app.getClock().recentIdleCrankPercent();
//    uint32_t idleDb = app.getDatabase().recentIdleDbPercent();
//
//    if ((idleClock < minIdle) || (idleDb < minIdle))
//    {
//        CLOG(WARNING, "Overlay") << "";
//        CLOG(WARNING, "Overlay") << "System appears to be overloaded";
//        CLOG(WARNING, "Overlay") << "Idle minimum " << minIdle << "% vs. "
//                                 << "clock " << idleClock << "%, "
//                                 << "DB " << idleDb << "%";
//        CLOG(WARNING, "Overlay") << "";
//
//        auto peers = app.getOverlayManager().getPeers();
//        reportLoads(peers, app);
//
//        // Look for the worst-behaved of the current peers and kick them out.
//        std::shared_ptr<Peer> victim;
//        std::shared_ptr<LoadManager::PeerCosts> victimCost;
//        for (auto peer : peers)
//        {
//            auto peerCost = getPeerCosts(peer->getPeerID());
//            if (!victim || victimCost->isLessThan(peerCost))
//            {
//                victim = peer;
//                victimCost = peerCost;
//            }
//        }
//
//        if (victim)
//        {
//            CLOG(WARNING, "Overlay")
//                << "Disconnecting suspected culprit "
//                << app.getConfig().toShortString(victim->getPeerID());
//
//            app.getMetrics()
//                .NewMeter({"overlay", "drop", "load-shed"}, "drop")
//                .Mark();
//
//            victim->drop();
//        }
//    }
//}
//
//LoadManager::PeerCosts::PeerCosts()
//    : mTimeSpent("nanoseconds")
//    , mBytesSend("byte")
//    , mBytesRecv("byte")
//    , mSQLQueries("query")
//{
//}
//
//bool
//LoadManager::PeerCosts::isLessThan(
//    std::shared_ptr<LoadManager::PeerCosts> other)
//{
//    double ownRates[4] = {
//        mTimeSpent.one_minute_rate(), mBytesSend.one_minute_rate(),
//        mBytesRecv.one_minute_rate(), mSQLQueries.one_minute_rate()};
//    double otherRates[4] = {other->mTimeSpent.one_minute_rate(),
//                            other->mBytesSend.one_minute_rate(),
//                            other->mBytesRecv.one_minute_rate(),
//                            other->mSQLQueries.one_minute_rate()};
//    return std::lexicographical_compare(ownRates, ownRates + 4, otherRates,
//                                        otherRates + 4);
//}
//
//std::shared_ptr<LoadManager::PeerCosts>
//LoadManager::getPeerCosts(NodeID const& node)
//{
//    if (mPeerCosts.exists(node))
//    {
//        return mPeerCosts.get(node);
//    }
//    auto p = std::make_shared<LoadManager::PeerCosts>();
//    mPeerCosts.put(node, p);
//    return p;
//}
//
//LoadManager::PeerContext::PeerContext(Application& app, NodeID const& node)
//    : mApp(app)
//    , mNode(node)
//    , mWorkStart(app.getClock().now())
//    , mBytesSendStart(Peer::getByteWriteMeter(app).count())
//    , mBytesRecvStart(Peer::getByteReadMeter(app).count())
//    , mSQLQueriesStart(app.getDatabase().getQueryMeter().count())
//{
//}
//
//LoadManager::PeerContext::~PeerContext()
//{
//    if (!isZero(mNode.ed25519()))
//    {
//        auto pc = mApp.getOverlayManager().getLoadManager().getPeerCosts(mNode);
//        auto time = std::chrono::duration_cast<std::chrono::nanoseconds>(
//            mApp.getClock().now() - mWorkStart);
//        auto send = Peer::getByteWriteMeter(mApp).count() - mBytesSendStart;
//        auto recv = Peer::getByteReadMeter(mApp).count() - mBytesRecvStart;
//        auto query =
//            (mApp.getDatabase().getQueryMeter().count() - mSQLQueriesStart);
//        if (Logging::logTrace("Overlay"))
//            CLOG(TRACE, "Overlay")
//                << "Debiting peer " << mApp.getConfig().toShortString(mNode)
//                << " time:" << timeMag(time.count())
//                << " send:" << byteMag(send) << " recv:" << byteMag(recv)
//                << " query:" << query;
//        pc->mTimeSpent.Mark(time.count());
//        pc->mBytesSend.Mark(send);
//        pc->mBytesRecv.Mark(recv);
//        pc->mSQLQueries.Mark(query);
//    }
//}
//    
}
