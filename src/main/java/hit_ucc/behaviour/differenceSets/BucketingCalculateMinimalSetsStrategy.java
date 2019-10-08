package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;

import java.util.*;

import static hit_ucc.behaviour.differenceSets.DifferenceSetDetector.insertMinimalDifferenceSets;

public class BucketingCalculateMinimalSetsStrategy implements ICalculateMinimalSetsStrategy {
	private int numberOfColumns;

	public BucketingCalculateMinimalSetsStrategy(int numberOfColumns) {
		this.numberOfColumns = numberOfColumns;
	}

	@Override
	public SerializableBitSet[] calculateMinimalDifferenceSets(Iterable<SerializableBitSet> uniqueSets) {
		ArrayList<SerializableBitSet> foundMinimalSets = new ArrayList<>();

		ArrayList<SerializableBitSet>[] bucketList = new ArrayList[numberOfColumns + 1];
		for (int i = 0; i < bucketList.length; i++) {
			bucketList[i] = new ArrayList<>();
		}

		for (SerializableBitSet set : uniqueSets) {
			bucketList[set.cardinality()].add(set);
		}

		for (ArrayList<SerializableBitSet> list : bucketList) {
			for (SerializableBitSet set : list) {
				insertMinimalDifferenceSets(foundMinimalSets, set);
			}
		}

		SerializableBitSet[] result = new SerializableBitSet[foundMinimalSets.size()];
		return foundMinimalSets.toArray(result);
	}
}
