package hitucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class RequestDataBatchMessage implements Serializable {
	private static final long serialVersionUID = 926493816405038261L;
	private int batchIdentifier;
	private int nextSplit;
}

