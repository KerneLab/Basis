package org.kernelab.basis;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;

public class Canal<I, O> implements Iterable<O>
{
	protected static abstract class AbstractPond<I, O> implements Pond<I, O>
	{
		private Pond<?, I> up;

		public abstract boolean hasNext();

		public abstract O next();

		@Override
		public void remove()
		{
		}

		public Pond<?, I> upstream()
		{
			return up;
		}

		public void upstream(Pond<?, I> up)
		{
			this.up = up;
		}
	}

	protected static abstract class AbstractTerminal<E, T> extends AbstractPond<E, E> implements Terminal<E, T>
	{
		@Override
		public void end()
		{
		}

		@Override
		public boolean hasNext()
		{
			return false;
		}

		@Override
		public E next()
		{
			return null;
		}
	}

	public static interface Action<E>
	{
		void action(E el);
	}

	protected static class ArraySource<E> extends Source<E>
	{
		protected final E[]	array;

		protected final int	begin;

		protected final int	end;

		protected int		cur;

		public ArraySource(E[] array, int begin, int end)
		{
			this.array = array;
			this.begin = begin;
			this.end = end;
			this.cur = this.begin;
		}

		@Override
		public boolean hasNext()
		{
			return cur < end;
		}

		@Override
		public E next()
		{
			return array[cur++];
		}
	}

	protected static class ArraySourcer<E> implements Sourcer<E>
	{
		protected final E[]	array;

		protected final int	begin;

		protected final int	end;

		public ArraySourcer(E[] array, int begin, int end)
		{
			this.array = array;
			this.begin = begin;
			this.end = end;
		}

		@Override
		public Source<E> newPond()
		{
			return new ArraySource<E>(array, begin, end);
		}
	}

	protected static class CollectOp<E> implements Evaluator<E, Collection<E>>
	{
		protected final Collection<E> result;

		public CollectOp(Collection<E> result)
		{
			this.result = result;
		}

		@Override
		public Terminal<E, Collection<E>> newPond()
		{
			return new Desilter<E>() {
				@Override
				protected Collection<E> newSediment()
				{
					return result == null ? super.newSediment() : result;
				}

				@Override
				protected void settle()
				{
					while (upstream().hasNext())
					{
						sediment.add(upstream().next());
					}
				}
			};
		}
	}

	protected static interface Converter<I, O> extends Operator<I, O>
	{
	}

	protected static class CountByValueOp<E> implements Evaluator<E, Map<E, Integer>>
	{
		@Override
		public Terminal<E, Map<E, Integer>> newPond()
		{
			return new CountByValuePond<E>();
		}
	}

	protected static class CountByValuePond<E> extends AbstractTerminal<E, Map<E, Integer>>
	{
		protected final Map<E, Integer> sediment = new LinkedHashMap<E, Integer>();

		@Override
		public void begin()
		{
			E val = null;
			while (upstream().hasNext())
			{
				val = upstream().next();
				if (!sediment.containsKey(val))
				{
					sediment.put(val, 1);
				}
				else
				{
					sediment.put(val, sediment.get(val) + 1);
				}
			}
		}

		@Override
		public Map<E, Integer> get()
		{
			return sediment;
		}
	}

	protected static class CountOp<E> implements Evaluator<E, Integer>
	{
		@Override
		public Terminal<E, Integer> newPond()
		{
			return new CountPond<E>();
		}
	}

	protected static class CountPond<E> extends AbstractTerminal<E, Integer>
	{
		protected int count = 0;

		@Override
		public void begin()
		{
			while (upstream().hasNext())
			{
				upstream().next();
				count++;
			}
		}

		@Override
		public Integer get()
		{
			return count;
		}
	}

	protected static abstract class Desilter<E> extends Heaper<E> implements Terminal<E, Collection<E>>
	{
		public Collection<E> get()
		{
			return sediment;
		}

		@Override
		protected Collection<E> newSediment()
		{
			return new LinkedList<E>();
		}
	}

	protected static abstract class Distiller
	{

	}

	protected static class DistinctOp<E> implements Converter<E, E>
	{
		protected final Comparator<E> cmp;

		public DistinctOp(Comparator<E> cmp)
		{
			this.cmp = cmp;
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new Heaper<E>() {
				@Override
				protected Collection<E> newSediment()
				{
					return cmp == null ? new LinkedHashSet<E>() : new TreeSet<E>(cmp);
				}

				@Override
				protected void settle()
				{
					while (upstream().hasNext())
					{
						sediment.add(upstream().next());
					}
				}
			};
		}
	}

