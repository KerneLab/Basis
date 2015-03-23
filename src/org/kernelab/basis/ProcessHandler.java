package org.kernelab.basis;

import java.io.File;
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

	private volatile boolean	running;

	private Exception			exception;

	private volatile boolean	terminated;

	/**
	 * Streams of Process.
	 */
	private OutputStream		pos;

	private InputStream			pis;

	private InputStream			pes;

	/**
	 * Streams of delegation.
	 */
	private OutputStream		outputStream;

	private OutputStream		errorStream;

	private StreamTransfer		tos;

	private StreamTransfer		tes;

	public ProcessHandler(String... cmd)
	{
		super();
		processBuilder = new ProcessBuilder(cmd);
	}

	public ProcessHandler call() throws Exception
	{
		synchronized (this)
		{
			started = false;
			running = false;
			terminated = false;
		}

		result = null;

		try
		{
			process = processBuilder.start();

			pos = process.getOutputStream();

			pis = process.getInputStream();

			pes = process.getErrorStream();

			tos = new StreamTransfer(pis, outputStream);

			tes = new StreamTransfer(pes, errorStream);

			new Thread(tos).start();

			new Thread(tes).start();
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
				while (!tos.isClosed())
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
				while (!tes.isClosed())
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
			}
		}

		this.terminate();

		this.accomplished();

		if (exception != null)
		{
			throw exception;
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
				finally
				{
					process = null;
				}

				tos = null;
				tes = null;

				outputStream = null;
				errorStream = null;

				pis = null;
				pos = null;
				pes = null;
			}
		}
		finally
		{
			this.notifyAll();
		}
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
