package org.kernelab.basis;

import java.util.Collection;

/**
 * The Mapper interface is to achieve the mapping operation.
 * 
 * @author Dilly King
 * 
 * @param <K>
 *            The generic type of the elements to be mapped in a certain
 *            Collection.
 * @param <V>
 *            The generic type of the mapping result.
 */
public interface IndexedMapper<K, V>
{
	/**
	 * To map a value into another value.
	 * 
	 * @param el
	 *            The certain value to be mapped.
	 * @param index
	 *            The index (ZERO based) of the element in the Iterable object.
	 * @return A mapped value.
	 * @throws Terminator
	 *             to terminate the mapping procedure. {@link Terminator#SIGNAL}
	 *             is recommended.
	 * @see Mapper#map(Object)
	 * @see Tools#map(Iterable, IndexedMapper, Collection)
	 */
	public V map(K el, int index);
}
