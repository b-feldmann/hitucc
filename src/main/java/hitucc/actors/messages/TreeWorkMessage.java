package hitucc.actors.messages;

import hitucc.model.TreeTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayDeque;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class TreeWorkMessage implements Serializable {
	private static final long serialVersionUID = 7331234322343211337L;

	private TreeTask task;
}