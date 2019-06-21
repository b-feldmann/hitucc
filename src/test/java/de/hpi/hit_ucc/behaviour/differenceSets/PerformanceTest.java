package de.hpi.hit_ucc.behaviour.differenceSets;

import de.hpi.hit_ucc.ReadDataTable;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.BitSet;

public class PerformanceTest {
	String[][] table;

	@BeforeClass(groups = { "performance"})
	private void setupTable() {
		try {
//			table = ReadDataTable.readTable("bridges.csv", ',');
//				table = ReadDataTable.readTable("nursery.csv", ',');
				table = ReadDataTable.readTable("chess.csv", ',');
//				table = ReadDataTable.readTable("ncvoter_Statewide.10000r.csv", ',', true);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void addTable(AbstractDifferenceSetDetector differenceSetDetector) {
		for (int i = 0; i < table.length; i++) {
			for (int k = i + 1; k < table.length; k++) {
				differenceSetDetector.addDifferenceSet(table[i], table[k]);
			}
		}
	}

	private void testDifferenceSetDetector(AbstractDifferenceSetDetector differenceSetDetector) {
//		Reporter.log(String.format("Performance Test %s [", differenceSetDetector.getClass().getSimpleName()), true);
		long startTime = System.currentTimeMillis();

		addTable(differenceSetDetector);
		long addTime = System.currentTimeMillis();
//		Reporter.log(String.format("Add: %dms, ", addTime - startTime), true);

		BitSet[] minimal = differenceSetDetector.getMinimalDifferenceSets();
		long getMinimalTime = System.currentTimeMillis();
//		Reporter.log(String.format("GetMinimal: %dms, ", getMinimalTime - addTime), true);

		BitSet[] merged = differenceSetDetector.mergeMinimalDifferenceSets(minimal);
		long mergeTime = System.currentTimeMillis();
//		Reporter.log(String.format("Merge: %dms]", mergeTime - getMinimalTime), true);

		Reporter.log(
				String.format("Performance Test %s [Add: %dms, GetMinimal: %dms, Merge: %dms] in %dms",
						differenceSetDetector.getClass().getSimpleName(),
						addTime - startTime,
						getMinimalTime - addTime,
						mergeTime - getMinimalTime,
						mergeTime - startTime
				),
				true);
	}

	@Test(invocationCount = 10, groups = { "performance"})
	private void testNaiveDifferenceSetDetector() {
		testDifferenceSetDetector(new NaiveDifferenceSetDetector());
	}

	@Test(invocationCount = 10, groups = { "performance"})
	private void testSortingDifferenceSetDetector() {
		testDifferenceSetDetector(new SortingDifferenceSetDetector());
	}

	@Test(invocationCount = 10, groups = { "performance"})
	private void testBucketingDifferenceSetDetector() {
		testDifferenceSetDetector(new BucketingDifferenceSetDetector(table[0].length));
	}
}
