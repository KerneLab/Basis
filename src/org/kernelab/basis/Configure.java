package org.kernelab.basis;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Configure
{
	public static String	SEPARATOR	= "\t";

	public static final Map<String, String> getConfigureMap(File file)
	{
		Map<String, String> map = new LinkedHashMap<String, String>();

		try {
			Scanner scanner = new Scanner(file);
			scanner.useDelimiter(System.getProperty("line.separator"));

			while (scanner.hasNext()) {
				String nextLine = scanner.nextLine();

				Scanner lineScanner = new Scanner(nextLine);
				lineScanner.useDelimiter("\\s*" + SEPARATOR + "\\s*");

				String key = lineScanner.next();
				String value = lineScanner.next();

				map.put(key, value);

				lineScanner.close();
			}

			scanner.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return map;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

}
