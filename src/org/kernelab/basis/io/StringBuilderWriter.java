package org.kernelab.basis.io;

import java.io.IOException;
import java.io.Writer;

public class StringBuilderWriter extends Writer
{
	private StringBuilder	builder;

	public StringBuilderWriter()
	{
		this.setBuilder(new StringBuilder());
	}

	public StringBuilderWriter(int capacity)
	{
		this.setBuilder(new StringBuilder(capacity));
	}

	public StringBuilderWriter(StringBuilder buffer)
	{
		this.setBuilder(buffer);
	}

	@Override
	public StringBuilderWriter append(char c)
	{
		write(c);
		return this;
	}

	@Override
	public StringBuilderWriter append(CharSequence seq)
	{
		if (seq == null)
		{
			builder.append("null");
		}
		else
		{
			this.append(seq, 0, seq.length());
		}
		return this;
	}

	@Override
	public StringBuilderWriter append(CharSequence seq, int start, int end)
	{
		builder.append(seq, start, end);
		return this;
	}

	@Override
	public void close() throws IOException
	{
	}

	@Override
	public void flush()
	{
	}

	public StringBuilder getBuilder()
	{
		return builder;
	}

	public StringBuilderWriter setBuilder(StringBuilder builder)
	{
		if (builder == null)
		{
			builder = new StringBuilder();
		}
		this.builder = builder;
		return this;
	}

	@Override
	public String toString()
	{
		return builder.toString();
	}

	@Override
	public void write(char cs[], int offset, int length)
	{
		if ((offset < 0) || (offset > cs.length) || (length < 0) || ((offset + length) > cs.length)
				|| ((offset + length) < 0))
		{
			throw new IndexOutOfBoundsException();
		}
		else if (length == 0)
		{
			return;
		}
		builder.append(cs, offset, length);
	}

	@Override
	public void write(int c)
	{
		builder.append((char) c);
	}

	@Override
	public void write(String string)
	{
		builder.append(string);
	}

	@Override
	public void write(String string, int offset, int length)
	{
		builder.append(string, offset, offset + length);
	}
}
