package org.kernelab.basis.io;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;

import org.kernelab.basis.Tools;
import org.kernelab.basis.io.ReaderFactory.DefaultReaderFactory;
import org.kernelab.basis.io.ReaderFactory.FileReaderFactory;

public class TextDataSource implements Iterable<String>
{
	protected class TextDataSourceIterator implements Iterator<String>
	{
		private Reader			reader;

		private char[]			lineTerm;

		private char[]			termBuff;

		private StringBuilder	buffer;

		private String			line;

		public TextDataSourceIterator() throws IOException
		{
			this.reader = getFactory().getReader();
			this.lineTerm = getLineSeparator().toCharArray();
			this.termBuff = new char[getLineSeparator().length()];
			this.buffer = new StringBuilder();
			this.line = readLine();
		}

		protected void close()
		{
			try
			{
				this.reader.close();
			}
			catch (Exception e)
			{
			}
			Tools.clearStringBuilder(this.buffer);
		}

		public boolean hasNext()
		{
			return this.line != null;
		}

		public String next()
		{
			try
			{
				return this.line;
			}
			finally
			{
				this.line = readLine();
			}
		}

		protected String readLine()
		{
			char ch = (char) -1;
			boolean first = true;
			int reads = -1, terms = -1, termCh;

			try
			{
				while ((reads = reader.read()) != -1)
				{
					first = false;

					ch = (char) reads;

					if (ch == lineTerm[0])
					{
						termBuff[0] = ch;
						terms = 1;

						for (terms = 1; terms < lineTerm.length; terms++)
						{
							termCh = reader.read();
							if (termCh == -1)
							{
								reads = -1;
								break;
							}
							else
							{
								termBuff[terms] = (char) termCh;
							}
						}

						if (terms == lineTerm.length && Arrays.equals(termBuff, lineTerm))
						{
							break;
						}
						else
						{
							buffer.append(termBuff, 0, terms);
						}
					}
					else
					{
						buffer.append(ch);
					}
				}
			}
			catch (IOException e)
			{
				reads = -1;
			}

			if (reads == -1)
			{
				try
				{
					if (first)
					{
						return null;
					}
					else
					{
						return buffer.toString();
					}
				}
				finally
				{
					this.close();
				}
			}
			else
			{
				try
				{
					return buffer.toString();
				}
				finally
				{
					Tools.clearStringBuilder(buffer);
				}
			}
		}
	}

	private ReaderFactory	factory;

	private String			lineSeparator;

	public TextDataSource(File file, Charset charset, String lineSeparator)
	{
		this(new FileReaderFactory(file, charset), lineSeparator);
	}

	public TextDataSource(Reader reader, String lineSeparator)
	{
		this(new DefaultReaderFactory(reader), lineSeparator);
	}

	public TextDataSource(ReaderFactory factory, String lineSeparator)
	{
		this.setFactory(factory);
		this.setLineSeparator(lineSeparator);
	}

	public ReaderFactory getFactory()
	{
		return factory;
	}

	public String getLineSeparator()
	{
		return lineSeparator;
	}

	public Iterator<String> iterator()
	{
		try
		{
			return new TextDataSourceIterator();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	protected void setFactory(ReaderFactory factory)
	{
		this.factory = factory;
	}

	protected void setLineSeparator(String lineSeparator)
	{
		this.lineSeparator = lineSeparator;
	}
}
