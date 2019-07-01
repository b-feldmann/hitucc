package hit_ucc.model;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class TrieSetTest {
	private TrieSet trieSet;

	private BitSet createBitSet(int... bits) {
		BitSet set = new BitSet();
		for (int i = 0; i < bits.length; i++) if (bits[i] == 1) set.set(i);
		return set;
	}

	@BeforeMethod
	private void beforeMethod() {
		trieSet = new TrieSet(5);
	}

	@Test
	private void testAdd() {
		BitSet a = createBitSet(0, 1, 0, 1, 0);
		BitSet b = createBitSet(1, 1, 0, 1, 0);
		BitSet c = createBitSet(1, 1, 1, 1, 0);

		trieSet.add(a);
		trieSet.add(b);
		trieSet.add(c);

		Assert.assertEquals(trieSet.size(), 3);

		trieSet.add(c);
		Assert.assertEquals(trieSet.size(), 3);
	}

	@Test(dependsOnMethods = {"testAdd"})
	private void testContains() {
		BitSet a = createBitSet(1, 1, 0, 1, 0);
		BitSet b = createBitSet(1, 0, 0, 0, 0);
		trieSet.add(a);
		Assert.assertEquals(trieSet.contains(a), true);
		Assert.assertEquals(trieSet.contains(b), false);
	}

	@Test(dependsOnMethods = {"testAdd"})
	private void testSize() {
		BitSet a = createBitSet(1, 1, 0, 1, 0);
		BitSet b = createBitSet(1, 0, 0, 0, 0);

		Assert.assertEquals(trieSet.size(), 0);
		trieSet.add(a);
		trieSet.add(b);
		Assert.assertEquals(trieSet.size(), 2);
		trieSet.add(a);
		Assert.assertEquals(trieSet.size(), 2);
	}

	@Test(dependsOnMethods = {"testAdd", "testSize", "testContains"})
	private void testDelete() {
		BitSet a = createBitSet(1, 1, 0, 1, 0);
		BitSet b = createBitSet(1, 0, 1, 1, 1);

		Assert.assertEquals(trieSet.size(), 0);
		trieSet.add(a);
		trieSet.add(b);
		Assert.assertEquals(trieSet.size(), 2);
		trieSet.delete(a);
		Assert.assertEquals(trieSet.contains(a), false);
		Assert.assertEquals(trieSet.contains(b), true);
		Assert.assertEquals(trieSet.size(), 1);
	}

	@Test(dependsOnMethods = {"testAdd", "testDelete"})
	private void testEmpty() {
		BitSet a = createBitSet(1, 1, 0, 1, 0);
		BitSet b = createBitSet(1, 0, 1, 0, 1);

		Assert.assertEquals(trieSet.isEmpty(), true);
		trieSet.add(a);
		trieSet.add(b);
		Assert.assertEquals(trieSet.isEmpty(), false);
		trieSet.delete(a);
		Assert.assertEquals(trieSet.isEmpty(), false);
		trieSet.delete(b);
		Assert.assertEquals(trieSet.isEmpty(), true);
	}

	@Test(dependsOnMethods = {"testAdd"})
	private void testIterator() {
		BitSet a = createBitSet(0, 1, 0, 1, 0);
		BitSet b = createBitSet(1, 1, 0, 1, 0);
		BitSet c = createBitSet(1, 1, 1, 1, 0);
		BitSet d = createBitSet(1, 0, 1, 1, 0);
		BitSet e = createBitSet(0, 1, 0, 1, 1);

		trieSet.add(a);
		trieSet.add(b);
		trieSet.add(c);
		trieSet.add(d);
		trieSet.add(e);

		List<BitSet> expectedList = new ArrayList<>();
		expectedList.add(a);
		expectedList.add(b);
		expectedList.add(c);
		expectedList.add(d);
		expectedList.add(e);

		List<BitSet> actualList = new ArrayList<>();
		for (BitSet set : trieSet) actualList.add(set);

		Assert.assertEqualsNoOrder(actualList.toArray(), expectedList.toArray());
	}

	@Test(dependsOnMethods = {"testAdd"})
	private void testKeysWithPrefix() {
	}

	@Test(dependsOnMethods = {"testAdd"})
	private void testKeysThatMatch() {
	}

	@Test(dependsOnMethods = {"testAdd"})
	private void testLongestPrefixOf() {
	}
}
