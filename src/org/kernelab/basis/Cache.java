package org.kernelab.basis;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
			return new EntryIterator(pairSet().iterator());
		}

		@Override
		public int size()
		{
			return pairSet().size();
		}
	}

	protected class Inspector implements Runnable
	{
		private long	last;

		@SuppressWarnings("unchecked")
		protected void clean()
		{
			if (0 <= keep() && keep() < hold().size())
			{
				int delta = hold().size() - keep();

				TreeSet<SoftPair<V>> expire = new TreeSet<SoftPair<V>>(hold().keySet());

				int count = 0;

				for (SoftPair<V> pair : expire)
				{
					hold().remove(pair);

					count++;

					if (count >= delta)
					{
						break;
					}
				}

				expire.clear();
			}

			SoftPair<V> pair = null;

			while ((pair = (SoftPair<V>) queue().poll()) != null)
			{
				map().remove(pair.key);
			}
		}

		protected long last()
		{
			return last;
		}

		protected void last(long last)
		{
			this.last = last;
		}

		public void run()
		{
			last(System.currentTimeMillis());

			while (true)
			{
				try
				{
					Thread.sleep(interval());
				}
				catch (InterruptedException e)
				{
				}
				if (System.currentTimeMillis() - last() >= delay())
				{
					this.clean();
					last(System.currentTimeMillis());
				}
			}
		}
	}

	protected static class SoftPair<V> extends SoftReference<V> implements Comparable<SoftPair<V>>
	{
		protected final Object	key;

		protected long			last;

		public SoftPair(Object key, V value, ReferenceQueue<V> queue)
		{
			super(value, queue);
			this.key = key;
			this.refresh();
		}

		public int compareTo(SoftPair<V> o)
		{
			long c = this.last - o.last;

			if (c == 0)
			{
				c = this.hashCode() - o.hashCode();
			}

			return c == 0 ? 0 : (c < 0 ? -1 : 1);
		}

		public SoftPair<V> refresh()
		{
			this.last = System.currentTimeMillis();
			return this;
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
			return new ValueIterator(pairSet().iterator());
		}

		@Override
		public int size()
		{
			return pairSet().size();
		}
	}

	public static final int		DEFAULT_KEEP_VALUES			= 256;

	public static final int		DEFAULT_INSPECT_INTERVAL	= 600000;

	public static final int		DEFAULT_INSPECT_DEALY		= 60000;

	public static final float	DEFAULT_LOAD_FACTOR			= 0.75f;

	public static final int		DEFAULT_CONC_LEVEL			= 16;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
	}

	private ConcurrentHashMap<K, SoftPair<V>>	map;

	private int									keep;

	private int									interval;

	private int									delay;

	private ConcurrentHashMap<SoftPair<V>, V>	hold;

	private ReferenceQueue<V>					queue;

	private Set<Map.Entry<K, SoftPair<V>>>		pairSet;

	private Set<Map.Entry<K, V>>				entrySet;

	private Collection<V>						values;

	private Inspector							inspector;

	private Thread								thread;

	public Cache()
	{
		this(DEFAULT_KEEP_VALUES);
	}

	public Cache(int keep)
	{
		this(keep, DEFAULT_INSPECT_INTERVAL);
	}

	public Cache(int keep, int interval)
	{
		this(keep, interval, DEFAULT_INSPECT_DEALY);
	}

	public Cache(int keep, int interval, int delay)
	{
		this(keep, interval, delay, keep, DEFAULT_LOAD_FACTOR);
	}

	public Cache(int keep, int interval, int delay, int initialCapacity, float loadFactor)
	{
		this(keep, interval, delay, initialCapacity, loadFactor, DEFAULT_CONC_LEVEL);
	}

	public Cache(int keep, int interval, int delay, int initialCapacity, float loadFactor, int concurrencyLevel)
	{
		this.keep(keep);
		this.interval(interval);
		this.delay(delay);
		this.map(new ConcurrentHashMap<K, SoftPair<V>>(initialCapacity, loadFactor, concurrencyLevel));
		this.hold(new ConcurrentHashMap<SoftPair<V>, V>(keep, loadFactor, concurrencyLevel));
		this.queue(new ReferenceQueue<V>());
		this.inspector(new Inspector());
		this.thread(new Thread(this.inspector()));
		this.thread().setDaemon(true);
		this.thread().start();
	}

	public Cache<K, V> clean()
	{
		clear();
		return this;
	}

	@Override
	public void clear()
	{
		hold().clear();
		map().clear();
	}

	protected int delay()
	{
		return delay;
	}

	protected Cache<K, V> delay(int delay)
	{
		this.delay = Math.min(delay, interval());
		return this;
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet()
	{
		return entrySet == null ? (entrySet = new EntrySet()) : entrySet;
	}

	@Override
	public V get(Object key)
	{
		V value = null;

		SoftPair<V> pair = map().get(key);

		if (pair != null)
		{
			value = pair.get();

			if (value != null)
			{
				hold(pair);
				inspect();
			}
		}

		return value;
	}

	protected ConcurrentHashMap<SoftPair<V>, V> hold()
	{
		return hold;
	}

	private Cache<K, V> hold(ConcurrentHashMap<SoftPair<V>, V> hold)
	{
		this.hold = hold;
		return this;
	}

	protected void hold(SoftPair<V> pair)
	{
		hold().put(pair.refresh(), pair.get());
	}

	protected void inspect()
	{
		if (0 <= keep() && keep() < hold().size())
		{
			thread().interrupt();
		}
	}

	protected Inspector inspector()
	{
		return inspector;
	}

	private Cache<K, V> inspector(Inspector inspector)
	{
		this.inspector = inspector;
		return this;
	}

	protected int interval()
	{
		return interval;
	}

	private Cache<K, V> interval(int inspect)
	{
		this.interval = inspect;
		return this;
	}

	public int keep()
	{
		return keep;
	}

	public Cache<K, V> keep(int keep)
	{
		this.keep = keep;
		return this;
	}

	@Override
	public Set<K> keySet()
	{
		return map.keySet();
	}

	protected Map<K, SoftPair<V>> map()
	{
		return map;
	}

	private Cache<K, V> map(ConcurrentHashMap<K, SoftPair<V>> map)
	{
		this.map = map;
		this.pairSet = map.entrySet();
		return this;
	}

	protected Set<Map.Entry<K, SoftPair<V>>> pairSet()
	{
		return pairSet;
	}

	@Override
	public V put(K key, V value)
	{
		return valueOf(map().put(key, new SoftPair<V>(key, value, queue())));
	}

	protected ReferenceQueue<V> queue()
	{
		return queue;
	}

	private Cache<K, V> queue(ReferenceQueue<V> queue)
	{
		this.queue = queue;
		return this;
	}

	@Override
	public V remove(Object key)
	{
		return valueOf(map().remove(key));
	}

	@Override
	public int size()
	{
		return map().size();
	}

	protected Thread thread()
	{
		return thread;
	}

	private Cache<K, V> thread(Thread thread)
	{
		this.thread = thread;
		return this;
	}

	protected V valueOf(SoftPair<V> pair)
	{
		return pair == null ? null : pair.get();
	}

	@Override
	public Collection<V> values()
	{
		return values == null ? (values = new ValuesCollection()) : values;
	}
}
