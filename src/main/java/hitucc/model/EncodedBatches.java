package hitucc.model;

import java.util.ArrayList;
import java.util.List;

public class EncodedBatches {
	private final EncodedRow[][] batches;
	private final boolean[] loading;
	private final int[] batchSizes;
	private final int[] insertIndex;

	public EncodedBatches(int batchCount, final int[] batchSizes) {
		batches = new EncodedRow[batchCount][];
		loading = new boolean[batchCount];
		insertIndex = new int[batchCount];
		for (int i = 0; i < batchCount; i++) {
			batches[i] = new EncodedRow[batchSizes[i]];
			insertIndex[i] = 0;
		}
		this.batchSizes = batchSizes;
	}

	public void setBatch(int identifier, EncodedRow[] batch) {
		batches[identifier] = batch;
		insertIndex[identifier] = batch.length;
	}

	public void addToBatch(int identifier, EncodedRow row) {
		batches[identifier][insertIndex[identifier]] = row;
		insertIndex[identifier] += 1;
	}

	public void addToBatch(int identifier, List<EncodedRow> batch) {
		for(int k = 0; k < batch.size(); k++) {
			batches[identifier][insertIndex[identifier] + k] = batch.get(k);
		}
		insertIndex[identifier] += batch.size();
	}

	public void addToBatch(int identifier, EncodedRow[] batch) {
//		for(int k = 0; k < batch.length; k++) {
//			batches[identifier][insertIndex[identifier] + k] = batch[k];
//		}
		System.arraycopy(batch, 0, batches[identifier], insertIndex[identifier], batch.length);
		insertIndex[identifier] += batch.length;
	}

	public boolean hasBatch(int identifier) {
		return insertIndex[identifier] == batchSizes[identifier];
	}

	public EncodedRow[] getBatch(int identifier) {
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
			batchSizes[i] = batches[i].length;
		}
	}
}
