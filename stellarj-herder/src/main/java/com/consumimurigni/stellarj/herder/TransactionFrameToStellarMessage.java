package com.consumimurigni.stellarj.herder;

import java.util.function.Function;

import com.consumimurigni.stellarj.transactions.TransactionFrame;
import com.consuminurigni.stellarj.overlay.xdr.StellarMessage;

public interface TransactionFrameToStellarMessage extends Function<TransactionFrame, StellarMessage> {

}
