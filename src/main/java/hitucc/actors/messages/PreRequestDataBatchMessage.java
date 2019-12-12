package hitucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class PreRequestDataBatchMessage implements Serializable {
	private static final long serialVersionUID = 4876493240444758376L;
	private PreRequestDataBatchMessage() {}
	private int batchIdentifier;
}

