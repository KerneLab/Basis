package org.kernelab.basis;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPoolDirector extends ThreadPoolExecutor
{
	public static class AbortPolicy implements RejectedExecutionHandler
	{
		public AbortPolicy()
		{
		}

		public void rejectedExecution(Runnable r, ThreadPoolExecutor e)
		{
			throw new RejectedExecutionException();
		}
	}

	public static class CallerRunsPolicy implements RejectedExecutionHandler
	{
		public CallerRunsPolicy()
		{
		}

		public void rejectedExecution(Runnable r, ThreadPoolExecutor e)
		{
			if (!e.isShutdown())
			{
				r.run();
			}
		}
	}

	public static class DiscardOldestPolicy implements RejectedExecutionHandler
	{
		public DiscardOldestPolicy()
		{
		}

		public void rejectedExecution(Runnable r, ThreadPoolExecutor e)
		{
			if (!e.isShutdown())
			{
				e.getQueue().poll();
				e.execute(r);
			}
		}
	}

	public static class DiscardPolicy implements RejectedExecutionHandler
	{
		public DiscardPolicy()
		{
		}

		public void rejectedExecution(Runnable r, ThreadPoolExecutor e)
		{
		}
	}

	private class Worker implements Runnable
	{
		private final ReentrantLock	runLock	= new ReentrantLock();

		private Runnable			firstTask;

		volatile long				completedTasks;

		Thread						thread;

		Worker(Runnable firstTask)
		{
			this.firstTask = firstTask;
		}

		void interruptIfIdle()
		{
			final ReentrantLock runLock = this.runLock;
			if (runLock.tryLock())
			{
				try
				{
					if (thread != Thread.currentThread())
					{
						thread.interrupt();
					}
				}
				finally
				{
					runLock.unlock();
				}
			}
		}

		void interruptNow()
		{
			thread.interrupt();
		}

		boolean isActive()
		{
			return runLock.isLocked();
		}

		public void run()
		{
			try
			{
				Runnable task = firstTask;
				firstTask = null;
				while (task != null || (task = takeTask()) != null)
				{
					runTask(task);
					task = null;
				}
			}
			catch (InterruptedException ie)
			{
			}
			finally
			{
				workerDone(this);
			}
		}

		private void runTask(Runnable task)
		{
			final ReentrantLock runLock = this.runLock;
			runLock.lock();
			try
			{
				if (runState == STOP)
				{
					return;
				}

				Thread.interrupted();
				boolean ran = false;
				beforeExecute(thread, task);
				try
				{
					task.run();
					ran = true;
					afterExecute(task, null);
					++completedTasks;
				}
				catch (RuntimeException ex)
				{
					if (!ran)
					{
						afterExecute(task, ex);
					}
					throw ex;
				}
			}
			finally
			{
				runLock.unlock();
			}
		}
	}

	private static final Runnable[]					EMPTY_RUNNABLE_ARRAY	= new Runnable[0];

	private static final RuntimePermission			shutdownPerm			= new RuntimePermission("modifyThread");

	static final int								RUNNING					= 0;

	static final int								SHUTDOWN				= 1;

	static final int								STOP					= 2;

	static final int								TERMINATED				= 3;

	private static final RejectedExecutionHandler	defaultHandler			= new AbortPolicy();

	private final BlockingQueue<Runnable>			workQueue;

	private final ReentrantLock						mainLock				= new ReentrantLock();

	private final Condition							termination				= mainLock.newCondition();

	private final HashSet<Worker>					workers					= new HashSet<Worker>();

	private volatile long							keepAliveTime;

	private volatile int							corePoolSize;

	private volatile int							maximumPoolSize;

	private volatile int							poolSize;

	volatile int									runState;

	private volatile RejectedExecutionHandler		handler;

	private volatile ThreadFactory					threadFactory;

	private int										largestPoolSize;

	private long									completedTaskCount;

	public ThreadPoolDirector(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue)
	{
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(),
				defaultHandler);
	}

	public ThreadPoolDirector(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler)
	{
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), handler);
	}

	public ThreadPoolDirector(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory)
	{
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, defaultHandler);
	}

	public ThreadPoolDirector(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler)
	{
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);

		if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize || keepAliveTime < 0)
		{
			throw new IllegalArgumentException();
		}
		if (workQueue == null || threadFactory == null || handler == null)
		{
			throw new NullPointerException();
		}
		this.corePoolSize = corePoolSize;
		this.maximumPoolSize = maximumPoolSize;
		this.workQueue = workQueue;
		this.keepAliveTime = unit.toNanos(keepAliveTime);
		this.threadFactory = threadFactory;
		this.handler = handler;
	}

	private boolean addIfUnderCorePoolSize(Runnable firstTask)
	{
		Thread t = null;
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			if (poolSize < corePoolSize)
			{
				t = addThread(firstTask);
			}
		}
		finally
		{
			mainLock.unlock();
		}
		if (t == null)
		{
			return false;
		}
		t.start();
		return true;
	}

	private Runnable addIfUnderMaximumPoolSize(Runnable firstTask)
	{
		Thread t = null;
		Runnable next = null;
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			if (poolSize < maximumPoolSize)
			{
				next = workQueue.poll();
				if (next == null)
				{
					next = firstTask;
				}
				t = addThread(next);
			}
		}
		finally
		{
			mainLock.unlock();
		}
		if (t == null)
		{
			return null;
		}
		t.start();
		return next;
	}

	private Thread addThread(Runnable firstTask)
	{
		Worker w = new Worker(firstTask);
		Thread t = threadFactory.newThread(w);
		if (t != null)
		{
			w.thread = t;
			workers.add(w);
			int nt = ++poolSize;
			if (nt > largestPoolSize)
			{
				largestPoolSize = nt;
			}
		}
		return t;
	}

	protected void afterExecute(Runnable r, Throwable t)
	{
	}

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
	{
		long nanos = unit.toNanos(timeout);
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			while (true)
			{
				if (runState == TERMINATED)
				{
					return true;
				}
				if (nanos <= 0)
				{
					return false;
				}
				nanos = termination.awaitNanos(nanos);
			}
		}
		finally
		{
			mainLock.unlock();
		}
	}

	protected void beforeExecute(Thread t, Runnable r)
	{
	}

	void doReject(Runnable command)
	{
		handler.rejectedExecution(command, this);
	}

	public void execute(Runnable command)
	{
		if (command == null)
		{
			throw new NullPointerException();
		}
		while (true)
		{
			if (runState != RUNNING)
			{
				doReject(command);
				return;
			}

			if (poolSize < corePoolSize && addIfUnderCorePoolSize(command))
			{
				return;
			}
			if (workQueue.offer(command))
			{
				return;
			}
			Runnable r = addIfUnderMaximumPoolSize(command);
			if (r == command)
			{
				return;
			}
			if (r == null)
			{
				doReject(command);
				return;
			}
		}
	}

	protected void finalize()
	{
		shutdown();
	}

	public int getActiveCount()
	{
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			int n = 0;
			for (Worker w : workers)
			{
				if (w.isActive())
				{
					++n;
				}
			}
			return n;
		}
		finally
		{
			mainLock.unlock();
		}
	}

	public long getCompletedTaskCount()
	{
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			long n = completedTaskCount;
			for (Worker w : workers)
			{
				n += w.completedTasks;
			}
			return n;
		}
		finally
		{
			mainLock.unlock();
		}
	}

	public int getCorePoolSize()
	{
		return corePoolSize;
	}

	public long getKeepAliveTime(TimeUnit unit)
	{
		return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
	}

	public int getLargestPoolSize()
	{
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			return largestPoolSize;
		}
		finally
		{
			mainLock.unlock();
		}
	}

	public int getMaximumPoolSize()
	{
		return maximumPoolSize;
	}

	public int getPoolSize()
	{
		return poolSize;
	}

	public BlockingQueue<Runnable> getQueue()
	{
		return workQueue;
	}

	public RejectedExecutionHandler getRejectedExecutionHandler()
	{
		return handler;
	}

	public long getTaskCount()
	{
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			long n = completedTaskCount;
			for (Worker w : workers)
			{
				n += w.completedTasks;
				if (w.isActive())
				{
					++n;
				}
			}
			return n + workQueue.size();
		}
		finally
		{
			mainLock.unlock();
		}
	}

	public ThreadFactory getThreadFactory()
	{
		return threadFactory;
	}

	public boolean isShutdown()
	{
		return runState != RUNNING;
	}

	public boolean isTerminated()
	{
		return runState == TERMINATED;
	}

	public boolean isTerminating()
	{
		return runState == STOP;
	}

	public int prestartAllCoreThreads()
	{
		int n = 0;
		while (addIfUnderCorePoolSize(null))
		{
			++n;
		}
		return n;
	}

	public boolean prestartCoreThread()
	{
		return addIfUnderCorePoolSize(null);
	}

	public void purge()
	{
		try
		{
			Iterator<Runnable> it = getQueue().iterator();
			while (it.hasNext())
			{
				Runnable r = it.next();
				if (r instanceof Future<?>)
				{
					Future<?> c = (Future<?>) r;
					if (c.isCancelled())
					{
						it.remove();
					}
				}
			}
		}
		catch (ConcurrentModificationException ex)
		{
			return;
		}
	}

	public boolean remove(Runnable task)
	{
		return getQueue().remove(task);
	}

	public void setCorePoolSize(int corePoolSize)
	{
		if (corePoolSize < 0)
		{
			throw new IllegalArgumentException();
		}
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			int extra = this.corePoolSize - corePoolSize;
			this.corePoolSize = corePoolSize;
			if (extra < 0)
			{
				int n = workQueue.size();
				while (extra++ < 0 && n-- > 0 && poolSize < corePoolSize)
				{
					Thread t = addThread(null);
					if (t != null)
					{
						t.start();
					}
					else
					{
						break;
					}
				}
			}
			else if (extra > 0 && poolSize > corePoolSize)
			{
				try
				{
					Iterator<Worker> it = workers.iterator();
					while (it.hasNext() && extra-- > 0 && poolSize > corePoolSize)
					{
						it.next().interruptIfIdle();
					}
				}
				catch (SecurityException ignore)
				{
				}
			}
		}
		finally
		{
			mainLock.unlock();
		}
	}

	public void setKeepAliveTime(long time, TimeUnit unit)
	{
		if (time < 0)
		{
			throw new IllegalArgumentException();
		}
		this.keepAliveTime = unit.toNanos(time);
	}

	public void setMaximumPoolSize(int maximumPoolSize)
	{
		if (maximumPoolSize < 0 || maximumPoolSize < corePoolSize)
		{
			throw new IllegalArgumentException();
		}
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			int extra = this.maximumPoolSize - maximumPoolSize;
			this.maximumPoolSize = maximumPoolSize;
			if (extra > 0 && poolSize > maximumPoolSize)
			{
				try
				{
					Iterator<Worker> it = workers.iterator();
					while (it.hasNext() && extra > 0 && poolSize > maximumPoolSize)
					{
						it.next().interruptIfIdle();
						--extra;
					}
				}
				catch (SecurityException ignore)
				{
				}
			}
		}
		finally
		{
			mainLock.unlock();
		}
	}

	public void setRejectedExecutionHandler(RejectedExecutionHandler handler)
	{
		if (handler == null)
		{
			throw new NullPointerException();
		}
		this.handler = handler;
	}

	public void setThreadFactory(ThreadFactory threadFactory)
	{
		if (threadFactory == null)
		{
			throw new NullPointerException();
		}
		this.threadFactory = threadFactory;
	}

	public void shutdown()
	{
		SecurityManager security = System.getSecurityManager();
		if (security != null)
		{
			java.security.AccessController.checkPermission(shutdownPerm);
		}

		boolean fullyTerminated = false;
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			if (workers.size() > 0)
			{
				if (security != null)
				{
					for (Worker w : workers)
					{
						security.checkAccess(w.thread);
					}
				}

				int state = runState;
				if (state == RUNNING)
				{
					runState = SHUTDOWN;
				}

				try
				{
					for (Worker w : workers)
					{
						w.interruptIfIdle();
					}
				}
				catch (SecurityException se)
				{
					runState = state;
					throw se;
				}
			}
			else
			{
				fullyTerminated = true;
				runState = TERMINATED;
				termination.signalAll();
			}
		}
		finally
		{
			mainLock.unlock();
		}
		if (fullyTerminated)
		{
			terminated();
		}
	}

	public List<Runnable> shutdownNow()
	{
		SecurityManager security = System.getSecurityManager();
		if (security != null)
		{
			java.security.AccessController.checkPermission(shutdownPerm);
		}

		boolean fullyTerminated = false;
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			if (workers.size() > 0)
			{
				if (security != null)
				{
					for (Worker w : workers)
					{
						security.checkAccess(w.thread);
					}
				}

				int state = runState;
				if (state != TERMINATED)
				{
					runState = STOP;
				}
				try
				{
					for (Worker w : workers)
					{
						w.interruptNow();
					}
				}
				catch (SecurityException se)
				{
					runState = state;
					throw se;
				}
			}
			else
			{
				fullyTerminated = true;
				runState = TERMINATED;
				termination.signalAll();
			}
		}
		finally
		{
			mainLock.unlock();
		}
		if (fullyTerminated)
		{
			terminated();
		}
		return Arrays.asList(workQueue.toArray(EMPTY_RUNNABLE_ARRAY));
	}

	Runnable takeTask() throws InterruptedException
	{
		while (true)
		{
			switch (runState)
			{
				case RUNNING:
				{
					if (poolSize <= corePoolSize)
					{
						return workQueue.take();
					}

					long timeout = keepAliveTime;
					if (timeout <= 0)
					{
						return null;
					}
					Runnable r = workQueue.poll(timeout, TimeUnit.NANOSECONDS);
					if (r != null)
					{
						return r;
					}
					if (poolSize > corePoolSize)
					{
						return null;
					}
					break;
				}

				case SHUTDOWN:
				{
					Runnable r = workQueue.poll();
					if (r != null)
					{
						return r;
					}

					if (workQueue.isEmpty())
					{
						wakeupIdleWorkers();
						return null;
					}

					try
					{
						return workQueue.take();
					}
					catch (InterruptedException ignore)
					{
					}
					break;
				}

				case STOP:
					return null;
			}
		}
	}

	protected void terminated()
	{
	}

	void wakeupIdleWorkers()
	{
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			for (Worker w : workers)
			{
				w.interruptIfIdle();
			}
		}
		finally
		{
			mainLock.unlock();
		}
	}

	void workerDone(Worker w)
	{
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		try
		{
			completedTaskCount += w.completedTasks;
			workers.remove(w);
			if (--poolSize > 0)
			{
				return;
			}

			int state = runState;

			if (state != STOP)
			{
				if (!workQueue.isEmpty())
				{
					if (maximumPoolSize > 0)
					{
						Thread t = addThread(null);
						if (t != null)
						{
							t.start();
						}
					}
					return;
				}

				if (state == RUNNING)
				{
					return;
				}
			}

			termination.signalAll();
			runState = TERMINATED;
		}
		finally
		{
			mainLock.unlock();
		}

		terminated();
	}
}
