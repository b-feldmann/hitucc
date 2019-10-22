package hitucc;

import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class HitUCCSystem {

	protected static Config createConfiguration(String actorSystemRole, String bindHost, int bindPort) {
		return ConfigFactory.parseString(
				"akka.remote.netty.tcp.bind-hostname = \"" + bindHost + "\"\n" +
						"akka.remote.netty.tcp.bind-port = \"\"\n" +
						"akka.cluster.roles = [" + actorSystemRole + "]\n")
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
