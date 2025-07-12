package org.kernelab.basis;

import java.util.ArrayList;
import java.util.List;

import org.kernelab.basis.Canal.Producer;
import org.kernelab.basis.Canal.Tuple;
import org.kernelab.basis.Canal.Tuple2;

public class CaseWhenMapper<D, T> implements Mapper<D, T>
{
	public static <D, T> CaseWhenMapper<D, T> of(Filter<? super D> cond, Mapper<? super D, ? extends T> then)
	{
		return new CaseWhenMapper<D, T>().when(cond, then);
	}

	public static <D, T> CaseWhenMapper<D, T> of(Filter<? super D> cond, Mapper<? super D, ? extends T> then,
			Producer<? extends T> other)
	{
		return new CaseWhenMapper<D, T>().when(cond, then).otherwise(other);
	}

	private List<Tuple2<Filter<D>, Mapper<D, T>>>	pairs	= new ArrayList<Tuple2<Filter<D>, Mapper<D, T>>>();

	private Producer<T>								other	= null;

	protected Producer<? extends T> getOther()
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

	@SuppressWarnings("unchecked")
	public CaseWhenMapper<D, T> otherwise(Producer<? extends T> other)
	{
		this.setOther((Producer<T>) other);
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

	@SuppressWarnings("unchecked")
	public CaseWhenMapper<D, T> when(Filter<? super D> cond, Mapper<? super D, ? extends T> then)
	{
		this.getPairs().add(Tuple.of((Filter<D>) cond, (Mapper<D, T>) then));
		return this;
	}
}
