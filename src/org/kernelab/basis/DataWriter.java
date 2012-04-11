package org.kernelab.basis;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * The class to output strings line by line.
 * 
 * @author Dilly King
 */
public class DataWriter extends AbstractAccomplishable implements Runnable
{
	protected class DataOutputStream extends OutputStream
	{
		private byte[]	buffer	= new byte[1];

		@Override
		public void write(int b) throws IOException
		{
			buffer[0] = (byte) b;
			print(new String(buffer));
		}

	}

	public static String	DEFAULT_CHARSET_NAME	= Charset.defaultCharset().name();

	private String			charSetName				= DEFAULT_CHARSET_NAME;

	protected PrintWriter	writer;

	private boolean			autoFlush;

	private boolean			writing;

	private OutputStream	outputStream;

	public DataWriter()
	{
		super();
		autoFlush = true;
		writing = false;
	}

	/**
	 * Close the writer and setting the writing status to false. This method
	 * will also flush the buffer before the writer being closed.
	 */
	public void close()
	{
		if (writing && writer != null) {
			writer.close();
			writing = false;
		}
	}

	@Override
	public ActionEvent getAccomplishedEvent()
	{
		return null;
	}

	public String getCharSetName()
	{
		return charSetName;
	}

	public OutputStream getOutputStream()
	{
		if (outputStream == null) {
			outputStream = new DataOutputStream();
		}
		return outputStream;
	}

	public PrintWriter getWriter()
	{
		return writer;
	}

	public boolean isAutoFlush()
	{
		return autoFlush;
	}

	public boolean isWriting()
	{
		return writing;
	}

	/**
	 * Print CharSequence data into file but doesn't terminate the line.
	 * 
	 * @param sequence
	 *            The CharSequence of data.
	 */
	public void print(CharSequence sequence)
	{
		if (writing) {
			int length = sequence.length();
			for (int i = 0; i < length; i++) {
				writer.print(sequence.charAt(i));
			}
		}
	}

	public void print(Object object)
	{
		if (writing) {
			writer.print(object);
		}
	}

	public void run()
	{
		this.resetAccomplishStatus();

		this.write();

		this.close();

		this.accomplished();
	}

	/**
	 * To point out whether the writer will flush the buffer automatically or
	 * not.<br />
	 * Calling this method while writing will take no effect.<br />
	 * <b>Attention</b> that this method will and only take effect before
	 * calling setWriter(Writer). Since setDataFile(File) will call
	 * setWriter(Writer) implicitly, this method will take effect only before
	 * calling setDataFile(File).<br />
	 * Directly calling setWriter(Writer,true/false) is equally.
	 * 
	 * @param autoFlush
	 *            If true, the writer will flush the buffer automatically.
	 * @return This DataWriter object.
	 * @see DataWriter#setWriter(Writer, boolean)
	 */
	public DataWriter setAutoFlush(boolean autoFlush)
	{
		if (!writing) {
			this.autoFlush = autoFlush;
		}
		return this;
	}

	public DataWriter setCharSetName(String charSetName)
	{
		if (Charset.isSupported(charSetName)) {
			this.charSetName = charSetName;
		}
		return this;
	}

	/**
	 * Set the file to be output with the data. Calling this method is equal to
	 * setDataFile(file,false) which means the writer would override the content
	 * already existed in the file.
	 * 
	 * @param file
	 *            The file to be output.
	 * @return This DataWriter object.
	 * @see DataWriter#setDataFile(File, boolean)
	 * @throws FileNotFoundException
	 */
	public DataWriter setDataFile(File file) throws FileNotFoundException
	{
		return this.setDataFile(file, false);
	}

	public DataWriter setDataFile(File file, boolean append) throws FileNotFoundException
	{
		try {
			this.setDataFile(file, append, charSetName);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return this;
	}

	public DataWriter setDataFile(File file, boolean append, String charSetName) throws UnsupportedEncodingException,
			FileNotFoundException
	{
		return this.setOutputStream(new FileOutputStream(file, append), charSetName);
	}

	public DataWriter setDataFile(File file, String charSetName) throws UnsupportedEncodingException,
			FileNotFoundException
	{
		return this.setDataFile(file, false, charSetName);
	}

	public DataWriter setOutputStream(OutputStream os)
	{
		try {
			this.setOutputStream(os, charSetName);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return this;
	}

	public DataWriter setOutputStream(OutputStream os, String charSetName) throws UnsupportedEncodingException
	{
		this.setWriter(new OutputStreamWriter(os, charSetName));
		return this;
	}

	/**
	 * To set the Writer.<br />
	 * Attention that if the writer is writing, this will take no effect.
	 * 
	 * @param writer
	 *            Writer
	 * @return This DataWriter object.
	 */
	public DataWriter setWriter(Writer writer)
	{
		return this.setWriter(writer, autoFlush);
	}

	/**
	 * To set the Writer.<br />
	 * The writing status will be true after calling this method.<br />
	 * Attention that if the writer is writing, this will take no effect.
	 * 
	 * @param writer
	 *            Writer
	 * @param autoFlush
	 *            if false, writer.flush() function will be called when close()
	 *            the writer.
	 * @return This DataWriter object.
	 */
	public DataWriter setWriter(Writer writer, boolean autoFlush)
	{
		if (!writing) {
			this.writer = new PrintWriter(writer, autoFlush);
			writing = true;
			this.resetAccomplishStatus();
		}
		return this;
	}

	protected void setWriting(boolean writing)
	{
		this.writing = writing;
	}

	/**
	 * To writer an empty line into the file.<br />
	 * Any subclass may override this method since this method would be called
	 * in run() and this object might be used as a Runnable object.
	 * 
	 * @see DataWriter#run()
	 */
	public void write()
	{
		if (writing) {
			writer.println();
		}
	}

	/**
	 * Write the line into file and terminates the line.
	 * 
	 * @param line
	 *            A line of data.
	 */
	public void write(CharSequence line)
	{
		if (writing) {
			int length = line.length();
			for (int i = 0; i < length; i++) {
				writer.print(line.charAt(i));
			}
			writer.println();
		}
	}

	public void write(Object object)
	{
		if (writing) {
			writer.println(object);
		}
	}
}
