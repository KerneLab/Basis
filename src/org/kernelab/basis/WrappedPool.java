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

	public WrappedPool(int limit, boolean lazy)
	{
		super(limit, lazy);
	}

	@Override
	protected Element<E> newElement()
	{
		return new Element<E>(this, newElementInstance());
	}

	protected abstract E newElementInstance();
}
