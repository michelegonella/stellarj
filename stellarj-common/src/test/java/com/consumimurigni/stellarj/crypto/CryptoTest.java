package com.consumimurigni.stellarj.crypto;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.Test;

import com.consuminurigni.stellarj.common.Hex;
import com.consuminurigni.stellarj.xdr.HmacSha256Key;
import com.consuminurigni.stellarj.xdr.PublicKey;
import com.consuminurigni.stellarj.xdr.Signature;

public class CryptoTest {

	static HashMap<byte[], String> hexTestVectors = new HashMap<>();
	static {
		hexTestVectors.put(new byte[] {}, "");
		hexTestVectors.put(new byte[] {0x72}, "72");
		hexTestVectors.put(new byte[] {0x54, 0x4c}, "544c");
		hexTestVectors.put(new byte[] {0x34, 0x75, 0x52, 0x45, 0x34, 0x75}, "347552453475");
		hexTestVectors.put(new byte[] {0x4f, 0x46, 0x79, 0x58, 0x43, 0x6d, 0x68, 0x37, 0x51}, "4f467958436d683751");
	}
		@Test
		public void test_random()
		{
		    SecretKey k1 = SecretKey.random();
		    SecretKey k2 = SecretKey.random();
//		    LOG(DEBUG) << "k1: " << k1.getStrKeySeed().value;
//		    LOG(DEBUG) << "k2: " << k2.getStrKeySeed().value;
		    assertTrue(! k1.getStrKeySeed().equals(k2.getStrKeySeed()));
		}

		@Test
		public void test_hex_tests()
		{
		    // Do some fixed test vectors.
		    for (Entry<byte[], String> pair : hexTestVectors.entrySet())
		    {
//		        LOG(DEBUG) << "fixed test vector hex: \"" << pair.second << "\"";

		        String enc = Hex.encode(pair.getKey());
		        assertTrue(enc.length() == pair.getValue().length());
		        assertEquals(enc, pair.getValue());

		        byte[] dec = Hex.decode(pair.getValue());
		        assertArrayEquals(pair.getKey(), dec);
		    }

//	    // Do 20 random round-trip tests.
//		    autocheck::check<byte[]>(
//		        [](byte[] v) {
//		            auto enc = binToHex(v);
//		            auto dec = hexToBin(enc);
////		            LOG(DEBUG) << "random round-trip hex: \"" << enc << "\"";
//		            assertTrue(v == dec);
//		            return v == dec;
//		        },
//		        20);
		}

