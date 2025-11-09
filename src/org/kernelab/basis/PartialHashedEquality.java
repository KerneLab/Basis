package org.kernelab.basis;

public class PartialHashedEquality<T, V> implements HashedEquality<T>
{
	public static <T, V> PartialHashedEquality<T, V> of(Mapper<? super T, ? extends V> mapper)
	{
		return new PartialHashedEquality<T, V>(mapper);
	}

	protected final Mapper<? super T, ? extends V> mapper;

	public PartialHashedEquality(Mapper<? super T, ? extends V> mapper)
	{
		if (mapper == null)
		{
			throw new NullPointerException();
		}
		this.mapper = mapper;
	}

	@Override
	public boolean equals(T a, T b)
	{
		try
		{
			return Tools.equals(map(a), map(b));
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode(T obj)
	{
		try
		{
			V value = map(obj);
			return value != null ? value.hashCode() : 0;
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	protected V map(T el) throws Exception
	{
		return mapper.map(el);
	}
}
