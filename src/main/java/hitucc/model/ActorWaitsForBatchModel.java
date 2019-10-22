package hitucc.model;

import akka.actor.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActorWaitsForBatchModel {
	private ActorRef actor;
	private int batchIdentifier;
}
