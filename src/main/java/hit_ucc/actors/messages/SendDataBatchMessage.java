package hit_ucc.actors.messages;

import hit_ucc.model.Row;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class SendDataBatchMessage implements Serializable {
	private static final long serialVersionUID = 932241116637382093L;
	private int batchIdentifier;
	private List<Row> batch;
}

