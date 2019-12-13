package hitucc.actors.messages;

import hitucc.actors.PeerWorker;
import hitucc.model.WorkerState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class WorkerStateChangedMessage implements Serializable {
	private static final long serialVersionUID = 4037295208965201337L;
	private WorkerState state;
	private PeerWorker.NETWORK_ACTION networkAction;
}
