package hit_ucc.actors.messages;

import hit_ucc.model.Row;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class RequestDataBatchMessage implements Serializable {
	private static final long serialVersionUID = 926493816405038261L;
	private RequestDataBatchMessage() {}
	private int batchIdentifier;
}

