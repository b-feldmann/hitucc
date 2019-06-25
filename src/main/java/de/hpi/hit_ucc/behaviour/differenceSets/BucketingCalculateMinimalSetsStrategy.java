package de.hpi.hit_ucc.behaviour.differenceSets;

import java.util.*;

import static de.hpi.hit_ucc.behaviour.differenceSets.DifferenceSetDetector.insertMinimalDifferenceSets;

public class BucketingCalculateMinimalSetsStrategy implements ICalculateMinimalSetsStrategy {
	@Override
	public BitSet[] calculateMinimalDifferenceSets(Iterable<BitSet> uniqueSets, int numberOfColumns) {
		ArrayList<BitSet> foundMinimalSets = new ArrayList<>();

		ArrayList<BitSet>[] bucketList = new ArrayList[numberOfColumns + 1];
		for (int i = 0; i < bucketList.length; i++) {
			bucketList[i] = new ArrayList<>();
		}

		for (BitSet set : uniqueSets) {
			bucketList[set.cardinality()].add(set);
		}

		for (ArrayList<BitSet> list : bucketList) {
			for (BitSet set : list) {
				insertMinimalDifferenceSets(foundMinimalSets, set);
			}
		}

		BitSet[] result = new BitSet[foundMinimalSets.size()];
		return foundMinimalSets.toArray(result);
	}
}
