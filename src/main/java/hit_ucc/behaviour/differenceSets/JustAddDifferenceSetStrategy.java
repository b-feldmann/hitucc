package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;

import java.util.ArrayList;

public class JustAddDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	ArrayList<SerializableBitSet> differenceSets;

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
	public void clearState() {
		differenceSets.clear();
	}
}
