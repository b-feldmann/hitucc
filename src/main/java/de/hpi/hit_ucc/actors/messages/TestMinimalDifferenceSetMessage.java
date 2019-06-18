package de.hpi.hit_ucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.BitSet;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class TestMinimalDifferenceSetMessage implements Serializable, IWorkMessage {
	private static final long serialVersionUID = -1233450836186800395L;
	private TestMinimalDifferenceSetMessage() {}
	private BitSet differenceSetA;
	private int indexA;
	private BitSet differenceSetB;
	private int indexB;
}
