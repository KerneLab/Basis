package org.kernelab.basis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * A chain of Comparators which is useful to compare a serials of Objects
 * according to variety attributes.
 * 
 * @author Dilly King
 * 
 * @param <E>
 */
public class ComparatorsChain<E> implements Comparator<E>
{
	private Collection<Comparator<? super E>> chain;

	public ComparatorsChain(Collection<Comparator<? super E>> chain)
	{
		if (chain == null)
		{
			chain = new ArrayList<Comparator<? super E>>();
		}
		this.chain = chain;
	}

	public ComparatorsChain<E> buckle(Comparator<E>... comparator)
	{
		for (Comparator<E> c : comparator)
		{
			chain.add(c);
		}
		return this;
	}

	public int compare(E o1, E o2)
	{
		int result = 0;

		for (Comparator<? super E> c : chain)
		{
			if ((result = c.compare(o1, o2)) != 0)
			{
				return result;
			}
		}

		return 0;
	}

	public Collection<Comparator<? super E>> getChain()
	{
		return chain;
	}

	public void setChain(Collection<Comparator<? super E>> chain)
	{
		this.chain = chain;
	}

	public ComparatorsChain<E> unbuckle()
	{
		chain.clear();
		return this;
	}

	public ComparatorsChain<E> unbuckle(Collection<Comparator<E>> comparators)
	{
		for (Comparator<E> c : comparators)
		{
			chain.remove(c);
		}
		return this;
	}

	public ComparatorsChain<E> unbuckle(Comparator<E>... comparator)
	{
		for (Comparator<E> c : comparator)
		{
			chain.remove(c);
		}
		return this;
	}
}
