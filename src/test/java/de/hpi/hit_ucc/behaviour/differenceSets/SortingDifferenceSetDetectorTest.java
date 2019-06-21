package de.hpi.hit_ucc.behaviour.differenceSets;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;

public class SortingDifferenceSetDetectorTest {
	private SortingDifferenceSetDetector differenceSetDetector;

	@BeforeMethod
	private void beforeMethod() {
		differenceSetDetector = new SortingDifferenceSetDetector();
	}

	private BitSet createBitSet(int... bits) {
		BitSet set = new BitSet();
		for (int i = 0; i < bits.length; i++) if (bits[i] == 1) set.set(i);
		return set;
	}

	@Test
	private void testAddDifferenceSet() {
		BitSet[] testSets = new BitSet[]{
				createBitSet(0, 1, 1, 0, 1),
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		BitSet testSet = createBitSet(1, 0, 0, 0, 0);

		for (BitSet set : testSets) differenceSetDetector.addDifferenceSet(set);

		// added 3 sets
		Assert.assertEquals(differenceSetDetector.uniqueDifferenceSets.size(), 3);
		Assert.assertEqualsNoOrder(differenceSetDetector.uniqueDifferenceSets.toArray(), testSets);

		// duplicates are added
		differenceSetDetector.addDifferenceSet(testSet);
		Assert.assertEquals(differenceSetDetector.uniqueDifferenceSets.size(), 3);
		Assert.assertEqualsNoOrder(differenceSetDetector.uniqueDifferenceSets.toArray(), testSets);

	}

	@Test(dependsOnMethods = {"testInsertMinimalDifferenceSets"})
	private void testCalculateMinimalDifferenceSets() {
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

	@Test(dependsOnMethods = {"testInsertMinimalDifferenceSets"})
	private void testMergeMinimalDifferenceSets() {
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
	private void testClearState() {
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

	@Test
	private void testIsSubset() {
		BitSet setA = createBitSet(0, 1, 0, 1);
		BitSet setB = createBitSet(0, 1, 1, 1);
		BitSet setC = createBitSet(0, 0, 1, 1);
		BitSet setD = createBitSet(0, 0, 0, 1);

		Assert.assertEquals(differenceSetDetector.isSubset(setA, setB), true);
		Assert.assertEquals(differenceSetDetector.isSubset(setB, setA), false);
		Assert.assertEquals(differenceSetDetector.isSubset(setA, setC), false);
		Assert.assertEquals(differenceSetDetector.isSubset(setC, setA), false);
		Assert.assertEquals(differenceSetDetector.isSubset(setA, setD), false);
		Assert.assertEquals(differenceSetDetector.isSubset(setD, setA), true);

		Assert.assertEquals(differenceSetDetector.isSubset(setB, setC), false);
		Assert.assertEquals(differenceSetDetector.isSubset(setC, setB), true);
		Assert.assertEquals(differenceSetDetector.isSubset(setB, setD), false);
		Assert.assertEquals(differenceSetDetector.isSubset(setD, setB), true);

		Assert.assertEquals(differenceSetDetector.isSubset(setC, setD), false);
		Assert.assertEquals(differenceSetDetector.isSubset(setD, setC), true);

		Assert.assertEquals(differenceSetDetector.isSubset(setA, setA), true);
		Assert.assertEquals(differenceSetDetector.isSubset(setB, setB), true);
		Assert.assertEquals(differenceSetDetector.isSubset(setC, setC), true);
		Assert.assertEquals(differenceSetDetector.isSubset(setD, setD), true);
	}

	@Test(dependsOnMethods = {"testIsSubset"})
	private void testInsertMinimalDifferenceSets() {
		List<BitSet> bitSets = new ArrayList<>();

		BitSet setA = createBitSet(0, 1, 0, 0);
		BitSet setB = createBitSet(0, 0, 1, 0);
		BitSet setC = createBitSet(0, 0, 1, 1);
		BitSet setD = createBitSet(0, 1, 0, 1);
		BitSet setE = createBitSet(0, 0, 1, 1);
		BitSet setF = createBitSet(0, 1, 1, 1);
		BitSet setG = createBitSet(1, 0, 0, 1);

		List<BitSet> listA = new ArrayList<>();
		listA.add(setA);

		List<BitSet> listBF = new ArrayList<>();
		listBF.add(setA);
		listBF.add(setB);

		List<BitSet> listG = new ArrayList<>();
		listG.add(setA);
		listG.add(setB);
		listG.add(setG);

		differenceSetDetector.insertMinimalDifferenceSets(bitSets, setA);
		Assert.assertEquals(bitSets.size(), 1);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listA.toArray());

		differenceSetDetector.insertMinimalDifferenceSets(bitSets, setB);
		Assert.assertEquals(bitSets.size(), 2);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listBF.toArray());

		differenceSetDetector.insertMinimalDifferenceSets(bitSets, setC);
		Assert.assertEquals(bitSets.size(), 2);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listBF.toArray());

		differenceSetDetector.insertMinimalDifferenceSets(bitSets, setD);
		Assert.assertEquals(bitSets.size(), 2);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listBF.toArray());

		differenceSetDetector.insertMinimalDifferenceSets(bitSets, setE);
		Assert.assertEquals(bitSets.size(), 2);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listBF.toArray());

		differenceSetDetector.insertMinimalDifferenceSets(bitSets, setF);
		Assert.assertEquals(bitSets.size(), 2);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listBF.toArray());

		differenceSetDetector.insertMinimalDifferenceSets(bitSets, setG);
		Assert.assertEquals(bitSets.size(), 3);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listG.toArray());
	}
}
