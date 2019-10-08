package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static hit_ucc.behaviour.differenceSets.DifferenceSetDetectorTest.createBitSet;

public class TwoSidedMergeMinimalSetsStrategyTest {
	private IMergeMinimalSetsStrategy mergeStrategy;

	@BeforeMethod
	private void beforeMethod() {
		mergeStrategy = new TwoSidedMergeMinimalSetsStrategy();
	}

	@Test
	public void mergeMinimalDifferenceSetsTest() {
		SerializableBitSet[] minimalSetsA = new SerializableBitSet[]{
				createBitSet(1, 0, 0, 0, 0),
				createBitSet(0, 1, 0, 1, 1),
				createBitSet(0, 1, 1, 0, 1)
		};

		SerializableBitSet[] minimalSetsB = new SerializableBitSet[]{
				createBitSet(0, 0, 0, 0, 1),
				createBitSet(1, 1, 0, 0, 0),
				createBitSet(0, 0, 1, 1, 0)
		};

		SerializableBitSet[] mergedSets = mergeStrategy.mergeMinimalDifferenceSets(minimalSetsA, minimalSetsB);
		SerializableBitSet[] expectedSets = new SerializableBitSet[]{
				createBitSet(1, 0, 0, 0, 0),
				createBitSet(0, 0, 0, 0, 1),
				createBitSet(0, 0, 1, 1, 0),
		};

		Assert.assertEqualsNoOrder(mergedSets, expectedSets);
	}
}
