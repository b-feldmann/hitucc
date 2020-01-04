package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AddSortUniqueDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	private List<SerializableBitSet> differenceSets;

	private boolean dirty = false;

	public AddSortUniqueDifferenceSetStrategy() {
		this.differenceSets = new ArrayList<>();
	}

	@Override
	public SerializableBitSet addDifferenceSet(SerializableBitSet differenceSet) {
		dirty = true;
		differenceSets.add(differenceSet);

		return differenceSet;
	}

	@Override
	public int getCachedDifferenceSetCount() {
		return differenceSets.size();
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

	@Override
	public Iterable<SerializableBitSet> getIterable() {
		removeDuplicates();
		return differenceSets;
	}

	@Override
	public void removeDuplicates() {
		if (differenceSets.size() > 0 && dirty) {
			dirty = false;
			differenceSets.sort((o1, o2) -> {
				int cardinality = o1.cardinality() - o2.cardinality();
				if(cardinality != 0) return cardinality;

				for (int i = 0; i < o1.logicalLength(); i++) {
					if(o1.get(i) && !o2.get(i)) return -1;
					if(!o1.get(i) && o2.get(i)) return 1;
				}

				return 0;
			});
			differenceSets = returnUnique(differenceSets);
		}
	}

	@Override
	public void clearState() {
		differenceSets.clear();
	}

	@Override
	public void setNeededCapacity(int capacity) {
		differenceSets = new ArrayList<>(capacity);
	}
}
