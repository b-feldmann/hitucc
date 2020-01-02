package hitucc.model;

import java.util.ArrayList;
import java.util.List;

public class EncodedBatches {
	private final List<EncodedRow>[] batches;
	private final boolean[] loading;
	private final int[] batchSizes;

	public EncodedBatches(int batchCount, final int[] batchSizes) {
		batches = new List[batchCount];
		loading = new boolean[batchCount];
		for (int i = 0; i < batchCount; i++) {
			batches[i] = new ArrayList<>();
		}
		this.batchSizes = batchSizes;
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
		return batches[identifier].size() == batchSizes[identifier];
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

	public int[] getBatchSizes() {
		return batchSizes;
	}

	public void updateBatchSizes() {
		for(int i = 0; i < batchSizes.length; i++) {
			batchSizes[i] = batches[i].size();
		}
	}
}
