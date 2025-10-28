package org.kernelab.basis;

public class DefaultHashedEquality<T, V> implements HashedEquality<T>, Mapper<T, V>
{
	public static <T, V> DefaultHashedEquality<T, V> of(Mapper<? super T, ? extends V> mapper)
	{
		return new DefaultHashedEquality<T, V>(mapper);
	}

	protected final Mapper<? super T, ? extends V> mapper;

	public DefaultHashedEquality(Mapper<? super T, ? extends V> mapper)
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

	@Override
	public V map(T el) throws Exception
	{
		return mapper.map(el);
	}
}
