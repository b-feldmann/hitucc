package hit_ucc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import hit_ucc.actors.PeerDataBouncer;
import hit_ucc.actors.PeerWorker;
import hit_ucc.actors.messages.TaskMessage;

import java.io.IOException;

public class HitUCCPeerHostSystem extends HitUCCSystem {

	public static final String PEER_HOST_ROLE = "peer-host";

	public static void start(String clusterName, int workers, int minWorkers, String input, char csvDelimiter, boolean csvSkipHeader, String output, int dataDuplicationFactor, boolean nullEqualsNull, String host, int port, String bindHost, int bindPort) {
		final Config config = createConfiguration(clusterName, PEER_HOST_ROLE, host, port, host, port, bindHost, bindPort);
		final ActorSystem system = createSystem(clusterName, config);

		final ActorRef[] dataBouncer = new ActorRef[1];

		Cluster.get(system).registerOnMemberUp(() -> {
//			system.actorOf(ClusterListener.props(), ClusterListener.DEFAULT_NAME);
//			system.actorOf(MetricsListener.props(), MetricsListener.DEFAULT_NAME);

			for (int i = 0; i < workers; i++) {
				system.actorOf(PeerWorker.props(), PeerWorker.DEFAULT_NAME + i + ":" + port);
			}

			dataBouncer[0] = system.actorOf(PeerDataBouncer.props(), PeerDataBouncer.DEFAULT_NAME + ":" + port);

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

			dataBouncer[0].tell(new TaskMessage(table, table[0].length, dataDuplicationFactor, nullEqualsNull, minWorkers < workers ? workers : minWorkers), ActorRef.noSender());
		});

		Cluster.get(system).registerOnMemberRemoved(() -> {
			System.out.println("Member removed.");
		});
	}
}
