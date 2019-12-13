package hitucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class PreRequestDataBatchMessage implements Serializable {
	private static final long serialVersionUID = 4876493240444758376L;
	private int batchIdentifier;
}

