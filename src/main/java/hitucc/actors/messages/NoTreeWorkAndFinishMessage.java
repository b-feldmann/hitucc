package hitucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class NoTreeWorkAndFinishMessage implements Serializable {
	private static final long serialVersionUID = 7333555555533492487L;
}