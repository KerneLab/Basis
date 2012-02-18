package org.kernelab.basis.gui;

import java.util.ArrayList;
import java.util.Collection;

public class VectorSequence extends ArrayList<Object> implements VectorAccessible
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 3892673417227290018L;

	public VectorSequence()
	{
		super();
	}

	public VectorSequence(Collection<Object> collection)
	{
		super(collection);
	}

	public VectorSequence(int itemsNumber)
	{
		super(itemsNumber);
	}

	public int vectorAccess()
	{
		return this.size();
	}

	public Object vectorAccess(int index)
	{
		return this.get(index);
	}

	public void vectorAccess(int index, Object element)
	{
		this.set(index, element);
	}

}
