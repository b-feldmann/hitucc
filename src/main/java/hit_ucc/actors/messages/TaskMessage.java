package hit_ucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class TaskMessage implements Serializable {
	private static final long serialVersionUID = 923014356212332172L;
	private String[][] inputFile;
	private int attributes;
	private int dataDuplicationFactor;
	private boolean nullEqualsNull;
	private int minWorker;

	private TaskMessage() {
	}
}