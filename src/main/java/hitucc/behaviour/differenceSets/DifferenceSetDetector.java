package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;

import java.util.List;

public class DifferenceSetDetector {
	public static final int NONE_MINIMAL = 0;
	public static final int FIRST_MINIMAL = 1;
	public static final int SECOND_MINIMAL = 2;
	public static final int EQUAL_SETS = 3;

	private static final int MAX_CACHED_DIFFERENCE_SETS = 700000;

	private final IAddDifferenceSetStrategy addStrategy;
	private final ICalculateMinimalSetsStrategy calculateMinimalStrategy;
	private final IMergeMinimalSetsStrategy mergeSetsStrategy;

	private boolean dirty = false;

	private SerializableBitSet[] minimalDifferenceSets = new SerializableBitSet[0];

	public DifferenceSetDetector(IAddDifferenceSetStrategy addStrategy, ICalculateMinimalSetsStrategy calculateMinimalStrategy, IMergeMinimalSetsStrategy mergeSetsStrategy) {
		this.addStrategy = addStrategy;
		this.calculateMinimalStrategy = calculateMinimalStrategy;
		this.mergeSetsStrategy = mergeSetsStrategy;
	}

	public static String bitSetToString(SerializableBitSet bitSet) {
		return bitSetToString(bitSet, bitSet.logicalLength());
	}

	public static String bitSetToString(SerializableBitSet bitSet, int length) {
		if (length == 0) return "";

		StringBuilder output = new StringBuilder();
		for (int i = 0; i < length - 1; i++) {
			output.append(bitSet.get(i) ? 1 : 0).append(", ");
		}
		output.append(bitSet.get(length - 1) ? 1 : 0);
		return output.toString();
	}

	protected static int testMinimalHittingSet(SerializableBitSet setA, SerializableBitSet setB) {
		if (setA.cardinality() == 0 || setB.cardinality() == 0) {
			if (setA.cardinality() == setB.cardinality()) return EQUAL_SETS;

			return NONE_MINIMAL;
		}

		// 0 - none minimal
		// 1 - setA minimal candidate
		// 2 - setB minimal candidate
		int minimalCandidate = EQUAL_SETS;

		int maxLength = Math.max(setA.logicalLength(), setB.logicalLength());
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
	protected static boolean isSubset(SerializableBitSet subset, SerializableBitSet superset) {
		for (int i = 0; i < subset.logicalLength(); i++) {
			if (subset.get(i) && !superset.get(i)) return false;
		}

		return true;
	}

	protected static void insertMinimalDifferenceSets(List<SerializableBitSet> minimalBitSets, SerializableBitSet potentialMinimal) {
		for (SerializableBitSet set : minimalBitSets) {
			if (isSubset(set, potentialMinimal)) return;
		}

		minimalBitSets.add(potentialMinimal);
	}

	protected static boolean isMinimal(List<SerializableBitSet> minimalBitSets, SerializableBitSet potentialMinimal) {
		for (SerializableBitSet set : minimalBitSets) {
			if (isSubset(set, potentialMinimal)) return false;
		}

		return true;
	}

	public SerializableBitSet addDifferenceSet(String[] rowA, String[] rowB) {
		return addDifferenceSet(rowA, rowB, false);
	}

	public SerializableBitSet addDifferenceSet(String[] rowA, String[] rowB, boolean nullEqualsNull) {
		dirty = true;

		SerializableBitSet bitSet = new SerializableBitSet(rowA.length);

		for (int i = 0; i < rowA.length; i++) {
			if (rowA[i] == null) {
				if (!nullEqualsNull || rowB[i] != null) bitSet.set(i);
			} else if (!rowA[i].equals(rowB[i])) {
				bitSet.set(i);
			}
		}

//		checkCachedDifferenceSetsBounds();

		return addStrategy.addDifferenceSet(bitSet);
	}

	public SerializableBitSet addDifferenceSet(final int[] rowA, final int[] rowB, final boolean nullEqualsNull) {
		return addDifferenceSet(rowA, rowB);
	}

	public SerializableBitSet addDifferenceSet(final int[] rowA, final int[] rowB) {
		dirty = true;

		SerializableBitSet bitSet = new SerializableBitSet(rowA.length);

		for (int i = 0; i < rowA.length; i++) {
			if (rowA[i] != rowB[i]) {
				bitSet.set(i);
			}
		}

//		checkCachedDifferenceSetsBounds();

		return addStrategy.addDifferenceSet(bitSet);
	}

	private void checkCachedDifferenceSetsBounds() {
		if (getCachedDifferenceSetCount() >= MAX_CACHED_DIFFERENCE_SETS) {
			getMinimalDifferenceSets();
		}
	}

	public int getCachedDifferenceSetCount() {
		return addStrategy.getCachedDifferenceSetCount();
	}

	public int getLastCountedMinimalDifferenceSetCount() {
		return minimalDifferenceSets == null ? 0 : minimalDifferenceSets.length;
	}

	public void removeDuplicates() {
		addStrategy.removeDuplicates();
	}

	/**
	 * Return all minimal difference Sets
	 */
	public SerializableBitSet[] getMinimalDifferenceSets() {
		if (dirty) {
			dirty = false;
			if (minimalDifferenceSets.length == 0) {
				minimalDifferenceSets = calculateMinimalStrategy.calculateMinimalDifferenceSets(addStrategy.getIterable());
				addStrategy.clearState();
				return minimalDifferenceSets;
			}

			minimalDifferenceSets = calculateMinimalStrategy.calculateMinimalDifferenceSets(this, addStrategy.getIterable(), minimalDifferenceSets);
			addStrategy.clearState();
		}

		return minimalDifferenceSets;
	}

	public SerializableBitSet[] mergeMinimalDifferenceSets(SerializableBitSet[] a, SerializableBitSet[] b) {
		return minimalDifferenceSets = mergeSetsStrategy.mergeMinimalDifferenceSets(a, b);
	}

	public SerializableBitSet[] mergeMinimalDifferenceSets(SerializableBitSet[] otherMinimalSets) {
		return minimalDifferenceSets = mergeSetsStrategy.mergeMinimalDifferenceSets(getMinimalDifferenceSets(), otherMinimalSets);
	}

	public void clearState() {
		addStrategy.clearState();
		setDirty();
	}

	public void setNeededCapacity(int capacity) {
		if(capacity < 0) capacity = 1000;
		addStrategy.setNeededCapacity(capacity);
	}

	/**
	 * The next time all minimal difference sets are requested they are recalculated
	 */
	public void setDirty() {
		this.dirty = true;
	}
}
