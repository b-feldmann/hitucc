package de.hpi.hit_ucc;

import java.util.*;

public class NaiveDifferenceSetDetector extends AbstractDifferenceSetDetector {
	Set<BitSet> uniqueDifferenceSets;

	public NaiveDifferenceSetDetector() {
		uniqueDifferenceSets = new HashSet<>();
	}

	@Override
	protected void addDifferenceSet(BitSet differenceSet) {
		uniqueDifferenceSets.add(differenceSet);
	}

	@Override
	protected BitSet[] calculateMinimalDifferenceSets() {
		List<BitSet> sets = new ArrayList<>();
		for (BitSet set : uniqueDifferenceSets) sets.add(set);

		int[] minimalState = new int[sets.size()];

		for (int indexA = 0; indexA < sets.size(); indexA++) {
			for (int indexB = indexA + 1; indexB < sets.size(); indexB++) {
				BitSet setA = sets.get(indexA);
				BitSet setB = sets.get(indexB);

				int testResult = testMinimalHittingSet(setA, setB);
				if (testResult == FIRST_MINIMAL) {
					minimalState[indexB] = -1;
				}
				if (testResult == SECOND_MINIMAL) {
					minimalState[indexA] = -1;
				}
			}
		}

		List<BitSet> tempList = new ArrayList<>();
		int i = 0;
		for (BitSet set : sets) {
			if (minimalState[i] == 0) {
				tempList.add(set);
			}
			i++;
		}

		BitSet[] minimalDifferenceSets = new BitSet[tempList.size()];
		tempList.toArray(minimalDifferenceSets);

		sets.clear();
		sets = null;

		return minimalDifferenceSets;
	}

	@Override
	protected BitSet[] mergeMinimalDifferenceSets(BitSet[] setsA, BitSet[] setsB) {
		Set<BitSet> tempSet = uniqueDifferenceSets;
		uniqueDifferenceSets = new HashSet<>();

		for(BitSet set : setsA) addDifferenceSet(set);
		for(BitSet set : setsB) addDifferenceSet(set);

		BitSet[] merged = calculateMinimalDifferenceSets();
		uniqueDifferenceSets = tempSet;
		return merged;
	}

	@Override
	protected void clearState() {
		uniqueDifferenceSets.clear();
	}
}
