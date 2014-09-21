package org.kernelab.basis.io;

import java.io.IOException;
import java.io.Writer;

import org.kernelab.basis.Tools;

public class StringBuilderWriter extends Writer
{
	private volatile boolean	closed;

	private StringBuilder		builder;

	public StringBuilderWriter()
	{
		this(new StringBuilder());
	}

	public StringBuilderWriter(int capacity)
	{
		this(new StringBuilder(capacity));
	}

	public StringBuilderWriter(StringBuilder builder)
	{
		this.setClosed(false).setBuilder(builder);
	}

	@Override
	public StringBuilderWriter append(char c) throws IOException
	{
		write(c);
		return this;
	}

	@Override
	public StringBuilderWriter append(CharSequence seq) throws IOException
	{
		this.ensure();
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
	public StringBuilderWriter append(CharSequence seq, int start, int end) throws IOException
	{
		this.ensure();
		builder.append(seq, start, end);
		return this;
	}

	@Override
	public void close() throws IOException
	{
		this.setClosed(true);
	}

	protected void ensure() throws IOException
	{
		if (this.isClosed())
		{
			throw new IOException();
		}
	}

	@Override
	public void flush()
	{
	}

	public StringBuilder getBuilder()
	{
		return builder;
	}

	public boolean isClosed()
	{
		return closed;
	}

	public void reset() throws IOException
	{
		this.ensure();
		Tools.clearStringBuilder(builder);
	}

	private <T extends StringBuilderWriter> T setBuilder(StringBuilder builder)
	{
		if (builder == null)
		{
			builder = new StringBuilder();
		}
		this.builder = builder;
		return Tools.cast(this);
	}

	private <T extends StringBuilderWriter> T setClosed(boolean closed)
	{
		this.closed = closed;
		return Tools.cast(this);
	}

	@Override
	public String toString()
	{
		return builder.toString();
	}

	@Override
	public void write(char cs[], int offset, int length) throws IOException
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
		this.ensure();
		builder.append(cs, offset, length);
	}

	@Override
	public void write(int c) throws IOException
	{
		this.ensure();
		builder.append((char) c);
	}

	@Override
	public void write(String string) throws IOException
	{
		this.ensure();
		builder.append(string);
	}

	@Override
	public void write(String string, int offset, int length) throws IOException
	{
		this.ensure();
		builder.append(string, offset, offset + length);
	}
}
