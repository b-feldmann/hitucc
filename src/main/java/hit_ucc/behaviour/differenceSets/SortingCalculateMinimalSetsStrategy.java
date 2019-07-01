package hit_ucc.behaviour.differenceSets;

import java.util.*;

public class SortingCalculateMinimalSetsStrategy implements ICalculateMinimalSetsStrategy {
	@Override
	public BitSet[] calculateMinimalDifferenceSets(Iterable<BitSet> uniqueSets) {
		List<BitSet> foundMinimalSets = new ArrayList<>();

		List<BitSet> sortedList = new ArrayList<>();
		for (BitSet i : uniqueSets) {
			sortedList.add(i);
		}
		sortedList.sort(Comparator.comparingInt(BitSet::cardinality));

		for (BitSet set : sortedList) {
			DifferenceSetDetector.insertMinimalDifferenceSets(foundMinimalSets, set);
		}

		BitSet[] result = new BitSet[foundMinimalSets.size()];
		return foundMinimalSets.toArray(result);
	}
}
