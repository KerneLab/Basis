package org.kernelab.basis;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HistoryQueue<E> extends AbstractQueue<E> implements Queue<E>
{
	public static interface ReserveRule<E>
	{
		public boolean reserve(E el);
	}

	private Queue<E>				queue;

	private int						history;

	private ReserveRule<E>			reserve;

	protected final ReadWriteLock	lock	= new ReentrantReadWriteLock();

	public HistoryQueue(int history)
	{
		this(history, null);
	}

	public HistoryQueue(int history, ReserveRule<E> reserve)
	{
		this(history, reserve, new LinkedList<E>());
	}

	public HistoryQueue(int history, ReserveRule<E> reserve, Queue<E> queue)
	{
		this.setQueue(queue);
		this.setReserve(reserve);
		this.setHistory(history);
	}

	@Override
	public boolean addAll(Collection<? extends E> c)
	{
		this.lock.writeLock().lock();
		try
		{
			boolean added = this.getQueue().addAll(c);

			this.truncateBatch();

			return added;
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	@Override
	public void clear()
	{
		this.lock.writeLock().lock();
		try
		{
			this.truncate(this.size(false));
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	public int getHistory()
	{
		this.lock.readLock().lock();
		try
		{
			return history;
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	protected int getHistory(boolean sync)
	{
		if (sync)
		{
			return this.getHistory();
		}
		else
		{
			return history;
		}
	}

	protected Queue<E> getQueue()
	{
		return queue;
	}

	public ReserveRule<E> getReserve()
	{
		return reserve;
	}

	@Override
	public Iterator<E> iterator()
	{
		this.lock.readLock().lock();
		try
		{
			return Collections.unmodifiableList(new LinkedList<E>(this.getQueue())).iterator();
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	public boolean offer(E o)
	{
		this.lock.writeLock().lock();
		try
		{
			boolean offered = this.getQueue().offer(o);

			this.truncateOne();

			return offered;
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	public E peek()
	{
		this.lock.readLock().lock();
		try
		{
			return this.getQueue().peek();
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	public E peek(boolean sync)
	{
		if (sync)
		{
			return this.peek();
		}
		else
		{
			return this.getQueue().peek();
		}
	}

	public E poll()
	{
		this.lock.writeLock().lock();
		try
		{
			return this.getQueue().poll();
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	public E poll(boolean sync)
	{
		if (sync)
		{
			return this.poll();
		}
		else
		{
			return this.getQueue().poll();
		}
	}

	public void setHistory(int history)
	{
		this.lock.writeLock().lock();
		try
		{
			this.history = history;

			this.truncateBatch();
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	protected void setQueue(Queue<E> queue)
	{
		this.queue = queue;
	}

	public void setReserve(ReserveRule<E> reserve)
	{
		this.reserve = reserve;
	}

	@Override
	public int size()
	{
		this.lock.readLock().lock();
		try
		{
			return this.getQueue().size();
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	public int size(boolean sync)
	{
		if (sync)
		{
			return this.size();
		}
		else
		{
			return this.getQueue().size();
		}
	}

	protected int truncate(int delta)
	{
		Iterator<E> iter = this.getQueue().iterator();

		ReserveRule<E> reserve = this.getReserve();

		E el;

		int rest = delta;

		while (iter.hasNext() && rest > 0)
		{
			el = iter.next();

			if (reserve == null || !reserve.reserve(el))
			{
				iter.remove();
				rest--;
			}
		}

		return delta - rest;
	}

	protected int truncateBatch()
	{
		int delta = 0;

		if (this.getHistory(false) >= 0 && (delta = this.size(false) - this.getHistory(false)) > 0)
		{
			return this.truncate(delta);
		}
		else
		{
			return -1;
		}
	}

	protected int truncateOne()
	{
		if (this.getHistory(false) >= 0 && this.size(false) - this.getHistory(false) > 0)
		{
			return this.truncate(1);
		}
		else
		{
			return -1;
		}
	}
}
