package de.hpi.hit_ucc;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.BitSet;
import java.util.LinkedHashSet;

public class DifferenceSetDetectorTest {

	private BitSet createBitSet(int... bits) {
		BitSet set = new BitSet();
		for (int i = 0; i < bits.length; i++) if (bits[i] == 1) set.set(i);
		return set;
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
	}

	@Test
	public void testGetMinimalDifferenceSets() {
		// Arrange
		BitSet setA = createBitSet(1, 1, 0, 1, 1); // x
		BitSet setB = createBitSet(1, 1, 1, 1, 1); // x
		BitSet setC = createBitSet(1, 0, 0, 0, 0); //
		BitSet setD = createBitSet(0, 0, 0, 1, 0); //
		BitSet setE = createBitSet(0, 1, 0, 1, 1); // x
		LinkedHashSet<BitSet> uniqueSets = new LinkedHashSet<>();
		uniqueSets.add(setA);
		uniqueSets.add(setB);
		uniqueSets.add(setD);
		uniqueSets.add(setE);
		uniqueSets.add(setC);

		// Act
		BitSet[] minimalDifferenceSets = DifferenceSetDetector.GetMinimalDifferenceSets(uniqueSets);

		// Assert
		Assert.assertEquals(uniqueSets.size(), 5);
		Assert.assertEquals(minimalDifferenceSets.length, 2);
		Assert.assertEqualsNoOrder(minimalDifferenceSets, new BitSet[]{setC, setD});
	}
}
