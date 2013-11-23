package org.kernelab.basis;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractPool<E> implements Pool<E>
{
	private List<E>	elements;

	private int		trace;

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
		this.elements = pool;
		this.trace = 0;
		this.limit = Math.max(limit, 1);
		this.lazy = lazy;

		this.init();
	}

	protected List<E> getElements()
	{
		return elements;
	}

	public int getLimit()
	{
		return limit;
	}

	public int getTrace()
	{
		return trace;
	}

	protected void init()
	{
		if (!lazy)
		{
			for (int i = 0; i < limit; i++)
			{
				elements.add(newElement());
			}
			trace = limit;
		}
	}

	public boolean isLazy()
	{
		return lazy;
	}

	protected abstract E newElement();

	public E provide()
	{
		E element = null;

		do
		{
			synchronized (elements)
			{
				if (elements.isEmpty())
				{
					if (trace < limit)
					{
						element = newElement();
						trace++;
					}
					else
					{
						try
						{
							elements.wait();
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
				else
				{
					element = elements.remove(0);
				}
			}
		} while (element == null);

		return element;
	}

	public void recycle(E element)
	{
		synchronized (elements)
		{
			elements.add(element);
			elements.notifyAll();
		}
	}

	public AbstractPool<E> setLimit(int limit)
	{
		this.limit = limit;
		return Tools.cast(this);
	}

	public int size()
	{
		synchronized (elements)
		{
			return elements.size();
		}
	}
}