	protected static class EmptySource<E> extends Source<E>
	{
		@Override
		public boolean hasNext()
		{
			return false;
		}

		@Override
		public E next()
		{
			return null;
		}
	}

	protected static class EmptySourcer<E> implements Sourcer<E>
	{
		@Override
		public Source<E> newPond()
		{
			return new EmptySource<E>();
		}
	}

	protected static interface Evaluator<E, T> extends Operator<E, E>
	{
		Terminal<E, T> newPond();
	}

	protected static class FilterOp<E> implements Converter<E, E>
	{
		protected final Filter<E> filter;

		protected FilterOp(Filter<E> filter)
		{
			this.filter = filter;
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new Wheel<E, E>() {
				private E next;

				@Override
				public boolean hasNext()
				{
					while (upstream().hasNext())
					{
						next = upstream().next();
						if (filter.filter(next))
						{
							return true;
						}
					}
					return false;
				}

				@Override
				public E next()
				{
					return next;
				}
			};
		}
	}

	protected static class FlatMapIndexedOp<I, O> implements Converter<I, O>
	{
		protected final IndexedMapper<I, Iterable<O>> mapper;

		public FlatMapIndexedOp(IndexedMapper<I, Iterable<O>> mapper)
		{
			this.mapper = mapper;
		}

		@Override
		public Pond<I, O> newPond()
		{
			return new Wheel<I, O>() {
				private Iterator<O> iter;

				@Override
				public boolean hasNext()
				{
					if (iter != null && iter.hasNext())
					{
						return true;
					}
					while (upstream().hasNext())
					{
						iter = mapper.map(upstream().next(), index++).iterator();
						if (iter.hasNext())
						{
							return true;
						}
					}
					return false;
				}

				@Override
				public O next()
				{
					return iter.next();
				}
			};
		}
	}

	protected static class FlatMapOp<I, O> implements Converter<I, O>
	{
		protected final Mapper<I, Iterable<O>> mapper;

		public FlatMapOp(Mapper<I, Iterable<O>> mapper)
		{
			this.mapper = mapper;
		}

		@Override
		public Pond<I, O> newPond()
		{
			return new Wheel<I, O>() {
				private Iterator<O> iter;

				@Override
				public boolean hasNext()
				{
					if (iter != null && iter.hasNext())
					{
						return true;
					}
					while (upstream().hasNext())
					{
						iter = mapper.map(upstream().next()).iterator();
						if (iter.hasNext())
						{
							return true;
						}
					}
					return false;
				}

				@Override
				public O next()
				{
					return iter.next();
				}
			};
		}
	}

	protected static class ForeachOp<E> implements Evaluator<E, Void>
	{
		protected final Action<E> action;

		public ForeachOp(Action<E> action)
		{
			this.action = action;
		}

		@Override
		public Terminal<E, Void> newPond()
		{
			return new ForeachPond<E>(action);
		}
	}

	protected static class ForeachPond<E> extends AbstractTerminal<E, Void>
	{
		protected final Action<E> action;

		public ForeachPond(Action<E> action)
		{
			this.action = action;
		}

		@Override
		public void begin()
		{
			while (upstream().hasNext())
			{
				action.action(upstream().next());
			}
		}

		@Override
		public Void get()
		{
			return null;
		}
	}

	// protected static abstract class Grouper<I, K, V> extends Desilter<I,
	// Map<K, V>>
	// {
	// // TODO
	// }

	protected static abstract class Heaper<E> extends AbstractPond<E, E>
	{
		protected final Collection<E>	sediment	= this.newSediment();

		private Iterator<E>				iter;

		public void begin()
		{
			this.settle();
			this.iter = sediment.iterator();
		}

		public void end()
		{
		}

		@Override
		public boolean hasNext()
		{
			return iter.hasNext();
		}

		protected abstract Collection<E> newSediment();

		@Override
		public E next()
		{
			return iter.next();
		}

		protected abstract void settle();
	}

	protected static class IterableSourcer<E> implements Sourcer<E>
	{
		protected final Iterable<E> iter;

		public IterableSourcer(Iterable<E> iter)
		{
			this.iter = iter;
		}

		@Override
		public Source<E> newPond()
		{
			return new IteratorSource<E>(iter.iterator());
		}
	}

