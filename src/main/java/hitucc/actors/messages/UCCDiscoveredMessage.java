package hitucc.actors.messages;

import hitucc.model.SerializableBitSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class UCCDiscoveredMessage implements Serializable {
	private static final long serialVersionUID = 997981649989901337L;
	private SerializableBitSet ucc;
}