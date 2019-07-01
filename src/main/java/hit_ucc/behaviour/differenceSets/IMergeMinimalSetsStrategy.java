package hit_ucc.behaviour.differenceSets;

import java.util.BitSet;

public interface IMergeMinimalSetsStrategy {
	BitSet[] mergeMinimalDifferenceSets(BitSet[] setsA, BitSet[] setsB);
}
