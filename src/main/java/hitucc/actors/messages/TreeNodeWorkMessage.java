package hitucc.actors.messages;

import hitucc.model.SerializableBitSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class TreeNodeWorkMessage implements Serializable {
	private static final long serialVersionUID = 2360129506196901337L;
	private SerializableBitSet x;
	private SerializableBitSet y;
	private int length;
	private SerializableBitSet[] differenceSets;
	private int numAttributes;

	private long nodeId;
}