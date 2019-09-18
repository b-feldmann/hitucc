package hit_ucc.actors.messages;

import hit_ucc.model.WorkerState;
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
