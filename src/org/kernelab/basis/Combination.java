package org.kernelab.basis;

import java.util.Set;
import java.util.TreeSet;

public class Combination<E> extends TreeSet<E>
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -9211154145491655169L;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	public static final <E> Combination<E> newInstance()
	{
		return new Combination<E>();
	}

	public static final <E> Combination<E> newInstance(E... array)
	{
		return new Combination<E>(array);
	}

	public static final <E> Combination<E> newInstance(Set<E> set)
	{
		return new Combination<E>(set);
	}

	public Combination()
	{
		super();
	}

	public Combination(E... array)
	{
		super();
		for (E e : array) {
			this.add(e);
		}
	}

	public Combination(Set<E> set)
	{
		super(set);
	}

	@Override
	public boolean equals(Object o)
	{
		boolean is = false;

		if (o != null) {
			if (o instanceof Combination<?>) {
				Combination<?> c = (Combination<?>) o;
				if (this.size() == c.size()) {
					is = true;
					for (E e : this) {
						if (!c.contains(e)) {
							is = false;
							break;
						}
					}
				}
			}
		}

		return is;
	}

	@Override
	public int hashCode()
	{
		StringBuffer buffer = new StringBuffer();

		for (E e : this) {
			if (buffer.length() > 0) {
				buffer.append('_');
			}
			buffer.append(e.hashCode());
		}

		return buffer.toString().hashCode();
	}

}
