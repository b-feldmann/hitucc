package hit_ucc.behaviour.differenceSets;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.BitSet;

import static hit_ucc.behaviour.differenceSets.DifferenceSetDetectorTest.createBitSet;

public class TwoSidedMergeMinimalSetsStrategyTest {
	private IMergeMinimalSetsStrategy mergeStrategy;

	@BeforeMethod
	private void beforeMethod() {
		mergeStrategy = new TwoSidedMergeMinimalSetsStrategy();
	}

	@Test
	public void mergeMinimalDifferenceSetsTest() {
		BitSet[] minimalSetsA = new BitSet[]{
				createBitSet(1, 0, 0, 0, 0),
				createBitSet(0, 1, 0, 1, 1),
				createBitSet(0, 1, 1, 0, 1)
		};

		BitSet[] minimalSetsB = new BitSet[]{
				createBitSet(0, 0, 0, 0, 1),
				createBitSet(1, 1, 0, 0, 0),
				createBitSet(0, 0, 1, 1, 0)
		};

		BitSet[] mergedSets = mergeStrategy.mergeMinimalDifferenceSets(minimalSetsA, minimalSetsB);
		BitSet[] expectedSets = new BitSet[]{
				createBitSet(1, 0, 0, 0, 0),
				createBitSet(0, 0, 0, 0, 1),
				createBitSet(0, 0, 1, 1, 0),
		};

		Assert.assertEqualsNoOrder(mergedSets, expectedSets);
	}
}
