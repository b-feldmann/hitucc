package de.hpi.hit_ucc;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.BitSet;

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
	public void testGetMinimalDifferenceSets() {
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
		BitSet[] minimalDifferenceSets = differenceSetDetector.getMinimalDifferenceSets();

		// Assert
		Assert.assertEquals(minimalDifferenceSets.length, 2);
		Assert.assertEqualsNoOrder(minimalDifferenceSets, new BitSet[]{setC, setD});
	}
}
