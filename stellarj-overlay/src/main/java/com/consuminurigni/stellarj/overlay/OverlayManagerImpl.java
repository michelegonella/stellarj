package com.consuminurigni.stellarj.overlay;

import java.util.List;
import java.util.Set;

import com.consuminurigni.stellarj.overlay.xdr.StellarMessage;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.Uint32;

/*

Connection process:
A wants to connect to B
A initiates a tcp connection to B
connection is established
A sends HELLO(CertA,NonceA) to B
B now has IP and listening port of A, sends HELLO(CertB,NonceB) back
A sends AUTH(signed([0],keyAB))
B verifies and either:
    sends AUTH(signed([0],keyBA)) back or
    disconnects, if it's full, optionally sending a list of other peers to try
first

keyAB and keyBA are per-connection HMAC keys derived from non-interactive
ECDH on random curve25519 keys conveyed in CertA and CertB (certs signed by
Node Ed25519 keys) the result of which is then fed through HKDF with the
per-connection nonces. See PeerAuth.h.

If any verify step fails, the peer disconnects immediately.

*/

public class OverlayManagerImpl implements OverlayManager {

	@Override
	public void ledgerClosed(Uint32 lastClosedledgerSeq) {
		// TODO Auto-generated method stub

	}

	@Override
	public void broadcastMessage(StellarMessage msg, boolean force) {
		// TODO Auto-generated method stub

	}

	@Override
	public void recvFloodedMsg(StellarMessage msg, Peer peer) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Peer> getRandomPeers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Peer getConnectedPeer(String ip, int port) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addConnectedPeer(Peer peer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dropPeer(Peer peer) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isPeerAccepted(Peer peer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Peer> getPeers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void connectTo(String addr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectTo(PeerRecord pr) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<Peer> getPeersKnows(Hash h) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PeerAuth getPeerAuth() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LoadManager getLoadManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isShuttingDown() {
		// TODO Auto-generated method stub
		return false;
	}

}
