package com.consumimurigni.stellarj.herder;

import java.util.List;
import java.util.stream.Collectors;

import com.consumimurigni.stellarj.ledger.xdr.StellarValue;
import com.consumimurigni.stellarj.scp.Slot;
import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;
import com.consuminurigni.stellarj.scp.xdr.SCPStatement;
import com.consuminurigni.stellarj.xdr.Hash;

public class HerderUtils {

	public static List<Hash> getTxSetHashes(SCPEnvelope envelope)
	{
		List<StellarValue> values = getStellarValues(envelope.getStatement());
		return values.stream().map((sv)-> sv.getTxSetHash()).collect(Collectors.toList());
	}

	static List<StellarValue> getStellarValues(SCPStatement statement)
	{
		List<StellarValue> result = Slot.getStatementValues(statement).stream().map((v) -> StellarValue.fromOpaque(v)).collect(Collectors.toList());
		return result;
	}

}
