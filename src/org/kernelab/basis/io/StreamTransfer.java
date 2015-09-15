package org.kernelab.basis.io;

import java.io.InputStream;
import java.io.OutputStream;

import org.kernelab.basis.Tools;

public class StreamTransfer implements Runnable
{
	private InputStream		inputStream;

	private OutputStream	outputStream;

	private byte[]			buffer;

	public StreamTransfer(InputStream inputStream, OutputStream outputStream)
	{
		this(inputStream, outputStream, null);
	}

	public StreamTransfer(InputStream inputStream, OutputStream outputStream, byte[] buffer)
	{
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.buffer = buffer;
	}

	public boolean isReleased()
	{
		return inputStream == null;
	}

	protected synchronized void release()
	{
		buffer = null;
		inputStream = null;
		outputStream = null;
		this.notifyAll();
	}

	public void run()
	{
		try
		{
			if (inputStream != null)
			{
				if (buffer == null || buffer.length == 0)
				{
					buffer = new byte[Tools.BUFFER_SIZE];
				}

				transfer();
			}
		}
		catch (Exception e)
		{
		}
		finally
		{
			if (outputStream != null)
			{
				try
				{
					outputStream.flush();
				}
				catch (Exception e)
				{
				}
			}
			release();
		}
	}

	public StreamTransfer start()
	{
		return start(false);
	}

	public StreamTransfer start(boolean daemon)
	{
		Thread thread = new Thread(this);

		if (thread.isDaemon() != daemon)
		{
			thread.setDaemon(daemon);
		}

		thread.start();

		return this;
	}

	protected void transfer() throws Exception
	{
		int length = -1;

		while ((length = inputStream.read(buffer)) != -1)
		{
			if (outputStream != null)
			{
				outputStream.write(buffer, 0, length);
			}
		}
	}
}
