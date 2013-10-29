package org.kernelab.basis;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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

	public static final String	MAC_LINE_SEPARATOR		= "\r";

	public static final String	UNIX_LINE_SEPARATOR		= "\n";

	public static final String	DOS_LINE_SEPARATOR		= "\r\n";

	public static String		DEFAULT_LINE_SEPARATOR	= System.getProperty("line.separator", UNIX_LINE_SEPARATOR);

	public static Charset		DEFAULT_CHARSET			= Charset.defaultCharset();

	private String				lineSeparator			= DEFAULT_LINE_SEPARATOR;

	private Charset				charset					= DEFAULT_CHARSET;

	private OutputStream		outputStream;

	protected PrintWriter		writer;

	private boolean				append;

	private boolean				autoFlush;

	private boolean				writing;

	private boolean				written;

	private boolean				bommed;

	private Thread				autoCloser;

	public DataWriter()
	{
		super();
		autoFlush = true;
		writing = false;
		append = false;
		bommed = false;
	}

	/**
	 * Close the writer and setting the writing status to false. This method
	 * will also flush the buffer before the writer being closed.
	 */
	public void close()
	{
		if (this.isWriting() && this.getWriter() != null)
		{
			if (!this.isWritten() && this.isBommed())
			{
				try
				{
					if ("UTF-7".equals(this.getCharsetName()))
					{
						this.getOutputStream().write(0x2D);
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			this.getWriter().close();
			this.setWriting(false);
			this.setWriter(null);
		}
	}

	@Override
	protected void finalize() throws Throwable
	{
		this.close();
		super.finalize();
	}

	@Override
	public ActionEvent getAccomplishedEvent()
	{
		return null;
	}

	public Charset getCharset()
	{
		return charset;
	}

	public String getCharsetName()
	{
		return charset.name();
	}

	public String getLineSeparator()
	{
		return lineSeparator;
	}

	public OutputStream getOutputStream()
	{
		if (outputStream == null)
		{
			outputStream = new DataOutputStream();
		}
		return outputStream;
	}

	public PrintWriter getWriter()
	{
		return writer;
	}

	public boolean isAppend()
	{
		return append;
	}

	public boolean isAutoClose()
	{
		return autoCloser != null;
	}

	public boolean isAutoFlush()
	{
		return autoFlush;
	}

	public boolean isBommed()
	{
		return bommed;
	}

	public boolean isWriting()
	{
		return writing;
	}

	public boolean isWritten()
	{
		return written;
	}

	/**
	 * Print CharSequence data into file but doesn't terminate the line.
	 * 
	 * @param sequence
	 *            The CharSequence of data.
	 */
	public <E extends DataWriter> E print(CharSequence sequence)
	{
		if (sequence != null && writing)
		{
			writer.print(sequence.toString());
			this.setWritten(true);
		}
		return Tools.cast(this);
	}

	public <E extends DataWriter> E print(Object object)
	{
		if (object != null && writing)
		{
			writer.print(object);
			this.setWritten(true);
		}
		return Tools.cast(this);
	}

	/**
	 * To print BOM bytes according to the given Charset. If the writer was
	 * append or did not ready to write or had already written some content,
	 * then this method would do nothing.
	 * 
	 * @return This DataWriter object.
	 * @throws IOException
	 */
	public <E extends DataWriter> E printBOM()
	{
		if (this.isWriting() && !this.isAppend() && !this.isBommed() && !this.isWritten())
		{
			byte[] bom = ByteOrderMarkScanner.getBOM(charset);

			if (bom != null)
			{
				try
				{
					this.getOutputStream().write(bom);
					this.setBommed(true);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		return Tools.cast(this);
	}

	public void run()
	{
		this.resetAccomplishStatus();

		this.write();

		this.close();

		this.accomplished();
	}

	public <E extends DataWriter> E setAppend(boolean append)
	{
		if (!this.isWriting())
		{
			this.append = append;
		}
		return Tools.cast(this);
	}

	public <E extends DataWriter> E setAutoClose(boolean auto)
	{
		if (autoCloser != null)
		{
			Runtime.getRuntime().removeShutdownHook(autoCloser);
		}

		if (auto)
		{
			autoCloser = new Thread(new Runnable() {

				public void run()
				{
					DataWriter.this.close();
				}
			});

			Runtime.getRuntime().addShutdownHook(autoCloser);
		}
		else
		{
			autoCloser = null;
		}

		return Tools.cast(this);
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
	public <E extends DataWriter> E setAutoFlush(boolean autoFlush)
	{
		if (!this.isWriting())
		{
			this.autoFlush = autoFlush;
		}
		return Tools.cast(this);
	}

	private <E extends DataWriter> E setBommed(boolean bommed)
	{
		this.bommed = bommed;
		return Tools.cast(this);
	}

	public <E extends DataWriter> E setCharset(Charset charset)
	{
		if (!this.isWriting())
		{
			this.charset = charset == null ? DEFAULT_CHARSET : charset;
		}
		return Tools.cast(this);
	}

	public <E extends DataWriter> E setCharsetName(String charsetName)
	{
		return this.setCharset(charsetName == null ? DEFAULT_CHARSET : Charset.forName(charsetName));
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
	public <E extends DataWriter> E setDataFile(File file) throws FileNotFoundException
	{
		return this.setDataFile(file, append);
	}

	public <E extends DataWriter> E setDataFile(File file, boolean append) throws FileNotFoundException
	{
		return this.setDataFile(file, append, charset);
	}

	public <E extends DataWriter> E setDataFile(File file, boolean append, Charset charset)
			throws FileNotFoundException
	{
		return this.setAppend(append).setOutputStream(new FileOutputStream(file, append), charset);
	}

	public <E extends DataWriter> E setDataFile(File file, boolean append, String charsetName)
			throws FileNotFoundException
	{
		return this.setAppend(append).setOutputStream(new FileOutputStream(file, append), charsetName);
	}

	public <E extends DataWriter> E setDataFile(File file, Charset charset) throws FileNotFoundException
	{
		return this.setDataFile(file, append, charset);
	}

	public <E extends DataWriter> E setDataFile(File file, String charsetName) throws FileNotFoundException
	{
		return this.setDataFile(file, append, charsetName);
	}

	public <E extends DataWriter> DataWriter setLineSeparator(String lineSeparator)
	{
		this.lineSeparator = lineSeparator;
		return Tools.cast(this);
	}

	public <E extends DataWriter> E setOutputStream(OutputStream os)
	{
		return this.setOutputStream(os, charset);
	}

	public <E extends DataWriter> E setOutputStream(OutputStream os, Charset charset)
	{
		this.outputStream = os;
		return this.setCharset(charset).setWriter(new OutputStreamWriter(os, charset));
	}

	public <E extends DataWriter> E setOutputStream(OutputStream os, String charsetName)
	{
		return this.setOutputStream(os, Charset.forName(charsetName));
	}

	/**
	 * To set the Writer.<br />
	 * Attention that if the writer is writing, this will take no effect.
	 * 
	 * @param writer
	 *            Writer
	 * @return This DataWriter object.
	 */
	public <E extends DataWriter> E setWriter(Writer writer)
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
	public <E extends DataWriter> E setWriter(Writer writer, boolean autoFlush)
	{
		if (!this.isWriting())
		{
			if (writer == null)
			{
				this.writer = null;
			}
			else
			{
				this.writer = new PrintWriter(writer, autoFlush);
				this.setWriting(true);
				this.setWritten(false);
				this.setBommed(false);
				this.resetAccomplishStatus();
			}
		}
		return Tools.cast(this);
	}

	protected <E extends DataWriter> E setWriting(boolean writing)
	{
		this.writing = writing;
		return Tools.cast(this);
	}

	protected <E extends DataWriter> E setWritten(boolean written)
	{
		this.written = written;
		return Tools.cast(this);
	}

	/**
	 * To writer an empty line into the file.<br />
	 * Any subclass may override this method since this method would be called
	 * in run() and this object might be used as a Runnable object.
	 * 
	 * @see DataWriter#run()
	 */
	public <E extends DataWriter> E write()
	{
		if (writing)
		{
			writer.print(lineSeparator);
			this.setWritten(true);
		}
		return Tools.cast(this);
	}

	/**
	 * Write the line into file and terminates the line.
	 * 
	 * @param line
	 *            A line of data.
	 */
	public <E extends DataWriter> E write(CharSequence line)
	{
		if (line != null && writing)
		{
			writer.print(line.toString());
			writer.print(lineSeparator);
			this.setWritten(true);
		}
		return Tools.cast(this);
	}

	public <E extends DataWriter> E write(Object object)
	{
		if (object != null && writing)
		{
			writer.print(object);
			writer.print(lineSeparator);
			this.setWritten(true);
		}
		return Tools.cast(this);
	}
}
