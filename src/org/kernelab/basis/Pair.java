package org.kernelab.basis;

import java.io.Serializable;

public class Pair<K, V> implements Serializable
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -3040321140468343293L;

	public final K				key;

	public final V				value;

	public Pair(K key, V value)
	{
		this.key = key;
		this.value = value;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Pair)
		{
			Pair<?, ?> p = (Pair<?, ?>) o;
			return Tools.equals(this.key, p.key) && Tools.equals(this.value, p.value);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return (key == null ? 0 : key.hashCode()) + (value == null ? 0 : value.hashCode());
	}

	@Override
	public String toString()
	{
		return toString("%s\t%s");
	}

	public String toString(String format)
	{
		return String.format(format, key, value);
	}
}
