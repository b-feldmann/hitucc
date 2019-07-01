package hit_ucc.behaviour.differenceSets;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class TwoSidedMergeMinimalSetsStrategy implements IMergeMinimalSetsStrategy {
	@Override
	public BitSet[] mergeMinimalDifferenceSets(BitSet[] setsA, BitSet[] setsB) {
		List<BitSet> minimalSets = new ArrayList<>();

		for(int a = 0, b = 0; a < setsA.length || b < setsB.length;){
			if(a == setsA.length) {
				DifferenceSetDetector.insertMinimalDifferenceSets(minimalSets, setsB[b]);
				b++;
			} else if(b == setsB.length) {
				DifferenceSetDetector.insertMinimalDifferenceSets(minimalSets, setsA[a]);
				a++;
			} else if(setsA[a].cardinality() < setsB[b].cardinality()) {
				DifferenceSetDetector.insertMinimalDifferenceSets(minimalSets, setsA[a]);
				a++;
			} else {
				DifferenceSetDetector.insertMinimalDifferenceSets(minimalSets, setsB[b]);
				b++;
			}
		}

		BitSet[] result = new BitSet[minimalSets.size()];
		return minimalSets.toArray(result);
	}
}
