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
	 * To provide an element in the pool and remove it away which means this
	 * element would not been provided again until it has been recycled.
	 * 
	 * @return
	 */
	public E provide();

	/**
	 * To recycle an element back into the pool so that this pool could provide
	 * the element to others again.
	 * 
	 * @param element
	 */
	public void recycle(E element);
}
