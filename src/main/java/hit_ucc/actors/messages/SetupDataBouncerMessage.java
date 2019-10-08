package hit_ucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class SetupDataBouncerMessage implements Serializable {
	private static final long serialVersionUID = 5818929133748493046L;
	private int batchCount;
}

