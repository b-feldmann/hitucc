package hit_ucc.model;

import java.io.Serializable;

public class Row implements Serializable {
	public final int anchor;
	public final String [] values;

	public Row(int anchor, String[] values) {
		this.anchor = anchor;
		this.values = values;
	}
}
