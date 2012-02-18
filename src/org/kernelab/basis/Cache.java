package org.kernelab.basis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

public class Cache<K, V> extends Hashtable<K, V>
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 2823892432767899354L;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private Collection<Filter<Relation<K, V>>>	filters;

	public Cache()
	{
		super();
		this.setFilters(new ArrayList<Filter<Relation<K, V>>>());
	}

	public Collection<Filter<Relation<K, V>>> getFilters()
	{
		return filters;
	}

	public boolean isLegal(K key, V value)
	{
		boolean is = true;
		for (Filter<Relation<K, V>> f : this.getFilters()) {
			if (f.filter(new Relation<K, V>(key, value))) {
				is = false;
				break;
			}
		}
		return is;
	}

	@Override
	public V put(K key, V value)
	{
		V v = null;
		if (this.isLegal(key, value)) {
			v = super.put(key, value);
		}
		return v;
	}

	private void setFilters(Collection<Filter<Relation<K, V>>> filters)
	{
		this.filters = filters;
	}

}
