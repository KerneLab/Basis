package org.kernelab.basis;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractPool<E> implements Pool<E>
{
	private List<E>	elements;

	private int		trace;

	private int		limit;

	private int		init;

	private boolean	closed;

	public AbstractPool(int limit)
	{
		this(limit, 0);
	}

	public AbstractPool(int limit, int init)
	{
		this(new LinkedList<E>(), limit, init);
	}

	protected AbstractPool(List<E> pool, int limit, int init)
	{
		this.setClosed(false);
		this.setElements(pool);
		this.setTrace(0);
		this.setLimit(limit);
		this.setInit(0);
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

	public int getInit()
	{
		return init;
	}

	public int getLimit()
	{
		return limit;
	}

	public int getRemain()
	{
		return elements.size();
	}

	public int getTrace()
	{
		return trace;
	}

	public boolean isClosed()
	{
		return closed;
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

	private AbstractPool<E> setInit(int init)
	{
		this.init = Tools.limitNumber(init, 0, this.getLimit());

		if (this.init > 0 && trace < this.init)
		{
			for (int i = trace; i < this.init; i++)
			{
				supplyElement(0);
			}
		}

		return Tools.cast(this);
	}

	public AbstractPool<E> setLimit(int limit)
	{
		this.limit = Math.max(limit, 1);

		return Tools.cast(this);
	}

	private AbstractPool<E> setTrace(int trace)
	{
		this.trace = trace;
		return Tools.cast(this);
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
