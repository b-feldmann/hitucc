package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;

public interface IMergeMinimalSetsStrategy {
	SerializableBitSet[] mergeMinimalDifferenceSets(SerializableBitSet[] setsA, SerializableBitSet[] setsB);
}
