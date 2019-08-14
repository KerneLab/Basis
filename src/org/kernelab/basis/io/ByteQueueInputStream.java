package org.kernelab.basis.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Queue;

import org.kernelab.basis.Tools;

public class ByteQueueInputStream extends InputStream
{
	private volatile boolean	closed;

	private Queue<Byte>			queue;

	private String				charset;

	public ByteQueueInputStream()
	{
		this(new LinkedList<Byte>());
	}

	public ByteQueueInputStream(Queue<Byte> queue)
	{
		this(queue, null);
	}

	public ByteQueueInputStream(Queue<Byte> queue, String charsetName)
	{
		this.setClosed(false).setQueue(queue).setCharset(charsetName);
	}

	@Override
	public synchronized int available() throws IOException
	{
		return queue.size();
	}

	@Override
	public synchronized void close() throws IOException
	{
		this.setClosed(true);
		this.remind();
	}

	protected void ensure() throws IOException
	{
		if (this.isClosed())
		{
			throw new IOException();
		}
	}

	public String getCharset()
	{
		return charset;
	}

	public Queue<Byte> getQueue()
	{
		return queue;
	}

	public synchronized <T extends ByteQueueInputStream> T input(char[] cs) throws IOException
	{
		return input(cs, false);
	}

	public synchronized <T extends ByteQueueInputStream> T input(char[] cs, boolean remind) throws IOException
	{
		return input(cs, 0, cs.length, remind);
	}

	public synchronized <T extends ByteQueueInputStream> T input(char[] cs, int offset, int length) throws IOException
	{
		return input(cs, offset, length, false);
	}

	public synchronized <T extends ByteQueueInputStream> T input(char[] cs, int offset, int length, boolean remind)
			throws IOException
	{
		this.ensure();

		for (byte b : new String(cs, offset, length).getBytes(charset))
		{
			queue.add(b);
		}

		if (remind)
		{
			return this.remind();
		}
		else
		{
			return Tools.cast(this);
		}
	}

	public synchronized <T extends ByteQueueInputStream> T input(CharSequence cs) throws IOException
	{
		return input(cs, false);
	}

	public synchronized <T extends ByteQueueInputStream> T input(CharSequence cs, boolean remind) throws IOException
	{
		this.ensure();

		for (byte b : cs.toString().getBytes(charset))
		{
			queue.add(b);
		}

		if (remind)
		{
			return this.remind();
		}
		else
		{
			return Tools.cast(this);
		}
	}

	public synchronized <T extends ByteQueueInputStream> T input(Object o) throws IOException
	{
		return input(o, false);
	}

	public synchronized <T extends ByteQueueInputStream> T input(Object o, boolean remind) throws IOException
	{
		this.ensure();

		for (byte b : String.valueOf(o).getBytes(charset))
		{
			queue.add(b);
		}

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
		return closed;
	}

	@Override
	public synchronized int read() throws IOException
	{
		while (!this.isClosed() && queue.isEmpty())
		{
			try
			{
				this.wait();
			}
			catch (InterruptedException e)
			{
			}
		}

		if (this.isClosed() && queue.isEmpty())
		{
			return -1;
		}
		else
		{
			return queue.poll();
		}
	}

	@Override
	public synchronized int read(byte bs[], int offset, int length) throws IOException
	{
		if ((offset < 0) || (offset > bs.length) || (length < 0) || ((offset + length) > bs.length)
				|| ((offset + length) < 0))
		{
			throw new IndexOutOfBoundsException();
		}
		else if (length == 0)
		{
			return 0;
		}

		while (!this.isClosed() && queue.isEmpty())
		{
			try
			{
				this.wait();
			}
			catch (InterruptedException e)
			{
			}
		}

		if (this.isClosed() && queue.isEmpty())
		{
			return -1;
		}
		else
		{
			int len = Math.min(Math.min(bs.length - offset, length), queue.size());

			int end = offset + len;

			for (int i = offset; i < end; i++)
			{
				bs[i] = queue.poll();
			}

			return len;
		}
	}

	public synchronized <T extends ByteQueueInputStream> T remind()
	{
		this.notifyAll();
		return Tools.cast(this);
	}

	@Override
	public synchronized void reset()
	{
		queue.clear();
	}

	private <T extends ByteQueueInputStream> T setCharset(String charset)
	{
		if (charset == null)
		{
			charset = Charset.defaultCharset().name();
		}
		this.charset = charset;
		return Tools.cast(this);
	}

	private <T extends ByteQueueInputStream> T setClosed(boolean closed)
	{
		this.closed = closed;
		return Tools.cast(this);
	}

	private <T extends ByteQueueInputStream> T setQueue(Queue<Byte> queue)
	{
		this.queue = queue;
		return Tools.cast(this);
	}
}
