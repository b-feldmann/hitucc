package hitucc.actors.messages;

import akka.actor.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class RegisterClusterMessage implements Serializable {
	private static final long serialVersionUID = 2666447144449736247L;
	private ActorRef[] clusterWorker;
}
