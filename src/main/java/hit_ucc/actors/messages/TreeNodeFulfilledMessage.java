package hit_ucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class TreeNodeFulfilledMessage implements Serializable {
	private static final long serialVersionUID = 1085642539643901337L;

	private long nodeId;
}
