package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;

public interface ICalculateMinimalSetsStrategy {
	SerializableBitSet[] calculateMinimalDifferenceSets(Iterable<SerializableBitSet> uniqueSets);
}
