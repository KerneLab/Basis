package org.kernelab.basis;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * This is a framework to read a data file line by line.
 * 
 * @author Dilly King
 */
public abstract class DataReader extends AbstractAccomplishable implements Runnable
{
	private static final char	LF						= '\n';

	private static final char	CR						= '\r';

	public static String		DEFAULT_CHARSET_NAME	= Charset.defaultCharset().name();

	private String				charSetName				= DEFAULT_CHARSET_NAME;

	protected Reader			reader;

	private boolean				reading;

	private StringBuilder		buffer;

	public DataReader()
	{
		super();
		reading = false;
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

	public Reader getReader()
	{
		return reader;
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
		if (!this.isReading())
		{
			this.resetAccomplishStatus();
			this.read();
			this.accomplished();
		}
	}

	protected <E extends DataReader> E setBuffer(StringBuilder buffer)
	{
		this.buffer = buffer;
		return Tools.cast(this);
	}

	public <E extends DataReader> E setCharSetName(String charSetName)
	{
		if (Charset.isSupported(charSetName))
		{
			this.charSetName = charSetName;
		}
		return Tools.cast(this);
	}

	public <E extends DataReader> E setDataFile(File file) throws FileNotFoundException
	{
		try
		{
			this.setDataFile(file, charSetName);
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		return Tools.cast(this);
	}

	public <E extends DataReader> E setDataFile(File file, String charSetName) throws UnsupportedEncodingException,
			FileNotFoundException
	{
		return this.setInputStream(new FileInputStream(file), charSetName);
	}

	public <E extends DataReader> E setInputStream(InputStream is)
	{
		try
		{
			this.setInputStream(is, charSetName);
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		return Tools.cast(this);
	}

	public <E extends DataReader> E setInputStream(InputStream is, String charSetName)
			throws UnsupportedEncodingException
	{
		return this.setReader(new InputStreamReader(is, charSetName));
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
		if (!reading)
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
