package com.consuminurigni.stellarj.overlay;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import com.consuminurigni.stellarj.common.Tuple2;
import com.consuminurigni.stellarj.common.VirtualTimer;
import com.consuminurigni.stellarj.metering.Meter;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.Uint64;

public interface AskPeer extends BiConsumer<Peer, Hash> {


}
