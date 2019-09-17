package hit_ucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ReportAndShutdownMessage implements Serializable {
	private static final long serialVersionUID = 1337457603749641337L;
}
