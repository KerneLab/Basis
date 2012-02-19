package org.kernelab.basis;

import java.util.Map.Entry;

/**
 * The class to describe the relation between key and value.
 * 
 * @author Dilly King
 * @version 1.3.3
 * @update 2011-01-14
 * 
 * @param <K>
 *            The generic type of key.
 * @param <V>
 *            The generic type of value.
 */
public class Relation<K, V> implements VectorAccessible, Entry<K, V>,
		Copieable<Relation<K, V>>
{

	private K	key;

	private V	value;

	public Relation()
	{
		key = null;
		value = null;
	}

	public Relation(Entry<K, V> entry)
	{
		this(entry.getKey(), entry.getValue());
	}

	public Relation(K key, V value)
	{
		this.set(key, value);
	}

	protected Relation(Relation<K, V> relation)
	{
		this.key = relation.key;
		this.value = relation.value;
	}

	@Override
	public Relation<K, V> clone()
	{
		return new Relation<K, V>(this);
	}

	@Override
	public boolean equals(Object o)
	{
		boolean is = false;

		if (o != null) {
			if (o instanceof Relation<?, ?>) {

				Relation<?, ?> r = (Relation<?, ?>) o;

				if (this.key.equals(r.key) && this.value.equals(r.value)) {
					is = true;
				}
			}
		}

		return is;
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
		return ((key == null ? 0 : key.hashCode()) + "_" + (value == null ? 0 : value
				.hashCode())).hashCode();
	}

	public void set(K key, V value)
	{
		this.key = key;
		this.value = value;
	}

	public K setKey(K key)
	{
		this.set(key, value);
		return key;
	}

	public V setValue(V value)
	{
		this.set(key, value);
		return value;
	}

	@Override
	public String toString()
	{
		return key.toString() + " |-> " + value.toString();
	}

	public int vectorAccess()
	{
		return 2;
	}

	public Object vectorAccess(int index)
	{
		Object value = null;

		switch (index)
		{
			case 0:
				value = this.getKey();
				break;

			case 1:
				value = this.getValue();
				break;
		}

		return value;
	}

	@SuppressWarnings("unchecked")
	public void vectorAccess(int index, Object element)
	{
		switch (index)
		{
			case 0:
				this.setKey((K) element);
				break;

			case 1:
				this.setValue((V) element);
				break;
		}
	}

}
