package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;

import java.util.ArrayList;
import java.util.List;

public class SortedNonUniqueCalculateMinimalSetsStrategy implements ICalculateMinimalSetsStrategy {
	@Override
	public SerializableBitSet[] calculateMinimalDifferenceSets(Iterable<SerializableBitSet> uniqueSets) {
		List<SerializableBitSet> foundMinimalSets = new ArrayList<>();

		SerializableBitSet lastSet = null;
		for (SerializableBitSet set : uniqueSets) {
			if (set.equals(lastSet)) continue;
			DifferenceSetDetector.insertMinimalDifferenceSets(foundMinimalSets, set);
			lastSet = set;
		}

		SerializableBitSet[] result = new SerializableBitSet[foundMinimalSets.size()];
		return foundMinimalSets.toArray(result);
	}
}
