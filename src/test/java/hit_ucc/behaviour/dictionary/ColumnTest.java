package hit_ucc.behaviour.dictionary;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ColumnTest {
	@Test
	public void testSetAndGetValues() {
		int[] columnData = new int[]{0, 1, 0, 2, 1};

		IColumn column = new Column(columnData.length);
		for (int i = 0; i < columnData.length; i++) {
			column.setValue(i, columnData[i]);
		}

		for (int i = 0; i < columnData.length; i++) {
			Assert.assertEquals(column.getValue(i), columnData[i]);
		}
	}

	@Test
	public void testSize() {
		int[] columnData = new int[]{0, 1, 0, 2, 1};

		IColumn column = new Column(columnData.length);
		for (int i = 0; i < columnData.length; i++) {
			column.setValue(i, columnData[i]);
		}

		Assert.assertEquals(column.size(), columnData.length);
	}
}
