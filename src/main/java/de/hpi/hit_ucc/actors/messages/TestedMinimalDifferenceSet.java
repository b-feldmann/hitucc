package de.hpi.hit_ucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class TestedMinimalDifferenceSet implements Serializable {
	private static final long serialVersionUID = 2341213111118862395L;
	private TestedMinimalDifferenceSet() {}
	private int result;
	private int indexA;
	private int indexB;
}
