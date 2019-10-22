package hitucc.model;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TrieSetTest {
	private TrieSet trieSet;

	private SerializableBitSet createBitSet(int... bits) {
		SerializableBitSet set = new SerializableBitSet(bits.length);
		for (int i = 0; i < bits.length; i++) if (bits[i] == 1) set.set(i);
		return set;
	}

	@BeforeMethod
	private void beforeMethod() {
		trieSet = new TrieSet(5);
	}

	@Test
	private void testAdd() {
		SerializableBitSet a = createBitSet(0, 1, 0, 1, 0);
		SerializableBitSet b = createBitSet(1, 1, 0, 1, 0);
		SerializableBitSet c = createBitSet(1, 1, 1, 1, 0);

		trieSet.add(a);
		trieSet.add(b);
		trieSet.add(c);

		Assert.assertEquals(trieSet.size(), 3);

		trieSet.add(c);
		Assert.assertEquals(trieSet.size(), 3);
	}

	@Test(dependsOnMethods = {"testAdd"})
	private void testContains() {
		SerializableBitSet a = createBitSet(1, 1, 0, 1, 0);
		SerializableBitSet b = createBitSet(1, 0, 0, 0, 0);
		trieSet.add(a);
		Assert.assertEquals(trieSet.contains(a), true);
		Assert.assertEquals(trieSet.contains(b), false);
	}

	@Test(dependsOnMethods = {"testAdd"})
	private void testSize() {
		SerializableBitSet a = createBitSet(1, 1, 0, 1, 0);
		SerializableBitSet b = createBitSet(1, 0, 0, 0, 0);

		Assert.assertEquals(trieSet.size(), 0);
		trieSet.add(a);
		trieSet.add(b);
		Assert.assertEquals(trieSet.size(), 2);
		trieSet.add(a);
		Assert.assertEquals(trieSet.size(), 2);
	}

	@Test(dependsOnMethods = {"testAdd", "testSize", "testContains"})
	private void testDelete() {
		SerializableBitSet a = createBitSet(1, 1, 0, 1, 0);
		SerializableBitSet b = createBitSet(1, 0, 1, 1, 1);

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
		SerializableBitSet a = createBitSet(1, 1, 0, 1, 0);
		SerializableBitSet b = createBitSet(1, 0, 1, 0, 1);

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
		SerializableBitSet a = createBitSet(0, 1, 0, 1, 0);
		SerializableBitSet b = createBitSet(1, 1, 0, 1, 0);
		SerializableBitSet c = createBitSet(1, 1, 1, 1, 0);
		SerializableBitSet d = createBitSet(1, 0, 1, 1, 0);
		SerializableBitSet e = createBitSet(0, 1, 0, 1, 1);

		trieSet.add(a);
		trieSet.add(b);
		trieSet.add(c);
		trieSet.add(d);
		trieSet.add(e);

		List<SerializableBitSet> expectedList = new ArrayList<>();
		expectedList.add(a);
		expectedList.add(b);
		expectedList.add(c);
		expectedList.add(d);
		expectedList.add(e);

		List<SerializableBitSet> actualList = new ArrayList<>();
		for (SerializableBitSet set : trieSet) actualList.add(set);

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
