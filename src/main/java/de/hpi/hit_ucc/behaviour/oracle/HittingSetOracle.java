package de.hpi.hit_ucc.behaviour.oracle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class HittingSetOracle {
	private static boolean isHittingSet(BitSet h, BitSet[] differenceSets) {
		for (BitSet e : differenceSets) {
			if (!e.intersects(h)) return false;
		}
		return true;
	}

	public static Status extendable(BitSet x, BitSet y, int length, BitSet[] differenceSets, int numAttributes) {
		// 2
		if (x.cardinality() == 0) {
			// 3
			if (isHittingSet(flippedCopy(y, numAttributes), differenceSets)) {
				return Status.EXTENDABLE;
			}

			// 4
			return Status.NOT_EXTENDABLE;
		}

		// 5
		List<BitSet> t = new ArrayList<>();
		// 6
		List<BitSet>[] s = new List[x.cardinality()];
		int[] xIndexToSIndex = new int[numAttributes];
		Arrays.fill(xIndexToSIndex, -1);
		int sIndex = -1;

		for (int xIndex = x.nextSetBit(0); xIndex != length && xIndex != -1; xIndex = x.nextSetBit(xIndex + 1)) {
			sIndex += 1;
			xIndexToSIndex[xIndex] = sIndex;
		}
		// 7
		for (BitSet e : differenceSets) {
			BitSet intersection = intersect(e, x);
			// 9
			if (intersection.cardinality() == 0) {
				t.add(difference(e, y));
				continue;
			}

			// 8
			if (intersection.cardinality() == 1) {
				int index = xIndexToSIndex[intersection.nextSetBit(0)];
				if (s[index] == null) s[index] = new ArrayList<>();
				s[index].add(difference(e, y));
			}
		}

		// 10
		for (List<BitSet> sx : s) {
			if (sx == null || sx.isEmpty()) return Status.NOT_EXTENDABLE;
		}

		// 11
		if (t.isEmpty()) return Status.MINIMAL;

		// 12
		int[] iterationPosition = new int[x.cardinality()];
		while (true) {
			// 13
			BitSet w = new BitSet(numAttributes);
			boolean increaseNext = true;
			for (int i = 0; i < s.length; ++i) {

				w.or(s[i].get(iterationPosition[i]));
				if (increaseNext) {
					++iterationPosition[i];
					if (iterationPosition[i] == s[i].size()) {
						iterationPosition[i] = 0;
					} else increaseNext = false;

				}
			}
			// 14
			boolean allNoSubset = true;
			for (BitSet e : t) {
				if (isSubsetOf(e, w)) {
					allNoSubset = false;
					break;
				}
			}
			if (allNoSubset) return Status.EXTENDABLE;
			if (increaseNext) break;
		}

		// 15
		return Status.NOT_EXTENDABLE;
	}

	private static boolean isSubsetOf(BitSet included, BitSet in) {
		return included.cardinality() == intersect(included, in).cardinality();
	}

	private static BitSet or(BitSet lhs, BitSet rhs) {
		BitSet intersection = copy(lhs);
		intersection.or(rhs);
		return intersection;
	}

	private static BitSet intersect(BitSet lhs, BitSet rhs) {
		BitSet intersection = copy(lhs);
		intersection.and(rhs);
		return intersection;
	}

	private static BitSet difference(BitSet lhs, BitSet rhs) {
		BitSet difference = copy(lhs);
		difference.andNot(rhs);
		return difference;
	}

	private static BitSet flippedCopy(BitSet set, int numAttributes) {
		BitSet copy = new BitSet(set.length());
		for (int i = 0; i < numAttributes; i++) {
			if (!set.get(i)) copy.set(i);
		}
		return copy;
	}

	private static BitSet copy(BitSet set) {
		BitSet copy = new BitSet(set.length());
		for (int i = 0; i < set.length(); i++) {
			if (set.get(i)) copy.set(i);
		}
		return copy;
	}

	public enum Status {MINIMAL, EXTENDABLE, NOT_EXTENDABLE, FAILED}
}
