package de.hpi.hit_ucc.behaviour.differenceSets;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;

public class JustAddDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	ArrayList<BitSet> differenceSets;

	public JustAddDifferenceSetStrategy() {
		this.differenceSets = new ArrayList<>();
	}

	@Override
	public BitSet addDifferenceSet(BitSet differenceSet) {
		differenceSets.add(differenceSet);
		return differenceSet;
	}

	@Override
	public Iterable<BitSet> getIterable() {
		return differenceSets;
	}

	@Override
	public void clearState() {
		differenceSets.clear();
	}
}
