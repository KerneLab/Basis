package org.kernelab.basis;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class WrappedLinkedHashSet<E> extends WrappedHashSet<E>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1504905058400818317L;

	public WrappedLinkedHashSet(HashedEquality<? super E> equal)
	{
		super(equal);
	}

	public WrappedLinkedHashSet(HashedEquality<? super E> equal, Collection<? extends E> c)
	{
		super(equal, c);
	}

	public WrappedLinkedHashSet(HashedEquality<? super E> equal, int initialCapacity)
	{
		super(equal, initialCapacity);
	}

	public WrappedLinkedHashSet(HashedEquality<? super E> equal, int initialCapacity, float loadFactor)
	{
		super(equal, initialCapacity, loadFactor);
	}

	@Override
	protected HashSet<Wrapper> newHashSet(int initialCapacity, float loadFactor)
	{
		return new LinkedHashSet<Wrapper>(initialCapacity, loadFactor);
	}
}
