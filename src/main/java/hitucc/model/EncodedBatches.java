package hitucc.model;

import java.util.ArrayList;
import java.util.List;

public class EncodedBatches {
	private List<EncodedRow>[] batches;
	private boolean[] loading;

	public EncodedBatches(int batchCount) {
		batches = new List[batchCount];
		loading = new boolean[batchCount];
		for (int i = 0; i < batchCount; i++) {
			batches[i] = new ArrayList<>();
		}
	}

	public void setBatch(int identifier, List<EncodedRow> batch) {
		batches[identifier] = batch;
	}

	public void addToBatch(int identifier, EncodedRow row) {
		batches[identifier].add(row);
	}

	public void addToBatch(int identifier, List<EncodedRow> batch) {
		for (EncodedRow row : batch) {
			batches[identifier].add(row);
		}
	}

	public boolean hasBatch(int identifier) {
		return batches[identifier].size() > 0;
	}

	public List<EncodedRow> getBatch(int identifier) {
		return batches[identifier];
	}

	public int count() {
		return batches.length;
	}

	public void setBatchLoading(int identifier) {
		loading[identifier] = true;
	}

	public void setBatchLoadingFinished(int identifier) {
		loading[identifier] = true;
	}

	public boolean isBatchLoading(int identifier) {
		return loading[identifier];
	}

	public boolean isBatchLoadingFinished(int identifier) {
		return !loading[identifier] && hasBatch(identifier);
	}
}
