package org.kernelab.basis;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class WrappedHashSet<E> extends WrappedContainer<E> implements Set<E>, Serializable
{
	/**
	 * 
	 */
	private static final long			serialVersionUID	= -7304948358489956579L;

	protected final HashSet<Wrapper>	set;

	public WrappedHashSet(HashedEquality<? super E> equal)
	{
		this(equal, WrappedHashMap.DEFAULT_CAPACITY);
	}

	public WrappedHashSet(HashedEquality<? super E> equal, Collection<? extends E> c)
	{
		this(equal, Math.max((int) (c.size() / WrappedHashMap.DEFAULT_LOAD_FACTOR) + 1, //
				WrappedHashMap.DEFAULT_CAPACITY));
		addAll(c);
	}

	public WrappedHashSet(HashedEquality<? super E> equal, int initialCapacity)
	{
		this(equal, initialCapacity, WrappedHashMap.DEFAULT_LOAD_FACTOR);
	}

	public WrappedHashSet(HashedEquality<? super E> equal, int initialCapacity, float loadFactor)
	{
		super(equal);
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
}
