package hit_ucc.behaviour.differenceSets;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class SortedNonUniqueCalculateMinimalSetsStrategy implements ICalculateMinimalSetsStrategy {
	@Override
	public BitSet[] calculateMinimalDifferenceSets(Iterable<BitSet> uniqueSets) {
		List<BitSet> foundMinimalSets = new ArrayList<>();

		BitSet lastSet = null;
		for (BitSet set : uniqueSets) {
			if (set.equals(lastSet)) continue;
			DifferenceSetDetector.insertMinimalDifferenceSets(foundMinimalSets, set);
			lastSet = set;
		}

		BitSet[] result = new BitSet[foundMinimalSets.size()];
		return foundMinimalSets.toArray(result);
	}
}
