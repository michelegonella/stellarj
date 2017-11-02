package com.consuminurigni.stellarj.overlay;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.consuminurigni.stellarj.common.Database;
import com.consuminurigni.stellarj.common.VirtualClock;
import com.consuminurigni.stellarj.overlay.xdr.PeerAddress;

public class PeerRecord {

	public PeerRecord(String ip, int intValue, Instant defaultNextAttempt, int fails) {
		// TODO Auto-generated constructor stub
	}

	public static List<PeerRecord> loadPeerRecords(Database database, int i, Instant now) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isPrivateAddress() {
		// TODO Auto-generated method stub
		return false;
	}

	public PeerAddress toXdr() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isSelfAddressAndPort(Object ip, int mRemoteListeningPort) {
		// TODO Auto-generated method stub
		return false;
	}

	public static @Nullable PeerRecord loadPeerRecord(Database database, String ip, int remoteListeningPort) {
		// TODO Auto-generated method stub
		return null;
	}

	public void resetBackOff(VirtualClock virtualClock) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}

	public void storePeerRecord(Database database) {
		// TODO Auto-generated method stub
		
	}

	public boolean isLocalhost() {
		// TODO Auto-generated method stub
		return false;
	}

	public void insertIfNew(Database database) {
		// TODO Auto-generated method stub
		
	}
}
