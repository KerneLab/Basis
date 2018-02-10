package org.kernelab.basis;

import java.io.Serializable;

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
public interface Reducer<E, R> extends Serializable
{
	/**
	 * To reduce the element in a certain Iterable object into the reduction
	 * result.
	 * 
	 * @param result
	 *            A former result of reduction. Attention that the result might
	 *            be null sometime in the reduce operation.
	 * @param element
	 *            An element in a certain Iterable object.
	 * @return The result of is reduce operation.
	 * @throws Terminator
	 *             to terminate the reduction procedure.
	 *             {@link Terminator#SIGNAL} is recommended.
	 * @see Tools#reduce(Iterable, Reducer)
	 */
	public R reduce(R result, E element);
}
