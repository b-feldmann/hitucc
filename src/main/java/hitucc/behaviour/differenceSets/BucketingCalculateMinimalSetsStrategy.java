package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;

import java.util.*;

import static hitucc.behaviour.differenceSets.DifferenceSetDetector.*;

public class BucketingCalculateMinimalSetsStrategy implements ICalculateMinimalSetsStrategy {
	private int numberOfColumns;

	public BucketingCalculateMinimalSetsStrategy(int numberOfColumns) {
		this.numberOfColumns = numberOfColumns;
	}

	@Override
	public SerializableBitSet[] calculateMinimalDifferenceSets(Iterable<SerializableBitSet> uniqueSets) {
		ArrayList<SerializableBitSet> foundMinimalSets = new ArrayList<>();
		ArrayList<SerializableBitSet> currentBucket = new ArrayList<>();

		ArrayList<SerializableBitSet>[] bucketList = new ArrayList[numberOfColumns + 1];
		for (int i = 0; i < bucketList.length; i++) {
			bucketList[i] = new ArrayList<>();
		}

		for (SerializableBitSet set : uniqueSets) {
			bucketList[set.cardinality()].add(set);
		}

		for (ArrayList<SerializableBitSet> list : bucketList) {
			for (SerializableBitSet set : list) {
				if(isMinimal(foundMinimalSets, set)) {
					currentBucket.add(set);
				}
			}
			for(SerializableBitSet set : currentBucket) {
				foundMinimalSets.add(set);
			}
			currentBucket.clear();
		}

		SerializableBitSet[] result = new SerializableBitSet[foundMinimalSets.size()];
		return foundMinimalSets.toArray(result);
	}
}
