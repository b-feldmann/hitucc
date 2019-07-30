package hit_ucc.actors.messages;

import hit_ucc.model.Row;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class FindDifferenceSetFromBatchSplitMessage implements Serializable {
	private static final long serialVersionUID = 3493143636548897331L;
	private FindDifferenceSetFromBatchSplitMessage() {}
	private int splitCount;
	private Row[] rows;
	private int batchId;
	private boolean nullEqualsNull;
}
