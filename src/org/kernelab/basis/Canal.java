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

	protected static class CartesianOp<A, B> implements Converter<A, Tuple2<A, B>>
	{
		protected final Canal<?, A>	self;

		protected final Canal<?, B>	that;

		public CartesianOp(Canal<?, A> self, Canal<?, B> that)
		{
			this.self = self;
			this.that = that;
		}

		@Override
		public Pond<A, Tuple2<A, B>> newPond()
		{
			return new CartesianPond<A, B>(self, that);
		}
	}

	protected static class CartesianPond<A, B> extends AbstractPond<A, Tuple2<A, B>>
	{
		protected final Canal<?, A>	self;

		protected final Canal<?, B>	that;

		private Pond<?, A>			here;

		private Pond<?, B>			there;

		private A					left;

		public CartesianPond(Canal<?, A> self, Canal<?, B> that)
		{
			this.self = self;
			this.that = that;
		}

		@Override
		public void begin()
		{
			here = self.build();
		}

		@Override
		public void end()
		{
		}

		@Override
		public boolean hasNext()
		{
			while (there == null || !there.hasNext())
			{
				if (!here.hasNext())
				{
					return false;
				}
				else
				{
					if (there == null)
					{
						there = that.build();
					}
					left = here.next();
					there = that.build();
				}
			}

			return true;
		}

		@Override
		public Tuple2<A, B> next()
		{
			return new Tuple2<A, B>(left, there.next());
		}
	}

	protected static class CollectAsMapOp<E, K, V> implements Evaluator<E, Map<K, V>>
	{
		protected final Map<K, V>		result;

		protected final Mapper<E, K>	kop;

		protected final Mapper<E, V>	vop;

		public CollectAsMapOp(Map<K, V> result, Mapper<E, K> kop, Mapper<E, V> vop)
		{
			this.result = result != null ? result : new LinkedHashMap<K, V>();
			this.kop = kop != null ? kop : new DefaultKop<E, K>();
			this.vop = vop != null ? vop : new DefaultVop<E, V>();
		}

		@Override
		public Terminal<E, Map<K, V>> newPond()
		{
			return new AbstractTerminal<E, Map<K, V>>()
			{
				@Override
				public void begin()
				{
					E el = null;
					while (upstream().hasNext())
					{
						el = upstream().next();
						result.put(kop.map(el), vop.map(el));
					}
				}

				@Override
				public Map<K, V> get()
				{
					return result;
				}
			};
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
			return new Desilter<E>()
			{
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

	protected static class CountByKeyOp<E, K> implements Evaluator<E, Map<K, Integer>>
	{
		protected final Map<K, Integer>	result;

		protected final Mapper<E, K>	kop;

		public CountByKeyOp(Map<K, Integer> result, Mapper<E, K> kop)
		{
			this.result = result != null ? result : new LinkedHashMap<K, Integer>();
			this.kop = kop != null ? kop : new DefaultKop<E, K>();
		}

		@Override
		public Terminal<E, Map<K, Integer>> newPond()
		{
			return new AbstractTerminal<E, Map<K, Integer>>()
			{
				@Override
				public void begin()
				{
					K key = null;
					while (upstream().hasNext())
					{
						key = kop.map(upstream().next());
						if (!result.containsKey(key))
						{
							result.put(key, 1);
						}
						else
						{
							result.put(key, result.get(key) + 1);
						}
					}
				}

				@Override
				public Map<K, Integer> get()
				{
					return result;
				}
			};
		}
	}

	protected static class CountByValueOp<E> implements Evaluator<E, Map<E, Integer>>
	{
		protected final Map<E, Integer> result;

		public CountByValueOp(Map<E, Integer> result)
		{
			this.result = result == null ? new LinkedHashMap<E, Integer>() : result;
		}

		@Override
		public Terminal<E, Map<E, Integer>> newPond()
		{
			return new AbstractTerminal<E, Map<E, Integer>>()
			{
				@Override
				public void begin()
				{
					E val = null;
					while (upstream().hasNext())
					{
						val = upstream().next();
						if (!result.containsKey(val))
						{
							result.put(val, 1);
						}
						else
						{
							result.put(val, result.get(val) + 1);
						}
					}
				}

				@Override
				public Map<E, Integer> get()
				{
					return result;
				}
			};
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

	public static class DefaultKop<E, K> implements Mapper<E, K>
	{
		@SuppressWarnings("unchecked")
		@Override
		public K map(E data)
		{
			return data == null ? null : ((Tuple2<K, ?>) data)._1;
		}
	}

	public static class DefaultVop<E, V> implements Mapper<E, V>
	{
		@SuppressWarnings("unchecked")
		@Override
		public V map(E data)
		{
			return data == null ? null : ((Tuple2<?, V>) data)._2;
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
			return new Heaper<E>()
			{
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
			return new Wheel<E, E>()
			{
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

	protected static class FirstIndexedOp<E> implements Evaluator<E, Option<E>>
	{
		protected final IndexedFilter<E> filter;

		public FirstIndexedOp(IndexedFilter<E> filter)
		{
			this.filter = filter;
		}

		@Override
		public Terminal<E, Option<E>> newPond()
		{
			return new AbstractTerminal<E, Option<E>>()
			{
				private boolean	found	= false;

				private E		result;

				@Override
				public void begin()
				{
					int index = 0;
					while (upstream().hasNext())
					{
						if (filter.filter(result = upstream().next(), index++))
						{
							found = true;
							break;
						}
					}
				}

				@Override
				public Option<E> get()
				{
					return found ? Canal.some(result) : Canal.<E> none();
				}
			};
		}
	}

	protected static class FirstOp<E> implements Evaluator<E, Option<E>>
	{
		protected final Filter<E> filter;

		public FirstOp(Filter<E> filter)
		{
			this.filter = filter;
		}

		@Override
		public Terminal<E, Option<E>> newPond()
		{
			return new AbstractTerminal<E, Option<E>>()
			{
				private boolean	found	= false;

				private E		result;

				@Override
				public void begin()
				{
					while (upstream().hasNext())
					{
						if (filter.filter(result = upstream().next()))
						{
							found = true;
							break;
						}
					}
				}

				@Override
				public Option<E> get()
				{
					return found ? Canal.some(result) : Canal.<E> none();
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
			return new Wheel<I, O>()
			{
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
			return new Wheel<I, O>()
			{
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

	protected static class FoldOp<E, R> implements Evaluator<E, R>
	{
		private R						result;

		protected final Reducer<E, R>	folder;

		public FoldOp(R init, Reducer<E, R> folder)
		{
			this.result = init;
			this.folder = folder;
		}

		@Override
		public Terminal<E, R> newPond()
		{
			return new AbstractTerminal<E, R>()
			{
				@Override
				public void begin()
				{
					while (upstream().hasNext())
					{
						result = folder.reduce(result, upstream().next());
					}
				}

				@Override
				public R get()
				{
					return result;
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
			return new Wheel<I, O>()
			{
				@Override
				public O next()
				{
					return mapper.map(upstream().next(), index++);
				}
			};
		}
	}

	// protected static abstract class Grouper<I, K, V> extends Desilter<I,
	// Map<K, V>>
	// {
	// // TODO
	// }

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
			return new Wheel<I, O>()
			{
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

		@Override
		public String toString()
		{
			return "None";
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

	protected static class ReduceOp<E> implements Evaluator<E, Option<E>>
	{
		protected final Reducer<E, E> reducer;

		public ReduceOp(Reducer<E, E> reducer)
		{
			this.reducer = reducer;
		}

		@Override
		public Terminal<E, Option<E>> newPond()
		{
			return new AbstractTerminal<E, Option<E>>()
			{
				private boolean	empty	= true;

				private E		result;

				@Override
				public void begin()
				{
					E el = null;
					while (upstream().hasNext())
					{
						el = upstream().next();
						if (empty)
						{
							empty = false;
							result = el;
						}
						else
						{
							result = reducer.reduce(result, el);
						}
					}
				}

				@Override
				public Option<E> get()
				{
					return empty ? Canal.<E> none() : Canal.some(result);
				}
			};
		}
	}

	public static class SelfMapper<E> implements Mapper<E, E>
	{
		@Override
		public E map(E key)
		{
			return key;
		}
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
			this.value = val;
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

		@Override
		public String toString()
		{
			return "Some(" + value + ")";
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
			return new Desilter<E>()
			{
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

		@Override
		public String toString()
		{
			return "(" + _1 + ")";
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

		@Override
		public String toString()
		{
			return "(" + _1 + ", " + _2 + ")";
		}
	}

	protected static class UnionOp<E> implements Converter<E, E>
	{
		protected final Canal<?, E>	self;

		protected final Canal<?, E>	that;

		public UnionOp(Canal<?, E> self, Canal<?, E> that)
		{
			this.self = self;
			this.that = that;
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new UnionPond<E>(self.build(), that.build());
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

	protected static <I, O> Pond<I, O> begin(Pond<I, O> pond)
	{
		if (pond.upstream() != null)
		{
			begin(pond.upstream());
		}
		pond.begin();
		return pond;
	}

	/**
	 * Make a None object.<br />
	 * Call like {@code Canal.<Type>none()}
	 * 
	 * @return
	 */
	public static <E> None<E> none()
	{
		return new None<E>();
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
		return value != null ? some(value) : Canal.<E> none();
	}

	public static <E> Some<E> some(E value)
	{
		return new Some<E>(value);
	}

	private Canal<?, I>		upstream;

	private Operator<I, O>	operator;

	protected Pond<I, O> build()
	{
		return begin(build(null));
	}

	protected Pond<I, O> build(Pond<O, ?> down)
	{
		Pond<I, O> pond = this.newPond();

		if (down != null)
		{
			down.upstream(pond);
		}

		// The upstream of source is null
		if (getUpstream() != null)
		{
			getUpstream().build(pond);
		}

		return pond;
	}

	/**
	 * Make Cartesian product result against another given
	 * {@code Canal<?,N>}.<br />
	 * The output type is {@code Tuple2<O,N>}
	 * 
	 * @param that
	 * @return
	 */
	public <N> Canal<O, Tuple2<O, N>> cartesian(Canal<?, N> that)
	{
		return this.follow(new CartesianOp<O, N>(this, that));
	}

	/**
	 * Collect elements into a Collection.
	 * 
	 * @return
	 */
	public Collection<O> collect()
	{
		return this.collect(null);
	}

	/**
	 * Collect elements into a given Collection.
	 * 
	 * @param result
	 *            The result Collection.
	 * @return
	 */
	public Collection<O> collect(Collection<O> result)
	{
		return this.follow(new CollectOp<O>(result)).evaluate();
	}

	/**
	 * Collect elements into map.<br />
	 * It assumes that the upstream is a pair Canal.<br />
	 * In order to indicate the key/value type <br />
	 * please call like {@code Canal.<K,V>collectAsMap()}
	 * 
	 * @return
	 */
	public <K, V> Map<K, V> collectAsMap()
	{
		return collectAsMap(null);
	}

	/**
	 * Collect elements into a given map.<br />
	 * It assumes that the upstream is a pair Canal.<br />
	 * 
	 * @param result
	 *            The result map.
	 * @return
	 */
	public <K, V> Map<K, V> collectAsMap(Map<K, V> result)
	{
		return collectAsMap(result, null, null);
	}

	/**
	 * Collect elements into a given map with a given KOP and VOP.<br />
	 * 
	 * @param result
	 *            The result map.
	 * @param kop
	 *            The "key of pair" recognizer.
	 * @param vop
	 *            The "value of pair" recognizer.
	 * @return
	 */
	public <K, V> Map<K, V> collectAsMap(Map<K, V> result, Mapper<O, K> kop, Mapper<O, V> vop)
	{
		return this.follow(new CollectAsMapOp<O, K, V>(result, kop, vop)).evaluate();
	}

	/**
	 * Count the number of elements.
	 * 
	 * @return
	 */
	public int count()
	{
		return this.follow(new CountOp<O>()).evaluate();
	}

	/**
	 * Count the number each key.<br />
	 * It assumes that the upstream is a pair Canal.
	 * 
	 * @return
	 */
	public <K> Map<K, Integer> countByKey()
	{
		return countByKey(null);
	}

	/**
	 * Count the number each key into a given result map.<br />
	 * It assumes that the upstream is a pair Canal.
	 * 
	 * @param result
	 * @return
	 */
	public <K> Map<K, Integer> countByKey(Map<K, Integer> result)
	{
		return countByKey(result, null);
	}

	/**
	 * 
	 * Count the number each key into a given result map with a given KOP.<br />
	 * 
	 * @param result
	 * @param kop
	 * @return
	 */
	public <K> Map<K, Integer> countByKey(Map<K, Integer> result, Mapper<O, K> kop)
	{
		return this.follow(new CountByKeyOp<O, K>(result, kop)).evaluate();
	}

	/**
	 * Count the number of each element.
	 * 
	 * @return
	 */
	public Map<O, Integer> countByValue()
	{
		return countByValue(null);
	}

	/**
	 * Count the number of each element into a given result map.
	 * 
	 * @param result
	 * @return
	 */
	public Map<O, Integer> countByValue(Map<O, Integer> result)
	{
		return this.follow(new CountByValueOp<O>(result)).evaluate();
	}

	/**
	 * Remove duplicate elements.
	 * 
	 * @return
	 */
	public Canal<O, O> distinct()
	{
		return this.distinct(null);
	}

	/**
	 * Remove duplicate elements with a given {@link Comparator}.
	 * 
	 * @param cmp
	 * @return
	 */
	public Canal<O, O> distinct(Comparator<O> cmp)
	{
		return this.follow(new DistinctOp<O>(cmp));
	}

	@SuppressWarnings("unchecked")
	protected <T> T evaluate()
	{
		return ((Terminal<I, T>) this.build()).get();
	}

	/**
	 * Filter the elements.
	 * 
	 * @param filter
	 * @return
	 */
	public Canal<O, O> filter(Filter<O> filter)
	{
		return this.follow(new FilterOp<O>(filter));
	}

	/**
	 * Get the first element.
	 * 
	 * @return
	 */
	public Option<O> first()
	{
		return first(new Filter<O>()
		{
			@Override
			public boolean filter(O element)
			{
				return true;
			}
		});
	}

	/**
	 * Get the first element that satisfied the given predicate.
	 * 
	 * @param filter
	 * @return
	 */
	public Option<O> first(Filter<O> filter)
	{
		return this.follow(new FirstOp<O>(filter)).evaluate();
	}

	/**
	 * Get the first element that satisfied the given predicate.
	 * 
	 * @param filter
	 * @return
	 */
	public Option<O> first(IndexedFilter<O> filter)
	{
		return this.follow(new FirstIndexedOp<O>(filter)).evaluate();
	}

	/**
	 * Map each element into a flat result.
	 * 
	 * @param mapper
	 * @return
	 */
	public <N> Canal<O, N> flatMap(IndexedMapper<O, Iterable<N>> mapper)
	{
		return this.follow(new FlatMapIndexedOp<O, N>(mapper));
	}

	/**
	 * Map each element into a flat result.
	 * 
	 * @param mapper
	 * @return
	 */
	public <N> Canal<O, N> flatMap(Mapper<O, Iterable<N>> mapper)
	{
		return this.follow(new FlatMapOp<O, N>(mapper));
	}

	/**
	 * Fold each element with a given initial value.
	 * 
	 * @param init
	 * @param folder
	 * @return
	 */
	public <R> R fold(R init, Reducer<O, R> folder)
	{
		return this.follow(new FoldOp<O, R>(init, folder)).evaluate();
	}

	protected <N> Canal<O, N> follow(Operator<O, N> op)
	{
		return new Canal<O, N>().setUpstream(this).setOperator(op);
	}

	/**
	 * Take action on each element.
	 * 
	 * @param action
	 */
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
		return this.build();
	}

	/**
	 * Map each element.
	 * 
	 * @param mapper
	 * @return
	 */
	public <N> Canal<O, N> map(IndexedMapper<O, N> mapper)
	{
		return this.follow(new MapIndexedOp<O, N>(mapper));
	}

	/**
	 * Map each element.
	 * 
	 * @param mapper
	 * @return
	 */
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

	/**
	 * Reduce each element.
	 * 
	 * @param reducer
	 * @return
	 */
	public Option<O> reduce(Reducer<O, O> reducer)
	{
		return this.follow(new ReduceOp<O>(reducer)).evaluate();
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

	/**
	 * Take first few elements within a given limit number.
	 * 
	 * @param limit
	 * @return
	 */
	public Collection<O> take(int limit)
	{
		return this.take(limit, null);
	}

	/**
	 * Take first few elements within a given limit number into a given result
	 * Collection.
	 * 
	 * @param limit
	 * @param result
	 * @return
	 */
	public Collection<O> take(int limit, Collection<O> result)
	{
		return this.follow(new TakeOp<O>(limit, result)).evaluate();
	}

	/**
	 * Union with another given Canal.
	 * 
	 * @param that
	 * @return
	 */
	public Canal<O, O> union(Canal<?, O> that)
	{
		return this.follow(new UnionOp<O>(this, that));
	}
}
