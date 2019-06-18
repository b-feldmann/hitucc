package de.hpi.hit_ucc;

import java.util.*;

public class DifferenceSetDetector {
	public static final int NONE_MINIMAL = 0;
	public static final int FIRST_MINIMAL = 1;
	public static final int SECOND_MINIMAL = 2;

	public static String bitSetToString(BitSet bitSet) {
		return bitSetToString(bitSet, bitSet.length());
	}

	public static String bitSetToString(BitSet bitSet, int length) {
		if (length == 0) return "";

		String output = "";
		for (int i = 0; i < length - 1; i++) {
			output += (bitSet.get(i) ? 1 : 0) + ", ";
		}
		output += (bitSet.get(length - 1) ? 1 : 0);
		return output;
	}

	public static BitSet calculateHittingset(String[] rowA, String[] rowB) {
		return calculateHittingset(rowA, rowB, false);
	}

	public static BitSet calculateHittingset(String[] rowA, String[] rowB, boolean nullEqualsNull) {
		BitSet bitSet = new BitSet(rowA.length);

		for (int i = 0; i < rowA.length; i++) {
			if(rowA[i] == null) {
				if(!nullEqualsNull || rowB[i] != null) bitSet.set(i);
			} else
			if (!rowA[i].equals(rowB[i])) {
				bitSet.set(i);
			}
		}

		return bitSet;
	}

	public static int testMinimalHittingSet(BitSet setA, BitSet setB) {
		if (setA.cardinality() == 0 || setB.cardinality() == 0) return NONE_MINIMAL;

		// 0 - none minimal
		// 1 - setA minimal candidate
		// 2 - setB minimal candidate
		int minimalCandidate = NONE_MINIMAL;

		int maxLength = Math.max(setA.length(), setB.length());
		for (int i = 0; i < maxLength; i++) {
			boolean valueA = setA.get(i);
			boolean valueB = setB.get(i);

			if (valueA == valueB) continue;

			if (valueA) {
				if (minimalCandidate == FIRST_MINIMAL) return NONE_MINIMAL;
				minimalCandidate = SECOND_MINIMAL;
			} else {
				if (minimalCandidate == SECOND_MINIMAL) return NONE_MINIMAL;
				minimalCandidate = FIRST_MINIMAL;
			}
		}

		return minimalCandidate;
	}

	public static BitSet[] GetMinimalDifferenceSets(Set<BitSet> uniqueDifferenceSets) {
		List<BitSet> sets = new ArrayList<>();
		for (BitSet set : uniqueDifferenceSets) sets.add(set);

		int[] minimalState = new int[sets.size()];

		for (int indexA = 0; indexA < sets.size(); indexA++) {
			for (int indexB = indexA + 1; indexB < sets.size(); indexB++) {
				BitSet setA = sets.get(indexA);
				BitSet setB = sets.get(indexB);

				int testResult = DifferenceSetDetector.testMinimalHittingSet(setA, setB);
				if (testResult == DifferenceSetDetector.FIRST_MINIMAL) {
					minimalState[indexB] = -1;
				}
				if (testResult == DifferenceSetDetector.SECOND_MINIMAL) {
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
