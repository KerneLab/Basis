package org.kernelab.basis;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class WrappedHashSet<E> implements Set<E>, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1848904484643612918L;

	public static interface Hasher<T>
	{
		public boolean equals(T a, T b);

		public int hashCode(T value);
	}

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
			return iter.next().value;
		}

		@Override
		public void remove()
		{
			iter.remove();
		}
	}

	protected class Wrapper
	{
		protected final E value;

		public Wrapper(E value)
		{
			this.value = value;
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

			if (!(obj instanceof WrappedHashSet.Wrapper))
			{
				return false;
			}

			@SuppressWarnings("unchecked")
			Wrapper other = (WrappedHashSet<E>.Wrapper) obj;

			if (value == other.value)
			{
				return true;
			}

			if (value == null || other.value == null)
			{
				return false;
			}

			return hasher.equals(value, other.value);
		}

		@Override
		public int hashCode()
		{
			return value == null ? 0 : hasher.hashCode(value);
		}
	}

	protected static final int			DEFAULT_CAPACITY	= 16;

	protected static final float		DEFAULT_LOAD_FACTOR	= 0.75f;

	protected final Hasher<E>			hasher;

	protected final HashSet<Wrapper>	set;

	public WrappedHashSet(Hasher<E> hasher)
	{
		this(hasher, DEFAULT_CAPACITY);
	}

	public WrappedHashSet(Hasher<E> hasher, Collection<? extends E> c)
	{
		this(hasher, Math.max((int) (c.size() / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_CAPACITY));
		addAll(c);
	}

	public WrappedHashSet(Hasher<E> hasher, int initialCapacity)
	{
		this(hasher, initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	public WrappedHashSet(Hasher<E> hasher, int initialCapacity, float loadFactor)
	{
		this.hasher = hasher;
		this.set = newHashSet(initialCapacity, loadFactor);
	}

	@Override
	public boolean add(E e)
	{
		return set.add(wrap(e));
	}

	@Override
	public boolean addAll(Collection<? extends E> c)
	{
		boolean mod = false;
		for (E e : c)
		{
			if (add(e))
			{
				mod = true;
			}
		}
		return mod;
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

	@Override
	public void clear()
	{
		set.clear();
	}

	@Override
	public boolean contains(Object o)
	{
		return set.contains(wrap(cast(o)));
	}

	@Override
	public boolean containsAll(Collection<?> c)
	{
		for (Object e : c)
		{
			if (!contains(e))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isEmpty()
	{
		return set.isEmpty();
	}

	@Override
	public Iterator<E> iterator()
	{
		return new WrappedIterator(set.iterator());
	}

	protected HashSet<Wrapper> newHashSet(int initialCapacity, float loadFactor)
	{
		return new HashSet<Wrapper>(initialCapacity, loadFactor);
	}

	@Override
	public boolean remove(Object o)
	{
		try
		{
			return set.remove(wrap(cast(o)));
		}
		catch (Exception e)
		{
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> c)
	{
		boolean mod = false;

		for (Object e : c)
		{
			if (remove(e))
			{
				mod = true;
			}
		}

		return mod;
	}

	@Override
	public boolean retainAll(Collection<?> c)
	{
		boolean mod = false;
		Iterator<E> i = iterator();
		while (i.hasNext())
		{
			if (!c.contains(i.next()))
			{
				i.remove();
				mod = true;
			}
		}
		return mod;
	}

	@Override
	public int size()
	{
		return set.size();
	}

	@Override
	public Object[] toArray()
	{
		Object[] arr = new Object[size()];

		int i = 0;
		for (E e : this)
		{
			arr[i++] = e;
		}

		return arr;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] arr)
	{
		int size = size();

		T[] dst = arr.length >= size ? arr
				: (T[]) java.lang.reflect.Array.newInstance(arr.getClass().getComponentType(), size);

		Iterator<E> iter = iterator();

		for (int i = 0; i < dst.length; i++)
		{
			if (iter.hasNext())
			{
				dst[i] = (T) iter.next();
			}
			else
			{
				if (arr != dst)
				{
					return Arrays.copyOf(dst, i);
				}
				else
				{
					dst[i] = null;
				}
			}
		}

		return dst;
	}

	protected Wrapper wrap(E e)
	{
		return new Wrapper(e);
	}
}
