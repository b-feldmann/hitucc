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

	public static void start(String clusterName, int workers, String host, int port, String hostHost, int hostPort) {
		final Config config = createConfiguration(clusterName, PEER_ROLE, host, port, hostHost, hostPort);
		final ActorSystem system = createSystem(clusterName, config);

		Cluster.get(system).registerOnMemberUp(() -> {
			for (int i = 0; i < workers; i++) {
				ActorRef ref = system.actorOf(PeerWorker.props(), PeerWorker.DEFAULT_NAME + (i + workers));
			}
		});

		Cluster.get(system).registerOnMemberRemoved(() -> {
			System.out.println("TODO: print UCCs to file");
		});
	}
}
