package de.hpi.hit_ucc.behaviour.differenceSets;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static de.hpi.hit_ucc.behaviour.differenceSets.DifferenceSetDetectorTest.createBitSet;

public class BucketingCalculateMinimalSetsStrategyTest {

	private ICalculateMinimalSetsStrategy minimalStrategy;

	@BeforeMethod
	private void beforeMethod() {
		minimalStrategy = new BucketingCalculateMinimalSetsStrategy(5);
	}

	@Test
	public void testCalculateMinimalDifferenceSets() {
		// Arrange
		BitSet a = createBitSet(1, 1, 0, 1, 1);
		BitSet b = createBitSet(1, 1, 1, 1, 1);
		BitSet c = createBitSet(1, 0, 0, 0, 0);
		BitSet d = createBitSet(0, 0, 0, 1, 0);
		BitSet e = createBitSet(0, 1, 0, 1, 1);

		List<BitSet> bitSets = new ArrayList<>();
		bitSets.add(a);
		bitSets.add(b);
		bitSets.add(c);
		bitSets.add(d);
		bitSets.add(e);

		// Act
		BitSet[] minimalDifferenceSets = minimalStrategy.calculateMinimalDifferenceSets(bitSets);

		// Assert
		Assert.assertEquals(minimalDifferenceSets.length, 2);
		Assert.assertEqualsNoOrder(minimalDifferenceSets, new BitSet[]{c, d});
	}
}
