package hit_ucc;

import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class HitUCCSystem {

	protected static Config createConfiguration(String clusterName, String actorSystemRole, String host, int port, String masterhost, int masterport, String bindHost, int bindPort) {
		if(host.equals(masterhost)) {
			System.out.println("Start on host " + host + ":" + port + " (bind to " + bindHost + ":" + bindPort + ")");
		} else {
			System.out.println("Start on host " + host + ":" + port + " (bind to " + bindHost + ":" + bindPort + ") | master is " + masterhost + ":" + masterport);
		}

		if (bindPort == -1) {
			return ConfigFactory.parseString(
				"akka.remote.netty.tcp.hostname = \"" + host + "\"\n" +
						"akka.remote.netty.tcp.bind-hostname = \"" + bindHost + "\"\n" +
						"akka.remote.netty.tcp.port = " + port + "\n" +
						"akka.remote.netty.tcp.bind-port = \"\"\n" +
						"akka.cluster.roles = [" + actorSystemRole + "]\n" +
						"akka.cluster.seed-nodes = [\"akka.tcp://" + clusterName + "@" + masterhost + ":" + masterport + "\"]")
				.withFallback(ConfigFactory.load("hitucc"));
		}

		// Create the Config with fallback to the application config
		return ConfigFactory.parseString(
				"akka.remote.netty.tcp.hostname = \"" + host + "\"\n" +
						"akka.remote.netty.tcp.bind-hostname = \"" + bindHost + "\"\n" +
						"akka.remote.netty.tcp.port = " + port + "\n" +
						"akka.remote.netty.tcp.bind-port = " + bindPort + "\n" +
						"akka.cluster.roles = [" + actorSystemRole + "]\n" +
						"akka.cluster.seed-nodes = [\"akka.tcp://" + clusterName + "@" + masterhost + ":" + masterport + "\"]")
				.withFallback(ConfigFactory.load("hitucc"));
	}

	protected static ActorSystem createSystem(String actorSystemName, Config config) {

		// Create the ActorSystem
		final ActorSystem system = ActorSystem.create(actorSystemName, config);

		System.out.println("Created System with name: " + system.name());

		// Register a callback that ends the program when the ActorSystem terminates
		system.registerOnTermination(() -> System.exit(0));

		// Register a callback that terminates the ActorSystem when it is detached from the cluster
		Cluster.get(system).registerOnMemberRemoved(() -> {
			system.terminate();

			new Thread(() -> {
				try {
					Await.ready(system.whenTerminated(), Duration.create(10, TimeUnit.SECONDS));
				} catch (Exception e) {
					System.exit(-1);
				}
			}).start();
		});

		return system;
	}
}
