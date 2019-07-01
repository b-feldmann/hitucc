package hit_ucc.behaviour.differenceSets;

import java.util.*;

public class OneSidedMergeMinimalSetsStrategy implements IMergeMinimalSetsStrategy {
	@Override
	public BitSet[] mergeMinimalDifferenceSets(BitSet[] setsA, BitSet[] setsB) {
		List<BitSet> sortedSets = new ArrayList<>();
		List<BitSet> minimalSets = new ArrayList<>();

		for (BitSet set : setsA) {
			DifferenceSetDetector.insertMinimalDifferenceSets(sortedSets, set);
		}
		for (BitSet set : setsB) {
			DifferenceSetDetector.insertMinimalDifferenceSets(sortedSets, set);
		}
		sortedSets.sort(Comparator.comparingInt(BitSet::cardinality));

		for(BitSet set : sortedSets) DifferenceSetDetector.insertMinimalDifferenceSets(minimalSets, set);

		return minimalSets.toArray(new BitSet[minimalSets.size()]);
	}
}
