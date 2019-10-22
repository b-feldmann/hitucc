package hitucc.actors.messages;

import hitucc.model.SerializableBitSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class DiscoveredAllDifferenceSetsMessage implements Serializable {
	private static final long serialVersionUID = 3333847489303736218L;
	private SerializableBitSet[] differenceSets;
	private int columnsInTable;
}

