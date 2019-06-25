package de.hpi.hit_ucc.behaviour.differenceSets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class OneSidedMergeMinimalSetsStrategy implements IMergeMinimalSetsStrategy {
	@Override
	public BitSet[] mergeMinimalDifferenceSets(BitSet[] setsA, BitSet[] setsB) {
		List<BitSet> minimalSets = new ArrayList<>();
		minimalSets.addAll(Arrays.asList(setsA));

		for (BitSet set : setsB) {
			DifferenceSetDetector.insertMinimalDifferenceSets(minimalSets, set);
		}

		BitSet[] result = new BitSet[minimalSets.size()];
		return minimalSets.toArray(result);
	}
}
