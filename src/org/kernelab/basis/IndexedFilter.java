package org.kernelab.basis;

public interface IndexedFilter<E>
{
	/**
	 * Filter some element from a container such as Collection, List and so on.
	 * 
	 * @param element
	 *            The element to be filtered.
	 * @param index
	 *            The index (ZERO based) of the element in the Iterable object.
	 * @return true or false defined by condition.
	 * @throws Terminator
	 *             to terminate the filtering procedure.
	 *             {@link Terminator#SIGNAL} is recommended.
	 * @see Filter#filter(Object)
	 * @see Tools#filter(Iterable, IndexedFilter, Collection)
	 */
	public boolean filter(E element, int index);
}
