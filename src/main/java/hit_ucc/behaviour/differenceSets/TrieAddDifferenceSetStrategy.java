package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;
import hit_ucc.model.TrieSet;

public class TrieAddDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	TrieSet differenceSets;

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
	public void clearState() {
		differenceSets.clear();
	}
}
