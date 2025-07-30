package org.kernelab.basis;

import java.io.Serializable;
import java.util.Iterator;

public abstract class WrappedContainer<E>
{
	protected class WrappedIterator implements Iterator<E>
	{
		protected final Iterator<Wrapper> iter;

		public WrappedIterator(Iterator<Wrapper> iter)
		{
			this.iter = iter;
		}

		@Override
		public boolean hasNext()
		{
			return iter.hasNext();
		}

		@Override
		public E next()
		{
			return iter.next().data;
		}

		@Override
		public void remove()
		{
			iter.remove();
		}
	}

	protected class Wrapper implements Serializable
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= 1L;

		protected final E			data;

		public Wrapper(E data)
		{
			this.data = data;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}

			if (obj == null)
			{
				return false;
			}

			if (!(obj instanceof WrappedContainer.Wrapper))
			{
				return false;
			}

			@SuppressWarnings("unchecked")
			Wrapper other = (WrappedContainer<E>.Wrapper) obj;

			if (this.data == other.data)
			{
				return true;
			}

			if (this.data == null || other.data == null)
			{
				return false;
			}

			return equal.equals(this.data, other.data);
		}

		@Override
		public int hashCode()
		{
			return data != null ? equal.hashCode(data) : 0;
		}
	}

	protected final HashedEquality<? super E> equal;

	public WrappedContainer(HashedEquality<? super E> equal)
	{
		this.equal = equal;
	}

	@SuppressWarnings("unchecked")
	protected E cast(Object e)
	{
		try
		{
			return (E) e;
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	protected Wrapper wrap(E e)
	{
		return new Wrapper(e);
	}
}
