package com.consumimurigni.stellarj.scp.xdr;

import static com.consumimurigni.stellarj.scp.QuorumSetUtils.isQuorumSetSane;
import static com.consumimurigni.stellarj.scp.QuorumSetUtils.normalizeQSet;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.consumimurigni.stellarj.crypto.CryptoUtils;
import com.consumimurigni.stellarj.crypto.SecretKey;
import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.xdr.PublicKey;
import com.consuminurigni.stellarj.xdr.Uint32;

public class QuorumSetTest {

	@Test//{ t: 0 }
	public void test1() {
		SCPQuorumSet qSet = newQuorumSet(0);
        check(qSet, false, qSet);
	}

    PublicKey makePublicKey(int i) {
        byte[] hash = CryptoUtils.sha256(new String("NODE_SEED_" + i).getBytes(StandardCharsets.US_ASCII));
        
        SecretKey secretKey = SecretKey.fromSeed(hash);
        return secretKey.getPublicKey();
    };

    SCPQuorumSet newQuorumSet(int threshold) {
		SCPQuorumSet qSet = new SCPQuorumSet();
        qSet.setThreshold(Uint32.ofPositiveInt(threshold));
        qSet.setInnerSets(new SCPQuorumSet[0]);
        qSet.setValidators(new PublicKey[0]);
        return qSet;
    }
    SCPQuorumSet makeSingleton(PublicKey key) {
    	SCPQuorumSet result = new SCPQuorumSet();
        result.setThreshold(Uint32.ONE);
        result.setValidators(new PublicKey[] {key});
        return result;
    };

    List<PublicKey> keys = new LinkedList<>();
    {
        for (int i = 0; i < 1001; i++)
        {
            keys.add(makePublicKey(i));
        }
    }

    void check (SCPQuorumSet qSetCheck, boolean expected,
                     SCPQuorumSet expectedSelfQSet) {
        // first, without normalization
        assertTrue(expected == isQuorumSetSane(qSetCheck, false));

        // secondary test: attempts to build local node with the set
        // (this normalizes the set)
        SCPQuorumSet normalizedQSet = new SCPQuorumSet(qSetCheck);
        normalizeQSet(normalizedQSet);
        boolean selfIsSane = isQuorumSetSane(qSetCheck, false);

        assertTrue(expected == selfIsSane);
        assertTrue(expectedSelfQSet.equals(normalizedQSet));
    };

}
