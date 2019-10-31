package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;

import java.util.HashSet;

public class HashAddDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	HashSet<SerializableBitSet> differenceSets;

	public HashAddDifferenceSetStrategy() {
		this.differenceSets = new HashSet<>();
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