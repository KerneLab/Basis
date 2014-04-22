package org.kernelab.basis;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractPool<E> implements Pool<E>
{
	private List<E>	elements;

	private int		trace;

	private int		limit;

	private boolean	lazy;

	private boolean	closed;

	public AbstractPool(int limit)
	{
		this(limit, true);
	}

	public AbstractPool(int limit, boolean lazy)
	{
		this(new LinkedList<E>(), limit, true);
	}

	protected AbstractPool(List<E> pool, int limit, boolean lazy)
	{
		this.setClosed(false);
		this.setElements(pool);
		this.setTrace(0);
		this.setLazy(lazy);
		this.setLimit(limit);
	}

	/**
	 * Close the pool which would stop waiting the provide request and attempt
	 * to provide with the current elements.
	 */
	public void close()
	{
		synchronized (elements)
		{
			this.setClosed(true);

			elements.notifyAll();
		}
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

	public boolean isClosed()
	{
		return closed;
	}

	public boolean isLazy()
	{
		return lazy;
	}

	/**
	 * Create a new element which should not be null. Any null element return by
	 * this method will be ignored which would not increase the trace.
	 * 
	 * @param timeout
	 * @return The new element.
	 */
	protected abstract E newElement(long timeout);

	public E provide(long timeout)
	{
		E element = null;

		synchronized (elements)
		{
			while (elements.isEmpty() && !closed)
			{
				if (trace < limit)
				{
					supplyElement(timeout);
				}
				else
				{
					try
					{
						elements.wait(timeout);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}

				if (timeout != 0L)
				{
					break;
				}
			}

			if (!elements.isEmpty())
			{
				element = elements.remove(0);
			}
		}

		return element;
	}

	public void recycle(E element)
	{
		if (element != null)
		{
			synchronized (elements)
			{
				elements.add(element);
				elements.notifyAll();
			}
		}
	}

	private void setClosed(boolean closed)
	{
		this.closed = closed;
	}

	private AbstractPool<E> setElements(List<E> elements)
	{
		this.elements = elements;
		return Tools.cast(this);
	}

	private AbstractPool<E> setLazy(boolean lazy)
	{
		this.lazy = lazy;
		return Tools.cast(this);
	}

	public AbstractPool<E> setLimit(int limit)
	{
		this.limit = Math.max(limit, 1);

		if (!lazy && trace < this.limit)
		{
			synchronized (elements)
			{
				for (int i = trace; i < this.limit; i++)
				{
					supplyElement(0);
				}
				elements.notifyAll();
			}
		}

		return Tools.cast(this);
	}

	private AbstractPool<E> setTrace(int trace)
	{
		this.trace = trace;
		return Tools.cast(this);
	}

	public int size()
	{
		synchronized (elements)
		{
			return elements.size();
		}
	}

	protected void supplyElement(long timeout)
	{
		E element = newElement(timeout);
		if (element != null)
		{
			elements.add(element);
			trace++;
		}
	}
}
