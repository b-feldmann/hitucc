package hitucc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import hitucc.actors.PeerDataBouncer;
import hitucc.actors.PeerWorker;
import hitucc.actors.messages.TaskMessage;

import java.io.IOException;

public class HitUCCPeerHostSystem extends HitUCCSystem {

	public static final String PEER_HOST_ROLE = "host";

	public static void start(int workers, String input, char csvDelimiter, boolean csvSkipHeader, String output, int dataDuplicationFactor, boolean nullEqualsNull, String bindHost, int bindPort) {
		final Config config = ConfigFactory.parseString("akka.cluster.roles = [" + PEER_HOST_ROLE + "]\n").withFallback(ConfigFactory.load());
		String clusterName = config.getString("clustering.cluster.name");
		final ActorSystem system = createSystem(clusterName, config);

		final ActorRef[] dataBouncer = new ActorRef[1];

		int port = config.getInt("clustering.port");
		String host = config.getString("clustering.ip");

		System.out.println("#################### START ACTOR SYSTEM ####################\n" +
				"Address: " + host + ":" + port + "\n" +
				"Memory: " + Runtime.getRuntime().maxMemory() + " Bytes\n" +
				"Workers: " + workers + "\n" +
				"#################### ------------------ ####################");

		Cluster.get(system).registerOnMemberUp(() -> {
//			system.actorOf(ClusterListener.props(), ClusterListener.DEFAULT_NAME);
//			system.actorOf(MetricsListener.props(), MetricsListener.DEFAULT_NAME);

			for (int i = 0; i < workers; i++) {
				system.actorOf(PeerWorker.props(), PeerWorker.DEFAULT_NAME + i + ":" + port);
			}

			dataBouncer[0] = system.actorOf(PeerDataBouncer.props(workers), PeerDataBouncer.DEFAULT_NAME + ":" + port);

			String[][] table = null;
			try {
				table = ReadDataTable.readTable("data/" + input, csvDelimiter, csvSkipHeader);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			int systemCount = config.getInt("clustering.systems");

			dataBouncer[0].tell(new TaskMessage(table, table[0].length, dataDuplicationFactor, nullEqualsNull, Math.max(systemCount, 1)), ActorRef.noSender());
		});

		Cluster.get(system).registerOnMemberRemoved(() -> {
			System.out.println("Member removed.");
		});
	}
}
