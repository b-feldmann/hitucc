package de.hpi.hit_ucc.behaviour.differenceSets;

import java.util.BitSet;

public class BucketingDifferenceSetDetector extends AbstractDifferenceSetDetector {
	@Override
	protected BitSet addDifferenceSet(BitSet differenceSet) {
		return null;
	}

	@Override
	protected BitSet[] calculateMinimalDifferenceSets() {
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
