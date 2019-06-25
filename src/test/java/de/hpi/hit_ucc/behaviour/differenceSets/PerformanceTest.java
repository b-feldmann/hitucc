package de.hpi.hit_ucc.behaviour.differenceSets;

import de.hpi.hit_ucc.ReadDataTable;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.BitSet;
import java.util.Random;

public class PerformanceTest {
	final int testTries = 3;
	String[][] table;
	String[][] warmupTable;

	@BeforeClass(groups = {"performance"})
	private void setupTable() {
		try {
//			table = ReadDataTable.readTable("bridges.csv", ',');
//			table = ReadDataTable.readTable("nursery.csv", ',');
				table = ReadDataTable.readTable("chess.csv", ',');
//				table = ReadDataTable.readTable("ncvoter_Statewide.10000r.csv", ',', true);

			warmupTable = new String[50][];
			int columns = table[0].length;
			Random random = new Random(0);
			for (int i = 0; i < warmupTable.length; i++) {
				warmupTable[i] = new String[columns];
				for (int k = 0; k < columns; k++) {
					warmupTable[i][k] = String.valueOf(random.nextInt(20));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void addTable(DifferenceSetDetector differenceSetDetector, String[][] table) {
		for (int i = 0; i < table.length; i++) {
			for (int k = i + 1; k < table.length; k++) {
				differenceSetDetector.addDifferenceSet(table[i], table[k]);
			}
		}
	}

	private void warmUp(DifferenceSetDetector differenceSetDetector) {
		addTable(differenceSetDetector, warmupTable);
		BitSet[] minimal = differenceSetDetector.getMinimalDifferenceSets();
		BitSet[] merged = differenceSetDetector.mergeMinimalDifferenceSets(minimal);
		differenceSetDetector.clearState();
	}

	private void testDifferenceSetDetector(String name, DifferenceSetDetector differenceSetDetector) {
		warmUp(differenceSetDetector);

		long sumAddTime = 0;
		long sumMinimalTime = 0;
		long sumMergeTime = 0;

		for (int i = 0; i < testTries; i++) {
			long startTime = System.currentTimeMillis();

			addTable(differenceSetDetector, table);
			long addTime = System.currentTimeMillis();

			BitSet[] minimal = differenceSetDetector.getMinimalDifferenceSets();
			long getMinimalTime = System.currentTimeMillis();

			BitSet[] merged = differenceSetDetector.mergeMinimalDifferenceSets(minimal);
			long mergeTime = System.currentTimeMillis();

			sumAddTime += addTime - startTime;
			sumMinimalTime += getMinimalTime - addTime;
			sumMergeTime += mergeTime - getMinimalTime;

			Reporter.log(
					String.format("Performance Test %s [Add: %dms, GetMinimal: %dms, Merge: %dms] in %dms (%d/%d)",
							name,
							addTime - startTime, getMinimalTime - addTime, mergeTime - getMinimalTime,
							mergeTime - startTime,
							i + 1, testTries
					),
					true);

			differenceSetDetector.clearState();
		}

		Reporter.log(
				String.format("Performance Test %s [Add: %dms, GetMinimal: %dms, Merge: %dms] in %dms (%d tries)",
						name,
						sumAddTime / testTries,
						sumMinimalTime / testTries,
						sumMergeTime / testTries,
						(sumMergeTime + sumMinimalTime + sumAddTime) / testTries,
						testTries
				),
				true);
	}

	@Test(groups = {"performance"})
	private void testHashNaiveOneSidedDifferenceSetDetector() {
		testDifferenceSetDetector("Hash-Naive-TwoSided", new DifferenceSetDetector(
				new HashAddDifferenceSetStrategy(),
				new NaiveCalculateMinimalSetsStrategy(),
				new TwoSidedMergeMinimalSetsStrategy()));
	}

	@Test(groups = {"performance"})
	private void testHashSortingOneSidedDifferenceSetDetector() {
		testDifferenceSetDetector("Hash-Sorting-TwoSided", new DifferenceSetDetector(
				new HashAddDifferenceSetStrategy(),
				new SortingCalculateMinimalSetsStrategy(),
				new TwoSidedMergeMinimalSetsStrategy()));
	}

	@Test(groups = {"performance"})
	private void testHashBucketingOneSidedDifferenceSetDetector() {
		testDifferenceSetDetector("Hash-Bucketing-TwoSided", new DifferenceSetDetector(
				new HashAddDifferenceSetStrategy(),
				new BucketingCalculateMinimalSetsStrategy(table[0].length),
				new TwoSidedMergeMinimalSetsStrategy()));
	}

	@Test(groups = {"performance"})
	private void testTrieBucketingTwoSidedDifferenceSetDetector() {
		testDifferenceSetDetector("Trie-Bucketing-TwoSided", new DifferenceSetDetector(
				new TrieAddDifferenceSetStrategy(table[0].length),
				new BucketingCalculateMinimalSetsStrategy(table[0].length),
				new TwoSidedMergeMinimalSetsStrategy()));
	}

	@Test(groups = {"performance"}, enabled = false)
	private void testJustBucketingTwoSidedDifferenceSetDetector() {
		testDifferenceSetDetector("Just-Bucketing-TwoSided", new DifferenceSetDetector(
				new JustAddDifferenceSetStrategy(),
				new BucketingCalculateMinimalSetsStrategy(table[0].length),
				new TwoSidedMergeMinimalSetsStrategy()));
	}
}
