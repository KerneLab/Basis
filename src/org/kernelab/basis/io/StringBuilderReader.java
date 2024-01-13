package org.kernelab.basis.io;

import java.io.IOException;
import java.io.Reader;

public class StringBuilderReader extends Reader
{
	private StringBuilder	builder;

	private int				pos		= 0;

	private int				mark	= 0;

	private boolean			commit	= false;

	public StringBuilderReader()
	{
		this(new StringBuilder());
	}

	public StringBuilderReader(int capacity)
	{
		this(new StringBuilder(capacity));
	}

	public StringBuilderReader(StringBuilder builder)
	{
		this(builder, true);
	}

	public StringBuilderReader(StringBuilder builder, boolean commit)
	{
		this.setBuilder(builder).setCommit(commit);
	}

	@Override
	public synchronized void close() throws IOException
	{
		this.setBuilder(null);
		this.remind();
	}

	public synchronized StringBuilderReader commit()
	{
		this.setCommit(true);
		this.remind();
		return this;
	}

	protected void ensure() throws IOException
	{
		if (this.isClosed())
		{
			throw new IOException("Reader has been closed");
		}
	}

	public StringBuilder getBuilder()
	{
		return builder;
	}

	public synchronized StringBuilderReader input(char[] cs) throws IOException
	{
		return input(cs, false);
	}

	public synchronized StringBuilderReader input(char[] cs, boolean remind) throws IOException
	{
		return input(cs, 0, cs.length, remind);
	}

	public synchronized StringBuilderReader input(char[] cs, int offset, int length) throws IOException
	{
		return input(cs, offset, length, false);
	}

	public synchronized StringBuilderReader input(char[] cs, int offset, int length, boolean remind) throws IOException
	{
		this.ensure();

		this.builder.append(cs, offset, length);

		if (remind)
		{
			return this.remind();
		}
		else
		{
			return this;
		}
	}

	public synchronized StringBuilderReader input(CharSequence cs) throws IOException
	{
		return input(cs, false);
	}

	public synchronized StringBuilderReader input(CharSequence cs, boolean remind) throws IOException
	{
		this.ensure();

		this.builder.append(cs);

		if (remind)
		{
			return this.remind();
		}
		else
		{
			return this;
		}
	}

	public synchronized StringBuilderReader input(Object o) throws IOException
	{
		return input(o, false);
	}

	public synchronized StringBuilderReader input(Object o, boolean remind) throws IOException
	{
		this.ensure();

		this.builder.append(o);

		if (remind)
		{
			return this.remind();
		}
		else
		{
			return this;
		}
	}

	public boolean isClosed()
	{
		return builder == null;
	}

	public synchronized boolean isCommit()
	{
		return commit;
	}

	@Override
	public synchronized void mark(int readAheadLimit) throws IOException
	{
		this.mark = this.pos;
	}

	@Override
	public boolean markSupported()
	{
		return true;
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

		this.ensure();

		while (!this.isCommit() && this.pos >= this.builder.length())
		{
			try
			{
				this.wait();
			}
			catch (InterruptedException e)
			{
			}
		}

		this.ensure();

		if (this.isCommit() && this.pos >= this.builder.length())
		{
			return -1;
		}
		else
		{
			int len = Math.min(this.builder.length() - this.pos, length);
			if (len > 0)
			{
				this.builder.getChars(this.pos, this.pos + len, cs, offset);
				this.pos += len;
			}
			return len;
		}
	}

	@Override
	public synchronized boolean ready() throws IOException
	{
		this.ensure();
		return this.isCommit() || this.pos < this.builder.length();
	}

	public StringBuilderReader remind()
	{
		this.notifyAll();
		return this;
	}

	@Override
	public synchronized void reset() throws IOException
	{
		reset(this.mark);
	}

	/**
	 * Reset this reader to the given position.
	 * 
	 * @param pos
	 * @return
	 * @throws IOException
	 */
	public synchronized StringBuilderReader reset(int pos) throws IOException
	{
		this.ensure();
		this.pos = Math.min(Math.max(pos, 0), this.builder.length());
		return this.remind();
	}

	public StringBuilderReader setBuilder(StringBuilder builder)
	{
		this.builder = builder;
		return this;
	}

	protected void setCommit(boolean commit)
	{
		this.commit = commit;
	}

	@Override
	public synchronized long skip(long n) throws IOException
	{
		this.ensure();
		int skip = (int) Math.min(this.builder.length() - this.pos, n);
		this.pos += skip;
		return skip;
	}
}
