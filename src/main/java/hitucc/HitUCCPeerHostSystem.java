package hitucc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import hitucc.actors.PeerDataBouncer;
import hitucc.actors.PeerWorker;
import hitucc.actors.Reaper;
import hitucc.actors.messages.TaskMessage;

import java.io.IOException;

public class HitUCCPeerHostSystem extends HitUCCSystem {

	public static final String PEER_HOST_ROLE = "host";

	public static void start(int workers, String input, char csvDelimiter, boolean csvSkipHeader, String output, int dataDuplicationFactor, boolean nullEqualsNull, String bindHost, int bindPort) {
		final Config config = ConfigFactory.parseString("akka.cluster.roles = [" + PEER_HOST_ROLE + "]\n").withFallback(ConfigFactory.load());
		String clusterName = config.getString("clustering.cluster.name");
		final ActorSystem system = createSystem(clusterName, config);

		int port = config.getInt("clustering.port");
		String host = config.getString("clustering.ip");

		System.out.println("#################### START ACTOR SYSTEM ####################\n" +
				"Address: " + host + ":" + port + "\n" +
				"Memory: " + Runtime.getRuntime().maxMemory() + " Bytes\n" +
				"Workers: " + workers + "\n" +
				"#################### ------------------ ####################");

//			system.actorOf(ClusterListener.props(), ClusterListener.DEFAULT_NAME);
//			system.actorOf(MetricsListener.props(), MetricsListener.DEFAULT_NAME);

//		system.actorOf(Reaper.props(), Reaper.DEFAULT_NAME + ":" + port);
		for (int i = 0; i < workers; i++) {
			system.actorOf(PeerWorker.props(), PeerWorker.DEFAULT_NAME + i + ":" + port);
		}
		final ActorRef dataBouncer = system.actorOf(PeerDataBouncer.props(workers), PeerDataBouncer.DEFAULT_NAME + ":" + port);

		Cluster.get(system).registerOnMemberUp(() -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			String[][] table = null;
			try {
				table = ReadDataTable.readTable("data/" + input, csvDelimiter, csvSkipHeader);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}

			int systemCount = config.getInt("clustering.systems");

			dataBouncer.tell(new TaskMessage(table, table[0].length, dataDuplicationFactor, nullEqualsNull, Math.max(systemCount, 1)), ActorRef.noSender());
		});

		Cluster.get(system).registerOnMemberRemoved(() -> {
			System.out.println("Member removed.");
		});
	}
}