		static TreeMap<String, String> sha256TestVectors = new TreeMap<>();
		static {
		sha256TestVectors.put("", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
		sha256TestVectors.put("a", "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb");
		sha256TestVectors.put("abc", "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
		sha256TestVectors.put("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq",
				"248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1");
		}

		@Test
		public void test_SHA256_tests()
		{
		    // Do some fixed test vectors.
		    for (Entry<String, String> pair : sha256TestVectors.entrySet())
		    {
//		        LOG(DEBUG) << "fixed test vector SHA256: \"" << pair.second << "\"";

		        String hash = Hex.encode(CryptoUtils.sha256(pair.getKey().getBytes(StandardCharsets.US_ASCII)));
		        assertEquals(hash, pair.getValue());
		    }
		}

		@Test
		public void test_Stateful_SHA256_tests()
		{
		    // Do some fixed test vectors.
		    for (Entry<String, String> pair : sha256TestVectors.entrySet())
		    {
//		        LOG(DEBUG) << "fixed test vector SHA256: \"" << pair.second << "\"";
		        SHA256 h = SHA256.create();
		        h.add(pair.getKey().getBytes(StandardCharsets.US_ASCII));
		        String hash = Hex.encode(h.finish().getUint256());
		        assertEquals(hash, pair.getValue());
		    }
		}

		@Test
		public void test_HMAC_vector()
		{
			HmacSha256Key k = HmacSha256Key.of(new byte[] {'k','e','y'});
		    byte[] s = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.US_ASCII);
		    byte[] h = Hex.decode(
		        "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
		    byte[] v = CryptoUtils.hmacSHA256(k, s);
		    assertArrayEquals(h, v);
		    assertTrue(CryptoUtils.hmacSHA256Verify(k, s, h));
		}

		@Test
		public void test_HKDF_vector()
		{
		    byte[] ikm = Hex.decode("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		    HmacSha256Key prk = HmacSha256Key.of(Hex.decode256(
		        "19ef24a32c717b167f33a91d6f648bdf96596776afdb6377ac434c1c293ccb04"));
		    HmacSha256Key okm = HmacSha256Key.of(Hex.decode256(
		        "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d"));
		    assertEquals(SHA256.hkdfExtract(ikm), prk);
		    byte[] empty = new byte[0];
		    assertEquals(SHA256.hkdfExpand(prk, empty), okm);
		}

		@Test
		public void test_sign_tests()
		{
		    SecretKey sk = SecretKey.random();
		    PublicKey pk = sk.getPublicKey();
//		    LOG(DEBUG) << "generated random secret key seed: " << sk.getStrKeySeed().value;
//		    LOG(DEBUG) << "corresponding public key: " << KeyUtils::toStrKey(pk);

		    assertEquals(SecretKey.fromStrKeySeed(sk.getStrKeySeed().getValue()), sk);

		    String msg = "hello";
		    Signature sig = sk.sign(msg.getBytes(StandardCharsets.UTF_8));

//		    LOG(DEBUG) << "formed signature: " << binToHex(sig);
//
//		    LOG(DEBUG) << "checking signature-verify";
		    assertTrue(PubKeyUtils.verifySig(pk, sig, msg.getBytes(StandardCharsets.UTF_8)));

//		    LOG(DEBUG) << "checking verify-failure on bad message";
		    assertFalse(PubKeyUtils.verifySig(pk, sig, "helloo".getBytes(StandardCharsets.UTF_8)));

//		    LOG(DEBUG) << "checking verify-failure on bad signature";
		    
		    byte[] modified = sig.getSignature();
		    modified[4] ^= 1;
		    sig.setSignature(modified);
		    assertFalse(PubKeyUtils.verifySig(pk, sig, msg.getBytes(StandardCharsets.UTF_8)));
		}
//
//		struct SignVerifyTestcase
//		{
//		    SecretKey key;
//		    PublicKey pub;
//		    byte[] msg;
//		    Signature sig;
//		    void
//		    sign()
//		    {
//		        sig = key.sign(msg);
//		    }
//		    void
//		    verify()
//		    {
//		        assertTrue(PubKeyUtils::verifySig(pub, sig, msg));
//		    }
//		    static SignVerifyTestcase
//		    create()
//		    {
//		        SignVerifyTestcase st;
//		        st.key = SecretKey::random();
//		        st.pub = st.key.getPublicKey();
//		        st.msg = randomBytes(256);
//		        return st;
//		    }
//		};
//
//		@Test
//		public void test_sign_and_verify_benchmarking()
//		{
//		    size_t n = 100000;
//		    std::vector<SignVerifyTestcase> cases;
//		    for (size_t i = 0; i < n; ++i)
//		    {
//		        cases.push_back(SignVerifyTestcase::create());
//		    }
//
////		    LOG(INFO) << "Benchmarking " << n << " signatures and verifications";
//		    {
//		        TIMED_SCOPE(timerBlkObj, "signing");
//		        for (auto& c : cases)
//		        {
//		            c.sign();
//		        }
//		    }
//
//		    {
//		        TIMED_SCOPE(timerBlkObj, "verifying");
//		        for (auto& c : cases)
//		        {
//		            c.verify();
//		        }
//		    }
//		}
//
//		@Test
//		public void test_StrKey_tests()
//		{
//		    std::regex b32("^([A-Z2-7])+$");
//		    std::regex b32Pad("^([A-Z2-7])+(=|===|====|======)?$");
//
//		    autocheck::generator<byte[]> input;
//
//		    uint8_t version = 2;
//
//		    // check round trip
//		    for (int size = 0; size < 100; size++)
//		    {
//		        byte[] in(input(size));
//
//		        String encoded = strKey::toStrKey(version, in).value;
//
//		        assertTrue(encoded.size() == ((size + 3 + 4) / 5 * 8));
//
//		        // check the no padding case
//		        if ((size + 3) % 5 == 0)
//		        {
//		            assertTrue(std::regex_match(encoded, b32));
//		        }
//		        else
//		        {
//		            assertTrue(std::regex_match(encoded, b32Pad));
//		        }
//
//		        uint8_t decodedVer = 0;
//		        byte[] decoded;
//		        assertTrue(strKey::fromStrKey(encoded, decodedVer, decoded));
//
//		        assertTrue(decodedVer == version);
//		        assertTrue(decoded == in);
//		    }
//
//		    // basic corruption check on a fixed size
//		    size_t n_corrupted = 0;
//		    size_t n_detected = 0;
//
//		    for (int round = 0; round < 5; round++)
//		    {
//		        const int expectedSize = 32;
//		        byte[] in(input(expectedSize));
//		        String encoded = strKey::toStrKey(version, in).value;
//
//		        for (size_t p = 0u; p < encoded.size(); p++)
//		        {
//		            for (int st = 0; st < 4; st++)
//		            {
//		                String corrupted(encoded);
//		                auto pos = corrupted.begin() + p;
//		                switch (st)
//		                {
//		                case 0:
//		                    if (corrupted[p] == 'A' && p + 1 == encoded.size())
//		                    {
//		                        // trailing 'A' is equivalent to 0 (and can be dropped)
//		                        continue;
//		                    }
//		                    else
//		                    {
//		                        corrupted.erase(pos);
//		                        break;
//		                    }
//		                case 1:
//		                    corrupted[p]++;
//		                    break;
//		                case 2:
//		                    corrupted.insert(pos, corrupted[p]);
//		                    break;
//		                default:
//		                    if (p > 0 && corrupted[p] != corrupted[p - 1])
//		                    {
//		                        std::swap(corrupted[p], corrupted[p - 1]);
//		                    }
//		                    else
//		                    {
//		                        continue;
//		                    }
//		                }
//		                uint8_t ver;
//		                byte[] dt;
//		                if (corrupted != encoded)
//		                {
//		                    n_corrupted++;
//		                    bool res = !strKey::fromStrKey(corrupted, ver, dt);
//		                    if (res)
//		                    {
//		                        ++n_detected;
//		                    }
//		                    else
//		                    {
//		                        LOG(WARNING) << "Failed to detect strkey corruption";
//		                        LOG(WARNING) << " original: " << encoded;
//		                        LOG(WARNING) << "  corrupt: " << corrupted;
//		                    }
//		                }
//		            }
//		        }
//		    }
//
//		    // CCITT CRC16 theoretical maximum "uncorrelated error" detection rate
//		    // is 99.9984% (1 undetected failure in 2^16); but we're not running an
//		    // infinite (or even 2^16) sized set of inputs and our mutations are
//		    // highly structured, so we give it some leeway. This is arbitrary but
//		    // from watching the test above we seem to only get one undetected
//		    // corruption pair in maybe 50 runs failing, each run being about 1000
//		    // cases. To give us good odds of making it through integration tests
//		    // we set the threshold quite wide here, to 98%. The test is very
//		    // slighly nondeterministic but this should give it plenty of leeway.
//
//		    double detectionRate =
//		        (((double)n_detected) / ((double)n_corrupted)) * 100.0;
//		    LOG(INFO) << "CRC16 error-detection rate " << detectionRate;
//		    assertTrue(detectionRate > 98.0);
//		}
//
//		@Test
//		public void test_base64_tests()
//		{
//		    autocheck::generator<byte[]> input;
//		    // check round trip
//		    for (int s = 0; s < 100; s++)
//		    {
//		        byte[] in(input(s));
//
//		        String encoded = bn::encode_b64(in);
//
//		        byte[] decoded;
//
//		        bn::decode_b64(encoded, decoded);
//
//		        assertTrue(in == decoded);
//		    }

}
