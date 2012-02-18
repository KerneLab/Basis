package org.kernelab.basis.gui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Filter files that has the same prefix.
 * 
 * <pre>
 * Example:
 * &quot;File.txt&quot;
 * &quot;File.dat&quot;
 * &quot;file.conf&quot;
 * 
 * filter by &quot;File&quot;
 * returns
 * 
 * &quot;File.txt&quot;
 * &quot;File.dat&quot;
 * </pre>
 * 
 * @author Dilly King
 * 
 */
public class FilePrefixNamesFilter implements FilenameFilter
{

	public static final String getFilenameBeforeSuffix(File file)
	{
		return getFilenameBeforeSuffix(file.getName());
	}

	public static final String getFilenameBeforeSuffix(String filename)
	{
		int index = filename.lastIndexOf('.');

		if (index == -1) {
			index = filename.length();
		}

		return filename.substring(0, index);
	}

	public static final Set<File> getFilesOfSamePrefixAround(File file)
	{
		Set<File> files = new LinkedHashSet<File>();

		FilePrefixNamesFilter filter = new FilePrefixNamesFilter(
				getFilenameBeforeSuffix(file));

		File dir = file.getParentFile();

		for (File f : dir.listFiles(filter)) {
			files.add(f);
		}

		return files;
	}

	private Set<String>	filePrefixNames;

	public FilePrefixNamesFilter(String... names)
	{
		filePrefixNames = new HashSet<String>();

		for (String name : names) {
			filePrefixNames.add(name);
		}
	}

	public boolean accept(File dir, String name)
	{
		return filePrefixNames.contains(getFilenameBeforeSuffix(name));
	}

	public Set<String> getFilePrefixNames()
	{
		return filePrefixNames;
	}

}
