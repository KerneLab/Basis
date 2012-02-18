package org.kernelab.basis;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * In many cases, we would use a bidirectional map to describe the connection
 * between two sets of which relationship is multi-to-multi relation that is
 * beyond bijective map.<br>
 * In BidirectionalHashMap, two Object is equal if their hash codes are equal.<br>
 * 
 * @author Dilly King
 * @version 1.0.0
 * @update 2009-09-07
 * 
 * @param <K>
 *            The generic type of key.
 * @param <V>
 *            The generic type of value.
 */
public class BidirectionalHashMap<K, V> implements Map<K, V>
{

	public static final int	KEY_SET		= 0;

	public static final int	VALUE_SET	= 1;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		BidirectionalHashMap<Integer, String> map = BidirectionalHashMap.newInstance();

		map.put(1, "One");

		map.put(1, "one");

		map.put(2, "Two");

		map.put(2, "two");

		map.put(1, "num");

		map.put(2, "num");

		Tools.debug(map.entrySet());
	}

	public static final <K, V> BidirectionalHashMap<K, V> newInstance()
	{
		return new BidirectionalHashMap<K, V>();
	}

	private Set<K>				keySet;

	private Set<V>				valueSet;

	private Set<Entry<K, V>>	relationSet;

	public BidirectionalHashMap()
	{
		keySet = new LinkedHashSet<K>();
		valueSet = new LinkedHashSet<V>();
		relationSet = new LinkedHashSet<Entry<K, V>>();
	}

	public void clear()
	{
		keySet.clear();
		valueSet.clear();
		relationSet.clear();
	}

	public boolean containsKey(Object key)
	{
		return keySet.contains(key);
	}

	public boolean containsValue(Object value)
	{
		return valueSet.contains(value);
	}

	public Set<java.util.Map.Entry<K, V>> entrySet()
	{
		return relationSet;
	}

	public V get(Object key)
	{
		V value = null;

		if (containsKey(key)) {

			for (Entry<K, V> entry : entrySet()) {
				if (entry.getKey().hashCode() == key.hashCode()) {
					value = entry.getValue();
					break;
				}
			}
		}

		return value;
	}

	public Set<K> getKeysOfValue(Object value)
	{
		Set<K> keys = new LinkedHashSet<K>();

		for (Entry<K, V> entry : this.getRelationsOfValue(value)) {
			keys.add(entry.getKey());
		}

		return keys;
	}

	public Set<Entry<K, V>> getRelations(Object object, int from)
	{
		Set<Entry<K, V>> relations = new LinkedHashSet<Entry<K, V>>();

		for (Entry<K, V> entry : entrySet()) {

			int hashCode = 0;
			switch (from)
			{
				case KEY_SET:
					hashCode = entry.getKey().hashCode();
					break;

				case VALUE_SET:
					hashCode = entry.getValue().hashCode();
					break;
			}

			if (hashCode == object.hashCode()) {
				relations.add(entry);
			}
		}

		return relations;
	}

	public Set<Entry<K, V>> getRelationsOfKey(Object key)
	{
		return getRelations(key, KEY_SET);
	}

	public Set<Entry<K, V>> getRelationsOfValue(Object value)
	{
		return getRelations(value, VALUE_SET);
	}

	public Set<V> getValuesOfKey(Object key)
	{
		Set<V> values = new LinkedHashSet<V>();

		for (Entry<K, V> entry : this.getRelationsOfKey(key)) {
			values.add(entry.getValue());
		}

		return values;
	}

	public boolean isEmpty()
	{
		return size() == 0;
	}

	public Set<K> keySet()
	{
		return keySet;
	}

	public void put(Entry<K, V> relation)
	{
		relationSet.add(relation);
		keySet.add(relation.getKey());
		valueSet.add(relation.getValue());
	}

	public V put(K key, V value)
	{
		put(new Relation<K, V>(key, value));
		return value;
	}

	@SuppressWarnings("unchecked")
	public void putAll(Map<? extends K, ? extends V> m)
	{
		for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put((Entry<K, V>) entry);
		}
	}

	public V remove(Object key)
	{
		V value = get(key);
		removeKey(key);
		return value;
	}

	public void removeKey(Object key)
	{
		if (containsKey(key)) {
			for (Entry<K, V> entry : this.getRelationsOfKey(key)) {
				relationSet.remove(entry);
			}
			keySet.remove(key);
		}
	}

	public void removeValue(Object value)
	{
		if (containsValue(value)) {
			for (Entry<K, V> entry : this.getRelationsOfValue(value)) {
				relationSet.remove(entry);
			}
			valueSet.remove(value);
		}
	}

	public int size()
	{
		return keySet.size();
	}

	public Collection<V> values()
	{
		return valueSet;
	}

}
