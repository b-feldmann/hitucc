package de.hpi.hit_ucc.behaviour.differenceSets;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class SortingDifferenceSetDetector extends AbstractDifferenceSetDetector {
	private List<BitSet> differenceSets;

	public SortingDifferenceSetDetector() {
		differenceSets = new ArrayList<>();
	}

	@Override
	protected BitSet addDifferenceSet(BitSet differenceSet) {
		differenceSets.add(differenceSet);
		return differenceSet;
	}

	@Override
	protected BitSet[] calculateMinimalDifferenceSets() {
		List<BitSet> foundMinimalSets = new ArrayList<>();



		return new BitSet[0];
	}

	@Override
	protected BitSet[] mergeMinimalDifferenceSets(BitSet[] setsA, BitSet[] setsB) {
		return new BitSet[0];
	}

	@Override
	protected void clearState() {

	}
}
