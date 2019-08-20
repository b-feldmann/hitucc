package hit_ucc.behaviour.differenceSets;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

public class JustAddSortDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	private List<BitSet> differenceSets;
	private List<BitSet> uniqueSortedDifferenceSets;
	private final int MAX_LENGTH = 10000;

	public JustAddSortDifferenceSetStrategy() {
		this.differenceSets = new ArrayList<>();
		uniqueSortedDifferenceSets = new ArrayList<>();
	}

	@Override
	public BitSet addDifferenceSet(BitSet differenceSet) {
		differenceSets.add(differenceSet);

		if (differenceSets.size() >= MAX_LENGTH) {
			mergeAll();
		}

		return differenceSet;
	}

	private void mergeAll() {
		differenceSets.sort(Comparator.comparingInt(BitSet::cardinality));
		uniqueSortedDifferenceSets = merge(uniqueSortedDifferenceSets, differenceSets);
		differenceSets.clear();
	}

	private List<BitSet> merge(List<BitSet> setsA, List<BitSet> setsB) {
		List<BitSet> merged = new ArrayList<>();
		for (int a = 0, b = 0; a < setsA.size() || b < setsB.size(); ) {
			if (a == setsA.size()) {
				if (merged.size() > 0 && !setsB.get(b).equals(merged.get(merged.size() - 1))) {
					merged.add(setsB.get(b));
				}
				b++;
			} else if (b == setsB.size()) {
				if (merged.size() > 0 && !setsA.get(a).equals(merged.get(merged.size() - 1))) {
					merged.add(setsA.get(a));
				}
				a++;
			} else if (setsA.get(a).equals(merged.get(merged.size() - 1))) {
				a++;
			} else if (setsB.get(b).equals(merged.get(merged.size() - 1))) {
				b++;
			} else if (setsA.get(a).cardinality() < setsB.get(b).cardinality()) {
				merged.add(setsA.get(a));
				a++;
			} else {
				merged.add(setsB.get(b));
				b++;
			}
		}
		return merged;
	}

	@Override
	public Iterable<BitSet> getIterable() {
		if (differenceSets.size() > 0) {
			mergeAll();
		}
		return uniqueSortedDifferenceSets;
	}

	@Override
	public void clearState() {
		differenceSets.clear();
		uniqueSortedDifferenceSets.clear();
	}
}
