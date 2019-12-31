package hitucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class PrepareForShutdownMessage implements Serializable {
	private static final long serialVersionUID = 7344478349553349537L;
}