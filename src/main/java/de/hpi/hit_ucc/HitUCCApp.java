package de.hpi.hit_ucc;

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
		MasterCommand masterCommand = new MasterCommand();
		SlaveCommand slaveCommand = new SlaveCommand();
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
					HitUCCPeerSystem.start(ACTOR_SYSTEM_NAME, peerCommand.workers, peerCommand.dataDuplicationFactor,peerCommand.nullEqualsNull, peerCommand.host, peerCommand.port);
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
		public static final int DEFAULT_MASTER_PORT = 7877;
		public static final int DEFAULT_SLAVE_PORT = 7879;
		public static final int DEFAULT_WORKERS = 4;
		public static final int DEFAULT_DATA_DUPLICATION_FACTOR = 5;
		public static final boolean DEFAULT_NULL_EQUALS_EQUALS = false;

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

		@Override
		int getDefaultPort() {
			return DEFAULT_PEER_PORT;
		}
	}

	@Parameters(commandDescription = "start a master actor system")
	static class MasterCommand extends CommandBase {

		@Override
		int getDefaultPort() {
			return DEFAULT_MASTER_PORT;
		}
	}

	@Parameters(commandDescription = "start a slave actor system")
	static class SlaveCommand extends CommandBase {

		@Parameter(names = {"-mp", "--masterport"}, description = "port of the master", required = false)
		int masterport = DEFAULT_MASTER_PORT;
		@Parameter(names = {"-mh", "--masterhost"}, description = "host name or IP of the master", required = true)
		String masterhost;

		@Override
		int getDefaultPort() {
			return DEFAULT_SLAVE_PORT;
		}
	}
}
