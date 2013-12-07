package org.kernelab.basis;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;

/**
 * This is a framework to read a data file line by line.
 * 
 * @author Dilly King
 */
public abstract class DataReader extends AbstractAccomplishable implements Runnable, Callable<Integer>
{
	private static final char	CR				= '\r';

	private static final char	LF				= '\n';

	public static Charset		DEFAULT_CHARSET	= Charset.defaultCharset();

	private Charset				charset			= DEFAULT_CHARSET;

	private boolean				bommed;

	protected Reader			reader;

	private boolean				reading;

	private int					lines;

	private StringBuilder		buffer;

	public DataReader()
	{
		super();
		reading = false;
	}

	public Integer call() throws Exception
	{
		if (!this.isReading())
		{
			this.resetAccomplishStatus();
			this.read();
			this.accomplished();
		}
		return lines;
	}

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

	public int getLines()
	{
		return lines;
	}

	public Reader getReader()
	{
		return reader;
	}

	public boolean isBommed()
	{
		return bommed;
	}

	public boolean isReading()
	{
		return reading;
	}

	public <E extends DataReader> E read()
	{
		if (this.reader != null)
		{
			reading = true;

			lines = 0;

			BufferedReader reader = new BufferedReader(this.reader);

			this.readPrepare();

			if (buffer == null)
			{
				buffer = new StringBuilder();
			}
			else
			{
				buffer.delete(0, buffer.length());
			}

			try
			{
				int i = -1;
				char c, l = 0;

				do
				{
					i = reader.read();

					c = (char) i;

					if (c != LF && c != CR && l != CR && i != -1)
					{
						buffer.append(c);
					}

					if (c == LF || l == CR || i == -1)
					{
						this.readLine(buffer);
						buffer.delete(0, buffer.length());
						lines++;
					}

					if (c != LF && c != CR && l == CR)
					{
						buffer.append(c);
					}

					l = c;

				} while (i != -1 && reading);

				// If the reading process was not terminated by
				// setReading(false), isReading() returns true here.
				this.readFinished();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				finally
				{
					reading = false;
				}
			}
		}

		return Tools.cast(this);
	}

	/**
	 * This method will be called after all lines have been read.<br />
	 * Attention that If the reading process was not terminated by
	 * setReading(false), here isReading() returns true.
	 */
	protected abstract void readFinished();

	/**
	 * This method will be called while reading each line.
	 * 
	 * @param line
	 *            One line in data file.
	 */
	protected abstract void readLine(CharSequence line);

	/**
	 * This method will be called before starting the reading process.
	 */
	protected abstract void readPrepare();

	public void run()
	{
		try
		{
			call();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private <E extends DataReader> E setBommed(boolean bommed)
	{
		this.bommed = bommed;
		return Tools.cast(this);
	}

	protected <E extends DataReader> E setBuffer(StringBuilder buffer)
	{
		this.buffer = buffer;
		return Tools.cast(this);
	}

	public <E extends DataReader> E setCharset(Charset charset)
	{
		this.charset = charset == null ? DEFAULT_CHARSET : charset;
		return Tools.cast(this);
	}

	public <E extends DataReader> E setCharsetName(String charsetName)
	{
		return this.setCharset(charsetName == null ? DEFAULT_CHARSET : Charset.forName(charsetName));
	}

	public <E extends DataReader> E setDataFile(File file) throws IOException
	{
		return this.setDataFile(file, charset);
	}

	public <E extends DataReader> E setDataFile(File file, Charset charset) throws IOException
	{
		return this.setInputStream(new FileInputStream(file), charset);
	}

	public <E extends DataReader> E setDataFile(File file, String charsetName) throws IOException
	{
		return this.setInputStream(new FileInputStream(file), charsetName);
	}

	public <E extends DataReader> E setInputStream(InputStream is) throws IOException
	{
		return this.setInputStream(is, charset);
	}

	public <E extends DataReader> E setInputStream(InputStream is, Charset charset) throws IOException
	{
		ByteOrderMarkScanner scanner = new ByteOrderMarkScanner().scan(is, charset);
		return this.setBommed(scanner.isBommed()).setCharset(scanner.getCharset()).setReader(scanner.getReader());
	}

	public <E extends DataReader> E setInputStream(InputStream is, String charsetName) throws IOException
	{
		return this.setInputStream(is, Charset.forName(charsetName));
	}

	/**
	 * To set the Reader.<br />
	 * Attention that if the Reader is reading, this will take no effect.
	 * 
	 * @param reader
	 *            Reader
	 * @return This DataReader object.
	 */
	public <E extends DataReader> E setReader(Reader reader)
	{
		if (!this.isReading())
		{
			this.reader = reader;
		}
		return Tools.cast(this);
	}

	protected <E extends DataReader> E setReading(boolean reading)
	{
		this.reading = reading;
		return Tools.cast(this);
	}
}
