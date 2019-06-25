package de.hpi.hit_ucc.behaviour.differenceSets;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;

public interface IAddDifferenceSetStrategy {
	BitSet addDifferenceSet(BitSet differenceSet);

	Iterable<BitSet> getIterable();

	void clearState();
}
