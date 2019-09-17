package hit_ucc.actors.messages;

import hit_ucc.model.SingleDifferenceSetTask;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class FindDifferenceSetFromBatchMessage implements Serializable {
	private static final long serialVersionUID = 3493143613678891337L;
	private FindDifferenceSetFromBatchMessage() {}
	private List<SingleDifferenceSetTask> differenceSetTasks;
	private int batchCount;
	private boolean nullEqualsNull;
}

