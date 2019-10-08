package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;
import org.apache.commons.collections4.trie.PatriciaTrie;

public class JavaTrieAddDifferenceSetStrategy implements IAddDifferenceSetStrategy {
	PatriciaTrie differenceSets;

	public JavaTrieAddDifferenceSetStrategy() {
		this.differenceSets = new PatriciaTrie();
	}

	@Override
	public SerializableBitSet addDifferenceSet(SerializableBitSet differenceSet) {
		differenceSets.put(differenceSet.toString(), differenceSet);
		return differenceSet;
	}

	@Override
	public int getCachedDifferenceSetCount() {
		return differenceSets.size();
	}

	@Override
	public Iterable<SerializableBitSet> getIterable() {
		return differenceSets.values();
	}

	@Override
	public void clearState() {
		differenceSets.clear();
	}
}
