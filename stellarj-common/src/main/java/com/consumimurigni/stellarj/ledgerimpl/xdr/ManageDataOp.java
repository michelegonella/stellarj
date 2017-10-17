// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.consumimurigni.stellarj.ledgerimpl.xdr;


import java.io.IOException;

import com.consumimurigni.stellarj.ledger.xdr.DataValue;
import com.consuminurigni.stellarj.xdr.String64;
import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

// === xdr source ============================================================

//  struct ManageDataOp
//  {
//      string64 dataName; 
//      DataValue* dataValue;   // set to null to clear
//  };

//  ===========================================================================
public class ManageDataOp  {
  public ManageDataOp () {}
  private String64 dataName;
  public String64 getDataName() {
    return this.dataName;
  }
  public void setDataName(String64 value) {
    this.dataName = value;
  }
  private DataValue dataValue;
  public DataValue getDataValue() {
    return this.dataValue;
  }
  public void setDataValue(DataValue value) {
    this.dataValue = value;
  }
  public static void encode(XdrDataOutputStream stream, ManageDataOp encodedManageDataOp) throws IOException{
    String64.encode(stream, encodedManageDataOp.dataName);
    if (encodedManageDataOp.dataValue != null) {
    stream.writeInt(1);
    DataValue.encode(stream, encodedManageDataOp.dataValue);
    } else {
    stream.writeInt(0);
    }
  }
  public static ManageDataOp decode(XdrDataInputStream stream) throws IOException {
    ManageDataOp decodedManageDataOp = new ManageDataOp();
    decodedManageDataOp.dataName = String64.decode(stream);
    int dataValuePresent = stream.readInt();
    if (dataValuePresent != 0) {
    decodedManageDataOp.dataValue = DataValue.decode(stream);
    }
    return decodedManageDataOp;
  }
}
