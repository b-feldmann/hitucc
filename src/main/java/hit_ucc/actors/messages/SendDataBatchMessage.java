package hit_ucc.actors.messages;

import hit_ucc.model.Row;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class SendDataBatchMessage implements Serializable {
	private static final long serialVersionUID = 932241116637382093L;
	private int batchIdentifier;
	private List<Row> batch;
	private int currentSplit;
	private int splitCount;
}

