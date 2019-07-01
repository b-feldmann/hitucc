package hit_ucc.behaviour.differenceSets;

import java.util.BitSet;
import java.util.List;

public class DifferenceSetDetector {
	public static final int NONE_MINIMAL = 0;
	public static final int FIRST_MINIMAL = 1;
	public static final int SECOND_MINIMAL = 2;
	public static final int EQUAL_SETS = 3;

	private IAddDifferenceSetStrategy addStrategy;
	private ICalculateMinimalSetsStrategy calculateMinimalStrategy;
	private IMergeMinimalSetsStrategy mergeSetsStrategy;

	private boolean dirty = false;

	private BitSet[] minimalDifferenceSets = new BitSet[0];

	public DifferenceSetDetector(IAddDifferenceSetStrategy addStrategy, ICalculateMinimalSetsStrategy calculateMinimalStrategy, IMergeMinimalSetsStrategy mergeSetsStrategy) {
		this.addStrategy = addStrategy;
		this.calculateMinimalStrategy = calculateMinimalStrategy;
		this.mergeSetsStrategy = mergeSetsStrategy;
	}

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

		return addStrategy.addDifferenceSet(bitSet);
	}

	/**
	 * Return all minimal difference Sets
	 */
	public BitSet[] getMinimalDifferenceSets() {
		if (dirty) {
			dirty = false;
			if (minimalDifferenceSets.length == 0) {
				minimalDifferenceSets = calculateMinimalStrategy.calculateMinimalDifferenceSets(addStrategy.getIterable());
				addStrategy.clearState();
				return minimalDifferenceSets;
			}

			minimalDifferenceSets = mergeSetsStrategy.mergeMinimalDifferenceSets(
					minimalDifferenceSets,
					calculateMinimalStrategy.calculateMinimalDifferenceSets(addStrategy.getIterable())
			);
			addStrategy.clearState();
			return minimalDifferenceSets;
		}

		return minimalDifferenceSets;
	}

	public BitSet[] mergeMinimalDifferenceSets(BitSet[] otherMinimalSets) {
		return minimalDifferenceSets = mergeSetsStrategy.mergeMinimalDifferenceSets(getMinimalDifferenceSets(), otherMinimalSets);
	}

	public void clearState(){
		addStrategy.clearState();
		setDirty();
	}

	/**
	 * The next time all minimal difference sets are requested they are recalculated
	 */
	public void setDirty() {
		this.dirty = true;
	}

	protected static int testMinimalHittingSet(BitSet setA, BitSet setB) {
		if (setA.cardinality() == 0 || setB.cardinality() == 0) {
			if (setA.cardinality() == setB.cardinality()) return EQUAL_SETS;

			return NONE_MINIMAL;
		}

		// 0 - none minimal
		// 1 - setA minimal candidate
		// 2 - setB minimal candidate
		int minimalCandidate = EQUAL_SETS;

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
	 * @param subset   potential subset
	 * @param superset potential superset
	 * @return return true if the @subset is a superset of @superset
	 * returns also true if both sets are equal
	 */
	protected static boolean isSubset(BitSet subset, BitSet superset) {
		for (int i = 0; i < subset.length(); i++) {
			if (subset.get(i) && !superset.get(i)) return false;
		}

		return true;
	}

	protected static void insertMinimalDifferenceSets(List<BitSet> minimalBitSets, BitSet potentialMinimal) {
		for (BitSet set : minimalBitSets) {
			if (isSubset(set, potentialMinimal)) return;
		}

		minimalBitSets.add(potentialMinimal);
	}
}
