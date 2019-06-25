package de.hpi.hit_ucc.behaviour.differenceSets;

import de.hpi.hit_ucc.model.TrieSet;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;

public class TrieAddDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	TrieSet differenceSets;

	public TrieAddDifferenceSetStrategy(int columns) {
		this.differenceSets = new TrieSet(columns);
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
