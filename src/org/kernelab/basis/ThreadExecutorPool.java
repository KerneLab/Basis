package org.kernelab.basis;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ThreadExecutorPool<V> implements CompletionService<V>
{
	private ExecutorService			executorService		= null;

	private CompletionService<V>	completionService	= null;

	private Variable<Integer>		tasks				= new Variable<Integer>(0);

	public ThreadExecutorPool()
	{
		this(Executors.newCachedThreadPool());
	}

	public ThreadExecutorPool(ExecutorService executorService)
	{
		this.setExecutorService(executorService);
		this.setCompletionService(new ExecutorCompletionService<V>(this.executorService));
	}

	public ThreadExecutorPool(int threads)
	{
		this(Executors.newFixedThreadPool(threads));
	}

	private void addTask()
	{
		synchronized (tasks)
		{
			this.tasks.value++;
		}
	}

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
	{
		return executorService.awaitTermination(timeout, unit);
	}

	private void delTask()
	{
		synchronized (tasks)
		{
			if (this.tasks.value > 0)
			{
				this.tasks.value--;
			}
		}
	}

	public CompletionService<V> getCompletionService()
	{
		return completionService;
	}

	public ExecutorService getExecutorService()
	{
		return executorService;
	}

	public Integer getTasks()
	{
		synchronized (tasks)
		{
			return tasks.value;
		}
	}

	/**
	 * Retrieves and removes the Future representing the next completed task,
	 * directly return null if none task was running, waiting if some task is to
	 * be done.
	 * 
	 * @return the Future representing the next completed task, or null if could
	 *         not fetch a complete task.
	 */
	public Future<V> grab()
	{
		synchronized (tasks)
		{
			if (tasks.value > 0)
			{
				tasks.value--;
			}
			else
			{
				return null;
			}
		}

		try
		{
			return completionService.take();
		}
		catch (InterruptedException e)
		{
			this.addTask();
		}

		return null;
	}

	public boolean isEmpty()
	{
		return this.getTasks() == 0;
	}

	public boolean isShutdown()
	{
		return executorService.isShutdown();
	}

	public boolean isTerminated()
	{
		return executorService.isTerminated();
	}

	public Future<V> poll()
	{
		Future<V> future = completionService.poll();

		if (future != null)
		{
			this.delTask();
		}

		return future;
	}

	public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException
	{
		Future<V> future = completionService.poll(timeout, unit);

		if (future != null)
		{
			this.delTask();
		}

		return future;
	}

	public void setCompletionService(CompletionService<V> completionService)
	{
		this.completionService = completionService;
	}

	public void setExecutorService(ExecutorService executorService)
	{
		this.executorService = executorService;
	}

	public void shutdown()
	{
		executorService.shutdown();
	}

	public List<Runnable> shutdownNow()
	{
		return executorService.shutdownNow();
	}

	public Future<V> submit(Callable<V> task)
	{
		try
		{
			Future<V> future = completionService.submit(task);
			this.addTask();
			return future;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public Future<V> submit(Runnable task, V result)
	{
		try
		{
			Future<V> future = completionService.submit(task, result);
			this.addTask();
			return future;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public Future<V> take() throws InterruptedException
	{
		Future<V> future = completionService.take();
		this.delTask();
		return future;
	}
}
