package hit_ucc.behaviour.dictionary;

import java.util.Collections;

public class BitCompressedDictionaryEncoder extends DictionaryEncoder {
	public BitCompressedDictionaryEncoder(int size) {
		super(size);
	}

	@Override
	protected void encode() {
		createDictionary();

		column = new BitEncodedColumn(rawData.length, dictionary.size());

		for(int i = 0;  i < rawData.length; i++) {
			int position = Collections.binarySearch(dictionary, rawData[i]);
			column.setValue(i, position);
		}
	}
}
