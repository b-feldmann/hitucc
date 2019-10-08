package hit_ucc.behaviour.differenceSets;

import hit_ucc.model.SerializableBitSet;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static hit_ucc.behaviour.differenceSets.DifferenceSetDetectorTest.createBitSet;

public class HashAddDifferenceSetStrategyTest {
	private IAddDifferenceSetStrategy addStrategy;

	private List<SerializableBitSet> iterableToArray(Iterable<SerializableBitSet> iterable) {
		List<SerializableBitSet> list = new ArrayList<>();
		for (SerializableBitSet set : iterable) list.add(set);
		return list;
	}

	@BeforeMethod
	private void beforeMethod() {
		addStrategy = new HashAddDifferenceSetStrategy();
	}

	@Test
	public void testAddDifferenceSet() {
		SerializableBitSet[] testSets = new SerializableBitSet[]{
				createBitSet(0, 1, 1, 0, 1),
				createBitSet(0, 1, 0, 0, 0),
				createBitSet(1, 0, 0, 0, 0)
		};

		for (SerializableBitSet set : testSets) addStrategy.addDifferenceSet(set);

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
		SerializableBitSet a = createBitSet(0, 1, 0, 1, 0);
		SerializableBitSet b = createBitSet(1, 1, 0, 1, 0);
		SerializableBitSet c = createBitSet(1, 1, 1, 1, 0);
		SerializableBitSet d = createBitSet(1, 0, 1, 1, 0);
		SerializableBitSet e = createBitSet(0, 1, 0, 1, 1);

		addStrategy.addDifferenceSet(a);
		addStrategy.addDifferenceSet(b);
		addStrategy.addDifferenceSet(c);
		addStrategy.addDifferenceSet(d);
		addStrategy.addDifferenceSet(e);

		List<SerializableBitSet> expectedList = new ArrayList<>();
		expectedList.add(a);
		expectedList.add(b);
		expectedList.add(c);
		expectedList.add(d);
		expectedList.add(e);

		Assert.assertEqualsNoOrder(iterableToArray(addStrategy.getIterable()).toArray(), expectedList.toArray());
	}
}
