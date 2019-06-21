package de.hpi.hit_ucc.behaviour.differenceSets;

import de.hpi.hit_ucc.behaviour.differenceSets.NaiveDifferenceSetDetector;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.BitSet;
import java.util.HashSet;

public class NaiveDifferenceSetDetectorTest {

	private NaiveDifferenceSetDetector differenceSetDetector;

	@BeforeMethod
	private void beforeMethod() {
		differenceSetDetector = new NaiveDifferenceSetDetector();
		differenceSetDetector.setDirty();
	}

	private BitSet createBitSet(int... bits) {
		BitSet set = new BitSet();
		for (int i = 0; i < bits.length; i++) if (bits[i] == 1) set.set(i);
		return set;
	}

	@Test
	public void testAddDifferenceSet() {
		BitSet[] testSets = new BitSet[]{
				createBitSet(0, 1, 1, 0, 1),
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		for (BitSet set : testSets) differenceSetDetector.addDifferenceSet(set);

		// added 3 sets
		Assert.assertEquals(differenceSetDetector.uniqueDifferenceSets.size(), 3);
		Assert.assertEqualsNoOrder(differenceSetDetector.uniqueDifferenceSets.toArray(), testSets);

		// not added because the new set is a duplicate
		differenceSetDetector.addDifferenceSet(createBitSet(1, 0, 0, 0, 0));
		Assert.assertEquals(differenceSetDetector.uniqueDifferenceSets.size(), 3);
		Assert.assertEqualsNoOrder(differenceSetDetector.uniqueDifferenceSets.toArray(), testSets);
	}

	@Test(dependsOnMethods = {"testAddDifferenceSet"})
	public void testCalculateMinimalDifferenceSets() {
		// Arrange
		BitSet setA = createBitSet(1, 1, 0, 1, 1);
		BitSet setB = createBitSet(1, 1, 1, 1, 1);
		BitSet setC = createBitSet(1, 0, 0, 0, 0);
		BitSet setD = createBitSet(0, 0, 0, 1, 0);
		BitSet setE = createBitSet(0, 1, 0, 1, 1);

		differenceSetDetector.addDifferenceSet(setA);
		differenceSetDetector.addDifferenceSet(setB);
		differenceSetDetector.addDifferenceSet(setD);
		differenceSetDetector.addDifferenceSet(setE);
		differenceSetDetector.addDifferenceSet(setC);

		// Act
		BitSet[] minimalDifferenceSets = differenceSetDetector.calculateMinimalDifferenceSets();

		// Assert
		Assert.assertEquals(minimalDifferenceSets.length, 2);
		Assert.assertEqualsNoOrder(minimalDifferenceSets, new BitSet[]{setC, setD});
	}

	@Test(dependsOnMethods = {"testCalculateMinimalDifferenceSets"})
	public void testMergeMinimalDifferenceSets() {
		BitSet addedDifferenceSet = createBitSet(1, 0, 1, 0, 1);
		differenceSetDetector.addDifferenceSet(addedDifferenceSet);

		BitSet[] testSetsA = new BitSet[]{
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(0, 0, 1, 1, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		BitSet[] testSetsB = new BitSet[]{
				createBitSet(0, 0, 0, 0, 1),
				createBitSet(0, 1, 1, 1, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		BitSet[] expectedMergedSets = new BitSet[]{
				createBitSet(0, 0, 0, 0, 1),
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(0, 0, 1, 1, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		BitSet[] actualMergedSets = differenceSetDetector.mergeMinimalDifferenceSets(testSetsA, testSetsB);
		Assert.assertEquals(actualMergedSets.length, expectedMergedSets.length);
		Assert.assertEqualsNoOrder(actualMergedSets, expectedMergedSets);

		// test that the merge don't interfere with the previously added sets
		Assert.assertEquals(differenceSetDetector.uniqueDifferenceSets.size(), 1);
		Assert.assertEqualsNoOrder(differenceSetDetector.uniqueDifferenceSets.toArray(), new BitSet[]{addedDifferenceSet});
	}

	@Test(dependsOnMethods = {"testAddDifferenceSet"})
	public void testClearState() {
		BitSet[] testSets = new BitSet[]{
				createBitSet(0, 1, 1, 0, 1),
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		for (BitSet set : testSets) differenceSetDetector.addDifferenceSet(set);
		differenceSetDetector.clearState();
		Assert.assertEquals(differenceSetDetector.uniqueDifferenceSets.size(), 0);
		Assert.assertEquals(differenceSetDetector.uniqueDifferenceSets.toArray(), new BitSet[0]);
		Assert.assertEquals(differenceSetDetector.uniqueDifferenceSets, new HashSet<BitSet>());
	}
}
