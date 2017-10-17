// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.consuminurigni.stellarj.overlay.xdr;


import java.io.IOException;

import com.consuminurigni.stellarj.xdr.Signature;
import com.consuminurigni.stellarj.xdr.SignatureHint;
import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

// === xdr source ============================================================

//  struct DecoratedSignature
//  {
//      SignatureHint hint;  // last 4 bytes of the public key, used as a hint
//      Signature signature; // actual signature
//  };

//  ===========================================================================
public class DecoratedSignature  {
  public DecoratedSignature () {}
  private SignatureHint hint;
  public SignatureHint getHint() {
    return this.hint;
  }
  public void setHint(SignatureHint value) {
    this.hint = value;
  }
  private Signature signature;
  public Signature getSignature() {
    return this.signature;
  }
  public void setSignature(Signature value) {
    this.signature = value;
  }
  public static void encode(XdrDataOutputStream stream, DecoratedSignature encodedDecoratedSignature) throws IOException{
    SignatureHint.encode(stream, encodedDecoratedSignature.hint);
    Signature.encode(stream, encodedDecoratedSignature.signature);
  }
  public static DecoratedSignature decode(XdrDataInputStream stream) throws IOException {
    DecoratedSignature decodedDecoratedSignature = new DecoratedSignature();
    decodedDecoratedSignature.hint = SignatureHint.decode(stream);
    decodedDecoratedSignature.signature = Signature.decode(stream);
    return decodedDecoratedSignature;
  }
}
