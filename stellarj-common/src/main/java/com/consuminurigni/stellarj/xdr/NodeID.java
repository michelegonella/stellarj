// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.consuminurigni.stellarj.xdr;


import java.io.IOException;

// === xdr source ============================================================

//  typedef PublicKey NodeID;

//  ===========================================================================
public class NodeID  extends PublicKey {
  private PublicKey NodeID;
public NodeID() {
	// TODO Auto-generated constructor stub
}
public PublicKey getNodeID() {
    return this.NodeID;
  }
  public void setNodeID(PublicKey value) {
    this.NodeID = value;
  }
  public static void encode(XdrDataOutputStream stream, NodeID  encodedNodeID) throws IOException {
  PublicKey.encode(stream, encodedNodeID.NodeID);
  }
  public static NodeID decode(XdrDataInputStream stream) throws IOException {
    NodeID decodedNodeID = new NodeID();
  decodedNodeID.NodeID = PublicKey.decode(stream);
    return decodedNodeID;
  }
public boolean eq(NodeID localNodeID) {
	// TODO Auto-generated method stub
	return false;
}
public byte[] encode() {
	// TODO Auto-generated method stub
	return null;
}
public static NodeID of(PublicKey n) {
	// TODO Auto-generated method stub
	return null;
}
}
