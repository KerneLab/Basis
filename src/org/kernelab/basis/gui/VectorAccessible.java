package org.kernelab.basis.gui;

/**
 * The interface that define the Object which can be accessed as a vector.<br>
 * Similar as object in some script languages, objects can be accessed as
 * {@code o.id} and also can be accessed {@code o["id"]}.<br>
 * Here, object can be accessed as a vector which is indexed by integer.
 * 
 * @author Dilly King
 * 
 */
public interface VectorAccessible
{
	/**
	 * Return the length of the vector.
	 * 
	 * @return The length of the vector.
	 */
	public int vectorAccess();

	/**
	 * Get the value in vector at position of index.
	 * 
	 * @param index
	 *            The position to get the value.
	 * @return The value in vector at index.
	 */
	public Object vectorAccess(int index);

	/**
	 * Set the value in vector at position of index.
	 * 
	 * @param index
	 *            The position to set the value.
	 * @param element
	 *            The value to set.
	 */
	public void vectorAccess(int index, Object element);
}
