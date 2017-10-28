package com.consuminurigni.stellarj.common;

import java.nio.charset.StandardCharsets;

import net.i2p.data.DataHelper;

public class Base32 {

	public byte[] decode(byte[] bytes) {
		// TODO
		return net.i2p.data.Base32.decode(new String(bytes, StandardCharsets.US_ASCII));
	}

	public byte[] encode(byte[] unencoded) {
		// TODO
		return DataHelper.getASCII(net.i2p.data.Base32.encode(unencoded));
	}

}
