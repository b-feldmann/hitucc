package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;

public interface IAddDifferenceSetStrategy {
	SerializableBitSet addDifferenceSet(SerializableBitSet differenceSet);

	int getCachedDifferenceSetCount();

	Iterable<SerializableBitSet> getIterable();

	void clearState();
}
