// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.consuminurigni.stellarj.overlay.xdr;


import java.io.IOException;

import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

// === xdr source ============================================================

//  typedef opaque SignatureHint[4];

//  ===========================================================================
public class SignatureHint  {
  private byte[] SignatureHint;
  public byte[] getSignatureHint() {
    return this.SignatureHint;
  }
  public void setSignatureHint(byte[] value) {
    this.SignatureHint = value;
  }
  public static void encode(XdrDataOutputStream stream, SignatureHint  encodedSignatureHint) throws IOException {
  int SignatureHintsize = encodedSignatureHint.SignatureHint.length;
  stream.write(encodedSignatureHint.getSignatureHint(), 0, SignatureHintsize);
  }
  public static SignatureHint decode(XdrDataInputStream stream) throws IOException {
    SignatureHint decodedSignatureHint = new SignatureHint();
  int SignatureHintsize = 4;
  decodedSignatureHint.SignatureHint = new byte[SignatureHintsize];
  stream.read(decodedSignatureHint.SignatureHint, 0, SignatureHintsize);
    return decodedSignatureHint;
  }
}
