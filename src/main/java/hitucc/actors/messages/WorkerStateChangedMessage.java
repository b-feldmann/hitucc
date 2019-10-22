package hitucc.actors.messages;

import hitucc.actors.PeerWorker;
import hitucc.model.WorkerState;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class WorkerStateChangedMessage implements Serializable {
	private static final long serialVersionUID = 4037295208965201337L;
	private WorkerState state;

	private WorkerStateChangedMessage() {
	}
}
