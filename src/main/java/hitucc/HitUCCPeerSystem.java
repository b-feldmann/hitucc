package hitucc;

import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import hitucc.actors.PeerDataBouncer;
import hitucc.actors.PeerWorker;
import hitucc.actors.Reaper;

public class HitUCCPeerSystem extends HitUCCSystem {

	public static final String PEER_ROLE = "peer";

	public static void start(int workers, String bindHost, int bindPort) {
		final Config config = ConfigFactory.parseString("akka.cluster.roles = [" + PEER_ROLE + "]\n").withFallback(ConfigFactory.load());
		String clusterName = config.getString("clustering.cluster.name");
		final ActorSystem system = createSystem(clusterName, config);

		int port = config.getInt("clustering.port");
		String host = config.getString("clustering.ip");

		System.out.println("#################### START ACTOR SYSTEM ####################\n" +
				"Address: " + host + ":" + port + "\n" +
				"Memory: " + Runtime.getRuntime().maxMemory() + " Bytes\n" +
				"Workers: " + workers + "\n" +
				"#################### ------------------ ####################");

//		system.actorOf(Reaper.props(), Reaper.DEFAULT_NAME + ":" + port);
		for (int i = 0; i < workers; i++) {
			system.actorOf(PeerWorker.props(), PeerWorker.DEFAULT_NAME + i + ":" + port);
		}
		system.actorOf(PeerDataBouncer.props(workers), PeerDataBouncer.DEFAULT_NAME + ":" + port);
	}
}
