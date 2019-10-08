package hit_ucc.model;

import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor(force = true)
public class Row implements Serializable {
	public final String [] values;

	public Row(String[] values) {
		this.values = values;
	}
}
