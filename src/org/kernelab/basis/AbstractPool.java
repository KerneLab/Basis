package org.kernelab.basis;

import java.util.LinkedList;
import java.util.Queue;

public abstract class AbstractPool<E> implements Pool<E>
{
	public static final int	DEFAULT_INTERVAL	= 100;

	private Queue<E>		elements;

	private int				trace;

	private int				limit;

	private int				init				= -1;

	private boolean			closed;

	public AbstractPool(int limit)
	{
		this(limit, 0);
	}

	public AbstractPool(int limit, int init)
	{
		this(new LinkedList<E>(), limit, init);
	}

	protected AbstractPool(Queue<E> pool, int limit, int init)
	{
		this.setClosed(false).setElements(pool).setTrace(0).setLimit(limit).setInit(init);
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

	public void discard(E element)
	{
		if (element != null)
		{
			synchronized (elements)
			{
				if (trace > 0)
				{
					trace--;
				}
			}
		}
	}

	public float getBusy()
	{
		return (this.getTrace() - this.getRemain()) * 1f / this.getLimit();
	}

	protected Queue<E> getElements()
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
	 * Check the given element is valid or not. Any invalid element would be
	 * discarded, not provided nor recycled.
	 * 
	 * @param element
	 *            the given element to be checked.
	 * @return true if and only if the given element is valid.
	 */
	protected abstract boolean isValid(E element);

	/**
	 * Create a new element which MUST NOT be null. Any null element return by
	 * this method will be ignored which would not increase the trace.
	 * 
	 * @param timeout
	 * @return The new element.
	 */
	protected abstract E newElement(long timeout) throws Exception;

	public E provide(long timeout) throws Exception
	{
		E element = null;

		synchronized (elements)
		{
			do
			{
				while (elements.isEmpty() && !isClosed())
				{
					if (trace < limit)
					{
						supplyElement(timeout);
					}
					else
					{
						try
						{
							if (timeout > 0)
							{
								elements.wait(timeout);
							}
							else if (timeout == 0)
							{
								elements.wait(DEFAULT_INTERVAL);
							}
							else
							{
								elements.wait(-timeout);
							}
						}
						catch (InterruptedException e)
						{
						}
					}

					if (timeout > 0)
					{
						break;
					}
				}

				element = elements.poll();

				if (element == null && timeout > 0)
				{
					break;
				}
				else if (element != null)
				{
					if (isValid(element))
					{
						break;
					}
					else
					{
						discard(element);
						element = null;
					}
				}
			}
			while (!isClosed());
		}

		return element;
	}

	public void recycle(E element)
	{
		if (element != null)
		{
			synchronized (elements)
			{
				if (trace <= limit && isValid(element))
				{
					try
					{
						resetElement(element);
						elements.add(element);
						elements.notifyAll();
					}
					catch (Exception e)
					{
						discard(element);
					}
				}
				else
				{
					discard(element);
				}
			}
		}
	}

	/**
	 * Reset the element before recycled. The default reset action does nothing.
	 * Any subclass may override this method to define the reset action. If any
	 * exception was thrown, the element would be discarded.
	 * 
	 * @param element
	 * @throws Exception
	 */
	protected void resetElement(E element) throws Exception
	{
	}

	private AbstractPool<E> setClosed(boolean closed)
	{
		this.closed = closed;
		return Tools.cast(this);
	}

	private AbstractPool<E> setElements(Queue<E> elements)
	{
		this.elements = elements;
		return Tools.cast(this);
	}

	/**
	 * To initialize the pool with a given initial size.<br />
	 * It is recommended that this method be called in constructor. So that,
	 * Subclass should override this method to ensure the dependent resources
	 * were available first which is need to supply new elements. And then,
	 * subclass should call this method after these resources were available.
	 * 
	 * @param init
	 * @return
	 */
	protected AbstractPool<E> setInit(int init)
	{
		if (this.init < 0)
		{
			this.init = Tools.limitNumber(init, 0, this.getLimit());

			if (this.init > 0 && this.trace < this.init)
			{
				try
				{
					for (int i = this.trace; i < this.init; i++)
					{
						supplyElement(DEFAULT_INTERVAL);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return Tools.cast(this);
	}

	public AbstractPool<E> setLimit(int limit)
	{
		synchronized (elements)
		{
			limit = Math.max(limit, 0);

			int delta = this.limit - limit;

			if (delta > 0)
			{
				for (int i = 0; i < delta && !elements.isEmpty(); i++)
				{
					discard(elements.poll());
				}
			}

			this.limit = limit;
		}

		return Tools.cast(this);
	}

	private AbstractPool<E> setTrace(int trace)
	{
		this.trace = trace;
		return Tools.cast(this);
	}

	protected void supplyElement(long timeout) throws Exception
	{
		E element = newElement(timeout);
		if (element != null)
		{
			synchronized (elements)
			{
				elements.add(element);
				trace++;
			}
		}
	}

	public AbstractPool<E> wakeUp()
	{
		synchronized (elements)
		{
			elements.notifyAll();
		}
		return Tools.cast(this);
	}
}
