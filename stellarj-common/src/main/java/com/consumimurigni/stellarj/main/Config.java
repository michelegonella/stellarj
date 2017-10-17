package com.consumimurigni.stellarj.main;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.consumimurigni.stellarj.crypto.SecretKey;
import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.xdr.Int32;
import com.consuminurigni.stellarj.xdr.PublicKey;
import com.consuminurigni.stellarj.xdr.Uint32;

public class Config {
    public static final int CURRENT_LEDGER_PROTOCOL_VERSION = 8;


    enum TestDbMode
    {
        TESTDB_DEFAULT,
        TESTDB_IN_MEMORY_SQLITE,
        TESTDB_ON_DISK_SQLITE,
        TESTDB_POSTGRESQL,
        TESTDB_MODES
    };
    
    // application config

    // The default way stellar-core starts is to load the state from disk and
    // catch
    // up to the network before starting SCP.
    // If you need different behavior you need to use --newdb or --force-scp
    // which sets the following flags:

    // SCP will start running immediately using the current local state to
    // participate in consensus. DO NOT INCLUDE THIS IN A CONFIG FILE
    public static final boolean FORCE_SCP;

    // This is a mode for testing. It prevents you from trying to connect to
    // other peers
    public static final boolean RUN_STANDALONE;

    // Mode for testing. Ledger will only close when told to over http
    public static final boolean MANUAL_CLOSE;

    // Whether to catchup "completely" (replaying all history); default is
    // false,
    // meaning catchup "minimally", using deltas to the most recent snapshot.
    public static final boolean CATCHUP_COMPLETE;

    // Number of "recent" ledgers before the current ledger to include in a
    // "minimal" catchup. Default is 0, and if CATCHUP_COMPLETE is set to
    // true, this is ignored.
    //
    // If you want, say, a week of history, set this to 120000.
    public static final Uint32 CATCHUP_RECENT;

    // Enables or disables automatic maintenance on startup
    public static final boolean MAINTENANCE_ON_STARTUP;

    // A config parameter that enables synthetic load generation on demand,
    // using the `generateload` runtime command (see CommandHandler.cpp). This
    // option only exists for stress-testing and should not be enabled in
    // production networks.
    public static final boolean ARTIFICIALLY_GENERATE_LOAD_FOR_TESTING;

    // A config parameter that reduces ledger close time to 1s and checkpoint
    // frequency to every 8 ledgers. Do not ever set this in production, as it
    // will make your history archives incompatible with those of anyone else.
    public static final boolean ARTIFICIALLY_ACCELERATE_TIME_FOR_TESTING;

    // A config parameter to override the close time (in seconds). Do not use
    // in production as it may render the network unstable.
    public static final Uint32 ARTIFICIALLY_SET_CLOSE_TIME_FOR_TESTING;

    // A config parameter that avoids resolving FutureBuckets before writing
    // them to the database's persistent state; this option exists only
    // for stress-testing the ability to resume from an interrupted merge,
    // and should be false in all normal cases.
    public static final boolean ARTIFICIALLY_PESSIMIZE_MERGES_FOR_TESTING;

    // A config to allow connections to localhost
    // this should only be enabled when testing as it's a security issue
    public static final boolean ALLOW_LOCALHOST_FOR_TESTING;

    // This is the number of failures you want to be able to tolerate.
    // You will need at least 3f+1 nodes in your quorum set.
    // If you don't have enough in your quorum set to tolerate the level you
    //  set here stellar-core won't run.
    public static final  Int32 FAILURE_SAFETY;

    // If set to true allows you to specify an unsafe quorum set.
    // Otherwise it won't start if you have your threshold % set too low.
    // You might want to set this if you are running your own network and
    //  aren't concerned with byzantine failures.
    public static final boolean UNSAFE_QUORUM;

    public static final Uint32 LEDGER_PROTOCOL_VERSION;
    public static final Optional<Instant> PREFERRED_UPGRADE_DATETIME;//TODO std::tm

    // note: all versions in the range
    // [OVERLAY_PROTOCOL_MIN_VERSION, OVERLAY_PROTOCOL_VERSION] must be handled
    public static final Uint32 OVERLAY_PROTOCOL_MIN_VERSION; // min overlay version understood
    public static final Uint32 OVERLAY_PROTOCOL_VERSION;     // max overlay version understood
    public static final String VERSION_STR;
    public static final String LOG_FILE_PATH;
    public static final String BUCKET_DIR_PATH;
    public static final Uint32 DESIRED_BASE_FEE;     // in stroops
    public static final Uint32 DESIRED_BASE_RESERVE; // in stroops
    public static final Uint32 DESIRED_MAX_TX_PER_LEDGER;
    public static final int HTTP_PORT; // what port to listen for commands
    public static final boolean PUBLIC_HTTP_PORT;    // if you accept commands from not localhost
    public static final int HTTP_MAX_CLIENT;      // maximum number of http clients, i.e backlog
    public static final String NETWORK_PASSPHRASE; // identifier for the network

    // overlay config
    public static final int PEER_PORT;
    public static final int TARGET_PEER_CONNECTIONS;
    public static final int MAX_PEER_CONNECTIONS;
    // Peers we will always try to stay connected to
    public static final List<String> PREFERRED_PEERS;
    public static final List<String> KNOWN_PEERS;

    // Preference can also be expressed by peer pubkey
    public static final List<String> PREFERRED_PEER_KEYS;

    // Whether to exclude peers that are not preferred.
    public static final boolean PREFERRED_PEERS_ONLY;

    // Percentage, between 0 and 100, of system activity (measured in terms
    // of both event-loop cycles and database time) below-which the system
    // will consider itself "loaded" and attempt to shed load. Set this
    // number low and the system will be tolerant of overloading. Set it
    // high and the system will be intolerant. By default it is 0, meaning
    // totally insensitive to overloading.
    public static final Uint32 MINIMUM_IDLE_PERCENT;

    // process-management config
    public static final int MAX_CONCURRENT_SUBPROCESSES;

    // SCP config
    public static final SecretKey NODE_SEED;
    public static final boolean NODE_IS_VALIDATOR;
    public static final SCPQuorumSet QUORUM_SET;

    // Invariants
    public static final boolean INVARIANT_CHECK_BALANCE;
    public static final boolean INVARIANT_CHECK_ACCOUNT_SUBENTRY_COUNT;
    public static final boolean INVARIANT_CHECK_CACHE_CONSISTENT_WITH_DATABASE;

    public static final Map<String, String> VALIDATOR_NAMES;

    // History config
    public static final Map<String, HistoryArchive> HISTORY;

    // Database config
    public static final SecretValue DATABASE;

    public static final List<String> COMMANDS;
    public static final List<String> REPORT_METRICS;

    public static final String NTP_SERVER;

	public String toShortString(PublicKey pk) {
		// TODO Auto-generated method stub
		return null;
	} // ntp server used to check if time is valid on host

	public @Nullable PublicKey resolveNodeID(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	public String toStrKey(PublicKey publicKey) {
		// TODO Auto-generated method stub
		return null;
	}


}
