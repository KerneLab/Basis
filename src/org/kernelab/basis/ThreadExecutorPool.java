package org.kernelab.basis;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadExecutorPool<V> implements CompletionService<V>
{
	private ExecutorService			executorService		= null;

	private CompletionService<V>	completionService	= null;

	private Variable<Integer>		tasks				= new Variable<Integer>(0);

	private Integer					limit				= null;

	public ThreadExecutorPool()
	{
		this(Executors.newCachedThreadPool());
	}

	public ThreadExecutorPool(ExecutorService executorService)
	{
		this.setExecutorService(executorService);
		this.setCompletionService(new ExecutorCompletionService<V>(this.executorService));
	}

	public ThreadExecutorPool(int limit)
	{
		this(limit, limit);
	}

	public ThreadExecutorPool(int limit, int init)
	{
		this(new ThreadPoolExecutor(init, limit, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()));
		this.limit = limit;
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

	/**
	 * Fetch the busy degree of the pool load between 0.0 and 1.0<br />
	 * If the pool was not defined by limit, but also the executorService was
	 * not a ThreadPoolExecutor, then this method returns -1.
	 * 
	 * @return The busy degree.
	 */
	public float getBusy()
	{
		if (this.getLimit() != null)
		{
			return 1f * this.getTasks() / this.getLimit();
		}
		else
		{
			if (executorService instanceof ThreadPoolExecutor)
			{
				ThreadPoolExecutor exec = (ThreadPoolExecutor) executorService;
				return 1f * exec.getActiveCount() / exec.getLargestPoolSize();
			}
			else
			{
				return -1;
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

	public Integer getLimit()
	{
		return limit;
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
