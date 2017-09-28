package com.consumimurigni.stellarj.crypto_cpp;

import org.stellar.sdk.xdr.Uint256;

public abstract class SHA256 {

	public static SHA256 create() {
		// TODO Auto-generated method stub
		return null;
	}

    
    public abstract void reset();
    public abstract void add(byte[] bin);
    public abstract Uint256 finish();
}
