package hitucc.actors.messages;

import akka.actor.ActorRef;
import hitucc.model.SerializableBitSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class StartTreeSearchMessage implements Serializable {
	private static final long serialVersionUID = 928827272822828393L;
	private SerializableBitSet[] minimalDifferenceSets;
	private int workerInCluster;
	private int columnsInTable;
}