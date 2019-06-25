package de.hpi.hit_ucc.behaviour.differenceSets;

import java.util.BitSet;
import java.util.Collection;

public interface ICalculateMinimalSetsStrategy {
	BitSet[] calculateMinimalDifferenceSets(Iterable<BitSet> uniqueSets, int numberOfColumns);
}
