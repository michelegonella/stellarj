// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.consumimurigni.stellarj.ledger.xdr;


import java.io.IOException;

import com.consuminurigni.stellarj.xdr.Uint32;
import com.consuminurigni.stellarj.xdr.Xdr;
import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

// === xdr source ============================================================

//  typedef opaque UpgradeType<128>;

//  ===========================================================================
public class UpgradeType  {
  private byte[] UpgradeType;
  public byte[] getUpgradeType() {
    return this.UpgradeType;
  }
  public void setUpgradeType(byte[] value) {
    this.UpgradeType = value;
  }
  public static void encode(XdrDataOutputStream stream, UpgradeType  encodedUpgradeType) throws IOException {
  int UpgradeTypesize = encodedUpgradeType.UpgradeType.length;
  stream.writeInt(UpgradeTypesize);
  stream.write(encodedUpgradeType.getUpgradeType(), 0, UpgradeTypesize);
  }
  public static UpgradeType decode(XdrDataInputStream stream) throws IOException {
    UpgradeType decodedUpgradeType = new UpgradeType();
  int UpgradeTypesize = stream.readInt();
  decodedUpgradeType.UpgradeType = new byte[UpgradeTypesize];
  stream.read(decodedUpgradeType.UpgradeType, 0, UpgradeTypesize);
    return decodedUpgradeType;
  }
public byte[] encode() {
	// TODO Auto-generated method stub
	return null;
}
public static UpgradeType decode(byte[] encode) {
	// TODO Auto-generated method stub
	return null;
}
public static Uint32 max_size() {
	// TODO Auto-generated method stub
	return Xdr.XDR_MAX_LEN;
}
public boolean isEmpty() {
	// TODO Auto-generated method stub
	return false;
}
}
