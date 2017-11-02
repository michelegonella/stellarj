package com.consuminurigni.stellarj.overlay;

import com.consuminurigni.stellarj.overlay.Peer.PeerRole;
import com.consuminurigni.stellarj.overlay.xdr.AuthCert;
import com.consuminurigni.stellarj.xdr.Curve25519Public;
import com.consuminurigni.stellarj.xdr.HmacSha256Key;
import com.consuminurigni.stellarj.xdr.NodeID;
import com.consuminurigni.stellarj.xdr.Uint256;

public class PeerAuth {

	public AuthCert getAuthCert() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean verifyRemoteAuthCert(NodeID peerID, AuthCert cert) {
		// TODO Auto-generated method stub
		return false;
	}

	public HmacSha256Key getSendingMacKey(Curve25519Public pubkey, Uint256 mSendNonce, Uint256 mRecvNonce,
			PeerRole mRole) {
		// TODO Auto-generated method stub
		return null;
	}

	public HmacSha256Key getReceivingMacKey(Curve25519Public pubkey, Uint256 mSendNonce, Uint256 mRecvNonce,
			PeerRole mRole) {
		// TODO Auto-generated method stub
		return null;
	}

}
