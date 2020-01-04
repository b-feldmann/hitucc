package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;
import hitucc.model.TrieSet;

public class TrieAddDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	final TrieSet differenceSets;

	public TrieAddDifferenceSetStrategy(int columns) {
		this.differenceSets = new TrieSet(columns);
	}

	@Override
	public SerializableBitSet addDifferenceSet(SerializableBitSet differenceSet) {
		differenceSets.add(differenceSet);
		return differenceSet;
	}

	@Override
	public int getCachedDifferenceSetCount() {
		return differenceSets.size();
	}

	@Override
	public Iterable<SerializableBitSet> getIterable() {
		return differenceSets;
	}

	@Override
	public void removeDuplicates() {

	}

	@Override
	public void clearState() {
		differenceSets.clear();
	}
}
