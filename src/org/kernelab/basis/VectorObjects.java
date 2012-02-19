package org.kernelab.basis;

import java.util.Iterator;

import org.kernelab.basis.Copieable;

/**
 * The sample class which implements VectorAccessible interface.
 * 
 * @author Dilly King
 * 
 */

public class VectorObjects implements VectorAccessible, Iterable<Object>,
		Copieable<VectorObjects>
{

	private class VectorIterator implements Iterator<Object>
	{

		private int	last;

		private int	index;

		public VectorIterator()
		{
			last = -1;
			index = 0;
		}

		public boolean hasNext()
		{
			return index < objects.length;
		}

		public Object next()
		{
			last = index++;
			return objects[index];
		}

		public void remove()
		{
			if (last > -1) {
				objects[last] = null;
			}
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private Object[]	objects;

	public VectorObjects(int size, Object... objects)
	{
		if (size > objects.length) {

			this.objects = new Object[size];

		} else {

			this.objects = new Object[objects.length];
		}

		for (int i = 0; i < objects.length; i++) {
			this.objects[i] = objects[i];
		}
	}

	public VectorObjects(Object... objects)
	{
		this.objects = objects;
	}

	protected VectorObjects(VectorObjects o)
	{
		this.objects = new Object[o.objects.length];
		for (int i = 0; i < objects.length; i++) {
			objects[i] = o.objects[i];
		}
	}

	@Override
	public VectorObjects clone()
	{
		return new VectorObjects(this);
	}

	public boolean equals(Object o)
	{
		boolean equal = false;

		if (o != null) {
			if (o instanceof VectorObjects) {

				VectorObjects v = (VectorObjects) o;

				if (this.vectorAccess() == v.vectorAccess()) {
					equal = true;

					for (int i = 0; i < this.vectorAccess() && equal; i++) {
						if (!this.objects[i].equals(v.objects[i])) {
							equal = false;
						}
					}
				}
			}
		}

		return equal;
	}

	protected Object[] getObjects()
	{
		return objects;
	}

	@Override
	public int hashCode()
	{
		StringBuffer s = new StringBuffer();

		for (int i = 0; i < objects.length; i++) {
			if (objects[i] != null) {
				s.append(objects[i].hashCode());
			} else {
				s.append("0");
			}
			s.append('|');
		}

		return s.toString().hashCode();
	}

	public Iterator<Object> iterator()
	{
		return new VectorIterator();
	}

	protected void setObjects(Object[] objects)
	{
		this.objects = objects;
	}

	public String toString()
	{
		StringBuilder s = new StringBuilder();

		for (int i = 0; i < objects.length; i++) {

			if (i != 0) {
				s.append('|');
			}

			if (objects[i] != null) {
				s.append(objects[i].toString());
			} else {
				s.append("null");
			}
		}

		return s.toString();
	}

	public int vectorAccess()
	{
		return objects.length;
	}

	public Object vectorAccess(int index)
	{
		return objects[index];
	}

	public void vectorAccess(int index, Object element)
	{
		objects[index] = element;
	}

	public void vectorAccess(Object... objects)
	{
		this.objects = objects;
	}

}
