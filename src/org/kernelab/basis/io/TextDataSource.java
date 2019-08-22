package org.kernelab.basis.io;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;

import org.kernelab.basis.Tools;
import org.kernelab.basis.io.ReaderFactory.DefaultReaderFactory;
import org.kernelab.basis.io.ReaderFactory.FileReaderFactory;

public class TextDataSource implements Iterable<String>
{
	protected class TextDataSourceIterator implements Iterator<String>
	{
		private Reader					reader;

		private char[]					lineTerm;

		private LinkedList<Character>	termBuff;

		private StringBuilder			buffer;

		private String					line;

		public TextDataSourceIterator() throws IOException
		{
			this.reader = TextDataSource.this.getFactory().getReader();
			this.lineTerm = TextDataSource.this.getLineSeparator().toCharArray();
			this.termBuff = new LinkedList<Character>();
			this.buffer = new StringBuilder();
			this.line = this.readLine();
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
				this.line = this.readLine();
			}
		}

		protected String readLine()
		{
			boolean first = true;
			int reads = -1;

			try
			{
				while (true)
				{
					while (termBuff.size() < lineTerm.length)
					{
						if ((reads = reader.read()) == -1)
						{
							break;
						}
						first = false;
						termBuff.add((char) reads);
					}

					if (lineTerm.length == termBuff.size() && startWith(lineTerm, termBuff))
					{
						termBuff.clear();
						break;
					}
					else
					{
						buffer.append(termBuff.poll());
						while (!termBuff.isEmpty() && !startWith(lineTerm, termBuff))
						{
							buffer.append(termBuff.poll());
						}
					}

					if (reads == -1)
					{
						while (!termBuff.isEmpty())
						{
							buffer.append(termBuff.poll());
						}
						break;
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

		protected boolean startWith(char[] a, LinkedList<Character> b)
		{
			if (a.length < b.size())
			{
				return false;
			}

			int i = 0;
			for (Character c : b)
			{
				if (a[i] != c)
				{
					return false;
				}
				i++;
			}
			return true;
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
			e.printStackTrace();
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
