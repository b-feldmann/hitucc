package hit_ucc;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HitUCCApp {

	public static final String ACTOR_SYSTEM_NAME = "hit-ucc";

	public static void main(String[] args) {

		PeerCommand peerCommand = new PeerCommand();
		JCommander jCommander = JCommander.newBuilder()
				.addCommand(HitUCCPeerSystem.PEER_ROLE, peerCommand)
				.build();

		try {
			jCommander.parse(args);

			if (jCommander.getParsedCommand() == null) {
				throw new ParameterException("No command given.");
			}

			switch (jCommander.getParsedCommand()) {
				case HitUCCPeerSystem.PEER_ROLE:
					HitUCCPeerSystem.start(ACTOR_SYSTEM_NAME, peerCommand.workers, peerCommand.input, peerCommand.csvDelimiter.charAt(0), peerCommand.csvSkipHeader, peerCommand.output, peerCommand.dataDuplicationFactor, peerCommand.nullEqualsNull, peerCommand.host, peerCommand.port);
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

	abstract static class CommandBase {

		public static final int DEFAULT_PEER_PORT = 7876;
		public static final int DEFAULT_WORKERS = 4;
		public static final int DEFAULT_DATA_DUPLICATION_FACTOR = 5;
		public static final boolean DEFAULT_NULL_EQUALS_EQUALS = false;
		public static final boolean DEFAULT_CSV_SKIP_HEADER = true;

		@Parameter(names = {"-h", "--host"}, description = "this machine's host name or IP to bind against")
		String host = this.getDefaultHost();
		@Parameter(names = {"-p", "--port"}, description = "port to bind against", required = false)
		int port = this.getDefaultPort();
		@Parameter(names = {"-w", "--workers"}, description = "number of workers to start locally", required = false)
		int workers = DEFAULT_WORKERS;

		String getDefaultHost() {
			try {
				return InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				return "localhost";
			}
		}

		abstract int getDefaultPort();
	}

	@Parameters(commandDescription = "start a peer to peer actor system")
	static class PeerCommand extends CommandBase {

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

		@Override
		int getDefaultPort() {
			return DEFAULT_PEER_PORT;
		}
	}
}
