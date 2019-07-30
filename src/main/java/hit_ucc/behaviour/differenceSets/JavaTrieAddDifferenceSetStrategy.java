package hit_ucc.behaviour.differenceSets;

import org.apache.commons.collections4.trie.PatriciaTrie;
import java.util.BitSet;

public class JavaTrieAddDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	PatriciaTrie differenceSets;

	public JavaTrieAddDifferenceSetStrategy() {
		this.differenceSets = new PatriciaTrie();
	}

	@Override
	public BitSet addDifferenceSet(BitSet differenceSet) {
		differenceSets.put(differenceSet.toString(), differenceSet);
		return differenceSet;
	}

	@Override
	public Iterable<BitSet> getIterable() {
		return differenceSets.values();
	}

	@Override
	public void clearState() {
		differenceSets.clear();
	}
}
