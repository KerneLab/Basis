package org.kernelab.basis.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.kernelab.basis.Tools;

public class ChannelOutputStream extends OutputStream
{
	private WritableByteChannel	channel;

	private ByteBuffer			buffer;

	public ChannelOutputStream(WritableByteChannel channel)
	{
		this(channel, ByteBuffer.allocate(Tools.BUFFER_BYTES));
	}

	public ChannelOutputStream(WritableByteChannel channel, ByteBuffer buffer)
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

	protected WritableByteChannel getChannel()
	{
		return channel;
	}

	private <T extends ChannelOutputStream> T setBuffer(ByteBuffer buffer)
	{
		this.buffer = buffer;
		return Tools.cast(this);
	}

	private <T extends ChannelOutputStream> T setChannel(WritableByteChannel channel)
	{
		this.channel = channel;
		return Tools.cast(this);
	}

	@Override
	public synchronized void write(byte b[], int off, int len) throws IOException
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
	public synchronized void write(int b) throws IOException
	{
		channel.write((ByteBuffer) ((ByteBuffer) buffer.limit(1).rewind()).put((byte) b).rewind());
	}
}
