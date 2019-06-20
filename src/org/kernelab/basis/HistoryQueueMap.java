package org.kernelab.basis;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HistoryQueueMap<K, V> implements Map<K, V>
{
	public static interface ReserveRule<K, V> extends Serializable
	{
		public boolean reserve(K key, V value);
	}

	private int						history;

	private Queue<K>				queue;

	private Map<K, V>				map;

	private ReserveRule<K, V>		reserve;

	protected final ReadWriteLock	lock	= new ReentrantReadWriteLock();

	public HistoryQueueMap(int history)
	{
		this(history, null);
	}

	public HistoryQueueMap(int history, ReserveRule<K, V> reserve)
	{
		this(history, reserve, new HashMap<K, V>());
	}

	public HistoryQueueMap(int history, ReserveRule<K, V> reserve, Map<K, V> map)
	{
		this.setHistory(history);
		this.setReserve(reserve);
		this.setMap(map);
		this.setQueue(new LinkedQueue<K>());
	}

	public int clean(boolean reserved)
	{
		this.lock.writeLock().lock();
		try
		{
			return this.truncate(reserved, -1, null).size();
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	public void clear()
	{
		this.lock.writeLock().lock();
		try
		{
			this.getMap().clear();
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	public boolean containsKey(Object key)
	{
		this.lock.readLock().lock();
		try
		{
			return this.getMap().containsKey(key);
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	public boolean containsValue(Object value)
	{
		this.lock.readLock().lock();
		try
		{
			return this.getMap().containsValue(value);
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	public Set<Entry<K, V>> entrySet()
	{
		this.lock.readLock().lock();
		try
		{
			return Collections.unmodifiableSet(new LinkedHashSet<Entry<K, V>>(this.getMap().entrySet()));
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	protected Map<K, V> fetch(int limit, boolean removing, boolean reserved, Map<K, V> result)
	{
		Iterator<K> iter = this.getQueue().iterator();

		ReserveRule<K, V> reserve = reserved ? this.getReserve() : null;

		if (result == null)
		{
			result = new LinkedHashMap<K, V>();
		}

		K key;
		V value;

		int rest = limit;

		while (iter.hasNext() && (rest > 0 || limit < 0))
		{
			key = iter.next();
			value = this.getMap().get(key);

			if (reserve == null || !reserve.reserve(key, value))
			{
				if (removing)
				{
					iter.remove();
					this.getMap().remove(key);
				}
				result.put(key, value);
				rest--;
			}
		}

		return result;
	}

	public V get(Object key)
	{
		this.lock.readLock().lock();
		try
		{
			return this.getMap().get(key);
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	public int getHistory()
	{
		this.lock.readLock().lock();
		try
		{
			return history;
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	protected int getHistory(boolean sync)
	{
		if (sync)
		{
			return this.getHistory();
		}
		else
		{
			return history;
		}
	}

	protected Map<K, V> getMap()
	{
		return map;
	}

	protected Queue<K> getQueue()
	{
		return queue;
	}

	public ReserveRule<K, V> getReserve()
	{
		return reserve;
	}

	public boolean isEmpty()
	{
		this.lock.readLock().lock();
		try
		{
			return this.getMap().isEmpty();
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	public Set<K> keySet()
	{
		this.lock.readLock().lock();
		try
		{
			return Collections.unmodifiableSet(new LinkedHashSet<K>(this.getMap().keySet()));
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	public Map<K, V> look(int limit, boolean reserved, Map<K, V> result)
	{
		this.lock.readLock().lock();
		try
		{
			return this.fetch(limit, false, reserved, result);
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	public V put(K key, V value)
	{
		this.lock.writeLock().lock();
		try
		{
			if (this.getMap().containsKey(key))
			{
				this.removeFromQueue(key);
			}

			this.getQueue().offer(key);

			V old = this.getMap().put(key, value);

			this.truncateOne(true);

			return old;
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	public void putAll(Map<? extends K, ? extends V> m)
	{
		this.lock.writeLock().lock();
		try
		{
			this.removeFromQueue(m.keySet());

			this.getQueue().addAll(m.keySet());

			this.getMap().putAll(m);

			this.truncateBatch(true);
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	public V remove(Object key)
	{
		this.lock.writeLock().lock();
		try
		{
			return this.getMap().remove(key);
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	protected void removeFromQueue(Collection<? extends K> keys)
	{
		if (!keys.isEmpty())
		{
			Iterator<K> iter = this.getQueue().iterator();

			while (iter.hasNext())
			{
				if (keys.contains(iter.next()))
				{
					iter.remove();
				}
			}
		}
	}

	protected void removeFromQueue(K key)
	{
		Iterator<K> iter = this.getQueue().iterator();

		while (iter.hasNext())
		{
			if (Tools.equals(key, iter.next()))
			{
				iter.remove();
				break;
			}
		}
	}

	public void setHistory(int history)
	{
		this.lock.writeLock().lock();
		try
		{
			this.history = history;
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	protected void setMap(Map<K, V> map)
	{
		this.map = map;
	}

	protected void setQueue(Queue<K> queue)
	{
		this.queue = queue;
	}

	public void setReserve(ReserveRule<K, V> reserve)
	{
		this.reserve = reserve;
	}

	public int size()
	{
		this.lock.readLock().lock();
		try
		{
			return this.getMap().size();
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}

	public int size(boolean sync)
	{
		if (sync)
		{
			return this.size();
		}
		else
		{
			return this.getMap().size();
		}
	}

	public Map<K, V> take(int limit, boolean reserved, Map<K, V> result)
	{
		this.lock.writeLock().lock();
		try
		{
			return this.truncate(reserved, limit, result);
		}
		finally
		{
			this.lock.writeLock().unlock();
		}
	}

	protected Map<K, V> truncate(boolean reserved, int limit, Map<K, V> result)
	{
		return this.fetch(limit, true, reserved, result);
	}

	protected Map<K, V> truncateBatch(boolean reserved)
	{
		int limit = 0;

		if (this.getHistory(false) >= 0 && (limit = this.size(false) - this.getHistory(false)) > 0)
		{
			return this.truncate(reserved, limit, null);
		}
		else
		{
			return Collections.emptyMap();
		}
	}

	protected Map<K, V> truncateOne(boolean reserved)
	{
		if (this.getHistory(false) >= 0 && this.size(false) - this.getHistory(false) > 0)
		{
			return this.truncate(reserved, 1, null);
		}
		else
		{
			return Collections.emptyMap();
		}
	}

	public Collection<V> values()
	{
		this.lock.readLock().lock();
		try
		{
			return Collections.unmodifiableList(new LinkedList<V>(this.getMap().values()));
		}
		finally
		{
			this.lock.readLock().unlock();
		}
	}
}
