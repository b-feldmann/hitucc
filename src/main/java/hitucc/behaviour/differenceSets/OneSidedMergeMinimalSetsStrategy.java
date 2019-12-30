package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;

import java.util.*;

public class OneSidedMergeMinimalSetsStrategy implements IMergeMinimalSetsStrategy {
	@Override
	public SerializableBitSet[] mergeMinimalDifferenceSets(SerializableBitSet[] setsA, SerializableBitSet[] setsB) {
		List<SerializableBitSet> sortedSets = new ArrayList<>();
		List<SerializableBitSet> minimalSets = new ArrayList<>();

		for (SerializableBitSet set : setsA) {
			DifferenceSetDetector.insertMinimalDifferenceSets(sortedSets, set);
		}
		for (SerializableBitSet set : setsB) {
			DifferenceSetDetector.insertMinimalDifferenceSets(sortedSets, set);
		}
		sortedSets.sort(Comparator.comparingInt(SerializableBitSet::cardinality));

		for(SerializableBitSet set : sortedSets) DifferenceSetDetector.insertMinimalDifferenceSets(minimalSets, set);

		return minimalSets.toArray(new SerializableBitSet[0]);
	}
}
