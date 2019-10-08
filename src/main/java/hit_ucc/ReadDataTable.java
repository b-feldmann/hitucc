package hit_ucc;

import com.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadDataTable {
	public static String[][] readTable(String path, char delimiter) throws IOException {
		return readTable(path, delimiter, false);
	}

	public static String[][] readTable(String path, char delimiter, boolean hasHeader) throws IOException {
		List<String[]> records = new ArrayList<>();
		FileReader reader = new FileReader(path);
		try (CSVReader csvReader = new CSVReader(reader, delimiter)) {
			if (hasHeader) {
				csvReader.readNext();
			}

			String[] values = null;
			while ((values = csvReader.readNext()) != null) {
				if(records.size() > 0 && records.get(0).length != values.length) {
					System.out.println("Skip Line!");
					continue;
				}

				records.add(values);
			}
		}

		return records.toArray(new String[records.size()][records.get(0).length]);
	}
}
