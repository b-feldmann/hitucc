package de.hpi.hit_ucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.BitSet;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class FoundDifferenceSetMessage implements Serializable, IWorkMessage {
	private static final long serialVersionUID = 8653767912124862395L;
	private BitSet hittingSet;

	private FoundDifferenceSetMessage() {
	}
}
