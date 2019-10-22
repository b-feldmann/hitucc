package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;

public interface ICalculateMinimalSetsStrategy {
	SerializableBitSet[] calculateMinimalDifferenceSets(Iterable<SerializableBitSet> uniqueSets);
}
