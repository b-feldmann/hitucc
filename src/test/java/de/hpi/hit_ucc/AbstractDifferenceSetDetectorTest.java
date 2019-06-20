package de.hpi.hit_ucc;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.BitSet;

public class AbstractDifferenceSetDetectorTest {

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
}
