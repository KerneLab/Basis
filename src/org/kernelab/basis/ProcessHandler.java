package org.kernelab.basis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.kernelab.basis.io.StreamTransfer;

public class ProcessHandler extends AbstractAccomplishable<ProcessHandler> implements Callable<ProcessHandler>,
		Runnable
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ProcessHandler ph = new ProcessHandler(args);
		ph.run();
	}

	private Process				process;

	private ProcessBuilder		processBuilder;

	private Integer				result;

	private volatile boolean	started;

	private Object				startLock;

	private volatile boolean	terminated;

	private OutputStream		pos;

	private InputStream			pis;

	/*
	 * Streams of Process.
	 */

	private InputStream			pes;

	private InputStream			inputStream;

	private OutputStream		outputStream;

	/*
	 * Streams of delegation.
	 */

	private OutputStream		errorStream;

	public ProcessHandler(String... cmd)
	{
		super();
		process = null;
		processBuilder = new ProcessBuilder(cmd);
		started = false;
		startLock = new byte[0];
		terminated = true;
		inputStream = null;
		outputStream = null;
	}

	public ProcessHandler call() throws Exception
	{
		result = null;

		process = processBuilder.start();

		started = true;

		terminated = false;

		pos = process.getOutputStream();

		pis = process.getInputStream();

		pes = process.getErrorStream();

		new Thread(new StreamTransfer(inputStream, pos)).start();

		new Thread(new StreamTransfer(pis, outputStream)).start();

		new Thread(new StreamTransfer(pes, errorStream)).start();

		synchronized (startLock)
		{
			startLock.notifyAll();
		}

		try
		{
			result = process.waitFor();
		}
		catch (InterruptedException e)
		{
		}

		try
		{
			this.terminate();
		}
		catch (Exception e)
		{
		}

		this.accomplished();

		return this;
	}

	protected ProcessHandler closeStream()
	{
		if (inputStream != null)
		{
			try
			{
				inputStream.close();
			}
			catch (IOException e)
			{
			}
		}

		if (outputStream != null)
		{
			try
			{
				outputStream.close();
			}
			catch (IOException e)
			{
			}
		}

		if (errorStream != null)
		{
			try
			{
				errorStream.close();
			}
			catch (IOException e)
			{
			}
		}

		return this;
	}

	@Override
	protected ProcessHandler getAccomplishableSubject()
	{
		return this;
	}

	public List<String> getCommand()
	{
		return processBuilder.command();
	}

	public String getCommandLine()
	{
		return Tools.jointStrings(" ", this.getCommand());
	}

	public File getDirectory()
	{
		return processBuilder.directory();
	}

	public Map<String, String> getEnvironment()
	{
		return processBuilder.environment();
	}

	public Process getProcess()
	{
		return process;
	}

	protected ProcessBuilder getProcessBuilder()
	{
		return processBuilder;
	}

	public InputStream getProcessErrorStream()
	{
		return pes;
	}

	public InputStream getProcessInputStream()
	{
		return pis;
	}

	public OutputStream getProcessOutputStream()
	{
		return pos;
	}

	public Integer getResult()
	{
		return result;
	}

	public Object getStartLock()
	{
		return startLock;
	}

	public boolean isRedirectErrorStream()
	{
		return processBuilder.redirectErrorStream();
	}

	public boolean isStarted()
	{
		return started;
	}

	public boolean isTerminated()
	{
		return terminated;
	}

	public void run()
	{
		try
		{
			this.call();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public ProcessHandler setCommand(List<String> cmd)
	{
		processBuilder.command(cmd);

		return this;
	}

	public ProcessHandler setCommand(String... cmd)
	{
		processBuilder.command(cmd);

		return this;
	}

	public ProcessHandler setDirectory(File directory)
	{
		processBuilder.directory(directory);

		return this;
	}

	public ProcessHandler setErrorStream(OutputStream errorStream)
	{
		this.errorStream = errorStream;

		return this;
	}

	public ProcessHandler setInputStream(InputStream inputStream)
	{
		this.inputStream = inputStream;

		return this;
	}

	public ProcessHandler setOutputStream(OutputStream targetStream)
	{
		this.outputStream = targetStream;

		return this;
	}

	public ProcessHandler setRedirectErrorStream(boolean redirect)
	{
		processBuilder.redirectErrorStream(redirect);

		return this;
	}

	/**
	 * Terminate the Process.<br>
	 * Attention, this operation would not trigger any AccomplishListener.
	 */
	public void terminate()
	{
		if (!terminated)
		{
			terminated = true;

			closeStream();

			process.destroy();
		}
	}
}
