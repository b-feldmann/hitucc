package hit_ucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

import hit_ucc.model.Row;
import java.io.Serializable;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class FindDifferenceSetFromBatchMessage implements Serializable {
	private static final long serialVersionUID = 3493143613678891337L;
	private FindDifferenceSetFromBatchMessage() {}
	private Row[] rows;
	private int batchId;
	private boolean nullEqualsNull;
}
