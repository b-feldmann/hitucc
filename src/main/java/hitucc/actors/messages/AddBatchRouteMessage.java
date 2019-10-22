package hitucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class AddBatchRouteMessage implements Serializable {
	private static final long serialVersionUID = 927216392711999884L;
	private int batchIdentifier;
}

