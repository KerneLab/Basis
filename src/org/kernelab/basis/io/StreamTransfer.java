package org.kernelab.basis.io;

import java.io.InputStream;
import java.io.OutputStream;

import org.kernelab.basis.Tools;

public class StreamTransfer implements Runnable
{
	private InputStream		inputStream;

	private OutputStream	outputStream;

	public StreamTransfer(InputStream inputStream, OutputStream outputStream)
	{
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}

	public void run()
	{
		if (inputStream != null)
		{
			byte[] buffer = new byte[Tools.BUFFER_BYTES];

			int length = -1;

			try
			{
				while ((length = inputStream.read(buffer)) != -1)
				{
					if (outputStream != null)
					{
						outputStream.write(buffer, 0, length);
						outputStream.flush();
					}
				}
			}
			catch (Exception e)
			{
			}
			finally
			{
				buffer = null;
				inputStream = null;
				outputStream = null;
			}
		}
	}
}
