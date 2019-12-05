package hitucc.model;

import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor(force = true)
public class EncodedRow implements Serializable {
	public final int [] values;

	public EncodedRow(int[] values) {
		this.values = values;
	}
}
