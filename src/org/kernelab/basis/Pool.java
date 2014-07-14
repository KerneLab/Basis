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
	 * To discard an element which means it would no longer be provided again.
	 * This method should only be called after {@code provide(long)} returned
	 * the element which means this method would not remove the element in the
	 * pool.
	 * 
	 * @param element
	 */
	public void discard(E element);

	/**
	 * To provide an element in the pool and remove it away which means this
	 * element would not been provided again until it has been recycled.
	 * 
	 * @param timeout
	 *            The maximum milliseconds to be waited, 0 means always wait
	 *            until an available element is returned.
	 * 
	 * @return
	 */
	public E provide(long timeout);

	/**
	 * To recycle an element back into the pool so that this pool could provide
	 * the element to others again. This method should only be called after
	 * {@code provide(long)} returned the element which means the method must
	 * not recycle the element in the pool.
	 * 
	 * @param element
	 */
	public void recycle(E element);
}
