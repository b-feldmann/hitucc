package hit_ucc.model;

import java.io.Serializable;

public class Row implements Serializable {
	public final String [] values;

	public Row(String[] values) {
		this.values = values;
	}
}
