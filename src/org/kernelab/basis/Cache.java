package org.kernelab.basis;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Cache<K, V> extends AbstractMap<K, V> implements Map<K, V>
{
	protected abstract class AbstractIterator<E> implements Iterator<E>
	{
		protected Iterator<Map.Entry<K, SoftPair<V>>>	iter;

		public AbstractIterator(Iterator<Map.Entry<K, SoftPair<V>>> iter)
		{
			this.iter = iter;
		}

		public boolean hasNext()
		{
			return iter.hasNext();
		}

		public void remove()
		{
			iter.remove();
		}
	}

	protected static class Entry<K, V> implements Map.Entry<K, V>
	{
		private K	key;
		private V	value;

		protected Entry(Map.Entry<K, SoftPair<V>> pair)
		{
			this.key = pair.getKey();
			this.value = pair.getValue().get();
		}

		@Override
		public boolean equals(Object o)
		{
			if (o instanceof Entry)
			{
				Entry<?, ?> e = (Entry<?, ?>) o;
				return Tools.equals(this.key, e.key) && Tools.equals(this.value, e.value);
			}
			else
			{
				return false;
			}
		}

		public K getKey()
		{
			return key;
		}

		public V getValue()
		{
			return value;
		}

		@Override
		public int hashCode()
		{
			return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
		}

		public V setValue(V value)
		{
			V old = this.value;
			this.value = value;
			return old;
		}

		@Override
		public String toString()
		{
			return getKey() + "=" + getValue();
		}
	}

	protected class EntryIterator extends AbstractIterator<Map.Entry<K, V>>
	{
		public EntryIterator(Iterator<java.util.Map.Entry<K, SoftPair<V>>> iter)
		{
			super(iter);
		}

		public java.util.Map.Entry<K, V> next()
		{
			return new Entry<K, V>(iter.next());
		}
	}

	protected class EntrySet extends AbstractSet<Map.Entry<K, V>>
	{
		@Override
		public Iterator<java.util.Map.Entry<K, V>> iterator()
		{
			return new EntryIterator(pairSet.iterator());
		}

		@Override
		public int size()
		{
			return pairSet.size();
		}
	}

	protected static class SoftPair<V> extends SoftReference<V>
	{
		protected final Object	key;

		public SoftPair(Object key, V value, ReferenceQueue<V> queue)
		{
			super(value, queue);
			this.key = key;
		}
	}

	protected class ValueIterator extends AbstractIterator<V>
	{
		public ValueIterator(Iterator<java.util.Map.Entry<K, SoftPair<V>>> iter)
		{
			super(iter);
		}

		public V next()
		{
			return iter.next().getValue().get();
		}
	}

	protected class ValuesCollection extends AbstractCollection<V>
	{
		@Override
		public Iterator<V> iterator()
		{
			return new ValueIterator(pairSet.iterator());
		}

		@Override
		public int size()
		{
			return pairSet.size();
		}
	}

	public static final int		DEFAULT_KEEP_VALUES		= 10;

	public static final int		DEFAULT_INIT_CAPACITY	= 16;

	public static final float	DEFAULT_LOAD_FACTOR		= 0.75f;

	public static final int		DEFAULT_CONC_LEVEL		= 16;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Map<String, Integer> c = new Cache<String, Integer>();

		for (int i = 0; i < 100; i++)
		{
			c.put(String.valueOf(i), i);
		}
		Tools.debug(c);

		Tools.debug("===========");
		Tools.debug(c.get("1"));
	}

	private Map<K, SoftPair<V>>				map;

	private int								keep;

	private LinkedList<V>					hold;

	private ReferenceQueue<V>				queue;

	private Set<Map.Entry<K, SoftPair<V>>>	pairSet;

	private Set<Map.Entry<K, V>>			entrySet;

	private Collection<V>					values;

	public Cache()
	{
		this(DEFAULT_KEEP_VALUES);
	}

	public Cache(int keep)
	{
		this(keep, DEFAULT_INIT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public Cache(int keep, int initialCapacity, float loadFactor)
	{
		this(keep, initialCapacity, loadFactor, DEFAULT_CONC_LEVEL);
	}

	public Cache(int keep, int initialCapacity, float loadFactor, int concurrencyLevel)
	{
		this.setMap(new ConcurrentHashMap<K, SoftPair<V>>(initialCapacity, loadFactor, concurrencyLevel));
		this.setKeep(keep);
		this.setHold(new LinkedList<V>());
		this.setQueue(new ReferenceQueue<V>());
	}

	@Override
	public void clear()
	{
		hold.clear();
		refresh();
		map.clear();
	}

	protected void initPairSet()
	{
		if (pairSet == null)
		{
			pairSet = map.entrySet();
		}
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet()
	{
		this.initPairSet();
		return entrySet == null ? (entrySet = new EntrySet()) : entrySet;
	}

	@Override
	public V get(Object key)
	{
		V value = null;

		SoftPair<V> softVal = map.get(key);

		if (softVal != null)
		{
			value = softVal.get();

			if (value == null)
			{
				map.remove(key);
			}
			else
			{
				hold(value);
			}
		}

		return value;
	}

	protected LinkedList<V> getHold()
	{
		return hold;
	}

	public int getKeep()
	{
		return keep;
	}

	protected ReferenceQueue<V> getQueue()
	{
		return queue;
	}

	public void hold(V value)
	{
		hold.addFirst(value);

		while (hold.size() > keep && !hold.isEmpty())
		{
			hold.removeLast();
		}
	}

	@Override
	public Set<K> keySet()
	{
		return map.keySet();
	}

	@Override
	public V put(K key, V value)
	{
		refresh();
		return valueOf(map.put(key, new SoftPair<V>(key, value, queue)));
	}

	@SuppressWarnings("unchecked")
	public void refresh()
	{
		SoftPair<V> pair = null;

		while ((pair = (SoftPair<V>) queue.poll()) != null)
		{
			map.remove(pair.key);
		}
	}

	public V remove(Object key)
	{
		refresh();
		return valueOf(map.remove(key));
	}

	private void setHold(LinkedList<V> hold)
	{
		this.hold = hold;
	}

	public void setKeep(int keep)
	{
		this.keep = keep;
	}

	private void setMap(Map<K, SoftPair<V>> map)
	{
		this.map = map;
	}

	private void setQueue(ReferenceQueue<V> queue)
	{
		this.queue = queue;
	}

	@Override
	public int size()
	{
		refresh();
		return map.size();
	}

	protected V valueOf(SoftPair<V> pair)
	{
		return pair == null ? null : pair.get();
	}

	@Override
	public Collection<V> values()
	{
		this.initPairSet();
		return values == null ? (values = new ValuesCollection()) : values;
	}
}