	protected static class IteratorSource<E> extends Source<E>
	{
		protected final Iterator<E> iter;

		public IteratorSource(Iterator<E> iter)
		{
			this.iter = iter;
		}

		@Override
		public boolean hasNext()
		{
			return iter.hasNext();
		}

		@Override
		public E next()
		{
			return iter.next();
		}
	}

	protected static class MapIndexedOp<I, O> implements Converter<I, O>
	{
		protected final IndexedMapper<I, O> mapper;

		public MapIndexedOp(IndexedMapper<I, O> mapper)
		{
			this.mapper = mapper;
		}

		@Override
		public Pond<I, O> newPond()
		{
			return new Wheel<I, O>() {
				@Override
				public O next()
				{
					return mapper.map(upstream().next(), index++);
				}
			};
		}
	}

	protected static class MapOp<I, O> implements Converter<I, O>
	{
		protected final Mapper<I, O> mapper;

		public MapOp(Mapper<I, O> mapper)
		{
			this.mapper = mapper;
		}

		@Override
		public Pond<I, O> newPond()
		{
			return new Wheel<I, O>() {
				@Override
				public O next()
				{
					return mapper.map(upstream().next());
				}
			};
		}
	}

	public static class None<E> extends Option<E>
	{
		public None()
		{
			this.setOperator(new EmptySourcer<E>());
		}

		@Override
		public E get()
		{
			throw new NoneValueGivenException();
		}

		@Override
		public boolean given()
		{
			return false;
		}

		@Override
		public E or(E defaultValue)
		{
			return defaultValue;
		}

		@Override
		public E orNull()
		{
			return null;
		}
	}

