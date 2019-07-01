package hit_ucc.behaviour.differenceSets;

import java.util.BitSet;

public interface IAddDifferenceSetStrategy {
	BitSet addDifferenceSet(BitSet differenceSet);

	Iterable<BitSet> getIterable();

	void clearState();
}
