package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;

import java.util.ArrayList;

public class JustAddDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	final ArrayList<SerializableBitSet> differenceSets;

	public JustAddDifferenceSetStrategy() {
		this.differenceSets = new ArrayList<>();
	}

	@Override
	public SerializableBitSet addDifferenceSet(SerializableBitSet differenceSet) {
		differenceSets.add(differenceSet);
		return differenceSet;
	}

	@Override
	public int getCachedDifferenceSetCount() {
		return differenceSets.size();
	}

	@Override
	public Iterable<SerializableBitSet> getIterable() {
		return differenceSets;
	}

	@Override
	public void removeDuplicates() {

	}

	@Override
	public void clearState() {
		differenceSets.clear();
	}
}
