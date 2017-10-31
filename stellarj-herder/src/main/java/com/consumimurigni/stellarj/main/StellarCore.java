package com.consumimurigni.stellarj.main;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;

public class StellarCore {

	public static void main(String[] args) {
		new StellarCore().go(args);
	}
	private void go(String[] args) {
	    String cfgFile = "stellar-core.cfg";
	    String command = null;
	    Level logLevel = Level.INFO;
	    List<String> rest = new LinkedList<>();

	    Boolean forceSCP = null;
	    boolean base64 = false;
	    boolean catchupComplete = false;
	    boolean inferQuorum = false;
	    boolean checkQuorum = false;
	    boolean graphQuorum = false;
	    boolean newDB = false;
	    boolean getOfflineInfo = false;
	    String loadXdrBucket = "";
	    List<String> newHistories = new LinkedList<>();
	    List<String> metrics = new LinkedList<>();
	    CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
	    try {
			cmd = parser.parse( OPTS, args);
		} catch (ParseException e) {
			usage();
			System.exit(-1);
		}
		if (cmd.hasOption(OPT_BASE64.getOpt())) {
			base64 = true;
		}
		if (cmd.hasOption(OPT_CATCHUP_COMPLETE.getOpt())) {
			catchupComplete = true;
		}
		if (cmd.hasOption(OPT_CMD.getOpt())) {
			rest.add(cmd.getOptionValue(OPT_CMD.getOpt()));
		}
		if (cmd.hasOption(OPT_CONF.getOpt())) {
			cfgFile = cmd.getOptionValue(OPT_CONF.getOpt());
		}
		if (cmd.hasOption(OPT_CONVERTID.getOpt())) {
			//TODO StrKeyUtils//::logKey(std::cout, std::string(optarg));
            return;
		}
		if (cmd.hasOption(OPT_BASE64.getOpt())) {
			base64 = true;
		}
		if (cmd.hasOption(OPT_BASE64.getOpt())) {
			base64 = true;
		}
		
	    usage();
	}
	private void usage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "stellar-core", OPTS );
		
	}
		
		private static final Option OPT_BASE64 = Option.builder().longOpt("base64").desc("Use base64 for --printtxn and --signtxn").build();
		private static final Option OPT_CATCHUP_COMPLETE = Option.builder().longOpt("catchup-complete").desc("Do a complete catchup, then quit").build();
		private static final Option OPT_CMD = Option.builder().longOpt("c").hasArg().desc("Send a command to local stellar-core. try '--c help' for more information").build();
		private static final Option OPT_CONF = Option.builder().longOpt("conf").hasArg().desc("Specify a config file ('-' for STDIN, default 'stellar-core.cfg')").argName("FILE").build();
		private static final Option OPT_CONVERTID = Option.builder().longOpt("convertid").hasArg().desc("Displays ID in all known forms").argName("ID").build();
		private static final Option OPT_DUMPXDR = Option.builder().longOpt("dumpxdr").hasArg().desc("Dump an XDR file, for debugging").argName("FILE").build();
		private static final Option OPT_LOADXDR = Option.builder().longOpt("loadxdr").hasArg().desc("Load an XDR bucket file, for testing").argName("FILE").build();
		private static final Option OPT_FORCESCP = Option.builder().longOpt("forcescp").optionalArg(true).desc("Next time stellar-core is run, SCP will start with the local ledger rather than waiting to hear from the network.").build();
		private static final Option OPT_FUZZ = Option.builder().longOpt("fuzz").hasArg().desc("Run a single fuzz input and exit").argName("FILE").build();
		private static final Option OPT_GENFUZZ = Option.builder().longOpt("genfuzz").hasArg().desc("Generate a random fuzzer input file").argName("FILE").build();
		private static final Option OPT_GENSEED = Option.builder().longOpt("genseed").desc("Generate and print a random node seed").build();
		private static final Option OPT_HELP = Option.builder().longOpt("help").desc("Display this string").build();
		private static final Option OPT_INFERQUORUM = Option.builder().longOpt("inferquorum").optionalArg(true).desc("Print a quorum set inferred from history").build();
		private static final Option OPT_CHECKQUORUM = Option.builder().longOpt("checkquorum").optionalArg(true).desc("Check quorum intersection from history").build();
		private static final Option OPT_GRAPHQUORUM = Option.builder().longOpt("graphquorum").optionalArg(true).desc("Print a quorum set graph from history").build();
		private static final Option OPT_OFFLINEINFO = Option.builder().longOpt("offlineinfo").desc("Return information for an offline instance").build();
		private static final Option OPT_LOGLEVEL = Option.builder().longOpt("ll").hasArg().desc("Set the log level. (redundant with --c ll but you need this form for the tests.) LEVEL can be: trace, debug, info, error, fatal").argName("LEVEL").build();
		private static final Option OPT_METRIC = Option.builder().longOpt("metric").hasArg().desc("Report metric METRIC on exit").argName("METRIC").build();
		private static final Option OPT_NEWDB = Option.builder().longOpt("newdb").desc("Creates or restores the DB to the genesis ledger").build();
		private static final Option OPT_NEWHIST = Option.builder().longOpt("newhist").hasArg().desc("Initialize the named history archive ARCH").argName("ARCH").build();
		private static final Option OPT_PRINTTXN = Option.builder().longOpt("printtxn").hasArg().desc("Pretty-print one transaction envelope, then quit").argName("FILE").build();
		private static final Option OPT_SIGNTXN = Option.builder().longOpt("signtxn").hasArg().desc("Add signature to transaction envelope, then quit; (Key is read from stdin or terminal, as  appropriate.)").argName("FILE").build();
		private static final Option OPT_SEC2PUB = Option.builder().longOpt("sec2pub").desc("Print the public key corresponding to a secret key").build();
		private static final Option OPT_NETID = Option.builder().longOpt("netid").hasArg().desc("Specify network ID for subsequent signtxn (Default is STELLAR_NETWORK_ID environmentvariable)").argName("STRING").build();
		private static final Option OPT_TEST = Option.builder().longOpt("test").desc("Run self-tests").build();
		private static final Option OPT_VERSION = Option.builder().longOpt("version").desc("Print version information").build();

		private static final Options OPTS = new Options();
		static {
		OPTS.addOption(OPT_BASE64);
		OPTS.addOption(OPT_CATCHUP_COMPLETE);
		OPTS.addOption(OPT_CMD);
		OPTS.addOption(OPT_CONF);
		OPTS.addOption(OPT_CONVERTID);
		OPTS.addOption(OPT_DUMPXDR);
		OPTS.addOption(OPT_LOADXDR);
		OPTS.addOption(OPT_FORCESCP);
		OPTS.addOption(OPT_FUZZ);
		OPTS.addOption(OPT_GENFUZZ);
		OPTS.addOption(OPT_GENSEED);
		OPTS.addOption(OPT_HELP);
		OPTS.addOption(OPT_INFERQUORUM);
		OPTS.addOption(OPT_CHECKQUORUM);
		OPTS.addOption(OPT_GRAPHQUORUM);
		OPTS.addOption(OPT_OFFLINEINFO);
		OPTS.addOption(OPT_LOGLEVEL);
		OPTS.addOption(OPT_METRIC);
		OPTS.addOption(OPT_NEWDB);
		OPTS.addOption(OPT_NEWHIST);
		OPTS.addOption(OPT_PRINTTXN);
		OPTS.addOption(OPT_SIGNTXN);
		OPTS.addOption(OPT_SEC2PUB);
		OPTS.addOption(OPT_NETID);
		OPTS.addOption(OPT_TEST);
		OPTS.addOption(OPT_VERSION);
	}
}
