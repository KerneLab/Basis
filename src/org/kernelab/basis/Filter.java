package org.kernelab.basis;

public interface Filter<E>
{
	/**
	 * Filter some element from a container such as Collection, List and so on.<br />
	 * If returns false, generally means preserve the element.
	 * 
	 * Usually, We use follow statements:
	 * 
	 * <pre>
	 * Collection&lt;E&gt; origin;
	 * Collection&lt;E&gt; result;
	 * for (E e : origin)
	 * {
	 * 	if (!filter(e))
	 * 	{
	 * 		result.add(e);
	 * 	}
	 * }
	 * return result;
	 * </pre>
	 * 
	 * Via these statements we could filter elements from an origin container to
	 * another container that obey the condition of {@link #filter()}
	 * 
	 * 
	 * @param element
	 *            The element to be filtered.
	 * @return true or false defined by condition.
	 * @throws Terminator
	 *             to terminate the filtering procedure.
	 *             {@link Terminator#SIGNAL} is recommended.
	 * @see Tools#filter(Iterable, Filter, Collection)
	 */
	public boolean filter(E element);
}
