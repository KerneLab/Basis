package org.kernelab.basis;

import java.util.Collection;

/**
 * The Additional interface for Runnable objects.<br />
 * In this interface, the call-back methods are available in a Collection.<br />
 * When the Runnable objects accomplished its task, these call-back method would
 * be called via {@code accomplished()}.
 * 
 * @author Dilly King
 * 
 */
public interface Accomplishable<E>
{
	public static interface AccomplishListener<E>
	{
		public void accomplish(E e);
	}

	/**
	 * This method should be called at the end of "run" in Runnable interface.
	 */
	public void accomplished();

	public Collection<AccomplishListener<E>> getAccomplishListeners();

	public boolean isAccomplished();
}
