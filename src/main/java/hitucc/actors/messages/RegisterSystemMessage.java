package hitucc.actors.messages;

import akka.actor.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class RegisterSystemMessage implements Serializable {
	private static final long serialVersionUID = 2476915001654869435L;
	private ActorRef dataBouncer;
	private List<ActorRef> worker;
	private ActorRef leadingWorker;
}

