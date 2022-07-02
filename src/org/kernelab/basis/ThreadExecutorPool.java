package org.kernelab.basis;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadExecutorPool<V> implements CompletionService<V>
{
	public static class GroupThreadFactory implements ThreadFactory
	{
		private ThreadGroup	group;

		private String		prefix;

		private int			count;

		public GroupThreadFactory(ThreadGroup group)
		{
			this.group = group;
			this.prefix = (group == null ? "null" : group.getName()) + "-";
			this.count = 0;
		}

		public Thread newThread(Runnable r)
		{
			return new Thread(group, r, prefix + next());
		}

		private synchronized int next()
		{
			return count++;
		}
	}

	private ExecutorService			executorService		= null;

	private CompletionService<V>	completionService	= null;

	protected final ReadWriteLock	lock				= new ReentrantReadWriteLock();

	private Variable<Integer>		tasks				= new Variable<Integer>(0);

	private Integer					limit				= null;

	private int						fakes				= 0;

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
		this(limit, init, Executors.defaultThreadFactory());
	}

	public ThreadExecutorPool(int limit, int init, ThreadFactory factory)
	{
		this(new ThreadPoolExecutor(init, limit, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
				factory));
		this.limit = limit;
	}

	public ThreadExecutorPool(int limit, int init, ThreadGroup group)
	{
		this(limit, init, new GroupThreadFactory(group));
	}

	private void addTask()
	{
		lock.writeLock().lock();
		try
		{
			this.tasks.value++;
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
	{
		return executorService.awaitTermination(timeout, unit);
	}

	private void delTask()
	{
		lock.writeLock().lock();
		try
		{
			if (this.tasks.value > 0)
			{
				this.tasks.value--;
			}
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}

	/**
	 * Fetch the busy degree of the pool load between 0.0 and 1.0<br />
	 * The value beyond 1.0 is the ratio of tasks awaiting to be executed.<br />
	 * If the limit is 0, then 1.0 will be returned.<br />
	 * If the pool was not defined by limit, but also the executorService was
	 * not a ThreadPoolExecutor, then this method returns -1.
	 * 
	 * @return The busy degree.
	 */
	public float getBusy()
	{
		lock.readLock().lock();
		try
		{
			return this.getBusyUsafe();
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	protected float getBusyUsafe()
	{
		Integer limit = this.getLimitUnsafe();

		if (limit != null)
		{
			if (limit > 0)
			{
				return 1f * this.getRemainUnsafe() / limit;
			}
			else
			{
				return 1f;
			}
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
				return -1f;
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

	/**
	 * Get the idle count.<br />
	 * A negative count means the number of tasks await.<br />
	 * If the pool was not defined by limit then null will be returned.
	 * 
	 * @return
	 */
	public Integer getIdles()
	{
		lock.readLock().lock();
		try
		{
			return this.getIdlesUnsafe();
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	protected Integer getIdlesUnsafe()
	{
		Integer limit = this.getLimitUnsafe();

		if (limit != null)
		{
			return limit - this.getRemainUnsafe();
		}
		else
		{
			return null;
		}
	}

	public Integer getLimit()
	{
		lock.readLock().lock();
		try
		{
			return this.getLimitUnsafe();
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	protected Integer getLimitUnsafe()
	{
		return limit;
	}

	/**
	 * The total number of tasks in pool.
	 * 
	 * @return
	 */
	public Integer getRemain()
	{
		lock.readLock().lock();
		try
		{
			return this.getRemainUnsafe();
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	protected Integer getRemainUnsafe()
	{
		return this.getTasksUnsafe() + fakes;
	}

	/**
	 * The total number of tasks submitted to the underlying completion service.
	 * 
	 * @return
	 */
	public Integer getTasks()
	{
		lock.readLock().lock();
		try
		{
			return this.getTasksUnsafe();
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	protected Integer getTasksUnsafe()
	{
		return tasks.value;
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
		lock.writeLock().lock();
		try
		{
			if (tasks.value > 0)
			{
				tasks.value--;
				fakes++;
			}
			else
			{
				return null;
			}
		}
		finally
		{
			lock.writeLock().unlock();
		}

		try
		{
			return completionService.take();
		}
		catch (InterruptedException e)
		{
			this.addTask();
		}
		finally
		{
			lock.writeLock().lock();
			try
			{
				fakes--;
			}
			finally
			{
				lock.writeLock().unlock();
			}
		}

		return null;
	}

	public boolean isEmpty()
	{
		return this.getRemain() == 0;
	}

	public boolean isShutdown()
	{
		return executorService.isShutdown();
	}

	public boolean isTerminated()
	{
		return executorService.isTerminated();
	}

	/**
	 * Waiting until all the tasks currently in this pool were end.
	 * 
	 * @throws ExecutionException
	 */
	public void join() throws ExecutionException
	{
		while (!this.isEmpty() && !this.isTerminated())
		{
			try
			{
				this.take().get();
			}
			catch (InterruptedException e)
			{
			}
		}
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

	protected void setCompletionService(CompletionService<V> completionService)
	{
		this.completionService = completionService;
	}

	protected void setExecutorService(ExecutorService executorService)
	{
		this.executorService = executorService;

		if (executorService instanceof ThreadPoolExecutor)
		{
			this.limit = ((ThreadPoolExecutor) executorService).getMaximumPoolSize();
		}
	}

	public void setLimit(int limit)
	{
		if (limit >= 0)
		{
			if (executorService instanceof ThreadPoolExecutor)
			{
				lock.writeLock().lock();
				try
				{
					ThreadPoolExecutor service = (ThreadPoolExecutor) executorService;

					service.setCorePoolSize(limit);
					service.setMaximumPoolSize(limit);

					this.limit = limit;
				}
				finally
				{
					lock.writeLock().unlock();
				}
			}
		}
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
