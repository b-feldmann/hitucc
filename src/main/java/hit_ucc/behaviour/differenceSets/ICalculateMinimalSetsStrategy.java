package hit_ucc.behaviour.differenceSets;

import java.util.BitSet;

public interface ICalculateMinimalSetsStrategy {
	BitSet[] calculateMinimalDifferenceSets(Iterable<BitSet> uniqueSets);
}
