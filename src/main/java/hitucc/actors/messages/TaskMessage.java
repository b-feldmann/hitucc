package hitucc.actors.messages;

import hitucc.model.AlgorithmTimerObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class TaskMessage implements Serializable {
	private static final long serialVersionUID = 923014356212332172L;
	private String[][] inputFile;
	private int attributes;
	private boolean greedyTaskDistribution;
	private int dataDuplicationFactor;
	private boolean nullEqualsNull;
	private int minSystems;
	private AlgorithmTimerObject timerObject;
}