package hit_ucc.behaviour.differenceSets;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;

public class JustAddSortDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	ArrayList<BitSet> differenceSets;
	boolean dirty;

	public JustAddSortDifferenceSetStrategy() {
		this.differenceSets = new ArrayList<>();
	}

	@Override
	public BitSet addDifferenceSet(BitSet differenceSet) {
		differenceSets.add(differenceSet);
		dirty = true;
		return differenceSet;
	}

	@Override
	public Iterable<BitSet> getIterable() {
		if (dirty) {
			differenceSets.sort(Comparator.comparingInt(BitSet::cardinality));
			dirty = false;
		}
		return differenceSets;
	}

	@Override
	public void clearState() {
		differenceSets.clear();
	}
}
