package org.kernelab.basis;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractPool<E> implements Pool<E>
{
	private List<E>	pool;

	private int		count;

	private int		limit;

	private boolean	lazy;

	public AbstractPool(int limit)
	{
		this(limit, true);
	}

	public AbstractPool(int limit, boolean lazy)
	{
		this(new LinkedList<E>(), limit, lazy);
	}

	protected AbstractPool(List<E> pool, int limit, boolean lazy)
	{
		this.pool = pool;
		this.count = 0;
		this.limit = Math.max(limit, 1);
		this.lazy = lazy;

		this.init();
	}

	public int getLimit()
	{
		return limit;
	}

	public void giveBack(E element)
	{
		synchronized (pool)
		{
			pool.add(element);
			pool.notifyAll();
		}
	}

	private void init()
	{
		if (!lazy)
		{
			for (int i = 0; i < limit; i++)
			{
				pool.add(newElement());
			}
			count = limit;
		}
	}

	public boolean isLazy()
	{
		return lazy;
	}

	protected abstract E newElement();

	public AbstractPool<E> setLimit(int limit)
	{
		this.limit = limit;
		return Tools.cast(this);
	}

	public E takeAway()
	{
		E element = null;

		do
		{
			synchronized (pool)
			{
				if (pool.isEmpty())
				{
					if (count < limit)
					{
						element = newElement();
						count++;
					}
					else
					{
						try
						{
							pool.wait();
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
				else
				{
					element = pool.remove(0);
				}
			}
		} while (element == null);

		return element;
	}
}
