package com.consuminurigni.stellarj.overlay;

import java.util.List;
import java.util.TreeSet;

import com.consuminurigni.stellarj.overlay.xdr.StellarMessage;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.Uint32;

/**
 * OverlayManager maintains a public broadcast network, consisting of a set of
 * remote TCP peers (TCPPeer), a mechanism for flooding messages to all peers
 * (FloodGate), and a mechanism for sending and receiving anycast request/reply
 * pairs (ItemFetcher).
 *
 * Overlay network messages are defined as the XDR structure type
 * `StellarMessage`, in the file src/xdr/Stellar-overlay.x
 *
 * They are minimally framed using the Record Marking (RM) standard of RFC5531
 * (https://tools.ietf.org/html/rfc5531#page-16) and the RM-framed messages are
 * transmitted over TCP/IP sockets, between peers.
 *
 * The `StellarMessage` union contains 3 logically distinct kinds of message:
 *
 *  - Messages directed to or from a specific peer, with or without a response:
 *    HELLO, GET_PEERS, PEERS, DONT_HAVE, ERROR_MSG
 *
 *  - One-way broadcast messages informing other peers of an event:
 *    TRANSACTION and SCP_MESSAGE
 *
 *  - Two-way anycast messages requesting a value (by hash) or providing it:
 *    GET_TX_SET, TX_SET, GET_SCP_QUORUMSET, SCP_QUORUMSET, GET_SCP_STATE
 *
 * Anycasts are initiated and serviced two instances of ItemFetcher
 * (mTxSetFetcher and mQuorumSetFetcher). Anycast messages are sent to
 * directly-connected peers, in sequence until satisfied. They are not
 * flooded between peers.
 *
 * Broadcasts are initiated by the Herder and sent to both the Herder _and_ the
 * local FloodGate, for propagation to other peers.
 *
 * The OverlayManager tracks its known peers in the Database and shares peer
 * records with other peers when asked.
 */

public interface OverlayManager {

//    // Drop all PeerRecords from the Database
//    static void dropAll(DataSource db);

    // Flush all FloodGate and ItemFetcher state for ledgers older than
    // `ledger`.
    // This is called by LedgerManager when a ledger closes.
    public void ledgerClosed(Uint32 lastClosedledgerSeq);

    // Send a given message to all peers, via the FloodGate. This is called by
    // Herder.
    public void broadcastMessage(StellarMessage msg, boolean force);

    // Make a note in the FloodGate that a given peer has provided us with a
    // given broadcast message, so that it is inhibited from being resent to
    // that peer. This does _not_ cause the message to be broadcast anew; to do
    // that, call broadcastMessage, above.
    public void recvFloodedMsg(StellarMessage msg,
                                Peer peer);

    // Return a list of random peers from the set of authenticated peers.
    public List<Peer> getRandomPeers();

    // Return an already-connected peer at the given ip address and port;
    // returns a `nullptr`-valued pointer if no such connected peer exists.
    public Peer getConnectedPeer(String ip, int port);

    // Add a peer to the in-memory set of connected peers.
    public void addConnectedPeer(Peer peer);

    // Forget about a peer, removing it from the in-memory set of connected
    // peers. Presumably due to it disconnecting.
    public void dropPeer(Peer peer);

    // Returns true if there is room for the provided peer in the in-memory set
    // of connected peers without evicting an existing peer, or if the provided
    // peer is a "preferred" peer (as specified in the config file's
    // PREFERRED_PEERS/PREFERRED_PEER_KEYS
    // setting). Otherwise returns false.
    public boolean isPeerAccepted(Peer peer);

    // Return the current in-memory set of connected peers.
    public List<Peer> getPeers();

    // Attempt to connect to a peer identified by string. The form of the string
    // should be an IP address or hostname, optionally followed by a colon and
    // a TCP port number.
    public void connectTo(String addr);

    // Attempt to connect to a peer identified by peer record.
    public void connectTo(PeerRecord pr);

    // returns the list of peers that sent us the item with hash `h`
    public TreeSet<Peer> getPeersKnows(Hash h);

    // Return the persistent p2p authentication-key cache.
    public PeerAuth getPeerAuth();

    // Return the persistent peer-load-accounting cache.
    public LoadManager getLoadManager();

    // start up all background tasks for overlay
    public void start();
    // drops all connections
    public void shutdown();

    public boolean isShuttingDown();
}
