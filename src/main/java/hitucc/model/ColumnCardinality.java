package hitucc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ColumnCardinality {
	private int columnIndex;
	private int cardinality;

	public void incCardinality() {
		cardinality++;
	}
}
