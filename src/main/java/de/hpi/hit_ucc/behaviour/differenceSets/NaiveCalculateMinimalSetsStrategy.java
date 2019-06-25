package de.hpi.hit_ucc.behaviour.differenceSets;

import java.util.*;

import static de.hpi.hit_ucc.behaviour.differenceSets.DifferenceSetDetector.FIRST_MINIMAL;
import static de.hpi.hit_ucc.behaviour.differenceSets.DifferenceSetDetector.SECOND_MINIMAL;

public class NaiveCalculateMinimalSetsStrategy implements ICalculateMinimalSetsStrategy {
	@Override
	public BitSet[] calculateMinimalDifferenceSets(Iterable<BitSet> uniqueSets) {
		ArrayList<BitSet> sets = new ArrayList<>();
		for (BitSet set : uniqueSets) sets.add(set);

		int[] minimalState = new int[sets.size()];

		for (int indexA = 0; indexA < sets.size(); indexA++) {
			for (int indexB = indexA + 1; indexB < sets.size(); indexB++) {
				BitSet setA = sets.get(indexA);
				BitSet setB = sets.get(indexB);

				int testResult = DifferenceSetDetector.testMinimalHittingSet(setA, setB);
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
}
