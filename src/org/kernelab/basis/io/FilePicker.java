package org.kernelab.basis.io;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.kernelab.basis.TextFiller;
import org.kernelab.basis.Tools;

public class FilePicker
{
	public static String	TARGET_PATH_KEY						= "target";

	public static String	TARGET_FILE_EXIST_TITLE				= "目标文件已存在";

	public static String	TARGET_FILE_EXIST_HINT				= "目标文件\n?target?\n已存在，是否覆盖？";

	public static String	TARGET_DIRECTORY_NOT_EXIST_TITLE	= "目标目录不存在";

	public static String	TARGET_DIRECTORY_NOT_EXIST_HINT		= "目标目录\n?target?\n不存在，是否创建？";

	/**
	 * @param args
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException
	{
		File file = new FilePicker().pickInputDirectory("");
		Tools.debug(file == null);
		if (file != null)
		{
			Tools.debug(file);
		}
	}

	private Component		parent							= null;

	private File			location						= new File(".");

	private JFileChooser	chooser							= new JFileChooser(location);

	private String			targetPathKey					= TARGET_PATH_KEY;

	private String			targetFileExistTitle			= TARGET_FILE_EXIST_TITLE;

	private String			targetFileExistHint				= TARGET_FILE_EXIST_HINT;

	private String			targetDirectoryNotExistTitle	= TARGET_DIRECTORY_NOT_EXIST_TITLE;

	private String			targetDirectoryNotExistHint		= TARGET_DIRECTORY_NOT_EXIST_HINT;

	public JFileChooser getChooser()
	{
		return chooser;
	}

	public File getLocation()
	{
		return location;
	}

	public Component getParent()
	{
		return parent;
	}

	public String getTargetDirectoryNotExistHint()
	{
		return targetDirectoryNotExistHint;
	}

	public String getTargetDirectoryNotExistTitle()
	{
		return targetDirectoryNotExistTitle;
	}

	public String getTargetFileExistHint()
	{
		return targetFileExistHint;
	}

	public String getTargetFileExistTitle()
	{
		return targetFileExistTitle;
	}

	public String getTargetPathKey()
	{
		return targetPathKey;
	}

	public File pickInputDirectory(String title)
	{
		return pickInputDirectory(title, parent);
	}

	public File pickInputDirectory(String title, Component parent)
	{
		File file = null;

		chooser.setCurrentDirectory(location);
		chooser.setDialogTitle(title);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);

		if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			this.setLocation(chooser.getCurrentDirectory());

			file = chooser.getSelectedFile();
		}

		return file;
	}

	public File pickInputFile(String title)
	{
		return pickInputFile(title, parent);
	}

	public File pickInputFile(String title, Component parent)
	{
		File file = null;

		chooser.setCurrentDirectory(location);
		chooser.setDialogTitle(title);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(false);

		if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			this.setLocation(chooser.getCurrentDirectory());

			file = chooser.getSelectedFile();
		}

		return file;
	}

	public File[] pickInputFiles(String title)
	{
		return pickInputFiles(title, parent);
	}

	public File[] pickInputFiles(String title, Component parent)
	{
		File[] files = null;

		chooser.setCurrentDirectory(location);
		chooser.setDialogTitle(title);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(true);

		if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			this.setLocation(chooser.getCurrentDirectory());

			files = chooser.getSelectedFiles();
		}

		return files;
	}

	public File pickOutputDirectory(String title, Component parent)
	{
		return pickOutputDirectory(title, targetDirectoryNotExistTitle, targetDirectoryNotExistHint, parent);
	}

	public File pickOutputDirectory(String title, String makeDirectoryTitle, String makeDirectoryHint)
	{
		return pickOutputDirectory(title, makeDirectoryTitle, makeDirectoryHint, parent);
	}

	public File pickOutputDirectory(String title, String makeDirectoryTitle, String makeDirectoryHint, Component parent)
	{
		File file = null;

		chooser.setCurrentDirectory(location);
		chooser.setDialogTitle(title);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);

		w: while (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			this.setLocation(chooser.getCurrentDirectory());

			file = chooser.getSelectedFile();

			if (!file.exists())
			{
				makeDirectoryHint = new TextFiller(makeDirectoryHint).reset()
						.fillWith(targetPathKey, file.getAbsolutePath()).result().toString();
				s: switch (JOptionPane.showConfirmDialog(parent, makeDirectoryHint, makeDirectoryTitle,
						JOptionPane.YES_NO_CANCEL_OPTION))
				{
					case JOptionPane.YES_OPTION:
						file.mkdirs();
						break w;

					case JOptionPane.NO_OPTION:
						file = null;
						break s;

					case JOptionPane.CANCEL_OPTION:
						file = null;
						break w;
				}
			}
		}

		return file;
	}

	public File pickOutputFile(String title)
	{
		return pickOutputFile(title, targetFileExistTitle, targetFileExistHint, parent);
	}

	public File pickOutputFile(String title, Component parent)
	{
		return pickOutputFile(title, targetFileExistTitle, targetFileExistHint, parent);
	}

	public File pickOutputFile(String title, String overwriteTitle, String overwriteHint)
	{
		return pickOutputFile(title, targetFileExistTitle, targetFileExistHint, parent);
	}

	public File pickOutputFile(String title, String overwriteTitle, String overwriteHint, Component parent)
	{
		File file = null;

		chooser.setCurrentDirectory(location);
		chooser.setDialogTitle(title);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(false);

		w: while (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			this.setLocation(chooser.getCurrentDirectory());

			file = chooser.getSelectedFile();

			if (file.exists())
			{
				overwriteHint = new TextFiller(overwriteHint).reset().fillWith(targetPathKey, file.getAbsolutePath())
						.result().toString();
				s: switch (JOptionPane.showConfirmDialog(parent, overwriteHint, overwriteTitle,
						JOptionPane.YES_NO_CANCEL_OPTION))
				{
					case JOptionPane.YES_OPTION:

						break w;

					case JOptionPane.NO_OPTION:
						file = null;
						break s;

					case JOptionPane.CANCEL_OPTION:
						file = null;
						break w;
				}
			}
			else
			{
				break w;
			}
		}

		return file;
	}

	public <E extends FilePicker> E setChooser(JFileChooser chooser)
	{
		this.chooser = chooser;
		return Tools.cast(this);
	}

	protected <E extends FilePicker> E setLocation(File location)
	{
		this.location = location;
		return Tools.cast(this);
	}

	public <E extends FilePicker> E setParent(Component parent)
	{
		this.parent = parent;
		return Tools.cast(this);
	}

	public <E extends FilePicker> E setTargetDirectoryNotExistHint(String targetDirectoryNotExistHint)
	{
		this.targetDirectoryNotExistHint = targetDirectoryNotExistHint;
		return Tools.cast(this);
	}

	public <E extends FilePicker> E setTargetDirectoryNotExistTitle(String targetDirectoryNotExistTitle)
	{
		this.targetDirectoryNotExistTitle = targetDirectoryNotExistTitle;
		return Tools.cast(this);
	}

	public <E extends FilePicker> E setTargetFileExistHint(String targetFileExistHint)
	{
		this.targetFileExistHint = targetFileExistHint;
		return Tools.cast(this);
	}

	public <E extends FilePicker> E setTargetFileExistTitle(String targetFileExistTitle)
	{
		this.targetFileExistTitle = targetFileExistTitle;
		return Tools.cast(this);
	}

	public <E extends FilePicker> E setTargetPathKey(String targetPathKey)
	{
		this.targetPathKey = targetPathKey;
		return Tools.cast(this);
	}
}
