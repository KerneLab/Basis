package org.kernelab.basis;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

public class ProcessHandler extends AbstractAccomplishable implements Runnable
{

	public static final String	PROCESS_ACCOMPLISHED_COMMAND	= "ACCOMPLISHED";

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ProcessHandler ph = new ProcessHandler("notepad");
		ph.run();
	}

	private Process			process;

	private ProcessBuilder	processBuilder;

	private boolean			terminated;

	private PrintStream		printStream;

	private ActionEvent		accomplishedEvent;

	public ProcessHandler(String... cmd)
	{
		super();
		process = null;
		processBuilder = new ProcessBuilder(cmd);
		terminated = true;
		printStream = null;
		accomplishedEvent = null;
	}

	@Override
	public ActionEvent getAccomplishedEvent()
	{
		ActionEvent event = accomplishedEvent;

		if (event == null) {
			event = new ActionEvent(this, process.exitValue(),
					ProcessHandler.PROCESS_ACCOMPLISHED_COMMAND, Tools.getTimeStamp(), 0);
		}

		return event;
	}

	public List<String> getCommand()
	{
		return processBuilder.command();
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

	public boolean isTerminated()
	{
		return terminated;
	}

	public void run()
	{
		try {

			String line = null;

			process = processBuilder.start();

			terminated = false;

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			while (!terminated && (line = bufferedReader.readLine()) != null) {
				if (printStream != null) {
					printStream.println(line);
				}
			}

			if (!terminated) {

				this.getProcess().waitFor();

				this.terminate();

				this.accomplished();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public ProcessHandler setAccomplishedEvent(ActionEvent finishedEvent)
	{
		this.accomplishedEvent = finishedEvent;

		return this;
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
		if (!terminated) {

			terminated = true;

			process.destroy();
		}
	}

}
