package org.kernelab.basis;

/**
 * A class implements the <code>Copieable</code> interface is to make the method
 * <code>clone()</code> become visible for every class so that the object could
 * be cloned every where needed such as in a Collection.
 * 
 * @param <T>
 *            The type of the class which implements this interface.
 * 
 * @author Dilly King
 * @version 1.0.1
 * @update 2008-08-17
 */
public interface Copieable<T> extends Cloneable
{
	public T clone();
}
