package org.kernelab.basis;

public interface HashedEquality<T> extends Equality<T>
{
	public int hashCode(T obj);
}
