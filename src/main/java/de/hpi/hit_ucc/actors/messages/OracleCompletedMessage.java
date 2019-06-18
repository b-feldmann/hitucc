package de.hpi.hit_ucc.actors.messages;

import de.hpi.hit_ucc.HittingSetOracle;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@SuppressWarnings("unused")
public class OracleCompletedMessage implements Serializable {
	private static final long serialVersionUID = 2341213111118862395L;
	private HittingSetOracle.Status result;

	private OracleCompletedMessage() {
	}


}
