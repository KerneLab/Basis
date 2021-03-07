package org.kernelab.basis;

/**
 * The Reducer interface is applied to the operation of reducing an Iterable
 * object.
 * 
 * @author Dilly King
 * 
 * @param <E>
 *            The generic type of the elements to be reduced in the Iterable
 *            object.
 * @param <R>
 *            The generic type of the reduce result.
 */
public interface IndexedReducer<E, R>
{
	/**
	 * To reduce the element in a certain Iterable object into the reduction
	 * result.
	 * 
	 * @param res
	 *            A former result of reduction. Attention that the result might
	 *            be null sometime in the reduce operation.
	 * @param el
	 *            An element in a certain Iterable object.
	 * @param index
	 *            The index (ZERO based) of the element in the Iterable object.
	 * @return The result of is reduce operation.
	 * @throws Terminator
	 *             to terminate the reduction procedure.
	 *             {@link Terminator#SIGNAL} is recommended.
	 * @see Tools#reduce(Iterable, IndexedReducer)
	 */
	public R reduce(R res, E el, int index);
}
