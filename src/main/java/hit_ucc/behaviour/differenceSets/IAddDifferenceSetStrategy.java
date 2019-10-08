package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;

public interface IAddDifferenceSetStrategy {
	SerializableBitSet addDifferenceSet(SerializableBitSet differenceSet);

	int getCachedDifferenceSetCount();

	Iterable<SerializableBitSet> getIterable();

	void clearState();
}
