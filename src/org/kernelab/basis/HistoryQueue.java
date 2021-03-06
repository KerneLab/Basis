package org.kernelab.basis;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HistoryQueue<E> extends AbstractQueue<E> implements Queue<E>, Serializable
{
	public static interface ReserveRule<E> extends Serializable
	{
		public boolean reserve(E el);
	}

	/**
	 * 
	 */
	private static final long		serialVersionUID	= -7352500245739585719L;

	private Queue<E>				queue;

	private int						history;

	private ReserveRule<E>			reserve;

	protected final ReadWriteLock	lock				= new ReentrantReadWriteLock();

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

			this.truncateBatch(true);

			return added;
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	public int clean(boolean reserved)
	{
		this.lock.writeLock().lock();
		try
		{
			return this.truncate(reserved, -1, null).size();
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
			this.getQueue().clear();
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	protected Collection<E> fetch(int limit, boolean removing, boolean reserved, Collection<E> result)
	{
		Iterator<E> iter = this.getQueue().iterator();

		ReserveRule<E> reserve = reserved ? this.getReserve() : null;

		if (result == null)
		{
			result = new LinkedList<E>();
		}

		E el;

		int rest = limit;

		while (iter.hasNext() && (rest > 0 || limit < 0))
		{
			el = iter.next();

			if (reserve == null || !reserve.reserve(el))
			{
				if (removing)
				{
					iter.remove();
				}
				result.add(el);
				rest--;
			}
		}

		return result;
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

	public Collection<E> look(int limit, boolean reserved, Collection<E> result)
	{
		this.lock.readLock().lock();
		try
		{
			return this.fetch(limit, false, reserved, result);
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

			this.truncateOne(true);

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

			this.truncateBatch(true);
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

	public Collection<E> take(int limit, boolean reserved, Collection<E> result)
	{
		this.lock.writeLock().lock();
		try
		{
			return this.truncate(reserved, limit, result);
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	protected Collection<E> truncate(boolean reserved, int limit, Collection<E> result)
	{
		return this.fetch(limit, true, reserved, result);
	}

	protected Collection<E> truncateBatch(boolean reserved)
	{
		int limit = 0;

		if (this.getHistory(false) >= 0 && (limit = this.size(false) - this.getHistory(false)) > 0)
		{
			return this.truncate(reserved, limit, null);
		}
		else
		{
			return Collections.emptyList();
		}
	}

	protected Collection<E> truncateOne(boolean reserved)
	{
		if (this.getHistory(false) >= 0 && this.size(false) - this.getHistory(false) > 0)
		{
			return this.truncate(reserved, 1, null);
		}
		else
		{
			return Collections.emptyList();
		}
	}
}
