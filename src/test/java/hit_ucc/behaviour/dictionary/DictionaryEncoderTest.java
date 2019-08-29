package hit_ucc.behaviour.dictionary;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DictionaryEncoderTest {

	private String[] columnData;
	private int size;
	private DictionaryEncoder dictionaryEncoder;

	@BeforeMethod
	public void beforeMethod() {
		columnData = new String[]{"A", "B", "A", "C", "B"};
		size = 5;
		dictionaryEncoder = new DictionaryEncoder(size);

		for (String value : columnData) {
			dictionaryEncoder.addValue(value);
		}
	}

	@Test
	public void testConstructor() {
		Assert.assertEquals(dictionaryEncoder.rawData.length, size);
		Assert.assertEquals(dictionaryEncoder.dictionary.size(), 0);
	}

	@Test
	public void testAddValue() {
		Assert.assertEquals(dictionaryEncoder.rawData.length, size);
		Assert.assertEquals(dictionaryEncoder.dictionary.size(), 0);

		for (int i = 0; i < columnData.length; i++) {
			Assert.assertEquals(dictionaryEncoder.rawData[i], columnData[i]);
		}
	}

	@Test
	public void testCreateDictionary() {
		Assert.assertEquals(dictionaryEncoder.dictionary.size(), 0);

		dictionaryEncoder.createDictionary();

		Assert.assertEquals(dictionaryEncoder.dictionary.size(), 3);
		Assert.assertEqualsNoOrder(dictionaryEncoder.dictionary.toArray(), new String[]{"A", "C", "B"});
		Assert.assertEquals(dictionaryEncoder.dictionary.toArray(), new String[]{"A", "B", "C"});
	}

	@Test
	public void testEncode() {
		IColumn column = dictionaryEncoder.getColumn();
		Assert.assertEquals(column.getValue(0), column.getValue(2));
		Assert.assertEquals(column.getValue(1), column.getValue(4));
		Assert.assertNotEquals(column.getValue(0), column.getValue(1));
		Assert.assertNotEquals(column.getValue(0), column.getValue(3));
		Assert.assertNotEquals(column.getValue(1), column.getValue(2));
	}
}
