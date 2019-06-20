package de.hpi.hit_ucc;

import java.util.BitSet;

public abstract class AbstractDifferenceSetDetector {
	public static final int NONE_MINIMAL = 0;
	public static final int FIRST_MINIMAL = 1;
	public static final int SECOND_MINIMAL = 2;

	private boolean dirty = false;

	private BitSet[] minimalDifferenceSets = new BitSet[0];

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

	/**
	 * Only for testing purposes
	 */
	@Deprecated
	public static BitSet calculateDifferenceSet(String[] rowA, String[] rowB) {
		return calculateDifferenceSet(rowA, rowB, false);
	}

	/**
	 * Only for testing purposes
	 */
	@Deprecated
	public static BitSet calculateDifferenceSet(String[] rowA, String[] rowB, boolean nullEqualsNull) {
		BitSet bitSet = new BitSet(rowA.length);

		for (int i = 0; i < rowA.length; i++) {
			if (rowA[i] == null) {
				if (!nullEqualsNull || rowB[i] != null) bitSet.set(i);
			} else if (!rowA[i].equals(rowB[i])) {
				bitSet.set(i);
			}
		}

		return bitSet;
	}

	/**
	 * Will be made protected and the static will be removed soon
	 */
	@Deprecated
	public static int testStaticMinimalHittingSet(BitSet setA, BitSet setB) {
		return new NaiveDifferenceSetDetector().testMinimalHittingSet(setA, setB);
	}

	protected abstract BitSet addDifferenceSet(BitSet differenceSet);

	protected abstract BitSet[] calculateMinimalDifferenceSets();

	protected abstract BitSet[] mergeMinimalDifferenceSets(BitSet[] setsA, BitSet[] setsB);

	/**
	 * This is important because other difference sets could be merged on top and therefore we need an empty state
	 */
	protected abstract void clearState();

	public BitSet addDifferenceSet(String[] rowA, String[] rowB) {
		return addDifferenceSet(rowA, rowB, false);
	}

	public BitSet addDifferenceSet(String[] rowA, String[] rowB, boolean nullEqualsNull) {
		dirty = true;

		BitSet bitSet = new BitSet(rowA.length);

		for (int i = 0; i < rowA.length; i++) {
			if (rowA[i] == null) {
				if (!nullEqualsNull || rowB[i] != null) bitSet.set(i);
			} else if (!rowA[i].equals(rowB[i])) {
				bitSet.set(i);
			}
		}

		return addDifferenceSet(bitSet);
	}

	public BitSet[] getMinimalDifferenceSets() {
		if (dirty) {
			dirty = false;
			if (minimalDifferenceSets.length == 0) {
				minimalDifferenceSets = calculateMinimalDifferenceSets();
				clearState();
				return minimalDifferenceSets;
			}

			minimalDifferenceSets = mergeMinimalDifferenceSets(minimalDifferenceSets, calculateMinimalDifferenceSets());
			clearState();
			return minimalDifferenceSets;
		}

		return minimalDifferenceSets;
	}

	public BitSet[] mergeMinimalDifferenceSets(BitSet[] otherMinimalSets) {
		return minimalDifferenceSets = mergeMinimalDifferenceSets(getMinimalDifferenceSets(), otherMinimalSets);
	}

	protected int testMinimalHittingSet(BitSet setA, BitSet setB) {
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

	/**
	 * The next time all minimal difference sets are requested they are recalculated
	 */
	public void setDirty() {
		this.dirty = true;
	}
}
