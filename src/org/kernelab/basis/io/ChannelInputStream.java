package org.kernelab.basis.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.kernelab.basis.Tools;

public class ChannelInputStream extends InputStream
{
	private ReadableByteChannel	channel;

	private ByteBuffer			buffer;

	public ChannelInputStream(ReadableByteChannel channel)
	{
		this(channel, ByteBuffer.allocate(Tools.BUFFER_BYTES));
	}

	public ChannelInputStream(ReadableByteChannel channel, ByteBuffer buffer)
	{
		this.setChannel(channel).setBuffer(buffer);
	}

	@Override
	public synchronized void close() throws IOException
	{
		channel.close();
	}

	protected ByteBuffer getBuffer()
	{
		return buffer;
	}

	protected ReadableByteChannel getChannel()
	{
		return channel;
	}

	@Override
	public synchronized int read() throws IOException
	{
		buffer.limit(1).rewind();

		if (channel.read(buffer) == -1)
		{
			return -1;
		}
		else
		{
			return buffer.get();
		}
	}

	@Override
	public synchronized int read(byte b[], int off, int len) throws IOException
	{
		if (b == null || channel == null || buffer == null)
		{
			throw new NullPointerException();
		}
		else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0))
		{
			throw new IndexOutOfBoundsException();
		}
		else if (len == 0)
		{
			return 0;
		}

		int read = 0, reads = -1;

		while ((reads = channel.read((ByteBuffer) buffer.limit(Math.min(len - read, buffer.capacity())).rewind())) != -1)
		{
			System.arraycopy(buffer.array(), 0, b, off, reads);

			read += reads;

			if (read >= len)
			{
				break;
			}

			off += reads;
		}

		return read == 0 ? -1 : read;
	}

	private <T extends ChannelInputStream> T setBuffer(ByteBuffer buffer)
	{
		this.buffer = buffer;
		return Tools.cast(this);
	}

	private <T extends ChannelInputStream> T setChannel(ReadableByteChannel channel)
	{
		this.channel = channel;
		return Tools.cast(this);
	}
}
