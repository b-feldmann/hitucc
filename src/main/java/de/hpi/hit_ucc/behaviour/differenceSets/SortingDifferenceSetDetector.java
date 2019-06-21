package de.hpi.hit_ucc.behaviour.differenceSets;

import java.util.*;

public class SortingDifferenceSetDetector extends AbstractDifferenceSetDetector {
	HashSet<BitSet> uniqueDifferenceSets;

	public SortingDifferenceSetDetector() {
		uniqueDifferenceSets = new HashSet<>();
	}

	@Override
	protected BitSet addDifferenceSet(BitSet differenceSet) {
		uniqueDifferenceSets.add(differenceSet);
		return differenceSet;
	}

	@Override
	protected BitSet[] calculateMinimalDifferenceSets() {
		List<BitSet> foundMinimalSets = new ArrayList<>();
		BitSet[] sorted = new BitSet[uniqueDifferenceSets.size()];


		int i = 0;
		for (BitSet set : uniqueDifferenceSets) {
			sorted[i] = set;
			i++;
		}

		Arrays.sort(sorted, Comparator.comparingInt(BitSet::cardinality));

		for (BitSet set : sorted) {
			insertMinimalDifferenceSets(foundMinimalSets, set);
		}

		BitSet[] result = new BitSet[foundMinimalSets.size()];
		return foundMinimalSets.toArray(result);
	}

	@Override
	protected BitSet[] mergeMinimalDifferenceSets(BitSet[] setsA, BitSet[] setsB) {
		List<BitSet> minimalSets = new ArrayList<>();
		minimalSets.addAll(Arrays.asList(setsA));

		for (BitSet set : setsB) {
			insertMinimalDifferenceSets(minimalSets, set);
		}

		BitSet[] result = new BitSet[minimalSets.size()];
		return minimalSets.toArray(result);
	}

	@Override
	protected void clearState() {
		uniqueDifferenceSets.clear();
	}

	/**
	 * @param subset   potential subset
	 * @param superset potential superset
	 * @return return true if the @subset is a superset of @superset
	 * returns also true if both sets are equal
	 */
	protected boolean isSubset(BitSet subset, BitSet superset) {
		for (int i = 0; i < subset.length(); i++) {
			if (subset.get(i) && !superset.get(i)) return false;
		}

		return true;
	}

	protected void insertMinimalDifferenceSets(List<BitSet> minimalBitSets, BitSet potentialMinimal) {
		for (BitSet set : minimalBitSets) {
			if (isSubset(set, potentialMinimal)) return;
		}

		minimalBitSets.add(potentialMinimal);
	}
}
