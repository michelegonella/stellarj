// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.consumimurigni.stellarj.ledger.xdr;


import java.io.IOException;

import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

// === xdr source ============================================================

//  struct SCPHistoryEntryV0
//  {
//      SCPQuorumSet quorumSets<>; // additional quorum sets used by ledgerMessages
//      LedgerSCPMessages ledgerMessages;
//  };

//  ===========================================================================
public class SCPHistoryEntryV0  {
  public SCPHistoryEntryV0 () {}
  private SCPQuorumSet[] quorumSets;
  public SCPQuorumSet[] getQuorumSets() {
    return this.quorumSets;
  }
  public void setQuorumSets(SCPQuorumSet[] value) {
    this.quorumSets = value;
  }
  private LedgerSCPMessages ledgerMessages;
  public LedgerSCPMessages getLedgerMessages() {
    return this.ledgerMessages;
  }
  public void setLedgerMessages(LedgerSCPMessages value) {
    this.ledgerMessages = value;
  }
  public static void encode(XdrDataOutputStream stream, SCPHistoryEntryV0 encodedSCPHistoryEntryV0) throws IOException{
    int quorumSetssize = encodedSCPHistoryEntryV0.getQuorumSets().length;
    stream.writeInt(quorumSetssize);
    for (int i = 0; i < quorumSetssize; i++) {
      SCPQuorumSet.encode(stream, encodedSCPHistoryEntryV0.quorumSets[i]);
    }
    LedgerSCPMessages.encode(stream, encodedSCPHistoryEntryV0.ledgerMessages);
  }
  public static SCPHistoryEntryV0 decode(XdrDataInputStream stream) throws IOException {
    SCPHistoryEntryV0 decodedSCPHistoryEntryV0 = new SCPHistoryEntryV0();
    int quorumSetssize = stream.readInt();
    decodedSCPHistoryEntryV0.quorumSets = new SCPQuorumSet[quorumSetssize];
    for (int i = 0; i < quorumSetssize; i++) {
      decodedSCPHistoryEntryV0.quorumSets[i] = SCPQuorumSet.decode(stream);
    }
    decodedSCPHistoryEntryV0.ledgerMessages = LedgerSCPMessages.decode(stream);
    return decodedSCPHistoryEntryV0;
  }
}
