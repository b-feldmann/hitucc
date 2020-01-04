package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AddSortUniqueDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	private final List<SerializableBitSet> differenceSets;
	private List<SerializableBitSet> uniqueSortedDifferenceSets;

	private boolean dirty = false;

	public AddSortUniqueDifferenceSetStrategy() {
		this.differenceSets = new ArrayList<>();
		uniqueSortedDifferenceSets = new ArrayList<>();
	}

	@Override
	public SerializableBitSet addDifferenceSet(SerializableBitSet differenceSet) {
		dirty = true;
		differenceSets.add(differenceSet);

//		int MAX_LENGTH = 1000001;
//		if (differenceSets.size() >= MAX_LENGTH) {
//			mergeAll();
//		}

		return differenceSet;
	}

	@Override
	public int getCachedDifferenceSetCount() {
		return differenceSets.size();
	}

	private void mergeAll() {
		differenceSets.sort((o1, o2) -> {
			int cardinality = o1.cardinality() - o2.cardinality();
			if(cardinality != 0) return cardinality;

			for (int i = 0; i < o1.logicalLength(); i++) {
				if(o1.get(i) && !o2.get(i)) return -1;
				if(!o1.get(i) && o2.get(i)) return 1;
			}

			return 0;
		});
		uniqueSortedDifferenceSets = merge(uniqueSortedDifferenceSets, returnUnique(differenceSets));
		differenceSets.clear();
	}

	private List<SerializableBitSet> returnUnique(List<SerializableBitSet> sets) {
		List<SerializableBitSet> uniqueSets = new ArrayList<>();

		if(sets.size() == 0) return uniqueSets;

		uniqueSets.add(sets.get(0));
		if(sets.size() == 1) return uniqueSets;

		for (int i = 1; i < sets.size(); i++) {
			if(!sets.get(i).equals(sets.get(i - 1))) {
				uniqueSets.add(sets.get(i));
			}
		}

		return uniqueSets;
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
		removeDuplicates();
		return uniqueSortedDifferenceSets;
	}

	@Override
	public void removeDuplicates() {
		if (differenceSets.size() > 0 && dirty) {
			dirty = false;
			mergeAll();
		}
	}

	@Override
	public void clearState() {
		differenceSets.clear();
		uniqueSortedDifferenceSets.clear();
	}
}
