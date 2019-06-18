package de.hpi.hit_ucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.BitSet;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class TreeOracleMessage implements Serializable, IWorkMessage {
	private static final long serialVersionUID = 2345432286807443083L;
	private TreeOracleMessage() {}
	private BitSet x;
	private BitSet y;
	private int length;
	private BitSet[] differenceSets;
	private int numAttributes;
}
