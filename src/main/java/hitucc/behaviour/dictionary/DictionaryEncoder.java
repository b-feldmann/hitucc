package hitucc.behaviour.dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DictionaryEncoder {
	protected final List<String> dictionary;

	protected final String[] rawData;
	private int index;
	private boolean dirty;

	protected IColumn column;

	public DictionaryEncoder(int size) {
		rawData = new String[size];
		dictionary = new ArrayList<>();
	}

	public void addValue(String value) {
		rawData[index] = value;
		index += 1;
		dirty = true;
	}

	void createDictionary() {
		dictionary.clear();
		String[] sortedData = Arrays.copyOf(rawData, rawData.length);
		Arrays.sort(sortedData);
		if (sortedData.length == 0) return;

		dictionary.add(sortedData[0]);
		if (sortedData.length == 1) return;
		for (int i = 1; i < sortedData.length; i++) {
			if(!sortedData[i].equals(sortedData[i - 1])) {
				dictionary.add(sortedData[i]);
			}
		}
		Collections.sort(dictionary);
	}

	protected void encode() {
		createDictionary();

		column = new Column(rawData.length);

		for(int i = 0;  i < rawData.length; i++) {
			int position = Collections.binarySearch(dictionary, rawData[i]);
			column.setValue(i, position);
		}
	}

	public List<String> getDictionary() {
		if(dirty) {
			encode();
			dirty = false;
		}
		return dictionary;
	}

	public IColumn getColumn() {
		if(dirty) {
			encode();
			dirty = false;
		}
		return column;
	}
}
