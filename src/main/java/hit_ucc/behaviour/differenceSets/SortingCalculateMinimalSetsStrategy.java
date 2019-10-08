package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;

import java.util.*;

public class SortingCalculateMinimalSetsStrategy implements ICalculateMinimalSetsStrategy {
	@Override
	public SerializableBitSet[] calculateMinimalDifferenceSets(Iterable<SerializableBitSet> uniqueSets) {
		List<SerializableBitSet> foundMinimalSets = new ArrayList<>();

		List<SerializableBitSet> sortedList = new ArrayList<>();
		for (SerializableBitSet i : uniqueSets) {
			sortedList.add(i);
		}
		sortedList.sort(Comparator.comparingInt(SerializableBitSet::cardinality));

		for (SerializableBitSet set : sortedList) {
			DifferenceSetDetector.insertMinimalDifferenceSets(foundMinimalSets, set);
		}

		SerializableBitSet[] result = new SerializableBitSet[foundMinimalSets.size()];
		return foundMinimalSets.toArray(result);
	}
}
