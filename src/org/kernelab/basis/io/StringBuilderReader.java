package org.kernelab.basis.io;

import java.io.IOException;
import java.io.Reader;

import org.kernelab.basis.Tools;

public class StringBuilderReader extends Reader
{
	private StringBuilder	builder;

	public StringBuilderReader()
	{
		this.setBuilder(new StringBuilder());
	}

	public StringBuilderReader(int capacity)
	{
		this.setBuilder(new StringBuilder(capacity));
	}

	public StringBuilderReader(StringBuilder builder)
	{
		this.setBuilder(builder);
	}

	@Override
	public synchronized void close() throws IOException
	{
		builder = null;
		this.remind();
	}

	protected void ensure() throws IOException
	{
		if (builder == null)
		{
			throw new IOException();
		}
	}

	public StringBuilder getBuilder()
	{
		return builder;
	}

	public synchronized <T extends StringBuilderReader> T input(char[] cs) throws IOException
	{
		return input(cs, false);
	}

	public synchronized <T extends StringBuilderReader> T input(char[] cs, boolean remind) throws IOException
	{
		return input(cs, 0, cs.length, remind);
	}

	public synchronized <T extends StringBuilderReader> T input(char[] cs, int offset, int length) throws IOException
	{
		return input(cs, offset, length, false);
	}

	public synchronized <T extends StringBuilderReader> T input(char[] cs, int offset, int length, boolean remind)
			throws IOException
	{
		this.ensure();

		builder.append(cs, offset, length);

		if (remind)
		{
			return this.remind();
		}
		else
		{
			return Tools.cast(this);
		}
	}

	public synchronized <T extends StringBuilderReader> T input(CharSequence cs) throws IOException
	{
		return input(cs, false);
	}

	public synchronized <T extends StringBuilderReader> T input(CharSequence cs, boolean remind) throws IOException
	{
		this.ensure();

		builder.append(cs);

		if (remind)
		{
			return this.remind();
		}
		else
		{
			return Tools.cast(this);
		}
	}

	public synchronized <T extends StringBuilderReader> T input(Object o) throws IOException
	{
		return input(o, false);
	}

	public synchronized <T extends StringBuilderReader> T input(Object o, boolean remind) throws IOException
	{
		this.ensure();

		builder.append(o);

		if (remind)
		{
			return this.remind();
		}
		else
		{
			return Tools.cast(this);
		}
	}

	public boolean isClosed()
	{
		return builder == null;
	}

	@Override
	public synchronized int read(char[] cs, int offset, int length) throws IOException
	{
		if ((offset < 0) || (offset > cs.length) || (length < 0) || ((offset + length) > cs.length)
				|| ((offset + length) < 0))
		{
			throw new IndexOutOfBoundsException();
		}
		else if (length == 0)
		{
			return 0;
		}

		while (builder != null && builder.length() == 0)
		{
			try
			{
				this.wait();
			}
			catch (InterruptedException e)
			{
			}
		}

		if (builder == null)
		{
			return -1;
		}
		else
		{
			int len = Math.min(Math.min(cs.length - offset, length), builder.length());

			builder.getChars(0, len, cs, offset);

			builder.delete(0, len);

			return len;
		}
	}

	@Override
	public boolean ready()
	{
		return !this.isClosed();
	}

	public synchronized <T extends StringBuilderReader> T remind()
	{
		this.notifyAll();
		return Tools.cast(this);
	}

	@Override
	public synchronized void reset() throws IOException
	{
		this.ensure();
		Tools.clearStringBuilder(builder);
	}

	private synchronized <T extends StringBuilderReader> T setBuilder(StringBuilder builder)
	{
		if (builder == null)
		{
			builder = new StringBuilder();
		}
		this.builder = builder;
		return this.remind();
	}
}
