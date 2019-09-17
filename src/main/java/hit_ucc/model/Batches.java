package hit_ucc.model;

import java.util.ArrayList;
import java.util.List;

public class Batches {
	private List<Row>[] batches;

	public Batches(int batchCount) {
		batches = new List[batchCount];
		for (int i = 0; i < batchCount; i++) {
			batches[i] = new ArrayList<>();
		}
	}

	public void setBatch(int identifier, List<Row> batch) {
		batches[identifier] = batch;
	}

	public boolean hasBatch(int identifier) {
		return batches[identifier].size() > 0;
	}

	public List<Row> getBatch(int identifier) {
		return batches[identifier];
	}

	public int count() {
		return batches.length;
	}
}
