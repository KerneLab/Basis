package org.kernelab.basis;

public interface Castable
{
	/**
	 * Cast this object to given class.<br />
	 * Return null if could not be cast.
	 * 
	 * @param cls
	 * @return
	 */
	public <T> T to(Class<T> cls);
}
