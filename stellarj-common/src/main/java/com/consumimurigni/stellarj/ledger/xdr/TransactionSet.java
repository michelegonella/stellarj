// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.consumimurigni.stellarj.ledger.xdr;


import java.io.IOException;

import com.consuminurigni.stellarj.overlay.xdr.TransactionEnvelope;
import com.consuminurigni.stellarj.xdr.Encodable;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

// === xdr source ============================================================

//  struct TransactionSet
//  {
//      Hash previousLedgerHash;
//      TransactionEnvelope txs<>;
//  };

//  ===========================================================================
public class TransactionSet implements Encodable {
  public TransactionSet () {}
  private Hash previousLedgerHash;
  public Hash getPreviousLedgerHash() {
    return this.previousLedgerHash;
  }
  public void setPreviousLedgerHash(Hash value) {
    this.previousLedgerHash = value;
  }
  private TransactionEnvelope[] txs;
  public TransactionEnvelope[] getTxs() {
    return this.txs;
  }
  public void setTxs(TransactionEnvelope[] value) {
    this.txs = value;
  }
  public static void encode(XdrDataOutputStream stream, TransactionSet encodedTransactionSet) throws IOException{
    Hash.encode(stream, encodedTransactionSet.previousLedgerHash);
    int txssize = encodedTransactionSet.getTxs().length;
    stream.writeInt(txssize);
    for (int i = 0; i < txssize; i++) {
      TransactionEnvelope.encode(stream, encodedTransactionSet.txs[i]);
    }
  }
  public static TransactionSet decode(XdrDataInputStream stream) throws IOException {
    TransactionSet decodedTransactionSet = new TransactionSet();
    decodedTransactionSet.previousLedgerHash = Hash.decode(stream);
    int txssize = stream.readInt();
    decodedTransactionSet.txs = new TransactionEnvelope[txssize];
    for (int i = 0; i < txssize; i++) {
      decodedTransactionSet.txs[i] = TransactionEnvelope.decode(stream);
    }
    return decodedTransactionSet;
  }
@Override
public byte[] encode() {
	// TODO Auto-generated method stub
	return null;
}
}
