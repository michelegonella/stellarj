// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.consumimurigni.stellarj.ledger.xdr;


import java.io.IOException;

import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

// === xdr source ============================================================

//  enum SignerKeyType
//  {
//      SIGNER_KEY_TYPE_ED25519 = KEY_TYPE_ED25519,
//      SIGNER_KEY_TYPE_PRE_AUTH_TX = KEY_TYPE_PRE_AUTH_TX,
//      SIGNER_KEY_TYPE_HASH_X = KEY_TYPE_HASH_X
//  };

//  ===========================================================================
public enum SignerKeyType  {
  SIGNER_KEY_TYPE_ED25519(0),
  SIGNER_KEY_TYPE_PRE_AUTH_TX(1),
  SIGNER_KEY_TYPE_HASH_X(2),
  ;
  private int mValue;

  SignerKeyType(int value) {
      mValue = value;
  }

  public int getValue() {
      return mValue;
  }

  static SignerKeyType decode(XdrDataInputStream stream) throws IOException {
    int value = stream.readInt();
    switch (value) {
      case 0: return SIGNER_KEY_TYPE_ED25519;
      case 1: return SIGNER_KEY_TYPE_PRE_AUTH_TX;
      case 2: return SIGNER_KEY_TYPE_HASH_X;
      default:
        throw new RuntimeException("Unknown enum value: " + value);
    }
  }

  static void encode(XdrDataOutputStream stream, SignerKeyType value) throws IOException {
    stream.writeInt(value.getValue());
  }
}
