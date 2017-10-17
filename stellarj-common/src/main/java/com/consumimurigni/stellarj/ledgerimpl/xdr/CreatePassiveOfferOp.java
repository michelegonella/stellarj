// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.consumimurigni.stellarj.ledgerimpl.xdr;


import java.io.IOException;

import com.consumimurigni.stellarj.ledger.xdr.Asset;
import com.consumimurigni.stellarj.ledger.xdr.Price;
import com.consuminurigni.stellarj.xdr.Int64;
import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

// === xdr source ============================================================

//  struct CreatePassiveOfferOp
//  {
//      Asset selling; // A
//      Asset buying;  // B
//      int64 amount;  // amount taker gets. if set to 0, delete the offer
//      Price price;   // cost of A in terms of B
//  };

//  ===========================================================================
public class CreatePassiveOfferOp  {
  public CreatePassiveOfferOp () {}
  private Asset selling;
  public Asset getSelling() {
    return this.selling;
  }
  public void setSelling(Asset value) {
    this.selling = value;
  }
  private Asset buying;
  public Asset getBuying() {
    return this.buying;
  }
  public void setBuying(Asset value) {
    this.buying = value;
  }
  private Int64 amount;
  public Int64 getAmount() {
    return this.amount;
  }
  public void setAmount(Int64 value) {
    this.amount = value;
  }
  private Price price;
  public Price getPrice() {
    return this.price;
  }
  public void setPrice(Price value) {
    this.price = value;
  }
  public static void encode(XdrDataOutputStream stream, CreatePassiveOfferOp encodedCreatePassiveOfferOp) throws IOException{
    Asset.encode(stream, encodedCreatePassiveOfferOp.selling);
    Asset.encode(stream, encodedCreatePassiveOfferOp.buying);
    Int64.encode(stream, encodedCreatePassiveOfferOp.amount);
    Price.encode(stream, encodedCreatePassiveOfferOp.price);
  }
  public static CreatePassiveOfferOp decode(XdrDataInputStream stream) throws IOException {
    CreatePassiveOfferOp decodedCreatePassiveOfferOp = new CreatePassiveOfferOp();
    decodedCreatePassiveOfferOp.selling = Asset.decode(stream);
    decodedCreatePassiveOfferOp.buying = Asset.decode(stream);
    decodedCreatePassiveOfferOp.amount = Int64.decode(stream);
    decodedCreatePassiveOfferOp.price = Price.decode(stream);
    return decodedCreatePassiveOfferOp;
  }
}
