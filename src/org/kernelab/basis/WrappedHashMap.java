package org.kernelab.basis;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class WrappedHashMap<K, V> extends WrappedContainer<K> implements Map<K, V>, Serializable
{
	protected class EntryIterator implements Iterator<Entry<K, V>>
	{
		protected final Iterator<Entry<Wrapper, V>> iter;

		public EntryIterator(Iterator<Entry<Wrapper, V>> iter)
		{
			this.iter = iter;
		}

		@Override
		public boolean hasNext()
		{
			return iter.hasNext();
		}

		@Override
		public Entry<K, V> next()
		{
			return new WrappedEntry(iter.next());
		}

		@Override
		public void remove()
		{
			iter.remove();
		}
	}

	protected class EntrySet extends AbstractSet<Entry<K, V>>
	{
		@Override
		public Iterator<Entry<K, V>> iterator()
		{
			return new EntryIterator(map.entrySet().iterator());
		}

		@Override
		public int size()
		{
			return map.entrySet().size();
		}
	}

	protected class KeySet extends AbstractSet<K>
	{
		@Override
		public Iterator<K> iterator()
		{
			return new WrappedIterator(map.keySet().iterator());
		}

		@Override
		public int size()
		{
			return map.keySet().size();
		}
	}

	protected class WrappedEntry implements Entry<K, V>
	{
		protected final Entry<Wrapper, V> entry;

		public WrappedEntry(Entry<Wrapper, V> entry)
		{
			this.entry = entry;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}

			if (!(obj instanceof Map.Entry))
			{
				return false;
			}

			Map.Entry<Wrapper, V> other = (Map.Entry<Wrapper, V>) obj;

			K ka = entry.getKey() != null ? entry.getKey().data : null;
			K kb = other.getKey() != null ? other.getKey().data : null;

			return (ka == kb || (ka != null && kb != null && equal.equals(ka, kb))) //
					&& Tools.equals(entry.getValue(), other.getValue());
		}

		@Override
		public K getKey()
		{
			return entry.getKey().data;
		}

		@Override
		public V getValue()
		{
			return entry.getValue();
		}

		@Override
		public int hashCode()
		{
			return (entry.getKey().data == null ? 0 : equal.hashCode(entry.getKey().data)) //
					^ (entry.getValue() == null ? 0 : entry.getValue().hashCode());
		}

		@Override
		public V setValue(V value)
		{
			return entry.setValue(value);
		}
	}

	/**
	 * 
	 */
	private static final long			serialVersionUID	= 4689199851336319754L;

	protected static final int			DEFAULT_CAPACITY	= 16;

	protected static final float		DEFAULT_LOAD_FACTOR	= 0.75f;

	protected final HashMap<Wrapper, V>	map;

	public WrappedHashMap(HashedEquality<K> equal)
	{
		this(equal, DEFAULT_CAPACITY);
	}

	public WrappedHashMap(HashedEquality<K> equal, int initialCapacity)
	{
		this(equal, initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	public WrappedHashMap(HashedEquality<K> equal, int initialCapacity, float loadFactor)
	{
		super(equal);
		this.map = newHashMap(initialCapacity, loadFactor);
	}

	public WrappedHashMap(HashedEquality<K> equal, Map<? extends K, ? extends V> map)
	{
		this(equal);
		putAll(map);
	}

	@Override
	public void clear()
	{
		map.clear();
	}

	@Override
	public boolean containsKey(Object key)
	{
		return map.containsKey(wrap(cast(key)));
	}

	@Override
	public boolean containsValue(Object value)
	{
		return map.containsValue(value);
	}

	@Override
	public Set<Entry<K, V>> entrySet()
	{
		return new EntrySet();
	}

	@Override
	public V get(Object key)
	{
		return map.get(wrap(cast(key)));
	}

	@Override
	public boolean isEmpty()
	{
		return map.isEmpty();
	}

	@Override
	public Set<K> keySet()
	{
		return new KeySet();
	}

	protected HashMap<Wrapper, V> newHashMap(int initialCapacity, float loadFactor)
	{
		return new HashMap<Wrapper, V>(initialCapacity, loadFactor);
	}

	@Override
	public V put(K key, V value)
	{
		return map.put(wrap(key), value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m)
	{
		for (Entry<? extends K, ? extends V> entry : m.entrySet())
		{
			this.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public V remove(Object key)
	{
		return map.remove(wrap(cast(key)));
	}

	@Override
	public int size()
	{
		return map.size();
	}

	@Override
	public Collection<V> values()
	{
		return map.values();
	}
}
