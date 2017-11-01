// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.consuminurigni.stellarj.overlay.xdr;


import java.io.IOException;

import com.consuminurigni.stellarj.xdr.Encodable;
import com.consuminurigni.stellarj.xdr.HmacSha256Mac;
import com.consuminurigni.stellarj.xdr.Uint32;
import com.consuminurigni.stellarj.xdr.Uint64;
import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

// === xdr source ============================================================

//  union AuthenticatedMessage switch (uint32 v)
//  {
//  case 0:
//      struct
//  {
//     uint64 sequence;
//     StellarMessage message;
//     HmacSha256Mac mac;
//      } v0;
//  };

//  ===========================================================================
public class AuthenticatedMessage implements Encodable {
  public AuthenticatedMessage () {}
  Uint32 v;
  public Uint32 getDiscriminant() {
    return this.v;
  }
  public void setDiscriminant(Uint32 value) {
    this.v = value;
  }
  private AuthenticatedMessageV0 v0;
  public AuthenticatedMessageV0 getV0() {
    return this.v0;
  }
  public void setV0(AuthenticatedMessageV0 value) {
    this.v0 = value;
  }
  public static void encode(XdrDataOutputStream stream, AuthenticatedMessage encodedAuthenticatedMessage) throws IOException {
  stream.writeInt(encodedAuthenticatedMessage.getDiscriminant().intValue());
  switch (encodedAuthenticatedMessage.getDiscriminant().intValue()) {
  case 0:
  AuthenticatedMessageV0.encode(stream, encodedAuthenticatedMessage.v0);
  break;
  }
  }
  public static AuthenticatedMessage decode(XdrDataInputStream stream) throws IOException {
  AuthenticatedMessage decodedAuthenticatedMessage = new AuthenticatedMessage();
  Uint32 discriminant = Uint32.decode(stream);
  decodedAuthenticatedMessage.setDiscriminant(discriminant);
  switch (decodedAuthenticatedMessage.getDiscriminant().intValue()) {
  case 0:
  decodedAuthenticatedMessage.v0 = AuthenticatedMessageV0.decode(stream);
  break;
  }
    return decodedAuthenticatedMessage;
  }

  public static class AuthenticatedMessageV0 {
    public AuthenticatedMessageV0 () {}
    private Uint64 sequence;
    public Uint64 getSequence() {
      return this.sequence;
    }
    public void setSequence(Uint64 value) {
      this.sequence = value;
    }
    private StellarMessage message;
    public StellarMessage getMessage() {
      return this.message;
    }
    public void setMessage(StellarMessage value) {
      this.message = value;
    }
    private HmacSha256Mac mac;
    public HmacSha256Mac getMac() {
      return this.mac;
    }
    public void setMac(HmacSha256Mac value) {
      this.mac = value;
    }
    public static void encode(XdrDataOutputStream stream, AuthenticatedMessageV0 encodedAuthenticatedMessageV0) throws IOException{
      Uint64.encode(stream, encodedAuthenticatedMessageV0.sequence);
      StellarMessage.encode(stream, encodedAuthenticatedMessageV0.message);
      HmacSha256Mac.encode(stream, encodedAuthenticatedMessageV0.mac);
    }
    public static AuthenticatedMessageV0 decode(XdrDataInputStream stream) throws IOException {
      AuthenticatedMessageV0 decodedAuthenticatedMessageV0 = new AuthenticatedMessageV0();
      decodedAuthenticatedMessageV0.sequence = Uint64.decode(stream);
      decodedAuthenticatedMessageV0.message = StellarMessage.decode(stream);
      decodedAuthenticatedMessageV0.mac = HmacSha256Mac.decode(stream);
      return decodedAuthenticatedMessageV0;
    }

  }

public byte[] encode() {
	// TODO Auto-generated method stub
	return null;
}
public static AuthenticatedMessage decode(byte[] msg) {
	// TODO Auto-generated method stub
	return null;
}
}