	public static class NoneValueGivenException extends RuntimeException
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -124083433722347402L;
	}

	protected static interface Operator<I, O>
	{
		Pond<I, O> newPond();
	}

	public static abstract class Option<E> extends Canal<E, E>
	{
		public abstract E get();

		public abstract boolean given();

		public abstract E or(E defaultValue);

		public abstract E orNull();
	}

	protected static interface Pond<I, O> extends Iterator<O>
	{
		void begin();

		void end();

		Pond<?, I> upstream();

		void upstream(Pond<?, I> up);
	}

	public static class Some<E> extends Option<E>
	{
		private static <E> E[] makeArray(E... es)
		{
			return es;
		}

		protected final E value;

		@SuppressWarnings("unchecked")
		public Some(E val)
		{
			if (val == null)
			{
				throw new NoneValueGivenException();
			}
			this.value = val;
			// @SuppressWarnings("unchecked")
			// E[] array = (E[]) Array.newInstance(val.getClass(), 1);
			// array[0] = val;
			this.setOperator(new ArraySourcer<E>(makeArray(val), 0, 1));
		}

		@Override
		public E get()
		{
			return value;
		}

		@Override
		public boolean given()
		{
			return true;
		}

		@Override
		public E or(E defaultValue)
		{
			return value;
		}

		@Override
		public E orNull()
		{
			return value;
		}
	}

	protected static abstract class Source<E> implements Pond<E, E>
	{
		public void begin()
		{
		}

		public void end()
		{
		}

		@Override
		public void remove()
		{
		}

		@Override
		public Pond<?, E> upstream()
		{
			return null;
		}

		@Override
		public void upstream(Pond<?, E> up)
		{
		}
	}

	protected static interface Sourcer<E> extends Operator<E, E>
	{
		Source<E> newPond();
	}

	protected static class TakeOp<E> implements Evaluator<E, Collection<E>>
	{
		protected final int				limit;

		protected final Collection<E>	result;

		public TakeOp(int limit, Collection<E> result)
		{
			this.limit = limit;
			this.result = result;
		}

		@Override
		public Terminal<E, Collection<E>> newPond()
		{
			return new Desilter<E>() {
				@Override
				protected Collection<E> newSediment()
				{
					return result == null ? super.newSediment() : result;
				}

				@Override
				protected void settle()
				{
					int i = 0;
					while (i < limit && upstream().hasNext())
					{
						sediment.add(upstream().next());
						i++;
					}
				}
			};
		}
	}

	protected static interface Terminal<E, T> extends Pond<E, E>
	{
		T get();
	}

	public static class Tuple implements Serializable
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1796022544991162056L;

		public static <E1> Tuple1<E1> of(E1 _1)
		{
			return new Tuple1<E1>(_1);
		}

		public static <E1, E2> Tuple2<E1, E2> of(E1 _1, E2 _2)
		{
			return new Tuple2<E1, E2>(_1, _2);
		}
	}

	public static class Tuple1<E1> extends Tuple
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= 6318017990620501184L;

		public final E1				_1;

		public Tuple1(E1 _1)
		{
			this._1 = _1;
		}
	}

	public static class Tuple2<E1, E2> extends Tuple1<E1>
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= -4920340973710779812L;

		public final E2				_2;

		public Tuple2(E1 _1, E2 _2)
		{
			super(_1);
			this._2 = _2;
		}
	}

	protected static class UnionOp<E> implements Converter<E, E>
	{
		protected final Canal<?, E>	here;

		protected final Canal<?, E>	that;

		public UnionOp(Canal<?, E> here, Canal<?, E> that)
		{
			this.here = here;
			this.that = that;
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new UnionPond<E>(here.newPond(), that.newPond());
		}
	}

	protected static class UnionPond<E> extends AbstractPond<E, E>
	{
		protected final Pond<?, E>	self;

		protected final Pond<?, E>	that;

		private boolean				here	= true;

		public UnionPond(Pond<?, E> self, Pond<?, E> that)
		{
			this.self = self;
			this.that = that;
		}

		@Override
		public void begin()
		{
		}

		@Override
		public void end()
		{
		}

		@Override
		public boolean hasNext()
		{
			if (self.hasNext())
			{
				return true;
			}
			else if (that.hasNext())
			{
				here = false;
				return true;
			}
			else
			{
				return false;
			}
		}

		@Override
		public E next()
		{
			if (here)
			{
				return self.next();
			}
			else
			{
				return that.next();
			}
		}
	}

	protected static abstract class Wheel<I, O> extends AbstractPond<I, O>
	{
		protected int index = 0;

		public void begin()
		{
		}

		public void end()
		{
		}

		@Override
		public boolean hasNext()
		{
			return upstream().hasNext();
		}
	}

	public static void main(String[] args)
	{
		Collection<Integer> coll = new LinkedList<Integer>();
		coll.add(1);
		coll.add(2);
		coll.add(2);
		coll.add(3);
		coll.add(4);
		coll.add(5);

		Canal<Integer, Integer> c = Canal.of(coll).filter(new Filter<Integer>() {
			@Override
			public boolean filter(Integer element)
			{
				return element > 2;
			}
		});

		Tools.debug(c.collect());
		Tools.debug("============");
		Tools.debug(c.map(new Mapper<Integer, Integer>() {
			@Override
			public Integer map(Integer key)
			{
				return key * 2;
			}
		}).take(2));

		Tools.debug("============");

		Integer[] array = new Integer[] { 1, 2, 3, 4, 4, 5, 6 };
		c = Canal.of(array).filter(new Filter<Integer>() {
			@Override
			public boolean filter(Integer element)
			{
				return element > 3;
			}
		});
		c.distinct().foreach(new Action<Integer>() {
			@Override
			public void action(Integer el)
			{
				Tools.debug(el + "..");
			}
		});
		Tools.debug("------------");
		Tools.debug(c.distinct().count());
		Tools.debug("============");
		Tools.debug(c.collect());
		Tools.debug("------------");
		Tools.debug(c.count());
		Tools.debug("============");
		Tools.debug(c.map(new IndexedMapper<Integer, String>() {
			@Override
			public String map(Integer key, int index)
			{
				return key + "-" + index;
			}
		}).take(2));
		Tools.debug("============");

		Integer[] array1 = new Integer[] { 1, 2, 3 };
		c = Canal.of(array1).flatMap(new Mapper<Integer, Iterable<Integer>>() {
			@Override
			public Iterable<Integer> map(Integer key)
			{
				Collection<Integer> a = new LinkedList<Integer>();
				for (int i = 1; i < key; i++)
				{
					a.add(key);
				}
				return a;
			}
		});
		Tools.debug(c.collect());
		Tools.debug("============");
		Tools.debug(c.countByValue());

		Tools.debug("============");
		Tools.debug(Canal.option(1).count());
		Tools.debug("============");
		Tools.debug(Canal.option(null).count());

		Tools.debug("============");
		Tools.debug(Canal.of(new Integer[] { 1, 2 }).union(Canal.of(new Integer[] { 4, 5 })).collect());

	}

	public static <E> Canal<E, E> of(E[] array)
	{
		return of(array, 0);
	}

	public static <E> Canal<E, E> of(E[] array, int begin)
	{
		return of(array, begin, array.length);
	}

	public static <E> Canal<E, E> of(E[] array, int begin, int end)
	{
		return new Canal<E, E>().setOperator(new ArraySourcer<E>(array, begin, end));
	}

	public static <E> Canal<E, E> of(Iterable<E> iter)
	{
		return new Canal<E, E>().setOperator(new IterableSourcer<E>(iter));
	}

	public static <E> Option<E> option(E value)
	{
		return value == null ? new None<E>() : new Some<E>(value);
	}

	private Canal<?, I>		upstream;

	private Operator<I, O>	operator;

	protected void begin(Pond<?, ?> pond)
	{
		if (pond.upstream() != null)
		{
			begin(pond.upstream());
		}
		pond.begin();
	}

	protected Pond<I, O> build(Pond<O, ?> down)
	{
		Pond<I, O> pond = this.newPond();

		if (down != null)
		{
			down.upstream(pond);
		}

		if (this.upstream != null)
		{
			this.upstream.build(pond);
		}

		return pond;
	}

	public Collection<O> collect()
	{
		return this.collect(null);
	}

	public Collection<O> collect(Collection<O> result)
	{
		return this.follow(new CollectOp<O>(result)).evaluate();
	}

	public int count()
	{
		return this.follow(new CountOp<O>()).evaluate();
	}

	public Map<O, Integer> countByValue()
	{
		return this.follow(new CountByValueOp<O>()).evaluate();
	}

	public Canal<O, O> distinct()
	{
		return this.distinct(null);
	}

	public Canal<O, O> distinct(Comparator<O> cmp)
	{
		return this.follow(new DistinctOp<O>(cmp));
	}

	protected <T> T evaluate()
	{
		@SuppressWarnings("unchecked")
		Terminal<I, T> t = ((Evaluator<I, T>) this.getOperator()).newPond();

		this.getUpstream().build(t);

		this.begin(t);

		return t.get();
	}

	public Canal<O, O> filter(Filter<O> filter)
	{
		return this.follow(new FilterOp<O>(filter));
	}

	public <N> Canal<O, N> flatMap(IndexedMapper<O, Iterable<N>> mapper)
	{
		return this.follow(new FlatMapIndexedOp<O, N>(mapper));
	}

	public <N> Canal<O, N> flatMap(Mapper<O, Iterable<N>> mapper)
	{
		return this.follow(new FlatMapOp<O, N>(mapper));
	}

	protected <N> Canal<O, N> follow(Operator<O, N> op)
	{
		return new Canal<O, N>().setUpstream(this).setOperator(op);
	}

	public void foreach(Action<O> action)
	{
		this.follow(new ForeachOp<O>(action)).evaluate();
	}

	protected Operator<I, O> getOperator()
	{
		return operator;
	}

	protected Canal<?, I> getUpstream()
	{
		return upstream;
	}

	@Override
	public Iterator<O> iterator()
	{
		Pond<I, O> pond = this.operator.newPond();
		this.upstream.build(pond);
		return pond;
	}

	public <N> Canal<O, N> map(IndexedMapper<O, N> mapper)
	{
		return this.follow(new MapIndexedOp<O, N>(mapper));
	}

	public <N> Canal<O, N> map(Mapper<O, N> mapper)
	{
		return this.follow(new MapOp<O, N>(mapper));
	}

	@SuppressWarnings("unchecked")
	protected Pond<I, O> newPond()
	{
		if (this.getOperator() instanceof Sourcer<?>)
		{
			return (Pond<I, O>) ((Sourcer<O>) this.getOperator()).newPond();
		}
		else
		{
			return this.getOperator().newPond();
		}
	}

	protected Canal<I, O> setOperator(Operator<I, O> operator)
	{
		this.operator = operator;
		return this;
	}

	protected Canal<I, O> setUpstream(Canal<?, I> upstream)
	{
		this.upstream = upstream;
		return this;
	}

	public Collection<O> take(int limit)
	{
		return this.take(limit, null);
	}

	public Collection<O> take(int limit, Collection<O> result)
	{
		return this.follow(new TakeOp<O>(limit, result)).evaluate();
	}

	public Canal<O, O> union(Canal<?, O> that)
	{
		return this.follow(new UnionOp<O>(this, that));
	}
}
