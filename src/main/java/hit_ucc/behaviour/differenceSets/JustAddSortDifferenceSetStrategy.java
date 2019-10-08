package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JustAddSortDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	private List<SerializableBitSet> differenceSets;
	private List<SerializableBitSet> uniqueSortedDifferenceSets;
	private final int MAX_LENGTH = 1000001;

	public JustAddSortDifferenceSetStrategy() {
		this.differenceSets = new ArrayList<>();
		uniqueSortedDifferenceSets = new ArrayList<>();
	}

	@Override
	public SerializableBitSet addDifferenceSet(SerializableBitSet differenceSet) {
		differenceSets.add(differenceSet);

		if (differenceSets.size() >= MAX_LENGTH) {
			mergeAll();
		}

		return differenceSet;
	}

	@Override
	public int getCachedDifferenceSetCount() {
		return differenceSets.size();
	}

	private void mergeAll() {
		differenceSets.sort(Comparator.comparingInt(SerializableBitSet::cardinality));
		uniqueSortedDifferenceSets = merge(uniqueSortedDifferenceSets, differenceSets);
		differenceSets.clear();
	}

	private List<SerializableBitSet> merge(List<SerializableBitSet> setsA, List<SerializableBitSet> setsB) {
		List<SerializableBitSet> merged = new ArrayList<>();

		int startA = 0;
		int startB = 0;

		if (setsA.size() > 0 && setsA.get(startA).cardinality() < setsB.get(startB).cardinality()) {
			merged.add(setsA.get(startA));
			startA++;
		} else {
			merged.add(setsB.get(startB));
			startB++;
		}

		for (int a = startA, b = startB; a < setsA.size() || b < setsB.size(); ) {
			if (a == setsA.size()) {
				if (!setsB.get(b).equals(merged.get(merged.size() - 1))) {
					merged.add(setsB.get(b));
				}
				b++;
			} else if (b == setsB.size()) {
				if (!setsA.get(a).equals(merged.get(merged.size() - 1))) {
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
	public Iterable<SerializableBitSet> getIterable() {
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
