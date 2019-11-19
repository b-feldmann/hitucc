package hitucc.actors.messages;

import hitucc.model.SerializableBitSet;
import hitucc.model.TreeTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class TreeWorkMessage implements Serializable {
	private static final long serialVersionUID = 7331234322343211337L;

	private ArrayDeque<TreeTask> taskQueue;
}