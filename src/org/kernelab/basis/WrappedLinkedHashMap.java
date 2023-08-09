package org.kernelab.basis;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WrappedLinkedHashMap<K, V> extends WrappedHashMap<K, V>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3935712026069274092L;

	public WrappedLinkedHashMap(HashedEquality<K> equal)
	{
		super(equal);
	}

	public WrappedLinkedHashMap(HashedEquality<K> equal, int initialCapacity)
	{
		super(equal, initialCapacity);
	}

	public WrappedLinkedHashMap(HashedEquality<K> equal, int initialCapacity, float loadFactor)
	{
		super(equal, initialCapacity, loadFactor);
	}

	public WrappedLinkedHashMap(HashedEquality<K> equal, Map<? extends K, ? extends V> map)
	{
		super(equal, map);
	}

	@Override
	protected HashMap<Wrapper, V> newHashMap(int initialCapacity, float loadFactor)
	{
		return new LinkedHashMap<Wrapper, V>(initialCapacity, loadFactor);
	}
}
