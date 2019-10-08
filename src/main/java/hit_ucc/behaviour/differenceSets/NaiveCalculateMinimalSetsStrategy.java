package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;

import java.util.*;

import static hit_ucc.behaviour.differenceSets.DifferenceSetDetector.FIRST_MINIMAL;
import static hit_ucc.behaviour.differenceSets.DifferenceSetDetector.SECOND_MINIMAL;

public class NaiveCalculateMinimalSetsStrategy implements ICalculateMinimalSetsStrategy {
	@Override
	public SerializableBitSet[] calculateMinimalDifferenceSets(Iterable<SerializableBitSet> uniqueSets) {
		ArrayList<SerializableBitSet> sets = new ArrayList<>();
		for (SerializableBitSet set : uniqueSets) sets.add(set);

		int[] minimalState = new int[sets.size()];

		for (int indexA = 0; indexA < sets.size(); indexA++) {
			for (int indexB = indexA + 1; indexB < sets.size(); indexB++) {
				SerializableBitSet setA = sets.get(indexA);
				SerializableBitSet setB = sets.get(indexB);

				int testResult = DifferenceSetDetector.testMinimalHittingSet(setA, setB);
				if (testResult == FIRST_MINIMAL) {
					minimalState[indexB] = -1;
				}
				if (testResult == SECOND_MINIMAL) {
					minimalState[indexA] = -1;
				}
			}
		}

		List<SerializableBitSet> tempList = new ArrayList<>();
		int i = 0;
		for (SerializableBitSet set : sets) {
			if (minimalState[i] == 0) {
				tempList.add(set);
			}
			i++;
		}

		SerializableBitSet[] minimalDifferenceSets = new SerializableBitSet[tempList.size()];
		tempList.toArray(minimalDifferenceSets);

		sets.clear();
		sets = null;

		return minimalDifferenceSets;
	}
}
