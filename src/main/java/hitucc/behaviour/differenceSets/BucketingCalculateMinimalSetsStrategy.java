package hitucc.behaviour.differenceSets;

import hitucc.model.SerializableBitSet;

import java.util.*;

import static hitucc.behaviour.differenceSets.DifferenceSetDetector.*;

public class BucketingCalculateMinimalSetsStrategy implements ICalculateMinimalSetsStrategy {
	private int numberOfColumns;

	public BucketingCalculateMinimalSetsStrategy(int numberOfColumns) {
		this.numberOfColumns = numberOfColumns;
	}

	private SerializableBitSet[] calculateMinimalDifferenceSets(ArrayList<SerializableBitSet>[] bucketList) {
		ArrayList<SerializableBitSet> foundMinimalSets = new ArrayList<>();
		ArrayList<SerializableBitSet> currentBucket = new ArrayList<>();

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

	@Override
	public SerializableBitSet[] calculateMinimalDifferenceSets(Iterable<SerializableBitSet> uniqueSets) {
		ArrayList<SerializableBitSet>[] bucketList = new ArrayList[numberOfColumns + 1];
		for(int i = 0; i < bucketList.length; i++){
			bucketList[i] = new ArrayList<>();
		}

		for (SerializableBitSet set : uniqueSets) {
			bucketList[set.cardinality()].add(set);
		}
		return calculateMinimalDifferenceSets(bucketList);
	}

	@Override
	public SerializableBitSet[] calculateMinimalDifferenceSets(DifferenceSetDetector differenceSetDetector, Iterable<SerializableBitSet> uniqueSets, SerializableBitSet[] oldMinimalSets) {
		ArrayList<SerializableBitSet>[] bucketList = new ArrayList[numberOfColumns + 1];
		for(int i = 0; i < bucketList.length; i++){
			bucketList[i] = new ArrayList<>();
		}

		for (SerializableBitSet set : oldMinimalSets) {
			bucketList[set.cardinality()].add(set);
		}
		for (SerializableBitSet set : uniqueSets) {
			bucketList[set.cardinality()].add(set);
		}
		return calculateMinimalDifferenceSets(bucketList);
	}
}
