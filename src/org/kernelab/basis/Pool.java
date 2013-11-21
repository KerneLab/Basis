package org.kernelab.basis;

/**
 * The interface of a Pool.
 * 
 * @author Dilly King
 * 
 * @param <E>
 */
public interface Pool<E>
{
	/**
	 * To give an element back to the pool.
	 * 
	 * @param element
	 */
	public void giveBack(E element);

	/**
	 * To take an element away from the pool.
	 * 
	 * @return
	 */
	public E takeAway();
}
