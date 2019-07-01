package hit_ucc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import hit_ucc.actors.PeerWorker;
import hit_ucc.actors.listeners.ClusterListener;
import hit_ucc.actors.messages.TaskMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HitUCCPeerSystem extends HitUCCSystem {

	public static final String PEER_ROLE = "peer";

	public static void start(String actorSystemName, int workers, String input, char csvDelimiter, boolean csvSkipHeader, String output, int dataDuplicationFactor, boolean nullEqualsNull, String host, int port) {
		final Config config = createConfiguration(actorSystemName, PEER_ROLE, host, port, host, port);
		final ActorSystem system = createSystem(actorSystemName, config);

		List<ActorRef> workerRefs = new ArrayList<>();

		Cluster.get(system).registerOnMemberUp(() -> {
			system.actorOf(ClusterListener.props(), ClusterListener.DEFAULT_NAME);
			//	system.actorOf(MetricsListener.props(), MetricsListener.DEFAULT_NAME);

			for (int i = 0; i < workers; i++) {
				ActorRef ref = system.actorOf(PeerWorker.props(), PeerWorker.DEFAULT_NAME + i);
				workerRefs.add(ref);
			}

			String[][] table = null;
			try {
				table = ReadDataTable.readTable(input, csvDelimiter, csvSkipHeader);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			workerRefs.get(0).tell(new TaskMessage(table, table[0].length, dataDuplicationFactor, nullEqualsNull), ActorRef.noSender());
		});

		Cluster.get(system).registerOnMemberRemoved(() -> {
			System.out.println("TODO: print UCCs to file");
		});
	}
}
