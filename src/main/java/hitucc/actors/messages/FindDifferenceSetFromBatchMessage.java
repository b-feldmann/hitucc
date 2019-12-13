package hitucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class FindDifferenceSetFromBatchMessage implements Serializable {
	private static final long serialVersionUID = 3493143613678891337L;
	private List<Integer> differenceSetTasksA;
	private List<Integer> differenceSetTasksB;
	private int batchCount;
	private boolean nullEqualsNull;
}

