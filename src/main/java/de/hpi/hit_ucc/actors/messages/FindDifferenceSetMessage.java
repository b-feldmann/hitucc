package de.hpi.hit_ucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class FindDifferenceSetMessage implements Serializable, IWorkMessage {
	private static final long serialVersionUID = 3493143613678899742L;
	private FindDifferenceSetMessage() {}
	private String[] rowA;
	private String[] rowB;
}
