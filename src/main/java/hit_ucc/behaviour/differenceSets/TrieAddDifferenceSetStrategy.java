package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.TrieSet;

import java.util.BitSet;

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
