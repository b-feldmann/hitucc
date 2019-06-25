package de.hpi.hit_ucc.behaviour.differenceSets;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class HashAddDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	HashSet<BitSet> differenceSets;

	public HashAddDifferenceSetStrategy() {
		this.differenceSets = new HashSet<>();
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
