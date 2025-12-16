package org.kernelab.basis;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.kernelab.basis.io.StreamTransfer;

public class ProcessHandler extends AbstractAccomplishable<ProcessHandler> implements Callable<ProcessHandler>, Runnable
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ProcessHandler ph = new ProcessHandler(args).useSystemOutputStreams();
		new StreamTransfer(System.in, ph.start().getProcessOutputStream()).start(true);
	}

	private Process			process;

	private ProcessBuilder	processBuilder;

	private Integer			result;

	private boolean			started;

	private boolean			running;

	private Exception		exception;

	private boolean			terminated;

	/**
	 * Streams of Process.
	 */
	private OutputStream	pos;

	private InputStream		pis;

	private InputStream		pes;

	/**
	 * Whether throws RuntimeException on exit with fail status. If enable this
	 * feature, the errorStream would be overridden.
	 */
	private boolean			throwsOnFail	= false;

	/**
	 * Streams of delegation.
	 */
	private OutputStream	outputStream;

	private OutputStream	errorStream;

	private StreamTransfer	tos;

	private StreamTransfer	tes;

	public ProcessHandler(String... cmd)
	{
		super();
		processBuilder = new ProcessBuilder(cmd);
	}

	@Override
	public ProcessHandler call() throws Exception
	{
		synchronized (this)
		{
			started = false;
			running = false;
			terminated = false;
		}

		final boolean redirect = processBuilder.redirectErrorStream();
		ByteArrayOutputStream catchStream = null;
		result = null;

		try
		{
			process = processBuilder.start();

			catchStream = this.isThrowsOnFail() ? new ByteArrayOutputStream() : null;

			pos = process.getOutputStream();

			pis = process.getInputStream();

			pes = process.getErrorStream();

			tos = new StreamTransfer(pis, redirect && catchStream != null ? catchStream : outputStream);
			tos.start(true);

			if (!redirect)
			{
				tes = new StreamTransfer(pes, catchStream != null ? catchStream : errorStream);
				tes.start(true);
			}
		}
		catch (Exception e)
		{
			exception = e;
		}
		finally
		{
			synchronized (this)
			{
				started = true;
				running = true;
				terminated = false;
				this.notifyAll();
			}
		}

		if (tos != null)
		{
			synchronized (tos)
			{
				while (!tos.isReleased())
				{
					try
					{
						tos.wait();
					}
					catch (InterruptedException e)
					{
					}
				}
			}
		}

		if (tes != null)
		{
			synchronized (tes)
			{
				while (!tes.isReleased())
				{
					try
					{
						tes.wait();
					}
					catch (InterruptedException e)
					{
					}
				}
			}
		}

		if (process != null)
		{
			try
			{
				result = process.waitFor();
			}
			catch (InterruptedException e)
			{
				if (exception == null)
				{
					exception = e;
				}
			}
		}

		this.terminate();

		this.destroy();

		this.accomplished();

		if (result != null && !Tools.equals(result, 0) && catchStream != null)
		{
			exception = new RuntimeException(catchStream.toString());
		}

		if (exception != null)
		{
			throw exception;
		}

		return this;
	}

	protected void destroy()
	{
		process = null;

		tos = null;
		tes = null;

		outputStream = null;
		errorStream = null;

		pos = null;
		pis = null;
		pes = null;
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

	public Exception getException()
	{
		return exception;
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

	public boolean isRedirectErrorStream()
	{
		return processBuilder.redirectErrorStream();
	}

	public boolean isRunning()
	{
		return running;
	}

	public boolean isStarted()
	{
		return started;
	}

	public boolean isTerminated()
	{
		return terminated;
	}

	public boolean isThrowsOnFail()
	{
		return throwsOnFail;
	}

	@Override
	public void run()
	{
		try
		{
			this.call();
		}
		catch (Exception e)
		{
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

	public ProcessHandler setOutputStream(OutputStream outputStream)
	{
		this.outputStream = outputStream;
		return this;
	}

	public ProcessHandler setRedirectErrorStream(boolean redirect)
	{
		processBuilder.redirectErrorStream(redirect);
		return this;
	}

	public ProcessHandler setThrowsOnFail(boolean throwsOnFail)
	{
		this.throwsOnFail = throwsOnFail;
		return this;
	}

	/**
	 * Start the Process Thread in normal mode and wait until it has been really
	 * started.
	 * 
	 * @return
	 */
	public ProcessHandler start()
	{
		return start(false);
	}

	/**
	 * Start the Process Thread and wait until it has been really started.
	 * 
	 * @param daemon
	 *            start the thread in daemon mode or not.
	 * @return
	 */
	public ProcessHandler start(boolean daemon)
	{
		Thread thread = new Thread(this);
		if (thread.isDaemon() != daemon)
		{
			thread.setDaemon(daemon);
		}
		thread.start();
		return this.waitForStarted();
	}

	/**
	 * Terminate the Process.<br>
	 * Attention, this operation would not trigger any AccomplishListener.
	 */
	public synchronized void terminate()
	{
		try
		{
			if (!terminated)
			{
				running = false;
				terminated = true;

				try
				{
					process.destroy();
				}
				catch (Exception e)
				{
				}
			}
		}
		finally
		{
			this.notifyAll();
		}
	}

	public ProcessHandler useSystemOutputStreams()
	{
		return this.setOutputStream(System.out).setErrorStream(System.err);
	}

	/**
	 * Waiting and blocked until the process is started.
	 * 
	 * @return
	 */
	public synchronized ProcessHandler waitForStarted()
	{
		while (!this.isStarted())
		{
			try
			{
				this.wait();
			}
			catch (InterruptedException e)
			{
			}
		}
		return this;
	}

	/**
	 * Waiting and blocked until the process is terminated.
	 * 
	 * @return
	 */
	public synchronized ProcessHandler waitForTerminated()
	{
		while (!this.isTerminated())
		{
			try
			{
				this.wait();
			}
			catch (InterruptedException e)
			{
			}
		}
		return this;
	}
}
