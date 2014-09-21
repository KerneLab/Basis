package org.kernelab.basis.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Queue;

import org.kernelab.basis.Tools;

public class ByteQueueOutputStream extends OutputStream
{
	private volatile boolean	closed;

	private Queue<Byte>			queue;

	private String				charset;

	public ByteQueueOutputStream()
	{
		this(Charset.defaultCharset());
	}

	public ByteQueueOutputStream(Charset charset)
	{
		this(null, charset);
	}

	public ByteQueueOutputStream(Queue<Byte> queue, Charset charset)
	{
		this.setClosed(false).setQueue(queue).setCharset(charset);
	}

	public ByteQueueOutputStream(Queue<Byte> queue, String charsetName)
	{
		this(queue, Charset.forName(charsetName));
	}

	public ByteQueueOutputStream(String charsetName)
	{
		this(null, charsetName);
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

	public String getCharset()
	{
		return charset;
	}

	public Queue<Byte> getQueue()
	{
		return queue;
	}

	public boolean isClosed()
	{
		return closed;
	}

	public void reset() throws IOException
	{
		this.ensure();
		queue.clear();
	}

	private <T extends ByteQueueOutputStream> T setCharset(Charset charset)
	{
		if (charset == null)
		{
			charset = Charset.defaultCharset();
		}
		this.charset = charset.name();
		return Tools.cast(this);
	}

	private <T extends ByteQueueOutputStream> T setClosed(boolean closed)
	{
		this.closed = closed;
		return Tools.cast(this);
	}

	private <T extends ByteQueueOutputStream> T setQueue(Queue<Byte> queue)
	{
		if (queue == null)
		{
			queue = new LinkedList<Byte>();
		}
		this.queue = queue;
		return Tools.cast(this);
	}

	@Override
	public String toString()
	{
		try
		{
			return this.toString(this.getCharset());
		}
		catch (UnsupportedEncodingException e)
		{
			return null;
		}
	}

	public String toString(String charsetName) throws UnsupportedEncodingException
	{
		byte[] bs = new byte[queue.size()];

		int i = 0;

		for (Byte b : queue)
		{
			bs[i] = b;
			i++;
		}

		return new String(bs, charsetName);
	}

	@Override
	public void write(byte bs[], int offset, int length) throws IOException
	{
		if ((offset < 0) || (offset > bs.length) || (length < 0) || ((offset + length) > bs.length)
				|| ((offset + length) < 0))
		{
			throw new IndexOutOfBoundsException();
		}
		else if (length == 0)
		{
			return;
		}
		this.ensure();
		int end = offset + length;
		for (int i = offset; i < end; i++)
		{
			queue.add(bs[i]);
		}
	}

	@Override
	public void write(int b) throws IOException
	{
		this.ensure();
		queue.add((byte) b);
	}
}
