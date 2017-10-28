package com.consumimurigni.stellarj.main;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class Main {

	public static int main(String[] args) {

		return -999999999;
	}
	static Options opts = new Options();
	static {
		Option OPT_BASE64 = Option.builder().longOpt("base64").desc("Use base64 for --printtxn and --signtxn").build();
		Option OPT_CATCHUP_COMPLETE = Option.builder().longOpt("catchup-complete").desc("Do a complete catchup, then quit").build();
		Option OPT_CMD = Option.builder().longOpt("c").hasArg().desc("Send a command to local stellar-core. try '--c help' for more information").build();
		Option OPT_CONF = Option.builder().longOpt("conf").hasArg().desc("Specify a config file ('-' for STDIN, default 'stellar-core.cfg')").argName("FILE").build();
		Option OPT_CONVERTID = Option.builder().longOpt("convertid").hasArg().desc("Displays ID in all known forms").argName("ID").build();
		Option OPT_DUMPXDR = Option.builder().longOpt("dumpxdr").hasArg().desc("Dump an XDR file, for debugging").argName("FILE").build();
		Option OPT_LOADXDR = Option.builder().longOpt("loadxdr").hasArg().desc("Load an XDR bucket file, for testing").argName("FILE").build();
		Option OPT_FORCESCP = Option.builder().longOpt("forcescp").optionalArg(true).desc("Next time stellar-core is run, SCP will start with the local ledger rather than waiting to hear from the network.").build();
		Option OPT_FUZZ = Option.builder().longOpt("fuzz").hasArg().desc("Run a single fuzz input and exit").argName("FILE").build();
		Option OPT_GENFUZZ = Option.builder().longOpt("genfuzz").hasArg().desc("Generate a random fuzzer input file").argName("FILE").build();
		Option OPT_GENSEED = Option.builder().longOpt("genseed").desc("Generate and print a random node seed").build();
		Option OPT_HELP = Option.builder().longOpt("help").desc("Display this string").build();
		Option OPT_INFERQUORUM = Option.builder().longOpt("inferquorum").optionalArg(true).desc("Print a quorum set inferred from history").build();
		Option OPT_CHECKQUORUM = Option.builder().longOpt("checkquorum").optionalArg(true).desc("Check quorum intersection from history").build();
		Option OPT_GRAPHQUORUM = Option.builder().longOpt("graphquorum").optionalArg(true).desc("Print a quorum set graph from history").build();
		Option OPT_OFFLINEINFO = Option.builder().longOpt("offlineinfo").desc("Return information for an offline instance").build();
		Option OPT_LOGLEVEL = Option.builder().longOpt("ll").hasArg().desc("Set the log level. (redundant with --c ll but you need this form for the tests.) LEVEL can be: trace, debug, info, error, fatal").argName("LEVEL").build();
		Option OPT_METRIC = Option.builder().longOpt("metric").hasArg().desc("Report metric METRIC on exit").argName("METRIC").build();
		Option OPT_NEWDB = Option.builder().longOpt("newdb").desc("Creates or restores the DB to the genesis ledger").build();
		Option OPT_NEWHIST = Option.builder().longOpt("newhist").hasArg().desc("Initialize the named history archive ARCH").argName("ARCH").build();
		Option OPT_PRINTTXN = Option.builder().longOpt("printtxn").hasArg().desc("Pretty-print one transaction envelope, then quit").argName("FILE").build();
		Option OPT_SIGNTXN = Option.builder().longOpt("signtxn").hasArg().desc("Add signature to transaction envelope, then quit; (Key is read from stdin or terminal, as  appropriate.)").argName("FILE").build();
		Option OPT_SEC2PUB = Option.builder().longOpt("sec2pub").desc("Print the public key corresponding to a secret key").build();
		Option OPT_NETID = Option.builder().longOpt("netid").hasArg().desc("Specify network ID for subsequent signtxn (Default is STELLAR_NETWORK_ID environmentvariable)").argName("STRING").build();
		Option OPT_TEST = Option.builder().longOpt("test").desc("Run self-tests").build();
		Option OPT_VERSION = Option.builder().longOpt("version").desc("Print version information").build();

		opts.addOption(OPT_BASE64);
		opts.addOption(OPT_CATCHUP_COMPLETE);
		opts.addOption(OPT_CMD);
		opts.addOption(OPT_CONF);
		opts.addOption(OPT_CONVERTID);
		opts.addOption(OPT_DUMPXDR);
		opts.addOption(OPT_LOADXDR);
		opts.addOption(OPT_FORCESCP);
		opts.addOption(OPT_FUZZ);
		opts.addOption(OPT_GENFUZZ);
		opts.addOption(OPT_GENSEED);
		opts.addOption(OPT_HELP);
		opts.addOption(OPT_INFERQUORUM);
		opts.addOption(OPT_CHECKQUORUM);
		opts.addOption(OPT_GRAPHQUORUM);
		opts.addOption(OPT_OFFLINEINFO);
		opts.addOption(OPT_LOGLEVEL);
		opts.addOption(OPT_METRIC);
		opts.addOption(OPT_NEWDB);
		opts.addOption(OPT_NEWHIST);
		opts.addOption(OPT_PRINTTXN);
		opts.addOption(OPT_SIGNTXN);
		opts.addOption(OPT_SEC2PUB);
		opts.addOption(OPT_NETID);
		opts.addOption(OPT_TEST);
		opts.addOption(OPT_VERSION);
	}
}
