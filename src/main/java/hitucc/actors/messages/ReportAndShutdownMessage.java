package hitucc.actors.messages;

import hitucc.model.SerializableBitSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class ReportAndShutdownMessage implements Serializable {
	private static final long serialVersionUID = 1337457603749641337L;
	int uccCount;
}
