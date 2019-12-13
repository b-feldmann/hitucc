package hitucc;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

public class HitUCCApp {
	public static void main(String[] args) {

		PeerCommand peerCommand = new PeerCommand();
		PeerHostCommand peerHostCommand = new PeerHostCommand();
		JCommander jCommander = JCommander.newBuilder()
				.addCommand(HitUCCPeerHostSystem.PEER_HOST_ROLE, peerHostCommand)
				.addCommand(HitUCCPeerSystem.PEER_ROLE, peerCommand)
				.build();

		try {
			jCommander.parse(args);

			if (jCommander.getParsedCommand() == null) {
				throw new ParameterException("No command given.");
			}

			switch (jCommander.getParsedCommand()) {
				case HitUCCPeerHostSystem.PEER_HOST_ROLE:
					HitUCCPeerHostSystem.start(peerHostCommand.workers, peerHostCommand.input, peerHostCommand.csvDelimiter.charAt(0), peerHostCommand.csvSkipHeader, peerHostCommand.output, peerHostCommand.dataDuplicationFactor, peerHostCommand.nullEqualsNull, peerHostCommand.bindHost, peerHostCommand.bindPort);
					break;
				case HitUCCPeerSystem.PEER_ROLE:
					HitUCCPeerSystem.start(peerCommand.workers, peerCommand.bindHost, peerCommand.bindPort);
					break;
				default:
					throw new AssertionError();
			}

		} catch (ParameterException e) {
			System.out.printf("Could not parse args: %s\n", e.getMessage());
			if (jCommander.getParsedCommand() == null) {
				jCommander.usage();
			} else {
				jCommander.usage(jCommander.getParsedCommand());
			}
			System.exit(1);
		}
	}

	static class PeerCommand {
		public static final int DEFAULT_WORKERS = Runtime.getRuntime().availableProcessors() - 1;
		public static final int DEFAULT_DATA_DUPLICATION_FACTOR = 0;
		public static final boolean DEFAULT_NULL_EQUALS_EQUALS = false;
		public static final boolean DEFAULT_CSV_SKIP_HEADER = false;

		@Parameter(names = {"-w", "--workers"}, description = "number of workers to start locally", required = false)
		int workers = DEFAULT_WORKERS;

		@Parameter(names = {"-bh", "--bind-host"}, description = "this machine's host name or IP to bind against")
		String bindHost = "0.0.0.0";
		@Parameter(names = {"-bp", "--bind-port"}, description = "port to bind against", required = false)
		int bindPort = -1;
	}

	@Parameters(commandDescription = "start a peer to peer host actor system")
	static class PeerHostCommand extends PeerCommand {

		@Parameter(names = {"-ddf", "--dataDuplicationFactor"},
				description = "Describes how often the data should be duplicated and send to other nodes in the network. Determines the batch count.",
				required = false)
		int dataDuplicationFactor = DEFAULT_DATA_DUPLICATION_FACTOR;

		@Parameter(names = {"-nen", "--nullEqualsNull"},
				description = "If null should equals null",
				required = false)
		boolean nullEqualsNull = DEFAULT_NULL_EQUALS_EQUALS;

		@Parameter(names = {"-i", "--input"},
				description = "Input csv file",
				required = true)
		String input;

		@Parameter(names = {"-csv_d", "--csvDelimiter"},
				description = "Delimiter of the csv file. Default is ','",
				required = false)
		String csvDelimiter = ",";

		@Parameter(names = {"-csv_sh", "--csvSkipHeader"},
				description = "Whether to skip the header of the csv or not. Default is 'true'",
				required = false)
		boolean csvSkipHeader = DEFAULT_CSV_SKIP_HEADER;

		@Parameter(names = {"-o", "--o"},
				description = "Output file with all accumulated UCCs",
				required = false)
		String output;
	}
}
