package hit_ucc.performance;

import hit_ucc.model.SerializableBitSet;
import javolution.util.FastBitSet;
import org.roaringbitmap.RoaringBitmap;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.util.Random;

public class BitSetPerformanceTest {

	private int bitSetCount = 10000;
	private int bitSetLength = 20;

	private SerializableBitSet createRandomBitSet(Random r, int length) {
		SerializableBitSet bitSet = new SerializableBitSet(length);

		int index = 0;
		while (index < length) {
			bitSet.flip(index);
			index += r.nextInt(5);
		}

		return bitSet;
	}

	private RoaringBitmap createRandomRoaringBitSet(Random r, int length) {
		RoaringBitmap bitSet = new RoaringBitmap();
//		RoaringBitmap bitSet = RoaringBitmap.bitmapOf(length);
//		bitSet.remove(length);

		int index = 0;
		while (index < length) {
			bitSet.flip(index);
			index += r.nextInt(5);
		}

		return bitSet;
	}

	private FastBitSet createRandomFastBitSet(Random r, int length) {
		FastBitSet bitSet = new FastBitSet(length);

		int index = 0;
		while (index < length) {
			bitSet.flip(index);
			index += r.nextInt(5);
		}

		return bitSet;
	}

	@Test()
	private void testBitSet() {
		SerializableBitSet[] serializableBitSets = new SerializableBitSet[bitSetCount];
		RoaringBitmap[] roaringBitmaps = new RoaringBitmap[bitSetCount];
		FastBitSet[] fastBitSets = new FastBitSet[bitSetCount];
		Random r = new Random(1337);

		Reporter.log("Create " + bitSetCount + " bitsets with length " + bitSetLength, true);
		long start = System.currentTimeMillis();
		for (int i = 0; i < bitSetCount; i++) {
			serializableBitSets[i] = createRandomBitSet(r, bitSetLength);
		}
		long end = System.currentTimeMillis();
		Reporter.log("Created sets in " + (end - start) + " ms [ser]", true);
		start = System.currentTimeMillis();
		for (int i = 0; i < bitSetCount; i++) {
			roaringBitmaps[i] = createRandomRoaringBitSet(r, bitSetLength);
		}
		end = System.currentTimeMillis();
		Reporter.log("Created sets in " + (end - start) + " ms [roar]", true);
		start = System.currentTimeMillis();
		for (int i = 0; i < bitSetCount; i++) {
			fastBitSets[i] = createRandomFastBitSet(r, bitSetLength);
		}
		end = System.currentTimeMillis();
		Reporter.log("Created sets in " + (end - start) + " ms [fast]", true);

		Reporter.log("Test " + (bitSetCount + 1) * bitSetCount / 2 + " bitset intersections", true);
		start = System.currentTimeMillis();
		for (int i = 0; i < bitSetCount; i++) {
			for (int k = i + 1; k < bitSetCount; k++) {
				serializableBitSets[i].and(serializableBitSets[k]);
			}
		}
		end = System.currentTimeMillis();
		Reporter.log("Intersected all sets in " + (end - start) + " ms [ser]", true);
		start = System.currentTimeMillis();
		for (int i = 0; i < bitSetCount; i++) {
			for (int k = i + 1; k < bitSetCount; k++) {
				roaringBitmaps[i].and(roaringBitmaps[k]);
			}
		}
		end = System.currentTimeMillis();
		Reporter.log("Intersected all sets in " + (end - start) + " ms [roar]", true);
		start = System.currentTimeMillis();
		for (int i = 0; i < bitSetCount; i++) {
			for (int k = i + 1; k < bitSetCount; k++) {
				fastBitSets[i].and(fastBitSets[k]);
			}
		}
		end = System.currentTimeMillis();
		Reporter.log("Intersected all sets in " + (end - start) + " ms [fast]", true);

		Reporter.log("Test " + bitSetCount + " random accesses", true);
		start = System.currentTimeMillis();
		for (int i = 0; i < bitSetCount; i++) {
			serializableBitSets[i].get(r.nextInt(bitSetLength));
		}
		end = System.currentTimeMillis();
		Reporter.log("Collected all in " + (end - start) + " ms [ser]", true);
		start = System.currentTimeMillis();
		for (int i = 0; i < bitSetCount; i++) {
			roaringBitmaps[i].contains(r.nextInt(bitSetLength));
		}
		end = System.currentTimeMillis();
		Reporter.log("Collected all in " + (end - start) + " ms [roar]", true);
		start = System.currentTimeMillis();
		for (int i = 0; i < bitSetCount; i++) {
			fastBitSets[i].get(r.nextInt(bitSetLength));
		}
		end = System.currentTimeMillis();
		Reporter.log("Collected all in " + (end - start) + " ms [fast]", true);

		Assert.assertEquals(serializableBitSets.length, bitSetCount);
		Assert.assertEquals(roaringBitmaps.length, bitSetCount);
		Assert.assertEquals(fastBitSets.length, bitSetCount);
	}
}
