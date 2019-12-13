package hitucc.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor(force = true)
public class EncodedRow implements Serializable {
	public final int[] values;

	public EncodedRow(int[] values) {
		this.values = values;
	}
}
