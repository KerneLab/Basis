package org.kernelab.basis;

/**
 * Throw a Terminator in a reduce, filter or map method to terminate the
 * corresponding procedure.
 * 
 * @author Dilly King
 * 
 */
public class Terminator extends RuntimeException
{
	/**
	 * 
	 */
	private static final long		serialVersionUID	= 8262355719510494368L;

	/**
	 * Throw this Terminator signal in a reduce, filter or map method to
	 * terminate the corresponding procedure.
	 */
	public static final Terminator	SIGNAL				= new Terminator();
}
