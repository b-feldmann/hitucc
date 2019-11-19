package hitucc.actors.messages;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class NeedTreeWorkOrFinishMessage implements Serializable {
	private static final long serialVersionUID = 1234342339009982024L;
}