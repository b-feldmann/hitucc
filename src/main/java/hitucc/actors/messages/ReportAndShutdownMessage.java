package hitucc.actors.messages;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor(force = true)
public class ReportAndShutdownMessage implements Serializable {
	private static final long serialVersionUID = 1337457603749641337L;
}
