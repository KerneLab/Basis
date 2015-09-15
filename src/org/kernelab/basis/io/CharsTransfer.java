package org.kernelab.basis.io;

import java.io.Reader;
import java.io.Writer;

import org.kernelab.basis.Tools;

public class CharsTransfer implements Runnable
{
	private Reader	reader;

	private Writer	writer;

	private char[]	buffer;

	public CharsTransfer(Reader reader, Writer writer)
	{
		this(reader, writer, null);
	}

	public CharsTransfer(Reader reader, Writer writer, char[] buffer)
	{
		this.reader = reader;
		this.writer = writer;
		this.buffer = buffer;
	}

	public boolean isReleased()
	{
		return reader == null;
	}

	protected synchronized void release()
	{
		buffer = null;
		reader = null;
		writer = null;
		this.notifyAll();
	}

	public void run()
	{
		try
		{
			if (reader != null)
			{
				if (buffer == null || buffer.length == 0)
				{
					buffer = new char[Tools.BUFFER_SIZE];
				}

				transfer();
			}
		}
		catch (Exception e)
		{
		}
		finally
		{
			release();
		}
	}

	public CharsTransfer start()
	{
		return start(false);
	}

	public CharsTransfer start(boolean daemon)
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

		while ((length = reader.read(buffer)) != -1)
		{
			if (writer != null)
			{
				writer.write(buffer, 0, length);
				writer.flush();
			}
		}
	}
}
