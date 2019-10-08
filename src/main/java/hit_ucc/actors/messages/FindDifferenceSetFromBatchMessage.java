package hit_ucc.actors.messages;

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
	private List<Integer> differenceSetTasksA;
	private List<Integer> differenceSetTasksB;
	private int batchCount;
	private boolean nullEqualsNull;
}

