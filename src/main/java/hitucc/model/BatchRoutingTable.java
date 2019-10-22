package hitucc.model;

import akka.actor.ActorRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BatchRoutingTable {
	private List<ActorRef>[] routingTable;
	private Random r;

	public BatchRoutingTable(int batchCount, ActorRef seedRef) {
		routingTable = new List[batchCount];
		for(int i = 0; i < batchCount; i += 1) {
			routingTable[i] = new ArrayList<>();
			routingTable[i].add(seedRef);
		}

		r = new Random();
	}

	public List<ActorRef> getRoutesForBatch(int batchIdentifier) {
		return routingTable[batchIdentifier];
	}

	public ActorRef routeToRandomActor(int batchIdentifier) {
		return routingTable[batchIdentifier].get(r.nextInt(routingTable[batchIdentifier].size()));
	}

	public void addRoute(int batchIdentifier, ActorRef actorRef) {
		routingTable[batchIdentifier].add(actorRef);
	}
}
