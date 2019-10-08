package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;

public interface IMergeMinimalSetsStrategy {
	SerializableBitSet[] mergeMinimalDifferenceSets(SerializableBitSet[] setsA, SerializableBitSet[] setsB);
}
