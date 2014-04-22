package org.kernelab.basis;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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

	private Process			process;

	private ProcessBuilder	processBuilder;

	private Integer			result;

	private boolean			terminated;

	private PrintStream		printStream;

	public ProcessHandler(String... cmd)
	{
		super();
		process = null;
		processBuilder = new ProcessBuilder(cmd);
		terminated = true;
		printStream = null;
	}

	public ProcessHandler call() throws Exception
	{
		result = null;

		String line = null;

		process = processBuilder.start();

		terminated = false;

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

		while (!terminated && (line = bufferedReader.readLine()) != null)
		{
			if (printStream != null)
			{
				printStream.println(line);
			}
		}

		if (!terminated)
		{
			result = this.getProcess().waitFor();

			this.terminate();

			this.accomplished();
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

	public PrintStream getPrintStream()
	{
		return printStream;
	}

	public Process getProcess()
	{
		return process;
	}

	protected ProcessBuilder getProcessBuilder()
	{
		return processBuilder;
	}

	public Integer getResult()
	{
		return result;
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

	public ProcessHandler setPrintStream(PrintStream printStream)
	{
		this.printStream = printStream;

		return this;
	}

	/**
	 * Terminate the Process.<br>
	 * Attention, this operation would not trigger any finishedListener.
	 */
	public void terminate()
	{
		if (!terminated)
		{
			terminated = true;

			process.destroy();
		}
	}
}
