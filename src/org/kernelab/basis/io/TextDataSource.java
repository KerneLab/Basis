package org.kernelab.basis.io;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.kernelab.basis.Tools;
import org.kernelab.basis.io.ReaderFactory.DefaultReaderFactory;
import org.kernelab.basis.io.ReaderFactory.FileReaderFactory;

public class TextDataSource implements Iterable<String>
{
	protected class TextDataSourceIterator implements Iterator<String>
	{
		private Reader			reader;

		private char[]			term;

		private char[]			buff;

		private int				len;

		private int				pos;

		private StringBuilder	builder;

		private String			line;

		public TextDataSourceIterator() throws IOException
		{
			this.reader = TextDataSource.this.getFactory().getReader();
			this.buff = new char[8192];
			this.len = 0;
			this.pos = 0;
			this.term = TextDataSource.this.getLineSeparator().toCharArray();
			this.builder = new StringBuilder();
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
			Tools.clearStringBuilder(this.builder);
		}

		protected boolean contains(char[] a, int from, char[] b)
		{
			for (int i = 0; i < b.length; i++)
			{
				if (a[i + from] != b[i])
				{
					return false;
				}
			}
			return true;
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
			int reads = -1;
			boolean first = true;
			boolean termed = false;
			while (true)
			{
				try
				{
					reads = reader.read(buff, len, buff.length - len);
				}
				catch (IOException e)
				{
					reads = -1;
				}

				if (reads > 0)
				{
					len += reads;
				}

				for (; pos < len; pos++)
				{
					first = false;

					if (pos <= len - term.length)
					{
						if (contains(buff, pos, term))
						{
							termed = true;
							break;
						}
					}
					else if (pos > len - term.length)
					{
						if (samePrefix(buff, pos, term))
						{
							break;
						}
					}
				}

				builder.append(buff, 0, pos);

				if (termed)
				{
					pos += term.length;
				}

				System.arraycopy(buff, pos, buff, 0, len - pos);
				len -= pos;
				pos = 0;

				if (termed || reads == -1 && pos >= len)
				{
					break;
				}
			}

			if (!termed)
			{
				try
				{
					if (first)
					{
						return null;
					}
					else
					{
						return builder.toString();
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
					return builder.toString();
				}
				finally
				{
					Tools.clearStringBuilder(builder);
				}
			}
		}

		public void remove()
		{
		}

		protected boolean samePrefix(char[] a, int from, char[] b)
		{
			for (int i = from; i < a.length; i++)
			{
				if (a[i] != b[i - from])
				{
					return false;
				}
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
