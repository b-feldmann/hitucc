package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static hit_ucc.behaviour.differenceSets.DifferenceSetDetectorTest.createBitSet;

public class SortingCalculateMinimalSetsStrategyTest {

	private ICalculateMinimalSetsStrategy minimalStrategy;

	@BeforeMethod
	private void beforeMethod() {
		minimalStrategy = new SortingCalculateMinimalSetsStrategy();
	}

	@Test
	public void testCalculateMinimalDifferenceSets() {
		// Arrange
		SerializableBitSet a = createBitSet(1, 1, 0, 1, 1);
		SerializableBitSet b = createBitSet(1, 1, 1, 1, 1);
		SerializableBitSet c = createBitSet(1, 0, 0, 0, 0);
		SerializableBitSet d = createBitSet(0, 0, 0, 1, 0);
		SerializableBitSet e = createBitSet(0, 1, 0, 1, 1);

		List<SerializableBitSet> bitSets = new ArrayList<>();
		bitSets.add(a);
		bitSets.add(b);
		bitSets.add(c);
		bitSets.add(d);
		bitSets.add(e);

		// Act
		SerializableBitSet[] minimalDifferenceSets = minimalStrategy.calculateMinimalDifferenceSets(bitSets);

		// Assert
		Assert.assertEquals(minimalDifferenceSets.length, 2);
		Assert.assertEqualsNoOrder(minimalDifferenceSets, new SerializableBitSet[]{c, d});
	}
}
