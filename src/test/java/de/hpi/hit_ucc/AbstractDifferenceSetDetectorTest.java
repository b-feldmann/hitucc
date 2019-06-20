package de.hpi.hit_ucc;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.BitSet;

public class AbstractDifferenceSetDetectorTest {

	private AbstractDifferenceSetDetector differenceSetDetector;

	@BeforeMethod
	private void beforeMethod() {
		// using the naive detector to test Abstract detector
		differenceSetDetector = new NaiveDifferenceSetDetector();
	}

	private BitSet createBitSet(int... bits) {
		BitSet set = new BitSet();
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

	@Test
	public void testGetMinimalDifferenceSets() {
		BitSet[] testSetsA = new BitSet[]{
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(0, 1, 1, 1, 0),
				createBitSet(1, 0, 0, 0, 0),
				createBitSet(1, 0, 1, 1, 1),
				createBitSet(1, 1, 0, 0, 0),
		};

		for(BitSet set : testSetsA) {
			differenceSetDetector.addDifferenceSet(set);
		}
		differenceSetDetector.setDirty();

		BitSet[] expectedMinimalSets = new BitSet[]{
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		BitSet[] actualSets = differenceSetDetector.getMinimalDifferenceSets();
		Assert.assertEquals(actualSets.length, expectedMinimalSets.length);
		Assert.assertEqualsNoOrder(actualSets, expectedMinimalSets);
	}

	@Test
	public void testMergeMinimalDifferenceSets() {
		BitSet[] testSetsA = new BitSet[]{
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(0, 0, 1, 1, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		for(BitSet set : testSetsA) {
			differenceSetDetector.addDifferenceSet(set);
		}
		differenceSetDetector.setDirty();

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

		BitSet[] actualMergedSets = differenceSetDetector.mergeMinimalDifferenceSets(testSetsB);
		Assert.assertEquals(actualMergedSets.length, expectedMergedSets.length);
		Assert.assertEqualsNoOrder(actualMergedSets, expectedMergedSets);
	}

	@Test
	public void testTestMinimalHittingSet() {
		// Arrange
		BitSet setA = createBitSet(1, 1, 0, 1, 1);
		BitSet setB = createBitSet(1, 1, 1, 1, 1);
		BitSet setC = createBitSet(1, 0, 0, 0, 0);
		BitSet setD = createBitSet(0, 0, 0, 1, 0);
		BitSet setE = createBitSet(0, 1, 0, 1, 1);

		// Act
		int resultAB = AbstractDifferenceSetDetector.testStaticMinimalHittingSet(setA, setB);
		int resultAC = AbstractDifferenceSetDetector.testStaticMinimalHittingSet(setA, setC);
		int resultAD = AbstractDifferenceSetDetector.testStaticMinimalHittingSet(setA, setD);
		int resultAE = AbstractDifferenceSetDetector.testStaticMinimalHittingSet(setA, setE);

		int resultBC = AbstractDifferenceSetDetector.testStaticMinimalHittingSet(setB, setC);
		int resultBD = AbstractDifferenceSetDetector.testStaticMinimalHittingSet(setB, setD);
		int resultBE = AbstractDifferenceSetDetector.testStaticMinimalHittingSet(setB, setE);

		int resultCD = AbstractDifferenceSetDetector.testStaticMinimalHittingSet(setC, setD);
		int resultCE = AbstractDifferenceSetDetector.testStaticMinimalHittingSet(setC, setE);

		int resultDE = AbstractDifferenceSetDetector.testStaticMinimalHittingSet(setD, setE);

		// Assert
		Assert.assertEquals(resultAB, AbstractDifferenceSetDetector.FIRST_MINIMAL);
		Assert.assertEquals(resultAC, AbstractDifferenceSetDetector.SECOND_MINIMAL);
		Assert.assertEquals(resultAD, AbstractDifferenceSetDetector.SECOND_MINIMAL);
		Assert.assertEquals(resultAE, AbstractDifferenceSetDetector.SECOND_MINIMAL);
		Assert.assertEquals(resultBC, AbstractDifferenceSetDetector.SECOND_MINIMAL);
		Assert.assertEquals(resultBD, AbstractDifferenceSetDetector.SECOND_MINIMAL);
		Assert.assertEquals(resultBE, AbstractDifferenceSetDetector.SECOND_MINIMAL);
		Assert.assertEquals(resultCD, AbstractDifferenceSetDetector.NONE_MINIMAL);
		Assert.assertEquals(resultCE, AbstractDifferenceSetDetector.NONE_MINIMAL);
		Assert.assertEquals(resultDE, AbstractDifferenceSetDetector.FIRST_MINIMAL);
	}

	@Test
	private void testSetDirty() {
		BitSet[] testSets = new BitSet[]{
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(0, 0, 1, 1, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		for(BitSet set : testSets) {
			differenceSetDetector.addDifferenceSet(set);
		}

		Assert.assertEquals(differenceSetDetector.getMinimalDifferenceSets().length, 0);
		Assert.assertEqualsNoOrder(differenceSetDetector.getMinimalDifferenceSets(), new BitSet[0]);

		differenceSetDetector.setDirty();

		Assert.assertEquals(differenceSetDetector.getMinimalDifferenceSets().length, testSets.length);
		Assert.assertEqualsNoOrder(differenceSetDetector.getMinimalDifferenceSets(), testSets);
	}
}
