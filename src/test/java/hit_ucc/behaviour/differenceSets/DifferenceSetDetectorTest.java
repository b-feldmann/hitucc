package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class DifferenceSetDetectorTest {

	private DifferenceSetDetector differenceSetDetector;
	private IAddDifferenceSetStrategy addDifferenceSetStrategy;

	@BeforeMethod
	private void beforeMethod() {
		// using the naive detector to test  detector
		addDifferenceSetStrategy = new HashAddDifferenceSetStrategy();
		differenceSetDetector = new DifferenceSetDetector(
				addDifferenceSetStrategy, new SortingCalculateMinimalSetsStrategy(), new OneSidedMergeMinimalSetsStrategy()
		);
	}

	protected static SerializableBitSet createBitSet(int... bits) {
		SerializableBitSet set = new SerializableBitSet(bits.length);
		for (int i = 0; i < bits.length; i++) if (bits[i] == 1) set.set(i);
		return set;
	}

	@Test
	public void testAddDifferenceSet() {
		String[] rowA = new String[]{"A", "A", "A"};
		String[] rowB = new String[]{"A", "B", "B"};
		String[] rowC = new String[]{"B", "A", "C"};
		String[] rowD = new String[]{"A", "A", null};
		String[] rowE = new String[]{"A", "A", null};

		Assert.assertEquals(differenceSetDetector.addDifferenceSet(rowA, rowB), createBitSet(0, 1, 1));
		Assert.assertEquals(differenceSetDetector.addDifferenceSet(rowA, rowC), createBitSet(1, 0, 1));
		Assert.assertEquals(differenceSetDetector.addDifferenceSet(rowA, rowD), createBitSet(0, 0, 1));
		Assert.assertEquals(differenceSetDetector.addDifferenceSet(rowA, rowE), createBitSet(0, 0, 1));

		Assert.assertEquals(differenceSetDetector.addDifferenceSet(rowB, rowC), createBitSet(1, 1, 1));
		Assert.assertEquals(differenceSetDetector.addDifferenceSet(rowB, rowD), createBitSet(0, 1, 1));
		Assert.assertEquals(differenceSetDetector.addDifferenceSet(rowB, rowE), createBitSet(0, 1, 1));

		Assert.assertEquals(differenceSetDetector.addDifferenceSet(rowC, rowD), createBitSet(1, 0, 1));
		Assert.assertEquals(differenceSetDetector.addDifferenceSet(rowC, rowE), createBitSet(1, 0, 1));

		Assert.assertEquals(differenceSetDetector.addDifferenceSet(rowD, rowE), createBitSet(0, 0, 1));
		Assert.assertEquals(differenceSetDetector.addDifferenceSet(rowD, rowE, false), createBitSet(0, 0, 1));
		Assert.assertEquals(differenceSetDetector.addDifferenceSet(rowD, rowE, true), createBitSet(0, 0, 0));
	}

	@Test(dependsOnMethods = {"testSetDirty"})
	public void testGetMinimalDifferenceSets() {
		SerializableBitSet[] testSetsA = new SerializableBitSet[]{
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(0, 1, 1, 1, 0),
				createBitSet(1, 0, 0, 0, 0),
				createBitSet(1, 0, 1, 1, 1),
				createBitSet(1, 1, 0, 0, 0),
		};

		for (SerializableBitSet set : testSetsA) {
			addDifferenceSetStrategy.addDifferenceSet(set);
		}
		differenceSetDetector.setDirty();

		SerializableBitSet[] expectedMinimalSets = new SerializableBitSet[]{
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		SerializableBitSet[] actualSets = differenceSetDetector.getMinimalDifferenceSets();
		Assert.assertEquals(actualSets.length, expectedMinimalSets.length);
		Assert.assertEqualsNoOrder(actualSets, expectedMinimalSets);
	}

	@Test(dependsOnMethods = {"testSetDirty"})
	public void testMergeMinimalDifferenceSets() {
		SerializableBitSet[] testSetsA = new SerializableBitSet[]{
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(0, 0, 1, 1, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		for (SerializableBitSet set : testSetsA) {
			addDifferenceSetStrategy.addDifferenceSet(set);
		}
		differenceSetDetector.setDirty();

		SerializableBitSet[] testSetsB = new SerializableBitSet[]{
				createBitSet(0, 0, 0, 0, 1),
				createBitSet(0, 1, 1, 1, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		SerializableBitSet[] expectedMergedSets = new SerializableBitSet[]{
				createBitSet(0, 0, 0, 0, 1),
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(0, 0, 1, 1, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		SerializableBitSet[] actualMergedSets = differenceSetDetector.mergeMinimalDifferenceSets(testSetsB);
		Assert.assertEquals(actualMergedSets.length, expectedMergedSets.length);
		Assert.assertEqualsNoOrder(actualMergedSets, expectedMergedSets);
	}

	@Test
	public void testTestMinimalHittingSet() {
		// Arrange
		SerializableBitSet setA = createBitSet(1, 1, 0, 1, 1);
		SerializableBitSet setB = createBitSet(1, 1, 1, 1, 1);
		SerializableBitSet setC = createBitSet(1, 0, 0, 0, 0);
		SerializableBitSet setD = createBitSet(0, 0, 0, 1, 0);
		SerializableBitSet setE = createBitSet(0, 1, 0, 1, 1);

		SerializableBitSet setF1 = createBitSet(0, 1, 0, 1, 1);
		SerializableBitSet setF2 = createBitSet(0, 1, 0, 1, 1);

		SerializableBitSet setG1 = createBitSet(0, 0, 0, 0, 0);
		SerializableBitSet setG2 = createBitSet(0, 0, 0, 0, 0);

		SerializableBitSet setH1 = createBitSet(0, 0, 0, 0, 0);
		SerializableBitSet setH2 = createBitSet(0, 1, 0, 1, 1);


		// Act
		int resultAB = DifferenceSetDetector.testMinimalHittingSet(setA, setB);
		int resultAC = DifferenceSetDetector.testMinimalHittingSet(setA, setC);
		int resultAD = DifferenceSetDetector.testMinimalHittingSet(setA, setD);
		int resultAE = DifferenceSetDetector.testMinimalHittingSet(setA, setE);

		int resultBC = DifferenceSetDetector.testMinimalHittingSet(setB, setC);
		int resultBD = DifferenceSetDetector.testMinimalHittingSet(setB, setD);
		int resultBE = DifferenceSetDetector.testMinimalHittingSet(setB, setE);

		int resultCD = DifferenceSetDetector.testMinimalHittingSet(setC, setD);
		int resultCE = DifferenceSetDetector.testMinimalHittingSet(setC, setE);

		int resultDE = DifferenceSetDetector.testMinimalHittingSet(setD, setE);

		int resultFF = DifferenceSetDetector.testMinimalHittingSet(setF1, setF2);
		int resultGG = DifferenceSetDetector.testMinimalHittingSet(setG1, setG2);
		int resultHH = DifferenceSetDetector.testMinimalHittingSet(setH1, setH2);

		// Assert
		Assert.assertEquals(resultAB, DifferenceSetDetector.FIRST_MINIMAL);
		Assert.assertEquals(resultAC, DifferenceSetDetector.SECOND_MINIMAL);
		Assert.assertEquals(resultAD, DifferenceSetDetector.SECOND_MINIMAL);
		Assert.assertEquals(resultAE, DifferenceSetDetector.SECOND_MINIMAL);
		Assert.assertEquals(resultBC, DifferenceSetDetector.SECOND_MINIMAL);
		Assert.assertEquals(resultBD, DifferenceSetDetector.SECOND_MINIMAL);
		Assert.assertEquals(resultBE, DifferenceSetDetector.SECOND_MINIMAL);
		Assert.assertEquals(resultCD, DifferenceSetDetector.NONE_MINIMAL);
		Assert.assertEquals(resultCE, DifferenceSetDetector.NONE_MINIMAL);
		Assert.assertEquals(resultDE, DifferenceSetDetector.FIRST_MINIMAL);

		Assert.assertEquals(resultFF, DifferenceSetDetector.EQUAL_SETS);
		Assert.assertEquals(resultGG, DifferenceSetDetector.EQUAL_SETS);
		Assert.assertEquals(resultHH, DifferenceSetDetector.NONE_MINIMAL);
	}

	@Test
	private void testClearState() {
		SerializableBitSet[] testSets = new SerializableBitSet[]{
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(0, 0, 1, 1, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		for (SerializableBitSet set : testSets) {
			addDifferenceSetStrategy.addDifferenceSet(set);
		}

		Assert.assertEquals(addDifferenceSetStrategy.getIterable().iterator().next(), testSets[0]);
		differenceSetDetector.clearState();
		Assert.assertFalse(addDifferenceSetStrategy.getIterable().iterator().hasNext());
	}

	@Test
	private void testSetDirty() {
		SerializableBitSet[] testSets = new SerializableBitSet[]{
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(0, 0, 1, 1, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		for (SerializableBitSet set : testSets) {
			addDifferenceSetStrategy.addDifferenceSet(set);
		}

		Assert.assertEquals(differenceSetDetector.getMinimalDifferenceSets().length, 0);
		Assert.assertEqualsNoOrder(differenceSetDetector.getMinimalDifferenceSets(), new SerializableBitSet[0]);

		differenceSetDetector.setDirty();

		Assert.assertEquals(differenceSetDetector.getMinimalDifferenceSets().length, testSets.length);
		Assert.assertEqualsNoOrder(differenceSetDetector.getMinimalDifferenceSets(), testSets);
	}

	@Test
	private void testIsSubset() {
		SerializableBitSet setA = createBitSet(0, 1, 0, 1);
		SerializableBitSet setB = createBitSet(0, 1, 1, 1);
		SerializableBitSet setC = createBitSet(0, 0, 1, 1);
		SerializableBitSet setD = createBitSet(0, 0, 0, 1);

		Assert.assertTrue(DifferenceSetDetector.isSubset(setA, setB));
		Assert.assertFalse(DifferenceSetDetector.isSubset(setB, setA));
		Assert.assertFalse(DifferenceSetDetector.isSubset(setA, setC));
		Assert.assertFalse(DifferenceSetDetector.isSubset(setC, setA));
		Assert.assertFalse(DifferenceSetDetector.isSubset(setA, setD));
		Assert.assertTrue(DifferenceSetDetector.isSubset(setD, setA));

		Assert.assertFalse(DifferenceSetDetector.isSubset(setB, setC));
		Assert.assertTrue(DifferenceSetDetector.isSubset(setC, setB));
		Assert.assertFalse(DifferenceSetDetector.isSubset(setB, setD));
		Assert.assertTrue(DifferenceSetDetector.isSubset(setD, setB));

		Assert.assertFalse(DifferenceSetDetector.isSubset(setC, setD));
		Assert.assertTrue(DifferenceSetDetector.isSubset(setD, setC));

		Assert.assertTrue(DifferenceSetDetector.isSubset(setA, setA));
		Assert.assertTrue(DifferenceSetDetector.isSubset(setB, setB));
		Assert.assertTrue(DifferenceSetDetector.isSubset(setC, setC));
		Assert.assertTrue(DifferenceSetDetector.isSubset(setD, setD));
	}

	@Test(dependsOnMethods = {"testIsSubset"})
	private void testInsertMinimalDifferenceSets() {
		List<SerializableBitSet> bitSets = new ArrayList<>();

		SerializableBitSet setA = createBitSet(0, 1, 0, 0);
		SerializableBitSet setB = createBitSet(0, 0, 1, 0);
		SerializableBitSet setC = createBitSet(0, 0, 1, 1);
		SerializableBitSet setD = createBitSet(0, 1, 0, 1);
		SerializableBitSet setE = createBitSet(0, 0, 1, 1);
		SerializableBitSet setF = createBitSet(0, 1, 1, 1);
		SerializableBitSet setG = createBitSet(1, 0, 0, 1);

		List<SerializableBitSet> listA = new ArrayList<>();
		listA.add(setA);

		List<SerializableBitSet> listBF = new ArrayList<>();
		listBF.add(setA);
		listBF.add(setB);

		List<SerializableBitSet> listG = new ArrayList<>();
		listG.add(setA);
		listG.add(setB);
		listG.add(setG);

		DifferenceSetDetector.insertMinimalDifferenceSets(bitSets, setA);
		Assert.assertEquals(bitSets.size(), 1);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listA.toArray());

		DifferenceSetDetector.insertMinimalDifferenceSets(bitSets, setB);
		Assert.assertEquals(bitSets.size(), 2);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listBF.toArray());

		DifferenceSetDetector.insertMinimalDifferenceSets(bitSets, setC);
		Assert.assertEquals(bitSets.size(), 2);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listBF.toArray());

		DifferenceSetDetector.insertMinimalDifferenceSets(bitSets, setD);
		Assert.assertEquals(bitSets.size(), 2);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listBF.toArray());

		DifferenceSetDetector.insertMinimalDifferenceSets(bitSets, setE);
		Assert.assertEquals(bitSets.size(), 2);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listBF.toArray());

		DifferenceSetDetector.insertMinimalDifferenceSets(bitSets, setF);
		Assert.assertEquals(bitSets.size(), 2);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listBF.toArray());

		DifferenceSetDetector.insertMinimalDifferenceSets(bitSets, setG);
		Assert.assertEquals(bitSets.size(), 3);
		Assert.assertEqualsNoOrder(bitSets.toArray(), listG.toArray());
	}
}
