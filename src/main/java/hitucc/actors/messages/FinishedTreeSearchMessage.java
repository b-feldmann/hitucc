package hitucc.actors.messages;

import hitucc.model.SerializableBitSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class FinishedTreeSearchMessage implements Serializable {
	private static final long serialVersionUID = 934730245843645754L;

	private List<SerializableBitSet> discoveredUCCs;
}