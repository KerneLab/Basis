package org.kernelab.basis;

import java.io.Serializable;
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
public interface Mapper<K, V> extends Serializable
{
	/**
	 * To map a value into another value.
	 * 
	 * @param key
	 *            The certain value to be mapped.
	 * @return A mapped value.
	 * @throws Terminator
	 *             to terminate the mapping procedure. {@link Terminator#SIGNAL}
	 *             is recommended.
	 * @see Tools#map(Iterable, Mapper, Collection)
	 */
	public V map(K key);
}
