package org.kernelab.basis;

import java.util.Comparator;

public class ComparatorHashedEquality<T> implements HashedEquality<T>
{
	public static <T> ComparatorHashedEquality<T> of(Comparator<T> cmp)
	{
		return new ComparatorHashedEquality<T>(cmp);
	}

	protected final Comparator<T> cmp;

	public ComparatorHashedEquality(Comparator<T> cmp)
	{
		if (cmp == null)
		{
			throw new NullPointerException();
		}
		this.cmp = cmp;
	}

	@Override
	public boolean equals(T a, T b)
	{
		return cmp.compare(a, b) == 0;
	}

	@Override
	public int hashCode(T obj)
	{
		return obj != null ? obj.hashCode() : 0;
	}
}
