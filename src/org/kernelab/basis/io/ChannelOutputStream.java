package org.kernelab.basis.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.WritableByteChannel;

import org.kernelab.basis.Tools;

public class ChannelOutputStream extends OutputStream
{
	private WritableByteChannel	channel;

	private ByteBuffer			buffer;

	public ChannelOutputStream()
	{
		this(null);
	}

	public ChannelOutputStream(Channel channel)
	{
		this(channel, ByteBuffer.allocate(8192));
	}

	public ChannelOutputStream(Channel channel, ByteBuffer buffer)
	{
		this.setChannel((WritableByteChannel) channel).setBuffer(buffer);
	}

	@Override
	public void close() throws IOException
	{
		channel.close();
	}

	public ByteBuffer getBuffer()
	{
		return buffer;
	}

	public WritableByteChannel getChannel()
	{
		return channel;
	}

	public <T extends ChannelOutputStream> T setBuffer(ByteBuffer buffer)
	{
		this.buffer = buffer;
		return Tools.cast(this);
	}

	public <T extends ChannelOutputStream> T setChannel(WritableByteChannel channel)
	{
		this.channel = channel;
		return Tools.cast(this);
	}

	@Override
	public void write(byte b[], int off, int len) throws IOException
	{
		if (b == null || channel == null || buffer == null)
		{
			throw new NullPointerException();
		}
		else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0))
		{
			throw new IndexOutOfBoundsException();
		}
		else if (len != 0)
		{
			int writes = -1;
			while (len > 0)
			{
				buffer.limit(Math.min(len, buffer.capacity())).rewind();
				buffer.put(b, off, buffer.limit()).rewind();
				writes = channel.write(buffer);
				off += writes;
				len -= writes;
			}
		}
	}

	@Override
	public void write(int b) throws IOException
	{
		channel.write((ByteBuffer) ((ByteBuffer) buffer.limit(1).rewind()).put((byte) b).rewind());
	}
}
