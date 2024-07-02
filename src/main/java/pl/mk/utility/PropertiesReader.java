package pl.mk.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PropertiesReader {

	public Properties initialize(final String propertiesFilepath) throws Exception {
		final File propertiesFile = new File(propertiesFilepath);
		if (!propertiesFile.canRead())
			throw new Exception("Properties file cannot be read. filepath: " + propertiesFilepath);
		return readPropertiesFile(propertiesFile);
	}

	private Properties readPropertiesFile(final File propertiesFile) throws Exception {
		Properties result = new Properties();
		result.load(preparePropertyFile(propertiesFile));
		return result;

	}

	private static Reader preparePropertyFile(File file) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			StringBuilder result = new StringBuilder();

			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line.trim().replace("\\", "\\\\"));
				result.append('\n');
			}
			return new StringReader(result.toString());
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * Gets property from file which can have multiple values - they must be
	 * splitted by "," (comma) character.
	 *
	 * @param multiPropertyName
	 * @return
	 */
	public static List<String> getMultiProperty(Properties properties, String multiPropertyName) {
		final String serverFilesNamesProperty = properties.getProperty(multiPropertyName);
		final String[] splitted = serverFilesNamesProperty.split(",");
		final List<String> result = new ArrayList<>(splitted.length);
		for (final String s : splitted) {
			if ((s != null) && !s.isEmpty())
				result.add(s);
		}
		return result;
	}
}
