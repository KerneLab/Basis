package org.kernelab.basis;

import org.kernelab.basis.WrappedPool.Element;

public abstract class WrappedPool<E> extends AbstractPool<Element<E>>
{
	public static class Element<E>
	{
		private WrappedPool<E>	pool;

		private E				element;

		public Element(WrappedPool<E> pool, E element)
		{
			this.pool = pool;
			this.element = element;
		}

		public E get()
		{
			return element;
		}

		public void release()
		{
			pool.recycle(this);
		}
	}

	public WrappedPool(int limit)
	{
		super(limit);
	}

	public WrappedPool(int limit, int init)
	{
		super(limit, init);
	}

	@Override
	protected Element<E> newElement(long timeout)
	{
		return new Element<E>(this, newElementInstance(timeout));
	}

	protected abstract E newElementInstance(long timeout);
}
