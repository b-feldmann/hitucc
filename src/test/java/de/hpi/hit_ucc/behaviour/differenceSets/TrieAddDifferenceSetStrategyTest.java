package de.hpi.hit_ucc.behaviour.differenceSets;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static de.hpi.hit_ucc.behaviour.differenceSets.DifferenceSetDetectorTest.createBitSet;

public class TrieAddDifferenceSetStrategyTest {
	private IAddDifferenceSetStrategy addStrategy;

	private List<BitSet> iterableToArray(Iterable<BitSet> iterable) {
		List<BitSet> list = new ArrayList<>();
		for (BitSet set : iterable) list.add(set);
		return list;
	}

	@BeforeMethod
	private void beforeMethod() {
		addStrategy = new TrieAddDifferenceSetStrategy(5);
	}

	@Test
	public void testAddDifferenceSet() {
		BitSet[] testSets = new BitSet[]{
				createBitSet(0, 1, 1, 0, 1),
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		for (BitSet set : testSets) addStrategy.addDifferenceSet(set);

		// added 3 sets
		Assert.assertEquals(iterableToArray(addStrategy.getIterable()).size(), 3);
		Assert.assertEqualsNoOrder(iterableToArray(addStrategy.getIterable()).toArray(), testSets);

		// not added because the new set is a duplicate
		addStrategy.addDifferenceSet(createBitSet(1, 0, 0, 0, 0));
		Assert.assertEquals(iterableToArray(addStrategy.getIterable()).size(), 3);
		Assert.assertEqualsNoOrder(iterableToArray(addStrategy.getIterable()).toArray(), testSets);
	}

	@Test(dependsOnMethods = {"testAddDifferenceSet"})
	private void testIterator() {
		BitSet a = createBitSet(0, 1, 0, 1, 0);
		BitSet b = createBitSet(1, 1, 0, 1, 0);
		BitSet c = createBitSet(1, 1, 1, 1, 0);
		BitSet d = createBitSet(1, 0, 1, 1, 0);
		BitSet e = createBitSet(0, 1, 0, 1, 1);

		addStrategy.addDifferenceSet(a);
		addStrategy.addDifferenceSet(b);
		addStrategy.addDifferenceSet(c);
		addStrategy.addDifferenceSet(d);
		addStrategy.addDifferenceSet(e);

		List<BitSet> expectedList = new ArrayList<>();
		expectedList.add(a);
		expectedList.add(b);
		expectedList.add(c);
		expectedList.add(d);
		expectedList.add(e);

		Assert.assertEqualsNoOrder(iterableToArray(addStrategy.getIterable()).toArray(), expectedList.toArray());
	}
}
