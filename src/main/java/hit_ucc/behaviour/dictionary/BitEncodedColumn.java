package hit_ucc.behaviour.dictionary;

public class BitEncodedColumn implements IColumn {

	private int size;
	private int bitsPerValue;

	private int[] data;

	public BitEncodedColumn(int size, int dictionarySize) {
		this.bitsPerValue = dictionarySize;
		this.size = size;
		this.data = new int[((size * bitsPerValue) / 32) + 1];
	}

	@Override
	public int getValue(int index) {
		int i = (index * bitsPerValue) / 32;
		int offset = (index * bitsPerValue) % 32;

		int value = 0;
		value = (data[i] >> offset) & ((1 << bitsPerValue) - 1);
		value |= (data[i + 1] << (32 - offset)) & ((1 << bitsPerValue) - 1);
		return value;

	}

	@Override
	public void setValue(int index, int value) {
		int i = (index * bitsPerValue) / (32);
		int offset = (i * bitsPerValue) % (32);

		data[i] |= (value << offset);
		data[i + 1] |= (value >> (32 - offset));
	}

	@Override
	public int size() {
		return size;
	}
}
