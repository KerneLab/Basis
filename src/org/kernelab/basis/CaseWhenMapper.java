package org.kernelab.basis;

import java.util.LinkedList;
import java.util.List;

import org.kernelab.basis.Canal.Producer;
import org.kernelab.basis.Canal.Tuple;
import org.kernelab.basis.Canal.Tuple2;

public class CaseWhenMapper<D, T> implements Mapper<D, T>
{
	public static <D, T> CaseWhenMapper<D, T> of(Filter<D> cond, Mapper<D, T> then)
	{
		return new CaseWhenMapper<D, T>().when(cond, then);
	}

	public static <D, T> CaseWhenMapper<D, T> of(Filter<D> cond, Mapper<D, T> then, Producer<T> other)
	{
		return new CaseWhenMapper<D, T>().when(cond, then).otherwise(other);
	}

	private List<Tuple2<Filter<D>, Mapper<D, T>>>	pairs	= new LinkedList<Tuple2<Filter<D>, Mapper<D, T>>>();

	private Producer<T>								other	= null;

	protected Producer<T> getOther()
	{
		return other;
	}

	protected List<Tuple2<Filter<D>, Mapper<D, T>>> getPairs()
	{
		return pairs;
	}

	@Override
	public T map(D data) throws Exception
	{
		for (Tuple2<Filter<D>, Mapper<D, T>> pair : this.getPairs())
		{
			if (pair._1.filter(data))
			{
				return pair._2.map(data);
			}
		}
		return this.getOther() != null ? this.getOther().produce() : null;
	}

	public CaseWhenMapper<D, T> otherwise(Producer<T> other)
	{
		this.setOther(other);
		return this;
	}

	protected void setOther(Producer<T> other)
	{
		this.other = other;
	}

	protected void setPairs(List<Tuple2<Filter<D>, Mapper<D, T>>> pairs)
	{
		this.pairs = pairs;
	}

	public CaseWhenMapper<D, T> when(Filter<D> cond, Mapper<D, T> then)
	{
		this.getPairs().add(Tuple.of(cond, then));
		return this;
	}
}
