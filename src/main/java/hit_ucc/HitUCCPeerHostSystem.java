package hit_ucc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import hit_ucc.actors.PeerDataBouncer;
import hit_ucc.actors.PeerWorker;
import hit_ucc.actors.listeners.ClusterListener;
import hit_ucc.actors.listeners.MetricsListener;
import hit_ucc.actors.messages.TaskMessage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HitUCCPeerHostSystem extends HitUCCSystem {

	public static final String PEER_HOST_ROLE = "peer-host";

	public static void start(String actorSystemName, int workers, String input, char csvDelimiter, boolean csvSkipHeader, String output, int dataDuplicationFactor, boolean nullEqualsNull, String host, int port) {
		final Config config = createConfiguration(actorSystemName, PEER_HOST_ROLE, host, port, host, port);
		final ActorSystem system = createSystem(actorSystemName, config);

		final ActorRef[] dataBouncer = new ActorRef[1];

		Cluster.get(system).registerOnMemberUp(() -> {
//			system.actorOf(ClusterListener.props(), ClusterListener.DEFAULT_NAME);
//			system.actorOf(MetricsListener.props(), MetricsListener.DEFAULT_NAME);

			dataBouncer[0] = system.actorOf(PeerDataBouncer.props(), PeerDataBouncer.DEFAULT_NAME);

			for (int i = 0; i < workers; i++) {
				system.actorOf(PeerWorker.props(), PeerWorker.DEFAULT_NAME + i);
			}

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

			dataBouncer[0].tell(new TaskMessage(table, table[0].length, dataDuplicationFactor, nullEqualsNull), ActorRef.noSender());
		});

		Cluster.get(system).registerOnMemberRemoved(() -> {
			try {
				String[] paths = new String[]{System.getProperty("user.dir"), System.getProperty("user.dir").concat("/data"), System.getProperty("user.dir").concat("/c/Users"), System.getProperty("c/Users"), "c/Users"};

				for (String path : paths) {
					System.out.println("Path: " + path);
					getResourceFiles(path).forEach(System.out::println);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("Member removed");
		});
	}

	private static List<String> getResourceFiles(String path) throws IOException {
		List<String> filenames = new ArrayList<>();

		File dir = new File(path);
		if (!dir.exists()) return filenames;
		if (dir.isFile()) {
			filenames.add(dir.getName());
			return filenames;
		}

		return filenames;
	}
}
