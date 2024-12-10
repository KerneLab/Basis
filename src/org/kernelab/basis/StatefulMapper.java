package org.kernelab.basis;

/**
 * The Stateful Mapper interface is to achieve the mapping operation with state.
 * 
 * @author Dilly King
 *
 * @param <K>
 *            The generic type of the elements to be mapped in a certain
 *            Collection.
 * @param <S>
 *            The generic type of the state.
 * @param <V>
 *            The generic type of the mapping result.
 */
public interface StatefulMapper<K, S, V>
{
	/**
	 * To map a value into another value with state.
	 * 
	 * @param el
	 *            The certain value to be mapped.
	 * @param st
	 *            A state.
	 * @return mapped value.
	 * @throws Terminator
	 *             to terminate the mapping procedure. {@link Terminator#SIGNAL}
	 *             is recommended.
	 */
	public V map(K el, S st) throws Exception;
}
