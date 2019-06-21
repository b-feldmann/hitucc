package de.hpi.hit_ucc.behaviour.differenceSets;

import java.util.*;

public class BucketingDifferenceSetDetector extends AbstractDifferenceSetDetector {
	HashSet<BitSet> uniqueDifferenceSets;
	private int columns;

	public BucketingDifferenceSetDetector(int columns) {
		this.columns = columns;
		uniqueDifferenceSets = new HashSet<>();
	}

	@Override
	protected BitSet addDifferenceSet(BitSet differenceSet) {
		uniqueDifferenceSets.add(differenceSet);
		return differenceSet;
	}

	@Override
	protected BitSet[] calculateMinimalDifferenceSets() {
		ArrayList<BitSet> foundMinimalSets = new ArrayList<>();

		ArrayList<BitSet>[] bucketList = new ArrayList[columns];
		for (int i = 0; i < bucketList.length; i++) {
			bucketList[i] = new ArrayList<>();
		}

		for (BitSet set : uniqueDifferenceSets) {
			bucketList[set.cardinality() - 1].add(set);
		}

		for (ArrayList<BitSet> list : bucketList) {
			for (BitSet set : list) {
				insertMinimalDifferenceSets(foundMinimalSets, set);
			}
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
