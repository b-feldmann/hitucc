package hitucc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class TreeTask implements Serializable {
	private SerializableBitSet x;
	private SerializableBitSet y;
	private int length;
	private int numAttributes;
}
