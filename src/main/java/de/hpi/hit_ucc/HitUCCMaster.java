package de.hpi.hit_ucc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import de.hpi.hit_ucc.actors.Profiler;
import de.hpi.hit_ucc.actors.Worker;
import de.hpi.hit_ucc.actors.listeners.ClusterListener;
import de.hpi.hit_ucc.actors.listeners.MetricsListener;
import de.hpi.hit_ucc.actors.messages.TaskMessage;

import java.io.IOException;

public class HitUCCMaster extends HitUCCSystem {

	public static final String MASTER_ROLE = "master";

	public static void start(String actorSystemName, int workers, String host, int port) {
		final Config config = createConfiguration(actorSystemName, MASTER_ROLE, host, port, host, port);
		final ActorSystem system = createSystem(actorSystemName, config);



		Cluster.get(system).registerOnMemberUp(() -> {
			System.out.println("start cluster");
			system.actorOf(ClusterListener.props(), ClusterListener.DEFAULT_NAME);
			//	system.actorOf(MetricsListener.props(), MetricsListener.DEFAULT_NAME);

			system.actorOf(Profiler.props(), Profiler.DEFAULT_NAME);

			for (int i = 0; i < workers; i++) {
				system.actorOf(Worker.props(), Worker.DEFAULT_NAME + i);
			}

			String[][] table = null;
			try {
				table = ReadDataTable.readTable("bridges.csv", ',');
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}

			system.actorSelection("/user/" + Profiler.DEFAULT_NAME).tell(new TaskMessage(table, table[0].length), ActorRef.noSender());
		});
	}
}
