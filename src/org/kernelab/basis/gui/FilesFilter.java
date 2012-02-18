package org.kernelab.basis.gui;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * The class which can be used as JFileChooser.setFileFilter(FilesFilter).<br />
 * Or to be used as:
 * 
 * <pre>
 * for (FileFilter ff : FilesFilter.getFilters().values()) {
 * 	JFileChooser.addChoosableFileFilter(ff);
 * }
 * </pre>
 * 
 * This is equal to FilesFilter.attachToFileChooser(JFileChooser).
 * 
 * @author Dilly King
 * 
 */
public class FilesFilter extends FileFilter
{

	public static class SingleFilter extends FileFilter
	{

		public String	extension;

		public String	description;

		public SingleFilter(String extension, String description)
		{
			this.extension = extension.toLowerCase();
			this.description = description;
		}

		@Override
		public boolean accept(File f)
		{
			boolean accept = f.isDirectory();

			if (!accept) {
				if (getFileExtension(f).toLowerCase().equals(extension)) {
					accept = true;
				}
			}

			return accept;
		}

		@Override
		public String getDescription()
		{
			return (description == null ? "" : description + ' ') + "(*." + extension
					+ ")";
		}

	}

	public static final String getFileExtension(File file)
	{
		return getFileExtension(file.getName());
	}

	public static final String getFileExtension(String fileName)
	{
		return fileName.substring(fileName.lastIndexOf('.') + 1);
	}

	private Map<String, SingleFilter>	filters;

	public FilesFilter()
	{
		this.filters = new LinkedHashMap<String, SingleFilter>();
	}

	public FilesFilter(Collection<String> extensions)
	{
		this();
		this.addExtensions(extensions);
	}

	public FilesFilter(Map<String, String> map)
	{
		this();
		this.addExtensions(map);
	}

	public FilesFilter(String extension)
	{
		this(extension, null);
	}

	public FilesFilter(String extension, String description)
	{
		this();
		this.addExtension(extension, description);
	}

	@Override
	public boolean accept(File f)
	{
		boolean accept = f.isDirectory();

		if (!accept) {
			String suffix = getFileExtension(f).toLowerCase();

			for (String extension : filters.keySet()) {
				if (extension.equals(suffix)) {
					accept = true;
					break;
				}
			}
		}

		return accept;
	}

	public SingleFilter addExtension(String extension)
	{
		return this.addExtension(extension, null);
	}

	public SingleFilter addExtension(String extension, String description)
	{
		extension = getFileExtension(extension).toLowerCase();

		SingleFilter filter = new SingleFilter(extension, description);

		this.filters.put(extension, filter);

		return filter;
	}

	public void addExtensions(Collection<String> extensions)
	{
		filters.clear();
		for (String extension : extensions) {
			this.addExtension(extension);
		}
	}

	public void addExtensions(Map<String, String> map)
	{
		map.clear();
		filters.clear();
		for (Entry<String, String> entry : map.entrySet()) {
			this.addExtension(entry.getKey(), entry.getValue());
		}
	}

	public void attachToFileChooser(JFileChooser fc)
	{
		for (SingleFilter sf : this.filters.values()) {
			fc.addChoosableFileFilter(sf);
		}
	}

	@Override
	public String getDescription()
	{
		StringBuilder des = new StringBuilder();
		StringBuilder ext = new StringBuilder();

		boolean firstExt = true;
		for (SingleFilter filter : filters.values()) {

			if (filter.description != null) {
				des.append(filter.description);
				des.append(' ');
			}

			if (firstExt) {
				firstExt = false;
				ext.append('(');
			} else {
				ext.append(';');
			}
			ext.append("*.");
			ext.append(filter.extension);
		}

		ext.append(')');

		des.append(ext);

		return des.toString();
	}

	public Map<String, SingleFilter> getFilters()
	{
		return filters;
	}

}
