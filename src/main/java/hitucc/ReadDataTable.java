package hitucc;

import com.opencsv.CSVParser;
import com.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadDataTable {
	public static String[][] readTable(String path, char delimiter) throws IOException {
		return readTable(path, delimiter, -1);
	}

	public static String[][] readTable(String path, char delimiter, int columns) throws IOException {
		return readTable(path, delimiter, false, CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.DEFAULT_ESCAPE_CHARACTER);
	}

	public static String[][] readTable(String path, char delimiter, boolean hasHeader, char quoteCharacter, char escapeCharacter) throws IOException {
		return readTable(path, delimiter, hasHeader, quoteCharacter, escapeCharacter, -1);
	}

	public static String[][] readTable(String path, char delimiter, boolean hasHeader, char quoteCharacter, char escapeCharacter, int columns) throws IOException {
		List<String[]> records = new ArrayList<>();
		FileReader reader = new FileReader(path);
		try (CSVReader csvReader = new CSVReader(reader, delimiter, quoteCharacter, escapeCharacter, hasHeader ? 1 : 0)) {
//			if (hasHeader) {
//				csvReader.readNext();
//			}

			String[] values;
			while ((values = csvReader.readNext()) != null) {
				if(records.size() > 0 && records.get(0).length != values.length) {
					System.out.println("Skip Line!");
					continue;
				}

				records.add(values);
			}
		}

		return records.toArray(new String[records.size()][columns <= -1 ? records.get(0).length : Math.min(columns, records.get(0).length)]);
	}
}
