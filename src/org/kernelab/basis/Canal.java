package org.kernelab.basis;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.JSON.Pair;
import org.kernelab.basis.sql.Row;

public class Canal<D> implements Iterable<D>
{
	public static abstract class AbstractAggregator<O> implements Aggregator<O>
	{
		private Item<?>[]		partBy;

		private Item<?>[]		orderBy;

		private boolean			byRows	= true;

		private Item<Double>[]	between;

		private String			alias;

		@Override
		public String as()
		{
			return alias;
		}

		@Override
		public AbstractAggregator<O> as(String alias)
		{
			this.alias = alias;
			return this;
		}

		@Override
		public Item<Double>[] between()
		{
			return between;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Aggregator<O> between(Item<Double> a, Item<Double> b)
		{
			this.between = new Item[] { a, b };
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public O express(int pos, Object gather, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception
		{
			return (O) gather;
		}

		@Override
		public Object gather(Object acc, Map<String, Object>[] rows, int levelFrom, int levelTo) throws Exception
		{
			return acc;
		}

		@Override
		public Object initial() throws Exception
		{
			return null;
		}

		@Override
		public boolean isByRow()
		{
			return byRows;
		}

		@Override
		public Item<?>[] orderBy()
		{
			return orderBy != null && orderBy.length > 0 ? orderBy : new Item[] { NULL };
		}

		@Override
		public Aggregator<O> orderBy(Item<?>... orders)
		{
			this.orderBy = orders;
			return this;
		}

		@Override
		public Item<?>[] partBy()
		{
			return partBy != null && partBy.length > 0 ? partBy : new Item[] { NULL };
		}

		@Override
		public Aggregator<O> partBy(Item<?>... keys)
		{
			this.partBy = keys;
			return this;
		}

		@Override
		public Aggregator<O> range()
		{
			this.byRows = false;
			return this;
		}

		@Override
		public Aggregator<O> rows()
		{
			this.byRows = true;
			return this;
		}

		@Override
		public Object update(Object gather, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception
		{
			return gather;
		}
	}

	protected static abstract class AbstractPond<U, D> implements Pond<U, D>
	{
		private Pond<?, U> up;

		@Override
		public void close() throws Exception
		{
			if (upstream() != null)
			{
				upstream().close();
			}
		}

		@Override
		public void end() throws Exception
		{
			this.close();
		}

		@Override
		public abstract boolean hasNext();

		@Override
		public abstract D next();

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Pond<?, U> upstream()
		{
			return up;
		}

		@Override
		public void upstream(Pond<?, U> up)
		{
			this.up = up;
		}
	}

	protected static abstract class AbstractTerminal<E, T> extends AbstractPond<E, E> implements Terminal<E, T>
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

	public static interface Action<E>
	{
		public void action(E el) throws Exception;
	}

	/**
	 * initial -> gather -> update -> express
	 * 
	 * @author 6715698
	 *
	 * @param <O>
	 */
	public static interface Aggregator<O> extends Window
	{
		public String as();

		public Aggregator<O> as(String alias);

		public Aggregator<O> between(Item<Double> a, Item<Double> b);

		public O express(int pos, Object gather, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception;

		public Object gather(Object buf, Map<String, Object>[] rows, int levelFrom, int levelTo) throws Exception;

		public Object initial() throws Exception;

		public Aggregator<O> orderBy(Item<?>... keys);

		public Aggregator<O> partBy(Item<?>... keys);

		public Aggregator<O> range();

		public Aggregator<O> rows();

		public Object update(Object gather, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception;
	}

	protected static class ArraySource<E> extends Source<E>
	{
		protected final E[]	array;

		protected final int	begin;

		protected final int	until;

		protected final int	step;

		protected int		index;

		public ArraySource(E[] array, int begin, int until, int step)
		{
			this.array = array;
			this.begin = begin;
			this.until = until;
			this.step = step;
			this.index = this.begin;
		}

		@Override
		public boolean hasNext()
		{
			if (begin < until)
			{
				return step > 0 && index < until;
			}
			else if (begin > until)
			{
				return step < 0 && index > until;
			}
			else
			{
				return false;
			}
		}

		@Override
		public E next()
		{
			try
			{
				return array[index];
			}
			finally
			{
				index += step;
			}
		}
	}

	protected static class ArraySourcer<E> implements Sourcer<E>
	{
		protected final E[]	array;

		protected final int	begin;

		protected final int	until;

		protected final int	step;

		public ArraySourcer(E[] array, Integer begin, Integer until, Integer step)
		{
			int len = array.length;
			int a = begin == null ? Integer.MIN_VALUE : begin;
			int b = until == null ? Integer.MAX_VALUE : until;
			int c = step == null ? 1 : step;

			if (step != null)
			{
				if (begin == null)
				{
					a = step < 0 ? Integer.MAX_VALUE : 0;
				}
				if (until == null)
				{
					b = step < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
				}
			}
			else
			{
				c = a > b ? -1 : 1;
			}

			if (a < 0)
			{
				a += len;
			}
			if (b < 0)
			{
				b += len;
			}

			if (c != 0)
			{
				a = Math.max(Math.min(a, Math.max(len - 1, 0)), 0);
				b = Math.max(Math.min(Math.max(b, -1), len), -len);
			}
			else
			{
				c = 1;
				b = a;
			}

			this.array = array;
			this.begin = a;
			this.until = b;
			this.step = c;
		}

		@Override
		public Source<E> newPond()
		{
			return new ArraySource<E>(array, begin, until, step);
		}
	}

	protected static class CartesianOp<A, B> implements Converter<A, Tuple2<A, B>>
	{
		protected final Canal<B> that;

		public CartesianOp(Canal<B> that)
		{
			if (that == null)
			{
				throw new NullPointerException();
			}
			this.that = that;
		}

		@Override
		public Pond<A, Tuple2<A, B>> newPond()
		{
			return new CartesianPond<A, B>(that);
		}
	}

	protected static class CartesianPond<A, B> extends Dam<A, B, Tuple2<A, B>>
	{
		private Pond<?, B>	there;

		private A			left;

		public CartesianPond(Canal<B> that)
		{
			super(that);
		}

		@Override
		public void begin()
		{
		}

		@Override
		public boolean hasNext()
		{
			while (there == null || !there.hasNext())
			{
				if (!upstream().hasNext())
				{
					return false;
				}
				else
				{
					left = upstream().next();
					there = that.build();
				}
			}
			return true;
		}

		@Override
		public Tuple2<A, B> next()
		{
			return Tuple.of(left, there.next());
		}
	}

	public static class CastMapper<F, T> implements Mapper<F, T>
	{
		@SuppressWarnings("unchecked")
		@Override
		public T map(F el)
		{
			return (T) el;
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
				public void begin() throws Exception
				{
					try
					{
						E el = null;
						while (upstream().hasNext())
						{
							el = upstream().next();
							result.put(kop.map(el), vop.map(el));
						}
					}
					finally
					{
						try
						{
							this.end();
						}
						catch (Exception e)
						{
						}
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

	protected static interface Converter<U, D> extends Operator<U, D>
	{
	}

	protected static class COUNT extends AbstractAggregator<Integer>
	{
		protected final Expr<?> vop;

		public COUNT(Expr<?> vop)
		{
			this.vop = vop;
		}

		@Override
		public Integer update(Object gather, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception
		{
			int count = 0;
			for (int i = winFrom; i < winTo; i++)
			{
				if (vop.map(rows[i]) != null)
				{
					count++;
				}
			}
			return count;
		}
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
				public void begin() throws Exception
				{
					try
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
					finally
					{
						try
						{
							this.end();
						}
						catch (Exception e)
						{
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
					try
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
					finally
					{
						try
						{
							this.end();
						}
						catch (Exception e)
						{
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
			try
			{
				while (upstream().hasNext())
				{
					upstream().next();
					count++;
				}
			}
			finally
			{
				try
				{
					this.end();
				}
				catch (Exception e)
				{
				}
			}
		}

		@Override
		public Integer get()
		{
			return count;
		}
	}

	protected static abstract class Creek<U, D> extends AbstractPond<U, D>
	{
		@Override
		public void begin()
		{
		}

		@Override
		public boolean hasNext()
		{
			return upstream().hasNext();
		}
	}

	protected static abstract class Dam<A, B, C> extends AbstractPond<A, C>
	{
		protected final Canal<B> that;

		public Dam(Canal<B> that)
		{
			this.that = that;
		}
	}

	protected static class DefaultComparator<E> implements Comparator<E>
	{
		@SuppressWarnings("unchecked")
		@Override
		public int compare(E o1, E o2)
		{
			return ((Comparable<E>) o1).compareTo(o2);
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

	protected static class DENSE_RANK extends AbstractAggregator<Integer>
	{
		@Override
		public Item<Double>[] between()
		{
			return null;
		}

		@Override
		public Object gather(Object acc, Map<String, Object>[] rows, int levelFrom, int levelTo) throws Exception
		{
			return ((Integer) acc) + 1;
		}

		@Override
		public Integer initial()
		{
			return 0;
		}
	}

	protected static abstract class Desilter<E> extends Heaper<E> implements Terminal<E, Collection<E>>
	{
		@Override
		public Collection<E> get()
		{
			return sediment;
		}

		@Override
		protected Collection<E> newSediment()
		{
			return new ArrayList<E>();
		}
	}

	protected static class DistinctOp<E> implements Converter<E, E>
	{
		protected final Comparator<E>		cmp;

		protected final HashedEquality<E>	eql;

		public DistinctOp(Comparator<E> cmp)
		{
			this.cmp = cmp;
			this.eql = null;
		}

		public DistinctOp(HashedEquality<E> eql)
		{
			this.cmp = null;
			this.eql = eql;
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new Heaper<E>()
			{
				@Override
				protected Collection<E> newSediment()
				{
					if (cmp != null)
					{
						return new TreeSet<E>(cmp);
					}
					else if (eql != null)
					{
						return new WrappedLinkedHashSet<E>(eql);
					}
					else
					{
						return new LinkedHashSet<E>();
					}
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
			throw new UnsupportedOperationException();
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

	public static class EnumerationIterator<E> implements Iterator<E>
	{
		protected final Enumeration<E> enumer;

		public EnumerationIterator(Enumeration<E> enumer)
		{
			if (enumer == null)
			{
				throw new NullPointerException();
			}
			this.enumer = enumer;
		}

		@Override
		public boolean hasNext()
		{
			return enumer.hasMoreElements();
		}

		@Override
		public E next()
		{
			return enumer.nextElement();
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	protected static interface Evaluator<E, T> extends Operator<E, E>
	{
		@Override
		Terminal<E, T> newPond();
	}

	public static interface Expr<T> extends Mapper<Map<String, Object>, T>
	{
	}

	protected static class FilterOp<E> implements Converter<E, E>
	{
		protected final Filter<? super E> filter;

		protected FilterOp(Filter<? super E> filter)
		{
			if (filter == null)
			{
				throw new NullPointerException();
			}
			this.filter = filter;
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new Creek<E, E>()
			{
				private E next;

				@Override
				public boolean hasNext()
				{
					try
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
				public E next()
				{
					return next;
				}
			};
		}
	}

	protected static class FIRST_VALUE<T> extends AbstractAggregator<T>
	{
		protected final Expr<T>	vop;

		protected final boolean	ignoreNulls;

		public FIRST_VALUE(Expr<T> vop, boolean ignoreNulls)
		{
			this.vop = vop;
			this.ignoreNulls = ignoreNulls;
		}

		@Override
		public T update(Object gather, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception
		{
			T d = null;
			for (int i = winFrom; i < winTo; i++)
			{
				if ((d = vop.map(rows[i])) != null || !ignoreNulls)
				{
					return d;
				}
			}
			return null;
		}
	}

	protected static class FirstOp<E> implements Evaluator<E, Option<E>>
	{
		protected final Filter<? super E> filter;

		public FirstOp(Filter<? super E> filter)
		{
			if (filter == null)
			{
				throw new NullPointerException();
			}
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
				public void begin() throws Exception
				{
					try
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
					finally
					{
						try
						{
							this.end();
						}
						catch (Exception e)
						{
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

	protected static class FlatMapOp<I, O> implements Converter<I, O>
	{
		protected final Mapper<I, Iterable<O>> mapper;

		public FlatMapOp(Mapper<I, Iterable<O>> mapper)
		{
			if (mapper == null)
			{
				throw new NullPointerException();
			}
			this.mapper = mapper;
		}

		@Override
		public Pond<I, O> newPond()
		{
			return new Creek<I, O>()
			{
				private Iterator<O> iter;

				@Override
				public boolean hasNext()
				{
					if (iter != null && iter.hasNext())
					{
						return true;
					}
					try
					{
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
			if (folder == null)
			{
				throw new NullPointerException();
			}
			this.result = init;
			this.folder = folder;
		}

		@Override
		public Terminal<E, R> newPond()
		{
			return new AbstractTerminal<E, R>()
			{
				@Override
				public void begin() throws Exception
				{
					try
					{
						while (upstream().hasNext())
						{
							result = folder.reduce(result, upstream().next());
						}
					}
					finally
					{
						try
						{
							this.end();
						}
						catch (Exception e)
						{
						}
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

	protected static class FoldUntilOp<E, R> implements Evaluator<E, Option<R>>
	{
		protected final Producer<R>		init;

		protected final Reducer<E, R>	folder;

		protected final Filter<R>		until;

		private R						result;

		public FoldUntilOp(Producer<R> init, Reducer<E, R> folder, Filter<R> until)
		{
			if (init == null || folder == null)
			{
				throw new NullPointerException();
			}
			this.init = init;
			this.folder = folder;
			this.until = until;
		}

		@Override
		public Terminal<E, Option<R>> newPond()
		{
			return new AbstractTerminal<E, Option<R>>()
			{
				private boolean	empty	= true;

				private boolean	meet	= false;

				@Override
				public void begin() throws Exception
				{
					try
					{
						meet = until == null;
						while (upstream().hasNext())
						{
							if (empty)
							{
								empty = false;
								result = init.produce();
							}
							result = folder.reduce(result, upstream().next());
							if (until != null && until.filter(result))
							{
								meet = true;
								break;
							}
						}
					}
					finally
					{
						try
						{
							this.end();
						}
						catch (Exception e)
						{
						}
					}
				}

				@Override
				public Option<R> get()
				{
					return meet && !empty ? Canal.some(result) : Canal.<R> none();
				}
			};
		}
	}

	protected static class ForallOp<E> implements Evaluator<E, Boolean>
	{
		protected final Filter<E> cond;

		public ForallOp(Filter<E> cond)
		{
			if (cond == null)
			{
				throw new NullPointerException();
			}
			this.cond = cond;
		}

		@Override
		public Terminal<E, Boolean> newPond()
		{
			return new ForallPond<E>(cond);
		}
	}

	protected static class ForallPond<E> extends AbstractTerminal<E, Boolean>
	{
		protected final Filter<E>	cond;

		protected boolean			meet	= false;

		public ForallPond(Filter<E> cond)
		{
			this.cond = cond;
		}

		@Override
		public void begin() throws Exception
		{
			try
			{
				while (upstream().hasNext())
				{
					if (!cond.filter(upstream().next()))
					{
						return;
					}
				}
				meet = true;
			}
			finally
			{
				try
				{
					this.end();
				}
				catch (Exception e)
				{
				}
			}
		}

		@Override
		public Boolean get()
		{
			return meet;
		}
	}

	protected static class ForeachOp<E> implements Evaluator<E, Void>
	{
		protected final Action<E> action;

		public ForeachOp(Action<E> action)
		{
			if (action == null)
			{
				throw new NullPointerException();
			}
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
		public void begin() throws Exception
		{
			try
			{
				while (upstream().hasNext())
				{
					action.action(upstream().next());
				}
			}
			finally
			{
				try
				{
					this.end();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}

		@Override
		public Void get()
		{
			return null;
		}
	}

	protected static class FullJoiner<L, R, K, U, V> extends Joiner<L, R, K, U, V, Option<U>, Option<V>>
	{
		public FullJoiner(Canal<R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
		{
			super(that, kol, kor, vol, vor);
		}

		@Override
		protected Set<K> keys(Set<K> left, Set<K> right)
		{
			return (Set<K>) Canal.of(left).union(Canal.of(right)).collect(new LinkedHashSet<K>());
		}

		@Override
		protected boolean needLeft(boolean isEmpty)
		{
			return true;
		}

		@Override
		protected boolean needRight(boolean isEmpty)
		{
			return true;
		}

		@Override
		protected Option<U> valLeft()
		{
			return Option.none();
		}

		@Override
		protected Option<U> valLeft(U u)
		{
			return Option.some(u);
		}

		@Override
		protected Option<V> valRight()
		{
			return Canal.none();
		}

		@Override
		protected Option<V> valRight(V v)
		{
			return Option.some(v);
		}
	}

	protected static class FullJoinOp<L, R, K, U, V> extends JoinOp<L, R, K, U, V>
			implements Converter<L, Tuple2<K, Tuple2<Option<U>, Option<V>>>>
	{
		public FullJoinOp(Canal<R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
		{
			super(that, kol, kor, vol, vor);
		}

		@Override
		public Pond<L, Tuple2<K, Tuple2<Option<U>, Option<V>>>> newPond()
		{
			return new FullJoiner<L, R, K, U, V>(that, kol, kor, vol, vor);
		}
	}

	protected static class GeneratedSource<E> extends Source<E>
	{
		protected final Producer<E> generator;

		public GeneratedSource(Producer<E> generator)
		{
			this.generator = generator;
		}

		@Override
		public void close() throws Exception
		{
			if (generator instanceof Closeable)
			{
				((Closeable) generator).close();
			}
		}

		@Override
		public boolean hasNext()
		{
			return true;
		}

		@Override
		public E next()
		{
			try
			{
				return generator.produce();
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
	}

	protected static class GeneratedSourcer<E> implements Sourcer<E>
	{
		protected final Producer<E> generator;

		public GeneratedSourcer(Producer<E> generator)
		{
			if (generator == null)
			{
				throw new NullPointerException();
			}
			this.generator = generator;
		}

		@Override
		public Source<E> newPond()
		{
			return new GeneratedSource<E>(generator);
		}
	}

	protected static class GroupByOp<E, K, V> implements Converter<E, Tuple2<K, Canal<V>>>
	{
		protected final Mapper<E, K>	kop;

		protected final Mapper<E, V>	vop;

		public GroupByOp(Mapper<E, K> kop, Mapper<E, V> vop)
		{
			if (kop == null || vop == null)
			{
				throw new NullPointerException();
			}
			this.kop = kop;
			this.vop = vop;
		}

		@Override
		public Pond<E, Tuple2<K, Canal<V>>> newPond()
		{
			return new Grouper<E, K, V>(kop, vop);
		}
	}

	protected static class Grouper<E, K, V> extends AbstractPond<E, Tuple2<K, Canal<V>>>
	{
		protected final Mapper<E, K>		kop;

		protected final Mapper<E, V>		vop;

		protected final Map<K, List<V>>		sediment;

		private Iterator<Entry<K, List<V>>>	iter;

		public Grouper(Mapper<E, K> kop, Mapper<E, V> vop)
		{
			this.kop = kop;
			this.vop = vop;
			this.sediment = new LinkedHashMap<K, List<V>>();
		}

		@Override
		public void begin() throws Exception
		{
			E el = null;
			K key = null;
			List<V> list = null;
			while (upstream().hasNext())
			{
				el = upstream().next();
				key = kop.map(el);
				if (!sediment.containsKey(key))
				{
					sediment.put(key, list = new ArrayList<V>());
				}
				else
				{
					list = sediment.get(key);
				}
				list.add(vop.map(el));
			}
			this.iter = sediment.entrySet().iterator();
		}

		@Override
		public boolean hasNext()
		{
			return iter.hasNext();
		}

		@Override
		public Tuple2<K, Canal<V>> next()
		{
			Entry<K, List<V>> entry = iter.next();
			return Tuple.<K, Canal<V>> of(entry.getKey(), Canal.of(entry.getValue()));
		}
	}

	protected static abstract class Heaper<E> extends AbstractPond<E, E>
	{
		protected final Collection<E>	sediment	= this.newSediment();

		private Iterator<E>				iter;

		@Override
		public void begin()
		{
			this.settle();
			this.iter = sediment.iterator();
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

	protected static class InnerJoiner<L, R, K, U, V> extends Joiner<L, R, K, U, V, U, V>
	{
		public InnerJoiner(Canal<R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
		{
			super(that, kol, kor, vol, vor);
		}

		@Override
		protected Set<K> keys(Set<K> left, Set<K> right)
		{
			return (Set<K>) Canal.of(left).intersection(Canal.of(right)).collect(new LinkedHashSet<K>());
		}

		@Override
		protected boolean needLeft(boolean isEmpty)
		{
			return !isEmpty;
		}

		@Override
		protected boolean needRight(boolean isEmpty)
		{
			return !isEmpty;
		}

		@Override
		protected U valLeft()
		{
			return null;
		}

		@Override
		protected U valLeft(U u)
		{
			return u;
		}

		@Override
		protected V valRight()
		{
			return null;
		}

		@Override
		protected V valRight(V v)
		{
			return v;
		}
	}

	protected static class InnerJoinOp<L, R, K, U, V> extends JoinOp<L, R, K, U, V>
			implements Converter<L, Tuple2<K, Tuple2<U, V>>>
	{
		public InnerJoinOp(Canal<R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
		{
			super(that, kol, kor, vol, vor);
		}

		@Override
		public Pond<L, Tuple2<K, Tuple2<U, V>>> newPond()
		{
			return new InnerJoiner<L, R, K, U, V>(that, kol, kor, vol, vor);
		}
	}

	protected static class IntersectionOp<E> implements Converter<E, E>
	{
		protected final Canal<E>		that;

		protected final Comparator<E>	cmp;

		@SuppressWarnings("unchecked")
		public IntersectionOp(Canal<? extends E> that, Comparator<E> cmp)
		{
			if (that == null)
			{
				throw new NullPointerException();
			}
			this.that = (Canal<E>) that;
			this.cmp = cmp;
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new IntersectionPond<E>(that, cmp);
		}
	}

	protected static class IntersectionPond<E> extends Dam<E, E, E>
	{
		protected final Comparator<E>	cmp;

		private Set<E>					there;

		private E						here;

		public IntersectionPond(Canal<E> that, Comparator<E> cmp)
		{
			super(that);
			this.cmp = cmp;
		}

		@Override
		public void begin()
		{
			there = (Set<E>) that.collect(cmp == null ? new HashSet<E>() : new TreeSet<E>(cmp));
		}

		@Override
		public boolean hasNext()
		{
			while (upstream().hasNext())
			{
				here = upstream().next();
				if (there.contains(here))
				{
					return true;
				}
			}
			return false;
		}

		@Override
		public E next()
		{
			return here;
		}
	}

	protected static class InverseComparator<E> implements Comparator<E>
	{
		protected final Comparator<E> cmp;

		public InverseComparator(Comparator<E> cmp)
		{
			this.cmp = cmp;
		}

		@Override
		public int compare(E o1, E o2)
		{
			return cmp.compare(o2, o1);
		}
	}

	public static abstract class Item<T extends Comparable<T>> implements Expr<T>
	{
		private Boolean	asc			= null;

		private Boolean	nullsLast	= null;

		public Item<T> asc()
		{
			this.asc = true;
			return this;
		}

		public Item<T> desc()
		{
			this.asc = false;
			return this;
		}

		public int getFactor()
		{
			return isAscend() ? 1 : -1;
		}

		public boolean isAscend()
		{
			return asc == null || asc == true;
		}

		public boolean isNullsLast()
		{
			return nullsLast != null ? nullsLast : this.isAscend();
		}

		public Item<T> nullsFirst()
		{
			this.nullsLast = false;
			return this;
		}

		public Item<T> nullsLast()
		{
			this.nullsLast = true;
			return this;
		}
	}

	protected static class IterablePairSourcer<K, V> implements Sourcer<Entry<K, V>>
	{
		protected final Iterable<Entry<K, V>> iter;

		@SuppressWarnings("unchecked")
		public IterablePairSourcer(Iterable<?> iter)
		{
			if (iter == null)
			{
				throw new NullPointerException();
			}
			this.iter = (Iterable<Entry<K, V>>) iter;
		}

		@Override
		public Source<Entry<K, V>> newPond()
		{
			return new IteratorSource<Entry<K, V>>(iter.iterator());
		}
	}

	protected static class IterableSourcer<E> implements Sourcer<E>
	{
		protected final Iterable<E> iter;

		public IterableSourcer(Iterable<E> iter)
		{
			if (iter == null)
			{
				throw new NullPointerException();
			}
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
		public void close() throws Exception
		{
			if (iter instanceof Closeable)
			{
				((Closeable) iter).close();
			}
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

	public static class JoinCanal<K, L, R> extends PairCanal<K, Tuple2<L, R>>
	{
		/**
		 * Map each joint in this Canal.
		 * 
		 * @param mapper
		 * @return
		 */
		public <W> Canal<W> mapJoint(final JointMapper<L, R, K, W> mapper)
		{
			return this.map(new Mapper<Tuple2<K, Tuple2<L, R>>, W>()
			{
				@Override
				public W map(Tuple2<K, Tuple2<L, R>> el)
				{
					return mapper.map(el._2._1, el._2._2, el._1);
				}
			});
		}
	}

	protected static abstract class Joiner<L, R, K, U, V, M, N> extends AbstractPond<L, Tuple2<K, Tuple2<M, N>>>
	{
		protected final Canal<R>		that;

		protected final Mapper<L, K>	kol;

		protected final Mapper<R, K>	kor;

		protected final Mapper<L, U>	vol;

		protected final Mapper<R, V>	vor;

		private Map<K, List<U>>			here;

		private Map<K, List<V>>			there;

		private Iterator<K>				iterK;

		private boolean					isEmptyK;

		private Iterator<U>				iterU;

		private Iterator<V>				iterV;

		private List<U>					listU;

		private List<V>					listV;

		private final List<U>			emptyU	= new ArrayList<U>();

		private final List<V>			emptyV	= new ArrayList<V>();

		private boolean					isEmptyU;

		private boolean					isEmptyV;

		private K						k;

		private M						m;

		private N						n;

		private final M					missM;

		private final N					missN;

		private byte					hasM;

		private byte					hasN;

		public Joiner(Canal<R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
		{
			this.that = that;
			this.kol = kol;
			this.kor = kor;
			this.vol = vol;
			this.vor = vor;
			this.missM = valLeft();
			this.missN = valRight();
		}

		@Override
		public void begin() throws Exception
		{
			this.here = group(this.upstream(), this.kol, this.vol);
			this.there = group(that.build(), this.kor, this.vor);
			this.iterK = keys(here.keySet(), there.keySet()).iterator();
			this.isEmptyK = !this.nextK();
		}

		@Override
		public boolean hasNext()
		{
			if (isEmptyK)
			{
				return false;
			}

			while (true)
			{
				if (iterV.hasNext())
				{
					n = valRight(iterV.next());
					hasN = 2;
				}
				else if (isEmptyV && needRight(isEmptyV))
				{
					n = missN;
					hasN = 1;
				}
				else
				{
					hasN = 0;
				}

				if (hasN < 2 || hasM < 2)
				{
					if (iterU.hasNext())
					{
						m = valLeft(iterU.next());
						hasM = 2;
						if (hasN == 0)
						{
							iterV = listV.iterator();
						}
					}
					else if (isEmptyU && needLeft(isEmptyU))
					{
						m = missM;
						hasM = 1;
					}
					else
					{
						hasM = 0;
					}
				}

				if (hasN <= 1 && hasM <= 1)
				{
					if (!iterK.hasNext())
					{
						return false;
					}
					this.nextK();
				}
				else if (hasN > 0 && hasM > 0)
				{
					break;
				}
			}

			return true;
		}

		protected abstract Set<K> keys(Set<K> left, Set<K> right);

		protected abstract boolean needLeft(boolean isEmpty);

		protected abstract boolean needRight(boolean isEmpty);

		@Override
		public Tuple2<K, Tuple2<M, N>> next()
		{
			return Tuple.of(k, Tuple.of(m, n));
		}

		private boolean nextK()
		{
			if (!iterK.hasNext())
			{
				return false;
			}

			k = iterK.next();
			listU = here.get(k);
			listV = there.get(k);
			listU = listU != null ? listU : emptyU;
			listV = listV != null ? listV : emptyV;
			isEmptyU = listU.isEmpty();
			isEmptyV = listV.isEmpty();
			iterU = listU.iterator();
			iterV = listV.iterator();
			hasM = 0;
			hasN = 0;

			return true;
		}

		protected abstract M valLeft();

		protected abstract M valLeft(U u);

		protected abstract N valRight();

		protected abstract N valRight(V v);
	}

	protected static abstract class JoinOp<L, R, K, U, V>
	{
		protected final Canal<R>		that;

		protected final Mapper<L, K>	kol;

		protected final Mapper<R, K>	kor;

		protected final Mapper<L, U>	vol;

		protected final Mapper<R, V>	vor;

		public JoinOp(Canal<R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
		{
			if (that == null)
			{
				throw new NullPointerException();
			}
			this.that = that;
			this.kol = kol;
			this.kor = kor;
			this.vol = vol;
			this.vor = vor;
		}
	}

	public static interface JointMapper<L, R, K, V>
	{
		public V map(L left, R right, K key);
	}

	protected static class LAG<T> extends AbstractAggregator<T>
	{
		protected final Expr<T>	vop;

		protected final int		offset;

		protected final T		deft;

		public LAG(Expr<T> vop, int offset, T deft)
		{
			this.vop = vop;
			this.offset = Math.abs(offset);
			this.deft = deft;
		}

		@Override
		public Item<Double>[] between()
		{
			return null;
		}

		@Override
		public T express(int pos, Object acc, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception
		{
			int index = pos - offset;
			return index < 0 ? deft : vop.map(rows[index]);
		}
	}

	protected static class LAST_VALUE<T> extends AbstractAggregator<T>
	{
		protected final Expr<T>	vop;

		protected final boolean	ignoreNulls;

		public LAST_VALUE(Expr<T> vop, boolean ignoreNulls)
		{
			this.vop = vop;
			this.ignoreNulls = ignoreNulls;
		}

		@Override
		public T update(Object acc, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception
		{
			T d = null;
			for (int i = winTo - 1; i >= winFrom; i--)
			{
				if ((d = vop.map(rows[i])) != null || !ignoreNulls)
				{
					return d;
				}
			}
			return null;
		}
	}

	protected static class LastOp<E> implements Evaluator<E, Option<E>>
	{
		protected final Filter<E> filter;

		public LastOp(Filter<E> filter)
		{
			if (filter == null)
			{
				throw new NullPointerException();
			}
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
				public void begin() throws Exception
				{
					try
					{
						E find = null;
						while (upstream().hasNext())
						{
							find = upstream().next();
							if (filter.filter(find))
							{
								result = find;
								found = true;
							}
						}
					}
					finally
					{
						try
						{
							this.end();
						}
						catch (Exception e)
						{
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

	protected static class LEAD<T> extends AbstractAggregator<T>
	{
		protected final Expr<T>	vop;

		protected final int		offset;

		protected final T		deft;

		public LEAD(Expr<T> vop, int offset, T deft)
		{
			this.vop = vop;
			this.offset = Math.abs(offset);
			this.deft = deft;
		}

		@Override
		public Item<Double>[] between()
		{
			return null;
		}

		@Override
		public T express(int pos, Object acc, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception
		{
			int index = pos + offset;
			return index >= rows.length ? deft : vop.map(rows[index]);
		}
	}

	protected static class LeftJoiner<L, R, K, U, V> extends Joiner<L, R, K, U, V, U, Option<V>>
	{
		public LeftJoiner(Canal<R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
		{
			super(that, kol, kor, vol, vor);
		}

		@Override
		protected Set<K> keys(Set<K> left, Set<K> right)
		{
			return left;
		}

		@Override
		protected boolean needLeft(boolean isEmpty)
		{
			return !isEmpty;
		}

		@Override
		protected boolean needRight(boolean isEmpty)
		{
			return true;
		}

		@Override
		protected U valLeft()
		{
			return null;
		}

		@Override
		protected U valLeft(U u)
		{
			return u;
		}

		@Override
		protected Option<V> valRight()
		{
			return Option.none();
		}

		@Override
		protected Option<V> valRight(V v)
		{
			return Option.some(v);
		}
	}

	protected static class LeftJoinOp<L, R, K, U, V> extends JoinOp<L, R, K, U, V>
			implements Converter<L, Tuple2<K, Tuple2<U, Option<V>>>>
	{
		public LeftJoinOp(Canal<R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
		{
			super(that, kol, kor, vol, vor);
		}

		@Override
		public Pond<L, Tuple2<K, Tuple2<U, Option<V>>>> newPond()
		{
			return new LeftJoiner<L, R, K, U, V>(that, kol, kor, vol, vor);
		}
	}

	protected static class LimitOp<E> implements Converter<E, E>
	{
		protected final int limit;

		public LimitOp(int limit)
		{
			this.limit = limit;
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new Wheel<E, E>()
			{
				@Override
				public boolean hasNext()
				{
					return (limit < 0 || index < limit) && upstream().hasNext();
				}

				@Override
				public E next()
				{
					index++;
					return upstream().next();
				}
			};
		}
	}

	protected static class MapOp<I, O> implements Converter<I, O>
	{
		protected final Mapper<I, O> mapper;

		public MapOp(Mapper<I, O> mapper)
		{
			if (mapper == null)
			{
				throw new NullPointerException();
			}
			this.mapper = mapper;
		}

		@Override
		public Pond<I, O> newPond()
		{
			return new Creek<I, O>()
			{
				@Override
				public O next()
				{
					try
					{
						return mapper.map(upstream().next());
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
			};
		}
	}

	protected static class MappedComparator<O, M extends Comparable<M>> implements Comparator<O>
	{
		protected final Mapper<O, M> mapper;

		public MappedComparator(Mapper<O, M> mapper)
		{
			this.mapper = mapper;
		}

		@Override
		public int compare(O o1, O o2)
		{
			try
			{
				return mapper.map(o1).compareTo(mapper.map(o2));
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
	}

	protected static class MapWithStateOp<I, S, O> implements Converter<I, O>
	{
		protected final Producer<S>				stater;

		protected final StatefulMapper<I, S, O>	mapper;

		public MapWithStateOp(Producer<S> stater, StatefulMapper<I, S, O> mapper)
		{
			if (stater == null || mapper == null)
			{
				throw new NullPointerException();
			}
			this.stater = stater;
			this.mapper = mapper;
		}

		@Override
		public Pond<I, O> newPond()
		{
			return new Creek<I, O>()
			{
				private boolean	init	= false;

				private S		stat	= null;

				@Override
				public O next()
				{
					try
					{
						if (!init)
						{
							stat = stater.produce();
							init = true;
						}
						return mapper.map(upstream().next(), stat);
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
			};
		}
	}

	protected static class MatchFindIterable implements Iterable<Matcher>
	{
		protected final Pattern			regex;

		protected final CharSequence	text;

		public MatchFindIterable(Pattern regex, CharSequence text)
		{
			if (regex == null || text == null)
			{
				throw new NullPointerException();
			}
			this.regex = regex;
			this.text = text;
		}

		@Override
		public Iterator<Matcher> iterator()
		{
			return new MatchFindIterator(regex, text);
		}
	}

	protected static class MatchFindIterator implements Iterator<Matcher>
	{
		protected final Matcher	m;

		private Boolean			find;

		public MatchFindIterator(Pattern regex, CharSequence text)
		{
			this.m = regex.matcher(text);
			this.find = m.find();
		}

		@Override
		public boolean hasNext()
		{
			if (find == null)
			{
				find = m.find();
			}
			return find;
		}

		@Override
		public Matcher next()
		{
			try
			{
				return m;
			}
			finally
			{
				find = null;
			}
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	protected static class MAX<T extends Comparable<T>> extends AbstractAggregator<T>
	{
		protected final Expr<T> vop;

		public MAX(Expr<T> vop)
		{
			this.vop = vop;
		}

		@Override
		public T update(Object acc, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception
		{
			T m = null, o = null;
			for (int i = winFrom; i < winTo; i++)
			{
				if ((o = vop.map(rows[i])) != null)
				{
					if (m == null || o.compareTo(m) > 0)
					{
						m = o;
					}
				}
			}
			return m;
		}
	}

	protected static class MIN<T extends Comparable<T>> extends AbstractAggregator<T>
	{
		protected final Expr<T> vop;

		public MIN(Expr<T> vop)
		{
			this.vop = vop;
		}

		@Override
		public T update(Object acc, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception
		{
			T m = null, o = null;
			for (int i = winFrom; i < winTo; i++)
			{
				if ((o = vop.map(rows[i])) != null)
				{
					if (m == null || o.compareTo(m) < 0)
					{
						m = o;
					}
				}
			}
			return m;
		}
	}

	public static class None<E> extends Option<E>
	{
		public None()
		{
			this.setOperator(new EmptySourcer<E>());
		}

		@Override
		public E get() throws NoSuchElementException
		{
			throw new NoSuchElementException();
		}

		@Override
		public E get(Producer<Exception> raiser) throws Exception
		{
			if (raiser != null)
			{
				Exception ex = null;
				try
				{
					ex = raiser.produce();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				if (ex != null)
				{
					throw ex;
				}
			}
			return this.get();
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
		public E or(Producer<? extends E> defaultProducer)
		{
			try
			{
				return defaultProducer.produce();
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

		@SuppressWarnings("unchecked")
		@Override
		public Option<E> orElse(Option<? extends E> opt)
		{
			return (Option<E>) opt;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Option<E> orElse(Producer<Option<? extends E>> opt)
		{
			try
			{
				return (Option<E>) opt.produce();
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

	protected static interface Operator<I, O>
	{
		Pond<I, O> newPond();
	}

	public static abstract class Option<E> extends Canal<E>
	{
		protected static final None<?> NONE = new None<Object>();

		@SuppressWarnings("unchecked")
		public static <T> None<T> none()
		{
			return (None<T>) NONE;
		}

		/**
		 * Please use {@link Option#Of(Object)} instead, in order to avoid
		 * ambiguous of {@code Canal.of(...)} methods.
		 */
		@Deprecated
		@SuppressWarnings("unchecked")
		public static <T> Option<T> of(T value)
		{
			return value != null ? new Some<T>(value) : (None<T>) NONE;
		}

		/**
		 * Get Option object by value.
		 * 
		 * @param value
		 * @return {@code None} if {@code value == null} otherwise
		 *         {@code Some(value)} will be returned.
		 */
		@SuppressWarnings("unchecked")
		public static <T> Option<T> Of(T value)
		{
			return value != null ? new Some<T>(value) : (None<T>) NONE;
		}

		public static <T> T or(T value, Producer<? extends T> deft)
		{
			try
			{
				return value != null ? value : deft.produce();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}
		}

		public static <T> T or(T value, T deft)
		{
			return value != null ? value : deft;
		}

		public static <T> Some<T> some(T value)
		{
			return new Some<T>(value);
		}

		@Override
		public Option<E> filter(Filter<? super E> filter)
		{
			try
			{
				if (this.given() && filter.filter(this.get()))
				{
					return this;
				}
				else
				{
					return none();
				}
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

		public abstract E get();

		public abstract E get(Producer<Exception> raiser) throws Exception;

		public abstract boolean given();

		@Override
		public <F> Option<F> map(Mapper<E, F> mapper)
		{
			if (this.given())
			{
				try
				{
					return some(mapper.map(this.get()));
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
			else
			{
				return none();
			}
		}

		public abstract E or(E defaultValue);

		public abstract E or(Producer<? extends E> defaultProducer);

		public abstract Option<E> orElse(Option<? extends E> opt);

		public abstract Option<E> orElse(Producer<Option<? extends E>> opt);

		public abstract E orNull();
	}

	public static class PairCanal<K, V> extends Canal<Tuple2<K, V>>
	{
		/**
		 * Collect elements into map.<br />
		 * It assumes that the upstream is a pair Canal.<br />
		 * In order to indicate the key/value type <br />
		 * please call like {@code Canal.<K,V>collectAsMap()}
		 * 
		 * @return
		 */
		public Map<K, V> collectAsMap()
		{
			return collectAsMap(null);
		}

		/**
		 * Count the number each key.<br />
		 * It assumes that the upstream is a pair Canal.
		 * 
		 * @return
		 */
		public Map<K, Integer> countByKey()
		{
			return countByKey(null);
		}

		/**
		 * Folder each value within same group.
		 * 
		 * @param initiator
		 * @param folder
		 * @return
		 */
		public <W> PairCanal<K, W> foldByKey(final Producer<W> initiator, final Reducer<V, W> folder)
		{
			return this.groupByKey().mapValues(new Mapper<Canal<V>, W>()
			{
				@Override
				public W map(Canal<V> el) throws Exception
				{
					return el.fold(initiator.produce(), folder);
				}
			});
		}

		/**
		 * Full join with another Canal.
		 * 
		 * @param that
		 * @return
		 */
		public <W> JoinCanal<K, Option<V>, Option<W>> fullJoin(Canal<Tuple2<K, W>> that)
		{
			return fullJoin(that, new DefaultKop<Tuple2<K, V>, K>(), new DefaultKop<Tuple2<K, W>, K>())
					.<K, Tuple2<Option<V>, Option<W>>> toPair().toJoin();
		}

		/**
		 * Gather each value into correspondent group identified by same key.
		 * 
		 * @return
		 */
		public PairCanal<K, Canal<V>> groupByKey()
		{
			return this.groupBy(new DefaultKop<Tuple2<K, V>, K>(), new DefaultVop<Tuple2<K, V>, V>());
		}

		/**
		 * Filter pairs whose value statisfy the pred.
		 * 
		 * @param pred
		 * @return
		 */
		public PairCanal<K, V> having(final Filter<V> pred)
		{
			return this.filter(new Filter<Tuple2<K, V>>()
			{
				@Override
				public boolean filter(Tuple2<K, V> el) throws Exception
				{
					return pred.filter(el._2);
				}
			}).toPair();
		}

		/**
		 * Inner join with another Canal.
		 * 
		 * @param that
		 * @return
		 */
		public <W> JoinCanal<K, V, W> join(Canal<Tuple2<K, W>> that)
		{
			return join(that, new DefaultKop<Tuple2<K, V>, K>(), new DefaultKop<Tuple2<K, W>, K>())
					.<K, Tuple2<V, W>> toPair().toJoin();
		}

		/**
		 * Pass each key of pair to downstream.
		 * 
		 * @return
		 */
		public Canal<K> keys()
		{
			return this.map(new DefaultKop<Tuple2<K, V>, K>());
		}

		/**
		 * Left join with another Canal.
		 * 
		 * @param that
		 * @return
		 */
		public <W> JoinCanal<K, V, Option<W>> leftJoin(Canal<Tuple2<K, W>> that)
		{
			return leftJoin(that, new DefaultKop<Tuple2<K, V>, K>(), new DefaultKop<Tuple2<K, W>, K>())
					.<K, Tuple2<V, Option<W>>> toPair().toJoin();
		}

		/**
		 * Map each value in pair into a new value.
		 * 
		 * @param mapper
		 * @return
		 */
		public <W> PairCanal<K, W> mapValues(final Mapper<V, W> mapper)
		{
			return this.map(new Mapper<Tuple2<K, V>, Tuple2<K, W>>()
			{
				@Override
				public Tuple2<K, W> map(Tuple2<K, V> el) throws Exception
				{
					return Tuple.of(el._1, mapper.map(el._2));
				}
			}).toPair();
		}

		/**
		 * Map each value in pair into a new value according to its key.
		 * 
		 * @param mapper
		 *            {@code (V,K) -> W}
		 * @return
		 */
		public <W> PairCanal<K, W> mapValues(final StatefulMapper<V, K, W> mapper)
		{
			return this.map(new Mapper<Tuple2<K, V>, Tuple2<K, W>>()
			{
				@Override
				public Tuple2<K, W> map(Tuple2<K, V> el) throws Exception
				{
					return Tuple.of(el._1, mapper.map(el._2, el._1));
				}
			}).toPair();
		}

		/**
		 * Reduce each value within same group.
		 * 
		 * @param reducer
		 * @return
		 */
		public PairCanal<K, V> reduceByKey(final Reducer<V, V> reducer)
		{
			return this.groupByKey().mapValues(new Mapper<Canal<V>, V>()
			{
				@Override
				public V map(Canal<V> el)
				{
					return el.reduce(reducer).get();
				}
			});
		}

		/**
		 * Right join with another Canal.
		 * 
		 * @param that
		 * @return
		 */
		public <W> JoinCanal<K, Option<V>, W> rightJoin(Canal<Tuple2<K, W>> that)
		{
			return rightJoin(that, new DefaultKop<Tuple2<K, V>, K>(), new DefaultKop<Tuple2<K, W>, K>())
					.<K, Tuple2<Option<V>, W>> toPair().toJoin();
		}

		@SuppressWarnings("unchecked")
		public <L, R> JoinCanal<K, L, R> toJoin()
		{
			return (JoinCanal<K, L, R>) new JoinCanal<K, L, R>().setUpstream(this).setOperator(
					new MapOp<Tuple2<K, V>, Tuple2<K, Tuple2<L, R>>>(new Mapper<Tuple2<K, V>, Tuple2<K, Tuple2<L, R>>>()
					{
						@Override
						public Tuple2<K, Tuple2<L, R>> map(Tuple2<K, V> el)
						{
							return (Tuple2<K, Tuple2<L, R>>) el;
						}
					}));
		}

		/**
		 * Pass each value of pair to downstream.
		 * 
		 * @return
		 */
		public Canal<V> values()
		{
			return this.map(new DefaultVop<Tuple2<K, V>, V>());
		}
	}

	protected static class PeekOp<E> implements Converter<E, E>
	{
		protected final Action<E> action;

		public PeekOp(Action<E> action)
		{
			if (action == null)
			{
				throw new NullPointerException();
			}
			this.action = action;
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new Creek<E, E>()
			{
				@Override
				public E next()
				{
					E el = upstream().next();
					try
					{
						action.action(el);
					}
					catch (RuntimeException e)
					{
						throw e;
					}
					catch (Exception e)
					{
						throw new RuntimeException(e);
					}
					return el;
				}
			};
		}
	}

	protected static interface Pond<I, O> extends CloseableIterator<O>
	{
		void begin() throws Exception;

		@Override
		void close() throws Exception;

		void end() throws Exception;

		Pond<?, I> upstream();

		void upstream(Pond<?, I> up);
	}

	public static interface Producer<E>
	{
		E produce() throws Exception;
	}

	protected static class RANK extends AbstractAggregator<Integer>
	{
		@Override
		public Item<Double>[] between()
		{
			return null;
		}

		@Override
		public Object gather(Object acc, Map<String, Object>[] rows, int levelFrom, int levelTo) throws Exception
		{
			return levelFrom + 1;
		}

		@Override
		public Integer initial()
		{
			return 0;
		}
	}

	protected static class ReduceOp<E> implements Evaluator<E, Option<E>>
	{
		protected final Reducer<E, E>	reducer;

		protected final Filter<E>		until;

		public ReduceOp(Reducer<E, E> reducer, Filter<E> until)
		{
			if (reducer == null)
			{
				throw new NullPointerException();
			}
			this.reducer = reducer;
			this.until = until;
		}

		@Override
		public Terminal<E, Option<E>> newPond()
		{
			return new AbstractTerminal<E, Option<E>>()
			{
				private boolean	empty	= true;

				private E		result;

				@Override
				public void begin() throws Exception
				{
					try
					{
						E el = null;
						boolean meet = until == null;
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
							if (until != null && until.filter(result))
							{
								meet = true;
								break;
							}
						}

						if (!meet)
						{
							empty = true;
						}
					}
					finally
					{
						try
						{
							this.end();
						}
						catch (Exception e)
						{
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

	protected static class ReverseOp<E> implements Converter<E, E>
	{
		@Override
		public Pond<E, E> newPond()
		{
			return new Heaper<E>()
			{
				@Override
				protected LinkedList<E> newSediment()
				{
					return new LinkedList<E>();
				}

				@Override
				protected void settle()
				{
					LinkedList<E> settle = (LinkedList<E>) this.sediment;

					while (upstream().hasNext())
					{
						settle.addFirst(upstream().next());
					}
				}
			};
		}
	}

	protected static class RightJoiner<L, R, K, U, V> extends Joiner<L, R, K, U, V, Option<U>, V>
	{
		public RightJoiner(Canal<R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
		{
			super(that, kol, kor, vol, vor);
		}

		@Override
		protected Set<K> keys(Set<K> left, Set<K> right)
		{
			return right;
		}

		@Override
		protected boolean needLeft(boolean isEmpty)
		{
			return true;
		}

		@Override
		protected boolean needRight(boolean isEmpty)
		{
			return !isEmpty;
		}

		@Override
		protected Option<U> valLeft()
		{
			return Option.none();
		}

		@Override
		protected Option<U> valLeft(U u)
		{
			return Option.some(u);
		}

		@Override
		protected V valRight()
		{
			return null;
		}

		@Override
		protected V valRight(V v)
		{
			return v;
		}
	}

	protected static class RightJoinOp<L, R, K, U, V> extends JoinOp<L, R, K, U, V>
			implements Converter<L, Tuple2<K, Tuple2<Option<U>, V>>>
	{
		public RightJoinOp(Canal<R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
		{
			super(that, kol, kor, vol, vor);
		}

		@Override
		public Pond<L, Tuple2<K, Tuple2<Option<U>, V>>> newPond()
		{
			return new RightJoiner<L, R, K, U, V>(that, kol, kor, vol, vor);
		}
	}

	protected static class ROW_NUMBER extends AbstractAggregator<Integer>
	{
		@Override
		public Item<Double>[] between()
		{
			return null;
		}

		@Override
		public Integer express(int pos, Object acc, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception
		{
			return pos + 1;
		}
	}

	public static class RowCanal<R extends Map<String, ?>> extends Canal<R>
	{
		protected static class ItemComparator<T extends Comparable<T>> implements Comparator<Map<String, Object>>
		{
			protected final Item<T> item;

			public ItemComparator(Item<T> item)
			{
				this.item = item;
			}

			@SuppressWarnings("unchecked")
			@Override
			public int compare(Map<String, Object> o1, Map<String, Object> o2)
			{
				try
				{
					Comparable<T> a = item.map(o1), b = item.map(o2);

					if (a == b)
					{
						return 0;
					}
					else if (a == null)
					{
						return item.isNullsLast() ? 1 : -1;
					}
					else if (b == null)
					{
						return item.isNullsLast() ? -1 : 1;
					}
					else
					{
						return a.compareTo((T) b) * item.getFactor();
					}
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
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public static <R extends Map<String, Object>> List<Comparator<R>> comparatorsOfItems(Item<?>... items)
		{
			List<Comparator<R>> list = new ArrayList<Comparator<R>>();

			for (Item<?> item : items)
			{
				list.add(new ItemComparator(item));
			}

			return list;
		}

		protected static WindowRanger getRanger(Window window)
		{
			if (window.between() != null)
			{
				if (window.isByRow())
				{
					return new SlidingRowsWindowRanger(window.between());
				}
				else
				{
					if (window.orderBy().length != 1)
					{
						throw new RuntimeException("Range window must specify exactly one order item");
					}
					return new SlidingRangeWindowRanger(window.orderBy()[0], window.between());
				}
			}
			else
			{
				return new SimpleWindowRanger();
			}
		}

		/**
		 * Stratify each elements into levels according to the given items.
		 * 
		 * @param items
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public Canal<Canal<R>> stratifyBy(Item<?>... items)
		{
			return this.stratifyWith((Comparator<R>) comparator(comparatorsOfItems(items)));
		}

		@SuppressWarnings("unchecked")
		public RowCanal<R> window(final Aggregator<?>... aggrs)
		{
			RowCanal<R> canal = this;

			for (final Aggregator<?> aggr : aggrs)
			{
				final WindowRanger ranger = getRanger(aggr);

				canal = canal.stratifyBy(aggr.partBy()).flatMap(new Mapper<Canal<R>, Iterable<R>>()
				{
					@Override
					public Iterable<R> map(Canal<R> partition) throws Exception
					{
						List<List<R>> ordered = (List<List<R>>) partition.<R> toRows().stratifyBy(aggr.orderBy())
								.map(new Mapper<Canal<R>, List<R>>()
								{
									@Override
									public List<R> map(Canal<R> el) throws Exception
									{
										return (List<R>) el.collect(new ArrayList<R>());
									}
								}).collect(new ArrayList<List<R>>());

						Collection<R> part = Canal.of(ordered).flatMap(new Mapper<List<R>, Iterable<R>>()
						{
							@Override
							public Iterable<R> map(List<R> el) throws Exception
							{
								return el;
							}
						}).collect(new ArrayList<R>());

						Map<String, Object>[] rows = part.toArray(new Map[0]);

						int from = 0, to = 0, levelFrom = 0, levelTo = 0, i = 0;
						int[] range = new int[] { 0, 0 };
						Object gather = aggr.initial(), update = null;

						for (List<R> level : ordered)
						{
							levelFrom = levelTo;
							levelTo += level.size();

							from = to;

							while (from < levelTo)
							{
								to = ranger.step(rows, from, levelFrom, levelTo);

								// Find range only once in each step
								ranger.range(range, rows, from, levelFrom, levelTo);

								gather = aggr.gather(gather, rows, levelFrom, levelTo);

								update = aggr.update(gather, rows, range[0], range[1]);

								for (i = from; i < to && i < levelTo; i++)
								{
									rows[i].put(aggr.as(), range[0] >= range[1] ? null
											: aggr.express(i, update, rows, range[0], range[1]));
								}

								from = to;
							}
						}

						return part;
					}
				}).toRows();
			}

			return canal;
		}
	}

	public static class SelfMapper<E> implements Mapper<E, E>
	{
		@Override
		public E map(E el)
		{
			return el;
		}
	}

	protected static class SimpleWindowRanger implements WindowRanger
	{
		@Override
		public void range(int[] range, Map<String, Object>[] rows, int from, int levelBegin, int levelEnd)
		{
			range[0] = 0;
			range[1] = levelEnd;
		}

		@Override
		public int step(Map<String, Object>[] rows, int from, int levelBegin, int levelEnd) throws Exception
		{
			return levelEnd;
		}
	}

	public static class SingleUseIterable<E> implements Iterable<E>
	{
		protected final Iterator<E> iter;

		public SingleUseIterable(Iterator<E> iter)
		{
			if (iter == null)
			{
				throw new NullPointerException();
			}
			this.iter = iter;
		}

		public Canal<E> canal()
		{
			return Canal.of(this);
		}

		@Override
		public Iterator<E> iterator()
		{
			return iter;
		}
	}

	protected static class SkipOp<E> implements Converter<E, E>
	{
		protected final int skip;

		public SkipOp(int skip)
		{
			this.skip = skip;
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new Wheel<E, E>()
			{
				@Override
				public void begin()
				{
					while (index++ < skip && upstream().hasNext())
					{
						upstream().next();
					}
				}

				@Override
				public E next()
				{
					return upstream().next();
				}
			};
		}
	}

	protected static class SlidingOp<E> implements Converter<E, Iterable<E>>
	{
		protected final int	size;

		protected final int	step;

		public SlidingOp(int size, int step)
		{
			this.size = size;
			this.step = step;
		}

		@Override
		public Pond<E, Iterable<E>> newPond()
		{
			return new Wheel<E, Iterable<E>>()
			{
				LinkedList<E>	window	= null;

				List<E>			next	= null;

				@Override
				public boolean hasNext()
				{
					if (size <= 0 || step <= 0)
					{
						return false;
					}

					if (next != null)
					{
						return true;
					}

					if (window == null)
					{
						window = new LinkedList<E>();
					}
					else
					{
						int gap = step - size;
						while (gap > 0 && upstream().hasNext())
						{
							gap--;
							upstream().next();
						}
					}

					boolean add = false;
					while (window.size() < size && upstream().hasNext())
					{
						window.add(upstream().next());
						add = true;
					}

					if (!add)
					{
						return false;
					}

					next = new ArrayList<E>(window);

					if (step >= size)
					{
						window.clear();
					}
					else if (step <= size / 2)
					{
						for (int i = 0; i < step && !window.isEmpty(); i++)
						{
							window.removeFirst();
						}
					}
					else
					{
						LinkedList<E> last = window;
						window = new LinkedList<E>();
						int rest = size - step;
						for (int i = 0; i < rest && !last.isEmpty(); i++)
						{
							window.addFirst(last.removeLast());
						}
						last.clear();
					}

					return true;
				}

				@Override
				public Iterable<E> next()
				{
					try
					{
						return next;
					}
					finally
					{
						next = null;
					}
				}
			};
		}
	}

	protected static class SlidingRangeWindowRanger implements WindowRanger
	{
		protected static int find(Item<?> item, Map<String, Object>[] rows, int from, boolean toTail, boolean gt,
				Number bound) throws Exception
		{
			int i = toTail ? from + 1 : from - 1;

			for (; toTail ? i < rows.length : i >= 0;)
			{
				if (gt)
				{
					if (((Number) item.map(rows[i])).doubleValue() > bound.doubleValue())
					{
						break;
					}
				}
				else
				{
					if (((Number) item.map(rows[i])).doubleValue() < bound.doubleValue())
					{
						break;
					}
				}

				if (toTail)
				{
					i++;
				}
				else
				{
					i--;
				}
			}

			return toTail ? i - 1 : i + 1;
		}

		protected final Item<?>			item;

		protected final Item<Double>	a;

		protected final Item<Double>	b;

		@SuppressWarnings("unchecked")
		public SlidingRangeWindowRanger(Item<?> item, Item<Double>[] between)
		{
			this.item = item;
			this.a = (Item<Double>) (between[0] == null ? NULL : between[0]);
			this.b = (Item<Double>) (between[1] == null ? NULL : between[1]);
		}

		@Override
		public void range(int[] range, Map<String, Object>[] rows, int from, int levelBegin, int levelEnd)
				throws Exception
		{
			Double aVal = a.map(rows[from]), bVal = b.map(rows[from]);

			if (aVal == null)
			{
				range[0] = 0;
			}
			else if (aVal == 0.0)
			{
				range[0] = levelBegin;
			}
			else
			{
				Double bound = ((Number) item.map(rows[from])).doubleValue() + (item.isAscend() ? 1 : -1) * aVal;
				if (item.isAscend())
				{
					range[0] = find(item, rows, aVal < 0 ? levelBegin : levelEnd - 1, aVal > 0, aVal > 0, bound)
							+ (aVal > 0 ? 1 : 0);
				}
				else
				{
					range[0] = find(item, rows, aVal < 0 ? levelBegin : levelEnd - 1, aVal > 0, aVal < 0, bound)
							+ (aVal > 0 ? 1 : 0);
				}
			}

			if (bVal == null)
			{
				range[1] = rows.length;
			}
			else if (bVal == 0.0)
			{
				range[1] = levelEnd;
			}
			else
			{
				Double bound = ((Number) item.map(rows[from])).doubleValue() + (item.isAscend() ? 1 : -1) * bVal;
				if (item.isAscend())
				{
					range[1] = find(item, rows, bVal > 0 ? levelEnd - 1 : levelBegin, bVal > 0, bVal > 0, bound)
							+ (bVal < 0 ? -1 : 0) + 1;
				}
				else
				{
					range[1] = find(item, rows, bVal > 0 ? levelEnd - 1 : levelBegin, bVal > 0, bVal < 0, bound)
							+ (bVal < 0 ? -1 : 0) + 1;
				}
			}
		}

		@Override
		public int step(Map<String, Object>[] rows, int from, int levelBegin, int levelEnd) throws Exception
		{
			return levelEnd;
		}
	}

	protected static class SlidingRowsWindowRanger implements WindowRanger
	{
		protected final Item<Double>	a;

		protected final Item<Double>	b;

		public SlidingRowsWindowRanger(Item<Double>[] between)
		{
			this.a = between[0];
			this.b = between[1];
		}

		@Override
		public void range(int[] range, Map<String, Object>[] rows, int from, int levelBegin, int levelEnd)
				throws Exception
		{
			range[0] = Math.min(Math.max(from + a.map(rows[from]).intValue(), 0), rows.length);
			range[1] = Math.min(Math.max(from + b.map(rows[from]).intValue() + 1, 0), rows.length);
		}

		@Override
		public int step(Map<String, Object>[] rows, int from, int levelBegin, int levelEnd) throws Exception
		{
			return from + 1;
		}
	}

	public static class Some<E> extends Option<E>
	{
		protected static class SomeSourcer<T> implements Sourcer<T>
		{
			protected class SomeSource extends Source<T>
			{
				protected boolean first = true;

				@Override
				public boolean hasNext()
				{
					return first;
				}

				@Override
				public T next()
				{
					try
					{
						return SomeSourcer.this.value;
					}
					finally
					{
						first = false;
					}
				}
			}

			protected final T value;

			protected SomeSourcer(T value)
			{
				this.value = value;
			}

			@Override
			public Source<T> newPond()
			{
				return new SomeSource();
			}
		}

		protected final E value;

		public Some(E val)
		{
			this.value = val;
			this.setOperator(new SomeSourcer<E>(val));
		}

		@Override
		public E get()
		{
			return value;
		}

		@Override
		public E get(Producer<Exception> raiser)
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
		public E or(Producer<? extends E> defaultProducer)
		{
			return value;
		}

		@Override
		public Option<E> orElse(Option<? extends E> opt)
		{
			return this;
		}

		@Override
		public Option<E> orElse(Producer<Option<? extends E>> opt)
		{
			return this;
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

	protected static class SortByOp<E> implements Converter<E, E>
	{
		protected final Comparator<E> cmps;

		public SortByOp(List<Comparator<E>> cmps)
		{
			this.cmps = comparator(cmps);
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new Heaper<E>()
			{
				@Override
				protected Collection<E> newSediment()
				{
					return new ArrayList<E>();
				}

				@Override
				protected void settle()
				{
					while (upstream().hasNext())
					{
						this.sediment.add(upstream().next());
					}
					Collections.sort((List<E>) this.sediment, cmps);
				}
			};
		}
	}

	protected static class SortWithOp<E> implements Converter<E, E>
	{
		protected final Comparator<E> cmp;

		public SortWithOp(final Comparator<E> cmp, boolean ascend)
		{
			this.cmp = ascend ? cmp : inverse(cmp);
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new Heaper<E>()
			{
				@Override
				protected Collection<E> newSediment()
				{
					return new ArrayList<E>();
				}

				@Override
				protected void settle()
				{
					while (upstream().hasNext())
					{
						this.sediment.add(upstream().next());
					}
					Collections.sort((List<E>) this.sediment, cmp);
				}
			};
		}
	}

	protected static abstract class Source<E> implements Pond<E, E>
	{
		@Override
		public void begin() throws Exception
		{
		}

		@Override
		public void close() throws Exception
		{
		}

		@Override
		public void end() throws Exception
		{
			this.close();
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
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

	protected static class StratifyPond<E> extends AbstractPond<E, Canal<E>>
	{
		protected final Comparator<E>	cmp;

		private Iterator<Iterable<E>>	res;

		public StratifyPond(Comparator<E> cmp)
		{
			this.cmp = cmp;
		}

		@Override
		public void begin()
		{
			List<E> dat = new ArrayList<E>();

			while (this.upstream().hasNext())
			{
				dat.add(this.upstream().next());
			}

			this.res = stratify(dat, cmp).iterator();
		}

		@Override
		public boolean hasNext()
		{
			return res.hasNext();
		}

		@Override
		public Canal<E> next()
		{
			return Canal.of(res.next());
		}
	}

	protected static class StratifyWithOp<E> implements Converter<E, Canal<E>>
	{
		protected final Comparator<E> cmp;

		public StratifyWithOp(Comparator<E> cmp)
		{
			this.cmp = cmp;
		}

		@Override
		public Pond<E, Canal<E>> newPond()
		{
			return new StratifyPond<E>(cmp);
		}
	}

	protected static class StringConcater<E> implements Evaluator<E, String>
	{
		protected final CharSequence	split;

		protected final CharSequence	prefix;

		protected final CharSequence	suffix;

		protected final boolean			emptyWrap;

		public StringConcater(CharSequence split, CharSequence prefix, CharSequence suffix, boolean emptyWrap)
		{
			this.split = split == null || split.length() == 0 ? null : split;
			this.prefix = prefix == null || prefix.length() == 0 ? null : prefix;
			this.suffix = suffix == null || suffix.length() == 0 ? null : suffix;
			this.emptyWrap = emptyWrap;
		}

		@Override
		public Terminal<E, String> newPond()
		{
			return new AbstractTerminal<E, String>()
			{
				private StringBuilder	buff	= new StringBuilder();

				private boolean			empty	= true;

				@Override
				public void begin()
				{
					try
					{
						while (upstream().hasNext())
						{
							if (empty)
							{
								empty = false;
							}
							else if (split != null)
							{
								buff.append(split);
							}
							buff.append(upstream().next());
						}

						if (emptyWrap || !empty)
						{
							if (prefix != null)
							{
								buff.insert(0, prefix);
							}
							if (suffix != null)
							{
								buff.append(suffix);
							}
						}
					}
					finally
					{
						try
						{
							this.end();
						}
						catch (Exception e)
						{
						}
					}
				}

				@Override
				public String get()
				{
					return buff.toString();
				}
			};
		}
	}

	protected static class SubtractOp<E> implements Converter<E, E>
	{
		protected final Canal<E>		that;

		protected final Comparator<E>	cmp;

		@SuppressWarnings("unchecked")
		public SubtractOp(Canal<? extends E> that, Comparator<E> cmp)
		{
			if (that == null)
			{
				throw new NullPointerException();
			}
			this.that = (Canal<E>) that;
			this.cmp = cmp;
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new SubtractPond<E>(that, cmp);
		}
	}

	protected static class SubtractPond<E> extends Dam<E, E, E>
	{
		protected final Comparator<E>	cmp;

		private Set<E>					there;

		private E						here;

		public SubtractPond(Canal<E> that, Comparator<E> cmp)
		{
			super(that);
			this.cmp = cmp;
		}

		@Override
		public void begin()
		{
			there = (Set<E>) that.collect(cmp == null ? new HashSet<E>() : new TreeSet<E>(cmp));
		}

		@Override
		public boolean hasNext()
		{
			while (upstream().hasNext())
			{
				here = upstream().next();
				if (!there.contains(here))
				{
					return true;
				}
			}
			return false;
		}

		@Override
		public E next()
		{
			return here;
		}
	}

	protected static class SUM extends AbstractAggregator<Double>
	{
		protected final Item<Double> vop;

		public SUM(Item<Double> vop)
		{
			this.vop = vop;
		}

		@Override
		public Double update(Object acc, Map<String, Object>[] rows, int winFrom, int winTo) throws Exception
		{
			double sum = 0;

			Double d = null;
			for (int i = winFrom; i < winTo; i++)
			{
				if ((d = vop.map(rows[i])) != null)
				{
					sum += d;
				}
			}

			return sum;
		}
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

	public static abstract class Tuple implements Serializable, Iterable<Object>, Comparable<Object>
	{
		public static class TupleIndexOutOfBoundsException extends IndexOutOfBoundsException
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 3877085772370116982L;

			public TupleIndexOutOfBoundsException()
			{
				super();
			}

			public TupleIndexOutOfBoundsException(int index)
			{
				super("Tuple index out of range: " + index);
			}

			public TupleIndexOutOfBoundsException(String msg)
			{
				super(msg);
			}
		}

		protected class TupleIterator implements Iterator<Object>
		{
			private int index = 1;

			@Override
			public boolean hasNext()
			{
				return index <= size();
			}

			@Override
			public Object next()
			{
				return get(index++);
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1796022544991162056L;

		@SuppressWarnings("unchecked")
		public static int compare(Tuple t1, Tuple t2)
		{
			if (t1.size() != t2.size())
			{
				return t1.size() - t2.size();
			}

			int c = 0;
			for (int i = 1; i <= t1.size(); i++)
			{
				if ((c = ((Comparable<Object>) t1.get(i)).compareTo(t2.get(i))) != 0)
				{
					return c;
				}
			}

			return 0;
		}

		public static <E1> Tuple1<E1> of(E1 _1)
		{
			return new Tuple1<E1>(_1);
		}

		public static <E1, E2> Tuple2<E1, E2> of(E1 _1, E2 _2)
		{
			return new Tuple2<E1, E2>(_1, _2);
		}

		public static <E1, E2, E3> Tuple3<E1, E2, E3> of(E1 _1, E2 _2, E3 _3)
		{
			return new Tuple3<E1, E2, E3>(_1, _2, _3);
		}

		@Override
		public int compareTo(Object o)
		{
			return compare(this, (Tuple) o);
		}

		protected void ensureIndex(int index)
		{
			if (index <= 0 || index > size())
			{
				throw new TupleIndexOutOfBoundsException(index);
			}
		}

		@Override
		public boolean equals(Object o)
		{
			if (o == null)
			{
				return false;
			}

			if (!this.getClass().equals(o.getClass()))
			{
				return false;
			}

			return this.compareTo(o) == 0;
		}

		protected Field field(int i)
		{
			try
			{
				return this.getClass().getDeclaredField("_" + i);
			}
			catch (Exception e)
			{
				return null;
			}
		}

		protected Object get(int i)
		{
			ensureIndex(i);
			try
			{
				return field(i).get(this);
			}
			catch (Exception e)
			{
				return null;
			}
		}

		@Override
		public int hashCode()
		{
			int hash = 1;
			Object v = null;

			for (int i = 1; i <= size(); i++)
			{
				v = get(i);
				hash = 31 * hash + ((v == null) ? 0 : v.hashCode());
			}

			return hash;
		}

		@Override
		public Iterator<Object> iterator()
		{
			return new TupleIterator();
		}

		public abstract <T extends Tuple> T reverse();

		public abstract int size();

		@Override
		public String toString()
		{
			String s = "(";

			for (int i = 1; i <= size(); i++)
			{
				if (i > 1)
				{
					s += ", ";
				}
				s += get(i);
			}

			return s + ")";
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

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Tuple> T reverse()
		{
			return (T) this;
		}

		@Override
		public int size()
		{
			return 1;
		}
	}

	public static class Tuple2<E1, E2> extends Tuple
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= -4920340973710779812L;

		public final E1				_1;

		public final E2				_2;

		public Tuple2(E1 _1, E2 _2)
		{
			this._1 = _1;
			this._2 = _2;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Tuple2<E2, E1> reverse()
		{
			return Tuple.of(_2, _1);
		}

		@Override
		public int size()
		{
			return 2;
		}
	}

	public static class Tuple3<E1, E2, E3> extends Tuple
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= 2128204413194342750L;

		public final E1				_1;

		public final E2				_2;

		public final E3				_3;

		public Tuple3(E1 _1, E2 _2, E3 _3)
		{
			this._1 = _1;
			this._2 = _2;
			this._3 = _3;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Tuple3<E3, E2, E1> reverse()
		{
			return Tuple.of(_3, _2, _1);
		}

		@Override
		public int size()
		{
			return 3;
		}
	}

	protected static class UnionOp<E> implements Converter<E, E>
	{
		protected final Canal<E>	self;

		protected final Canal<E>	that;

		@SuppressWarnings("unchecked")
		public UnionOp(Canal<E> self, Canal<? extends E> that)
		{
			if (self == null || that == null)
			{
				throw new NullPointerException();
			}
			this.self = self;
			this.that = (Canal<E>) that;
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

	protected static class UntilOp<E> implements Converter<E, E>
	{
		protected final Filter<E>	until;

		protected final Option<E>[]	drop;

		public UntilOp(Filter<E> until, Option<E>[] drop)
		{
			if (until == null)
			{
				throw new NullPointerException();
			}
			this.until = until;
			this.drop = drop;
		}

		@Override
		public Pond<E, E> newPond()
		{
			return new Wheel<E, E>()
			{
				boolean	over	= false;
				boolean	take	= true;
				E		next	= null;

				@Override
				public void begin()
				{
					if (drop != null && drop.length > 0)
					{
						drop[0] = Option.none();
					}
					super.begin();
				}

				@Override
				public boolean hasNext()
				{
					if (over)
					{
						return false;
					}
					else if (!take)
					{
						return true;
					}
					else if (!upstream().hasNext())
					{
						return false;
					}
					else
					{
						next = upstream().next();
						try
						{
							if (until.filter(next))
							{
								if (drop != null && drop.length > 0)
								{
									drop[0] = Option.some(next);
								}
								over = true;
								next = null;
								take = true;
								return false;
							}
							else
							{
								take = false;
								return true;
							}
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
				}

				@Override
				public E next()
				{
					try
					{
						return next;
					}
					finally
					{
						take = true;
					}
				}
			};
		}
	}

	protected static abstract class Wheel<I, O> extends AbstractPond<I, O>
	{
		/**
		 * The index of current iterating element.<br />
		 * This index should {@code +1} just after {@code upstream().next()} is
		 * called and before any other action.
		 */
		protected long index = 0;

		@Override
		public void begin()
		{
		}

		@Override
		public boolean hasNext()
		{
			return upstream().hasNext();
		}
	}

	public static interface Window
	{
		public Item<Double>[] between();

		public boolean isByRow();

		public Item<?>[] orderBy();

		public Item<?>[] partBy();
	}

	protected static interface WindowRanger
	{
		/**
		 * To get the range of window. The range bound may be beyond level range
		 * since the preceding and following arguments.
		 * 
		 * @param range
		 *            the result range, from (including) to (excluding).
		 * @param rows
		 * @param from
		 * @param levelBegin
		 * @param levelEnd
		 * @throws Exception
		 */
		public void range(int[] range, Map<String, Object>[] rows, int from, int levelBegin, int levelEnd)
				throws Exception;

		/**
		 * To get the end of next step.
		 * 
		 * @param rows
		 * @param from
		 *            the start point of next step (including).
		 * @param levelBegin
		 * @param levelEnd
		 * @return the end of next step (excluding).
		 * @throws Exception
		 */
		public int step(Map<String, Object>[] rows, int from, int levelBegin, int levelEnd) throws Exception;
	}

	protected static class ZipOp<A, B> implements Converter<A, Tuple2<A, B>>
	{
		protected final Canal<B> that;

		public ZipOp(Canal<B> that)
		{
			if (that == null)
			{
				throw new NullPointerException();
			}
			this.that = that;
		}

		@Override
		public Pond<A, Tuple2<A, B>> newPond()
		{
			return new ZipPond<A, B>(that);
		}
	}

	protected static class ZipOuterOp<A, B> implements Converter<A, Tuple2<Option<A>, Option<B>>>
	{
		protected final Canal<B> that;

		public ZipOuterOp(Canal<B> that)
		{
			if (that == null)
			{
				throw new NullPointerException();
			}
			this.that = that;
		}

		@Override
		public Pond<A, Tuple2<Option<A>, Option<B>>> newPond()
		{
			return new ZipOuterPond<A, B>(that);
		}
	}

	protected static class ZipOuterPond<A, B> extends Dam<A, B, Tuple2<Option<A>, Option<B>>>
	{
		private Pond<?, B>	there;

		private boolean		hasThis;

		private boolean		hasThat;

		public ZipOuterPond(Canal<B> that)
		{
			super(that);
		}

		@Override
		public void begin()
		{
			this.there = that.build();
		}

		@Override
		public boolean hasNext()
		{
			return (hasThis = upstream().hasNext()) | (hasThat = there.hasNext());
		}

		@Override
		public Tuple2<Option<A>, Option<B>> next()
		{
			return Tuple.of(hasThis ? Option.some(upstream().next()) : Option.<A> none(),
					hasThat ? Option.some(there.next()) : Option.<B> none());
		}
	}

	protected static class ZipPond<A, B> extends Dam<A, B, Tuple2<A, B>>
	{
		private Pond<?, B> there;

		public ZipPond(Canal<B> that)
		{
			super(that);
		}

		@Override
		public void begin()
		{
			this.there = that.build();
		}

		@Override
		public boolean hasNext()
		{
			return upstream().hasNext() && there.hasNext();
		}

		@Override
		public Tuple2<A, B> next()
		{
			return Tuple.of(upstream().next(), there.next());
		}
	}

	protected static class ZipWithIndexLongOp<E> implements Converter<E, Tuple2<E, Long>>
	{
		@Override
		public Pond<E, Tuple2<E, Long>> newPond()
		{
			return new Wheel<E, Tuple2<E, Long>>()
			{
				@Override
				public Tuple2<E, Long> next()
				{
					return Tuple.of(upstream().next(), index++);
				}
			};
		}
	}

	protected static class ZipWithIndexOp<E> implements Converter<E, Tuple2<E, Integer>>
	{
		@Override
		public Pond<E, Tuple2<E, Integer>> newPond()
		{
			return new Wheel<E, Tuple2<E, Integer>>()
			{
				@Override
				public Tuple2<E, Integer> next()
				{
					return Tuple.of(upstream().next(), (int) index++);
				}
			};
		}
	}

	protected static class ZipWithPhaseOp<E> implements Converter<E, Tuple2<E, Integer>>
	{
		@Override
		public Pond<E, Tuple2<E, Integer>> newPond()
		{
			return new Creek<E, Tuple2<E, Integer>>()
			{
				private Boolean	has;

				private E		next;

				private int		head	= 1, body = -1;

				@Override
				public boolean hasNext()
				{
					if (has == null)
					{
						has = body >= 0 ? body < 2 : upstream().hasNext();
						if (has)
						{
							next = upstream().next();
							body = upstream().hasNext() ? 0 : 2;
						}
						else
						{
							body = -1;
						}
					}
					return has;
				}

				@Override
				public Tuple2<E, Integer> next()
				{
					try
					{
						return Tuple.of(next, head | body);
					}
					finally
					{
						has = null;
						head = 0;
					}
				}
			};
		}
	}

	public static final Filter<?>		NOT_NULL	= new Filter<Object>()
													{
														@Override
														public boolean filter(Object el) throws Exception
														{
															return el != null;
														}
													};

	public static final Item<?>			NULL		= new Item<Double>()
													{
														@Override
														public Double map(Map<String, Object> el) throws Exception
														{
															return null;
														}
													};

	public static final Item<Double>	CURRENT_ROW	= new Item<Double>()
													{
														@Override
														public Double map(Map<String, Object> el) throws Exception
														{
															return 0.0;
														}
													};

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Item<?> $(final String key)
	{
		return new Item()
		{
			@Override
			public Comparable<?> map(Object el) throws Exception
			{
				return (Comparable<?>) (((Map<String, Object>) el).get(key));
			}
		};
	}

	protected static <I, O> Pond<I, O> begin(Pond<I, O> pond)
	{
		if (pond.upstream() != null)
		{
			begin(pond.upstream());
		}
		try
		{
			pond.begin();
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		return pond;
	}

	public static <E> Comparator<E> comparator(List<Comparator<E>> cmps)
	{
		return new ComparatorsChain<E>(cmps);
	}

	public static <E, M extends Comparable<M>> Comparator<E> comparator(Mapper<E, M> mapper)
	{
		return new MappedComparator<E, M>(mapper);
	}

	/**
	 * Make a list of comparators.<br />
	 * The parameters could be either Mapper or Boolean.<br />
	 * The Mapper type parameter stands for a vop to extract value to be
	 * compared from a given element.<br />
	 * The Boolean type parameter means the ascending order of its previous
	 * Mapper parameter.
	 * 
	 * @param orders
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <E> List<Comparator<E>> comparatorsOfOrders(Object... orders)
	{
		List<Comparator<E>> list = new ArrayList<Comparator<E>>();
		Comparator<E> cmp = null;

		for (Object o : orders)
		{
			if (o instanceof Item<?>)
			{
				cmp = (Comparator<E>) new RowCanal.ItemComparator((Item<?>) o);
			}
			else if (o instanceof Mapper<?, ?>)
			{
				if (cmp != null)
				{
					list.add(cmp);
				}
				cmp = comparator((Mapper<E, Comparable>) o);
			}
			else if (o instanceof Boolean)
			{
				if (cmp != null)
				{
					boolean asc = (Boolean) o;
					list.add(asc ? cmp : inverse(cmp));
					cmp = null;
				}
			}
		}

		if (cmp != null)
		{
			list.add(cmp);
		}

		return list;
	}

	public static Aggregator<Integer> COUNT(Expr<?> vop)
	{
		return new COUNT(vop);
	}

	public static Aggregator<Integer> DENSE_RANK()
	{
		return new DENSE_RANK();
	}

	public static <T> Aggregator<T> FIRST_VALUE(Expr<T> vop)
	{
		return FIRST_VALUE(vop, false);
	}

	public static <T> Aggregator<T> FIRST_VALUE(Expr<T> vop, boolean ignoreNulls)
	{
		return new FIRST_VALUE<T>(vop, ignoreNulls);
	}

	public static Item<Double> following(Double amount)
	{
		Double amt = amount == null ? null : Math.abs(amount);
		return item(amt);
	}

	public static Item<Double> following(Integer amount)
	{
		Double amt = amount == null ? null : (1.0 * Math.abs(amount));
		return item(amt);
	}

	public static <E, K, V> Map<K, List<V>> group(Iterator<E> iter, Mapper<E, K> kop, Mapper<E, V> vop) throws Exception
	{
		Map<K, List<V>> map = new LinkedHashMap<K, List<V>>();
		E el;
		K k;
		while (iter.hasNext())
		{
			el = iter.next();
			k = kop.map(el);
			if (!map.containsKey(k))
			{
				map.put(k, new ArrayList<V>());
			}
			map.get(k).add(vop.map(el));
		}
		return map;
	}

	public static <E> Comparator<E> inverse(Comparator<E> cmp)
	{
		return new InverseComparator<E>(cmp);
	}

	public static <T extends Comparable<T>> Item<T> item(final T t)
	{
		return new Item<T>()
		{
			@Override
			public T map(Map<String, Object> el) throws Exception
			{
				return t;
			}
		};
	}

	public static <E> SingleUseIterable<E> iterable(Enumeration<E> enumer)
	{
		return iterable(iterator(enumer));
	}

	public static <E> SingleUseIterable<E> iterable(Iterator<E> iter)
	{
		return new SingleUseIterable<E>(iter);
	}

	public static Iterable<Matcher> iterable(Pattern regex, CharSequence text)
	{
		return new MatchFindIterable(regex, text);
	}

	public static <E> EnumerationIterator<E> iterator(Enumeration<E> enumer)
	{
		return new EnumerationIterator<E>(enumer);
	}

	public static <T> Aggregator<T> LAG(Expr<T> vop)
	{
		return LAG(vop, 1);
	}

	public static <T> Aggregator<T> LAG(Expr<T> vop, int offset)
	{
		return LAG(vop, offset, null);
	}

	public static <T> Aggregator<T> LAG(Expr<T> vop, int offset, T deft)
	{
		return new LAG<T>(vop, offset, deft);
	}

	public static <T> Aggregator<T> LAST_VALUE(Expr<T> vop)
	{
		return LAST_VALUE(vop, false);
	}

	public static <T> Aggregator<T> LAST_VALUE(Expr<T> vop, boolean ignoreNulls)
	{
		return new LAST_VALUE<T>(vop, ignoreNulls);
	}

	public static <T> Aggregator<T> LEAD(Expr<T> vop)
	{
		return LEAD(vop, 1);
	}

	public static <T> Aggregator<T> LEAD(Expr<T> vop, int offset)
	{
		return LEAD(vop, offset, null);
	}

	public static <T> Aggregator<T> LEAD(Expr<T> vop, int offset, T deft)
	{
		return new LEAD<T>(vop, offset, deft);
	}

	public static <T extends Comparable<T>> Aggregator<T> MAX(Item<T> vop)
	{
		return new MAX<T>(vop);
	}

	public static <T extends Comparable<T>> Aggregator<T> MIN(Item<T> vop)
	{
		return new MIN<T>(vop);
	}

	/**
	 * Make a None object.<br />
	 * Call like {@code Canal.<Type>none()}
	 * 
	 * @return
	 */
	public static <E> None<E> none()
	{
		return Option.none();
	}

	public static <E> Canal<E> of(E[] array)
	{
		return of(array, 0);
	}

	public static <E> Canal<E> of(E[] array, Integer begin)
	{
		return of(array, begin, null);
	}

	public static <E> Canal<E> of(E[] array, Integer begin, Integer until)
	{
		return of(array, begin, until, null);
	}

	public static <E> Canal<E> of(E[] array, Integer begin, Integer until, Integer step)
	{
		return new Canal<E>().setOperator(new ArraySourcer<E>(array, begin, until, step));
	}

	@SuppressWarnings("unchecked")
	public static <E> Canal<E> of(Iterable<? extends E> iter)
	{
		return iter instanceof Canal ? (Canal<E>) iter //
				: new Canal<E>().setOperator(new IterableSourcer<E>((Iterable<E>) iter));
	}

	public static PairCanal<String, Object> of(JSON json)
	{
		return new Canal<JSON.Pair>() //
				.setOperator(new IterableSourcer<JSON.Pair>(json.pairs())) //
				.map(new Mapper<JSON.Pair, Tuple2<String, Object>>()
				{
					@Override
					public Tuple2<String, Object> map(Pair el)
					{
						return Tuple.of(el.key, el.val());
					}
				}).toPair();
	}

	public static <K, V> PairCanal<K, V> of(Map<? extends K, ? extends V> map)
	{
		return new Canal<Entry<K, V>>() //
				.setOperator(new IterablePairSourcer<K, V>(map.entrySet())) //
				.map(new Mapper<Entry<K, V>, Tuple2<K, V>>()
				{
					@Override
					public Tuple2<K, V> map(Entry<K, V> el)
					{
						return Tuple.of(el.getKey(), el.getValue());
					}
				}).toPair();
	}

	public static Canal<Matcher> of(Pattern regex, CharSequence text)
	{
		return of(iterable(regex, text));
	}

	@SuppressWarnings("unchecked")
	public static <E> Canal<E> of(Producer<? extends E> spring)
	{
		return new Canal<E>().setOperator(new GeneratedSourcer<E>((Producer<E>) spring));
	}

	public static <E> Option<E> option(E value)
	{
		return Option.of(value);
	}

	public static Item<Double> preceding(Double amount)
	{
		Double amt = amount == null ? null : -Math.abs(amount);
		return item(amt);
	}

	public static Item<Double> preceding(Integer amount)
	{
		Double amt = amount == null ? null : (-1.0 * Math.abs(amount));
		return item(amt);
	}

	public static Iterable<Integer> range(int begin, int until)
	{
		return range(begin, until, 1);
	}

	public static Iterable<Integer> range(final int begin, final int until, final int step)
	{
		return new Iterable<Integer>()
		{
			@Override
			public Iterator<Integer> iterator()
			{
				return new Iterator<Integer>()
				{
					private int index = begin;

					@Override
					public boolean hasNext()
					{
						if (begin < until)
						{
							return step >= 0 && index < until;
						}
						else if (begin > until)
						{
							return step <= 0 && index > until;
						}
						else
						{
							return false;
						}
					}

					@Override
					public Integer next()
					{
						try
						{
							return index;
						}
						finally
						{
							if (step != 0)
							{
								index += step;
							}
							else
							{
								index = until;
							}
						}
					}

					@Override
					public void remove()
					{
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	public static Aggregator<Integer> RANK()
	{
		return new RANK();
	}

	public static Aggregator<Integer> ROW_NUMBER()
	{
		return new ROW_NUMBER();
	}

	public static <E> Some<E> some(E value)
	{
		return Option.some(value);
	}

	public static <E> List<Iterable<E>> stratify(Iterable<E> data, Comparator<E> cmp)
	{
		List<Iterable<E>> res = new ArrayList<Iterable<E>>();

		E last = null;
		int c = 0;
		List<E> lvl = null;

		for (E el : Canal.of(data).sortWith(cmp))
		{
			if (lvl != null)
			{
				c = cmp.compare(last, el);
			}

			if (c != 0 || lvl == null)
			{
				lvl = new ArrayList<E>();
				res.add(lvl);
			}

			lvl.add(el);

			last = el;
		}

		return res;
	}

	public static Aggregator<Double> SUM(Item<Double> vop)
	{
		return new SUM(vop);
	}

	private Canal<?>		upstream;

	private Operator<?, D>	operator;

	protected Pond<?, D> build()
	{
		return begin(build(null));
	}

	@SuppressWarnings("unchecked")
	protected <U> Pond<U, D> build(Pond<D, ?> down)
	{
		Pond<U, D> pond = (Pond<U, D>) this.newPond();

		if (down != null)
		{
			down.upstream(pond);
		}

		// The upstream of source is null
		if (this.getUpstream() != null)
		{
			this.<U> getUpstream().build(pond);
		}

		return pond;
	}

	/**
	 * Make Cartesian product result against another given {@code Canal<?,N>}.
	 * 
	 * @param that
	 * @return
	 */
	public <N> PairCanal<D, N> cartesian(Canal<N> that)
	{
		return this.follow(new CartesianOp<D, N>(that)).toPair();
	}

	/**
	 * Collect elements into a Collection.
	 * 
	 * @return
	 */
	public Collection<D> collect()
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
	public Collection<D> collect(Collection<D> result)
	{
		return this.follow(new CollectOp<D>(result)).evaluate();
	}

	public JSAN collectAsJSAN()
	{
		return this.collectAsJSAN(new JSAN());
	}

	public JSAN collectAsJSAN(final JSAN jsan)
	{
		return jsan.addAll(this);
	}

	/**
	 * Collect elements into a given map.<br />
	 * It assumes that the upstream is a pair Canal.
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
	 * Collect elements into a given map with a given KOP and VOP.
	 * 
	 * @param result
	 *            The result map.
	 * @param kop
	 *            {@code (D data)->K key} the "key of pair" recognizer.
	 * @param vop
	 *            {@code (D data)->V value} the "value of pair" recognizer.
	 * @return
	 */
	public <K, V> Map<K, V> collectAsMap(Map<K, V> result, Mapper<D, K> kop, Mapper<D, V> vop)
	{
		return this.follow(new CollectAsMapOp<D, K, V>(result, kop, vop)).evaluate();
	}

	/**
	 * Count the number of elements.
	 * 
	 * @return
	 */
	public int count()
	{
		return this.follow(new CountOp<D>()).evaluate();
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
	public <K> Map<K, Integer> countByKey(Map<K, Integer> result, Mapper<D, K> kop)
	{
		return this.follow(new CountByKeyOp<D, K>(result, kop)).evaluate();
	}

	/**
	 * Count the number of each element.
	 * 
	 * @return
	 */
	public Map<D, Integer> countByValue()
	{
		return countByValue(null);
	}

	/**
	 * Count the number of each element into a given result map.
	 * 
	 * @param result
	 * @return
	 */
	public Map<D, Integer> countByValue(Map<D, Integer> result)
	{
		return this.follow(new CountByValueOp<D>(result)).evaluate();
	}

	/**
	 * Remove duplicate elements.
	 * 
	 * @return
	 */
	public Canal<D> distinct()
	{
		return this.distinct((Comparator<D>) null);
	}

	/**
	 * Remove duplicate elements with a given {@link Comparator}.
	 * 
	 * @param cmp
	 * @return
	 */
	public Canal<D> distinct(Comparator<D> cmp)
	{
		return this.follow(new DistinctOp<D>(cmp));
	}

	/**
	 * Remove duplicate elements with a given {@link HashedEquality}.
	 * 
	 * @param eql
	 * @return
	 */
	public Canal<D> distinct(HashedEquality<D> eql)
	{
		return this.follow(new DistinctOp<D>(eql));
	}

	@SuppressWarnings("unchecked")
	protected <T> T evaluate()
	{
		return ((Terminal<?, T>) this.build()).get();
	}

	/**
	 * Filter the elements.
	 * 
	 * @param filter
	 *            {@code (D data)->boolean} returns true if and only if the
	 *            element need to be passed to the downstream.
	 * @return
	 */
	public Canal<D> filter(Filter<? super D> filter)
	{
		return this.follow(new FilterOp<D>(filter));
	}

	/**
	 * Get the first element.
	 * 
	 * @return
	 */
	public Option<D> first()
	{
		return first(new Filter<D>()
		{
			@Override
			public boolean filter(D element)
			{
				return true;
			}
		});
	}

	/**
	 * Get the first element that satisfied the given predicate.
	 * 
	 * @param filter
	 *            {@code (D data)->boolean}
	 * @return
	 */
	public Option<D> first(Filter<? super D> filter)
	{
		return this.follow(new FirstOp<D>(filter)).evaluate();
	}

	/**
	 * Map each element into a flat result.
	 * 
	 * @param mapper
	 *            {@code (D data)->Iterable<V>}
	 * @return
	 */
	public <V> Canal<V> flatMap(Mapper<D, Iterable<V>> mapper)
	{
		return this.follow(new FlatMapOp<D, V>(mapper));
	}

	/**
	 * Fold each element with a given initial value until the condition
	 * satisfied.
	 * 
	 * @param init
	 *            {@code ()->R res} a result initializer called on the first
	 *            fold.
	 * @param folder
	 *            {@code (R res,D data)->R res} a fold reducer.
	 * @param until
	 *            the stop condition.
	 * @return Some(res) if and only if at least one element be folded and the
	 *         condition satisfied, otherwise None will be returned.
	 */
	public <R> Option<R> fold(Producer<R> init, Reducer<D, R> folder, Filter<R> until)
	{
		return this.follow(new FoldUntilOp<D, R>(init, folder, until)).evaluate();
	}

	/**
	 * Fold each element with a given initial value.
	 * 
	 * @param init
	 * @param folder
	 *            {@code (R res,D data)->R res} a fold reducer.
	 * @return
	 */
	public <R> R fold(R init, Reducer<D, R> folder)
	{
		return this.follow(new FoldOp<D, R>(init, folder)).evaluate();
	}

	protected <N> Canal<N> follow(Operator<D, N> op)
	{
		return new Canal<N>().setUpstream(this).setOperator(op);
	}

	/**
	 * Determine whether all the data meet the condition.
	 * 
	 * @param cond
	 * @return
	 */
	public boolean forall(Filter<D> cond)
	{
		return this.follow(new ForallOp<D>(cond)).evaluate();
	}

	/**
	 * Take action on each element.
	 * 
	 * @param action
	 *            {@code (D data)->void} the action to be applied to each
	 *            element.
	 */
	public void foreach(Action<D> action)
	{
		this.follow(new ForeachOp<D>(action)).evaluate();
	}

	/**
	 * Full join with another Canal.
	 * 
	 * @param that
	 *            the data on right side.
	 * @param kol
	 *            {@code (D data)->K key} the kop of left.
	 * @param kor
	 *            {@code (R data)->K key} the kop of right.
	 * @return
	 */
	protected <R, K, M, N> Canal<Tuple2<K, Tuple2<Option<M>, Option<N>>>> fullJoin(Canal<R> that, Mapper<D, K> kol,
			Mapper<R, K> kor)
	{
		return fullJoin(that, kol, kor, new DefaultVop<D, M>(), new DefaultVop<R, N>());
	}

	/**
	 * Full join with another Canal.
	 * 
	 * @param that
	 *            the data on right side.
	 * @param kol
	 *            {@code (D data)->K key} the kop of left.
	 * @param kor
	 *            {@code (R data)->K key} the kop of right.
	 * @param vol
	 *            {@code (D data)->M value} the vop of left.
	 * @param vor
	 *            {@code (R data)->N value} the vop of right.
	 * @return
	 */
	public <R, K, M, N> Canal<Tuple2<K, Tuple2<Option<M>, Option<N>>>> fullJoin(Canal<R> that, Mapper<D, K> kol,
			Mapper<R, K> kor, Mapper<D, M> vol, Mapper<R, N> vor)
	{
		return this.follow(new FullJoinOp<D, R, K, M, N>(that, kol, kor, vol, vor));
	}

	protected Operator<?, D> getOperator()
	{
		return operator;
	}

	@SuppressWarnings("unchecked")
	protected <U> Canal<U> getUpstream()
	{
		return (Canal<U>) upstream;
	}

	/**
	 * Gather each element into correspondent group identified by same key.
	 * 
	 * @param kop
	 *            {@code (D data)->K key} the kop of data.
	 * @return
	 */
	public <K> PairCanal<K, Canal<D>> groupBy(Mapper<D, K> kop)
	{
		return groupBy(kop, new SelfMapper<D>());
	}

	/**
	 * Gather each value into correspondent group identified by same key.
	 * 
	 * @param kop
	 *            {@code (D data)->K key} the kop of data.
	 * @param vop
	 *            {@code (D data)->V value} the vop of data.
	 * @return
	 */
	public <K, V> PairCanal<K, Canal<V>> groupBy(Mapper<D, K> kop, Mapper<D, V> vop)
	{
		return this.follow(new GroupByOp<D, K, V>(kop, vop)).toPair();
	}

	/**
	 * Pass the element that both in this and that Canal to the downstream.
	 * 
	 * @param that
	 * @return
	 */
	public Canal<D> intersection(Canal<? extends D> that)
	{
		return intersection(that, null);
	}

	/**
	 * Pass the element that both in this and that Canal to the downstream.
	 * 
	 * @param that
	 * @param cmp
	 * @return
	 */
	public Canal<D> intersection(Canal<? extends D> that, Comparator<D> cmp)
	{
		return this.follow(new IntersectionOp<D>(that, cmp));
	}

	@Override
	public Iterator<D> iterator()
	{
		return this.build();
	}

	/**
	 * Inner join with another Canal.
	 * 
	 * @param that
	 *            the data on right side.
	 * @param kol
	 *            {@code (D data)->K key} the kop of left.
	 * @param kor
	 *            {@code (R data)->K key} the kop of right.
	 * @return
	 */
	protected <R, K, M, N> Canal<Tuple2<K, Tuple2<M, N>>> join(Canal<R> that, Mapper<D, K> kol, Mapper<R, K> kor)
	{
		return join(that, kol, kor, new DefaultVop<D, M>(), new DefaultVop<R, N>());
	}

	/**
	 * Inner join with another Canal.
	 * 
	 * @param that
	 *            the data on right side.
	 * @param kol
	 *            {@code (D data)->K key} the kop of left.
	 * @param kor
	 *            {@code (R data)->K key} the kop of right.
	 * @param vol
	 *            {@code (D data)->M value} the vop of left.
	 * @param vor
	 *            {@code (R data)->N value} the vop of right.
	 * @return
	 */
	public <R, K, M, N> Canal<Tuple2<K, Tuple2<M, N>>> join(Canal<R> that, Mapper<D, K> kol, Mapper<R, K> kor,
			Mapper<D, M> vol, Mapper<R, N> vor)
	{
		return this.follow(new InnerJoinOp<D, R, K, M, N>(that, kol, kor, vol, vor));
	}

	/**
	 * Map each element into {@code Tuple2<K,O>}. The first element is the key
	 * defined by kop.
	 * 
	 * @param kop
	 *            {@code (D data)->K key} the kop of data.
	 * @return
	 */
	public <K> PairCanal<K, D> keyBy(final Mapper<D, K> kop)
	{
		return this.map(new Mapper<D, Tuple2<K, D>>()
		{
			@Override
			public Tuple2<K, D> map(D key) throws Exception
			{
				return Tuple.of(kop.map(key), key);
			}
		}).toPair();
	}

	/**
	 * Get the last element.
	 * 
	 * @return
	 */
	public Option<D> last()
	{
		return last(new Filter<D>()
		{
			@Override
			public boolean filter(D element)
			{
				return true;
			}
		});
	}

	/**
	 * Get the last element that satisfied the given predicate.
	 * 
	 * @param filter
	 *            {@code (D data)->boolean}
	 * @return
	 */
	public Option<D> last(Filter<D> filter)
	{
		return this.follow(new LastOp<D>(filter)).evaluate();
	}

	/**
	 * Left join with another Canal.
	 * 
	 * @param that
	 *            the data on right side.
	 * @param kol
	 *            {@code (D data)->K key} the kop of left.
	 * @param kor
	 *            {@code (R data)->K key} the kop of right.
	 * @return
	 */
	protected <R, K, M, N> Canal<Tuple2<K, Tuple2<M, Option<N>>>> leftJoin(Canal<R> that, Mapper<D, K> kol,
			Mapper<R, K> kor)
	{
		return leftJoin(that, kol, kor, new DefaultVop<D, M>(), new DefaultVop<R, N>());
	}

	/**
	 * Left join with another Canal.
	 * 
	 * @param that
	 *            the data on right side.
	 * @param kol
	 *            {@code (D data)->K key} the kop of left.
	 * @param kor
	 *            {@code (R data)->K key} the kop of right.
	 * @param vol
	 *            {@code (D data)->M value} the vop of left.
	 * @param vor
	 *            {@code (R data)->N value} the vop of right.
	 * @return
	 */
	public <R, K, M, N> Canal<Tuple2<K, Tuple2<M, Option<N>>>> leftJoin(Canal<R> that, Mapper<D, K> kol,
			Mapper<R, K> kor, Mapper<D, M> vol, Mapper<R, N> vor)
	{
		return this.follow(new LeftJoinOp<D, R, K, M, N>(that, kol, kor, vol, vor));
	}

	/**
	 * Pass at most {@code limit} elements to downstream.
	 * 
	 * @param limit
	 * @return
	 */
	public Canal<D> limit(int limit)
	{
		return this.follow(new LimitOp<D>(limit));
	}

	/**
	 * Map each element.
	 * 
	 * @param mapper
	 *            {@code (D data)->V value}
	 * @return
	 */
	public <V> Canal<V> map(Mapper<D, V> mapper)
	{
		return this.follow(new MapOp<D, V>(mapper));
	}

	/**
	 * Map each element to pair and convert result Canal to PairCanal.
	 * 
	 * @param mapper
	 *            {@code (D data)->(K key, V value)}
	 * @return
	 */
	public <K, V> PairCanal<K, V> mapToPair(Mapper<D, Tuple2<K, V>> mapper)
	{
		return this.map(mapper).toPair();
	}

	/**
	 * Map each element with state.
	 * 
	 * @param stater
	 *            state producer which generates a initial state.
	 * @param mapper
	 *            {@code (D data, S state)->V value}
	 * @return
	 */
	public <S, V> Canal<V> mapWithState(Producer<S> stater, StatefulMapper<D, S, V> mapper)
	{
		return this.follow(new MapWithStateOp<D, S, V>(stater, mapper));
	}

	@SuppressWarnings("unchecked")
	protected Pond<?, D> newPond()
	{
		if (this.getOperator() instanceof Sourcer<?>)
		{
			return (Pond<?, D>) ((Sourcer<D>) this.getOperator()).newPond();
		}
		else
		{
			return this.getOperator().newPond();
		}
	}

	/**
	 * Peek the elements in this Canal, take some action and pass them to the
	 * downstream.
	 * 
	 * @param action
	 *            {@code (D data)->void} the action to be applied to each
	 *            element.
	 * @return
	 */
	public Canal<D> peek(Action<D> action)
	{
		return this.follow(new PeekOp<D>(action));
	}

	/**
	 * Reduce each element.
	 * 
	 * @param reducer
	 *            {@code (D a,D b)->D res}
	 * @return The reduce result or {@link Canal.None} only if this Canal is
	 *         empty.
	 */
	public Option<D> reduce(Reducer<D, D> reducer)
	{
		return this.reduce(reducer, null);
	}

	/**
	 * Reduce each element until the condition satisfied.
	 * 
	 * @param reducer
	 *            {@code (D a,D b)->D res}
	 * @param condition
	 * @return Some(res) if and only if the condition satisfied or None will be
	 *         returned.
	 */
	public Option<D> reduce(Reducer<D, D> reducer, Filter<D> condition)
	{
		return this.follow(new ReduceOp<D>(reducer, condition)).evaluate();
	}

	/**
	 * Reverse the elements' order in this Canal.
	 * 
	 * @return
	 */
	public Canal<D> reverse()
	{
		return this.follow(new ReverseOp<D>());
	}

	/**
	 * Right join with another Canal.
	 * 
	 * @param that
	 *            the data on right side.
	 * @param kol
	 *            {@code (D data)->K key} the kop of left.
	 * @param kor
	 *            {@code (R data)->K key} the kop of right.
	 * @return
	 */
	protected <R, K, M, N> Canal<Tuple2<K, Tuple2<Option<M>, N>>> rightJoin(Canal<R> that, Mapper<D, K> kol,
			Mapper<R, K> kor)
	{
		return rightJoin(that, kol, kor, new DefaultVop<D, M>(), new DefaultVop<R, N>());
	}

	/**
	 * Right join with another Canal.
	 * 
	 * @param that
	 *            the data on right side.
	 * @param kol
	 *            {@code (D data)->K key} the kop of left.
	 * @param kor
	 *            {@code (R data)->K key} the kop of right.
	 * @param vol
	 *            {@code (D data)->M value} the vop of left.
	 * @param vor
	 *            {@code (R data)->N value} the vop of right.
	 * @return
	 */
	public <R, K, M, N> Canal<Tuple2<K, Tuple2<Option<M>, N>>> rightJoin(Canal<R> that, Mapper<D, K> kol,
			Mapper<R, K> kor, Mapper<D, M> vol, Mapper<R, N> vor)
	{
		return this.follow(new RightJoinOp<D, R, K, M, N>(that, kol, kor, vol, vor));
	}

	protected Canal<D> setOperator(Operator<?, D> operator)
	{
		this.operator = operator;
		return this;
	}

	protected Canal<D> setUpstream(Canal<?> upstream)
	{
		this.upstream = upstream;
		return this;
	}

	/**
	 * Skip at most {@code skip} elements and pass the rests to downstream.
	 * 
	 * @param skip
	 * @return
	 */
	public Canal<D> skip(int skip)
	{
		return this.follow(new SkipOp<D>(skip));
	}

	/**
	 * Make window to hold data from upstream, pass each window to downstream
	 * and slide the window as the size of window.
	 * 
	 * @param size
	 *            The max size of the window to hold data.
	 * @return
	 */
	public Canal<Iterable<D>> sliding(int size)
	{
		return this.follow(new SlidingOp<D>(size, size));
	}

	/**
	 * Make window to hold data from upstream, pass each window to downstream
	 * and slide the window by step.
	 * 
	 * @param size
	 *            The max size of the window to hold data.
	 * @param step
	 *            Sliding step length.
	 * @return
	 */
	public Canal<Iterable<D>> sliding(int size, int step)
	{
		return this.follow(new SlidingOp<D>(size, step));
	}

	/**
	 * Sort each element in this Canal by given
	 * 
	 * @param orders
	 *            Either Mapper or Boolean.<br />
	 *            The Mapper type parameter stands for a vop to extract value to
	 *            be compared from a given element.<br />
	 *            The Boolean type parameter means the ascending order of its
	 *            previous Mapper parameter.
	 * @return
	 */
	public Canal<D> sortBy(Object... orders)
	{
		return this.follow(new SortByOp<D>(Canal.<D> comparatorsOfOrders(orders)));
	}

	/**
	 * Sort each element in this Canal by natural ascending order.
	 * 
	 * @return
	 */
	public Canal<D> sortWith()
	{
		return sortWith(null);
	}

	/**
	 * Sort each element in this Canal by natural order.
	 * 
	 * @param ascend
	 * @return
	 */
	public Canal<D> sortWith(boolean ascend)
	{
		return sortWith(null, ascend);
	}

	/**
	 * Sort each element in this Canal by a given Comparator in ascending order.
	 * 
	 * @param cmp
	 * @return
	 */
	public Canal<D> sortWith(Comparator<D> cmp)
	{
		return sortWith(cmp, true);
	}

	/**
	 * Sort each element in this Canal by a given Comparator.
	 * 
	 * @param cmp
	 * @param ascend
	 * @return
	 */
	public Canal<D> sortWith(Comparator<D> cmp, boolean ascend)
	{
		if (cmp == null && !ascend)
		{
			return this.follow(new SortWithOp<D>(new DefaultComparator<D>(), ascend));
		}
		else
		{
			return this.follow(new SortWithOp<D>(cmp, ascend));
		}
	}

	/**
	 * Stratify each elements into levels according to the given orders.
	 * 
	 * @param orders
	 *            Either Mapper or Boolean.<br />
	 *            The Mapper type parameter stands for a vop to extract value to
	 *            be compared from a given element.<br />
	 *            The Boolean type parameter means the ascending order of its
	 *            previous Mapper parameter.
	 * @return
	 */
	public Canal<Canal<D>> stratifyBy(Object... orders)
	{
		return this.stratifyWith(comparator(Canal.<D> comparatorsOfOrders(orders)));
	}

	/**
	 * Stratify each elements into levels according to the given Comparator.
	 * 
	 * @param cmp
	 * @return
	 */
	public Canal<Canal<D>> stratifyWith(Comparator<D> cmp)
	{
		return this.follow(new StratifyWithOp<D>(cmp));
	}

	/**
	 * Stratify each elements into levels according to the given Comparator and
	 * order.
	 * 
	 * @param cmp
	 * @param ascend
	 * @return
	 */
	public Canal<Canal<D>> stratifyWith(Comparator<D> cmp, boolean ascend)
	{
		return this.stratifyWith(ascend ? cmp : inverse(cmp));
	}

	/**
	 * Pass the elements in this Canal but not in that Canal to the downstream.
	 * 
	 * @param that
	 * @return
	 */
	public Canal<D> subtract(Canal<? extends D> that)
	{
		return subtract(that, null);
	}

	/**
	 * Pass the elements in this Canal but not in that Canal to the downstream.
	 * 
	 * @param that
	 * @param cmp
	 * @return
	 */
	public Canal<D> subtract(Canal<? extends D> that, Comparator<D> cmp)
	{
		return this.follow(new SubtractOp<D>(that, cmp));
	}

	/**
	 * Take first few elements within a given limit number.
	 * 
	 * @param limit
	 * @return
	 */
	public Collection<D> take(int limit)
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
	public Collection<D> take(int limit, Collection<D> result)
	{
		return this.follow(new TakeOp<D>(limit, result)).evaluate();
	}

	/**
	 * Convert this Canal to PairCanal.<br />
	 * The elements' type in this Canal MUST be {@code Tuple2<K,V>}.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <K, V> PairCanal<K, V> toPair()
	{
		return (PairCanal<K, V>) new PairCanal<K, V>().setUpstream(this.getUpstream())
				.setOperator((Operator<?, Tuple2<K, V>>) this.getOperator());
	}

	/**
	 * Convert this Canal to RowCanal.<br />
	 * Only the elements' type of {@code Map<String,Object>} will be taken into
	 * the downstream.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <R extends Map<String, ?>> RowCanal<R> toRows()
	{
		Canal<D> canal = this.filter(new Filter<D>()
		{
			@Override
			public boolean filter(D el) throws Exception
			{
				try
				{
					R r = (R) el;
					return r != null;
				}
				catch (Exception e)
				{
					return false;
				}
			}
		});
		RowCanal<R> rows = new RowCanal<R>();
		rows.setUpstream((Canal<R>) canal.getUpstream());
		rows.setOperator((Operator<R, R>) canal.getOperator());
		return rows;
	}

	/**
	 * Convert this Canal to RowCanal.<br />
	 * Only the elements' type of {@code Map<String,Object>} will be converted
	 * to the target class and taken into the downstream.
	 * 
	 * @param cls
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <R extends Map<String, Object>> RowCanal<R> toRows(final Class<R> cls)
	{
		Canal<R> canal = this.map(new Mapper<D, R>()
		{
			@Override
			public R map(D el) throws Exception
			{
				if (el instanceof Map<?, ?>)
				{
					try
					{
						Map<String, Object> m = (Map<String, Object>) el;
						if (cls == Map.class)
						{
							return (R) m;
						}
						else if (cls.isInstance(m))
						{
							return (R) m;
						}
						else if (cls == Row.class)
						{
							return (R) Row.of(m);
						}
						else if (cls == JSON.class)
						{
							return (R) JSON.Of(m);
						}
						else
						{
							R r = cls.newInstance();
							r.putAll(m);
							return r;
						}
					}
					catch (Exception e)
					{
						return null;
					}
				}
				else
				{
					return null;
				}
			}
		}).filter(new Filter<R>()
		{
			@Override
			public boolean filter(R el) throws Exception
			{
				return el != null;
			}
		});
		RowCanal<R> rows = new RowCanal<R>();
		rows.setUpstream((Canal<R>) canal.getUpstream());
		rows.setOperator((Operator<R, R>) canal.getOperator());
		return rows;
	}

	/**
	 * To concatenate elements in this Canal to a String using comma as
	 * delimiter and wrapped by brackets.
	 */
	@Override
	public String toString()
	{
		return toString(",", "[", "]");
	}

	/**
	 * To concatenate elements in this Canal to a String with given delimiter.
	 * 
	 * @param delimiter
	 * @return
	 */
	public String toString(CharSequence delimiter)
	{
		return toString(delimiter, null, null);
	}

	/**
	 * To concatenate elements in this Canal to a String with given delimiter,
	 * wrapped by given prefix and suffix.
	 * 
	 * @param delimiter
	 * @param prefix
	 * @param suffix
	 * @return
	 */
	public String toString(CharSequence delimiter, CharSequence prefix, CharSequence suffix)
	{
		return toString(delimiter, prefix, suffix, true);
	}

	/**
	 * To concatenate elements in this Canal to a String with given delimiter,
	 * wrapped by given prefix and suffix. When the Canal is empty and the
	 * emptyWrap is false then the prefix and suffix will not be wrapped.
	 * 
	 * @param delimiter
	 * @param prefix
	 * @param suffix
	 * @param emptyWrap
	 * @return
	 */
	public String toString(CharSequence delimiter, CharSequence prefix, CharSequence suffix, boolean emptyWrap)
	{
		return this.follow(new StringConcater<D>(delimiter, prefix, suffix, emptyWrap)).evaluate();
	}

	/**
	 * Union with another given Canal.
	 * 
	 * @param that
	 * @return
	 */
	public Canal<D> union(Canal<? extends D> that)
	{
		return this.follow(new UnionOp<D>(this, that));
	}

	/**
	 * Pass the elements to downstream until some one satisfied the until
	 * condition.
	 * 
	 * @param until
	 * @return
	 */
	public Canal<D> until(Filter<D> until)
	{
		return this.follow(new UntilOp<D>(until, null));
	}

	/**
	 * Pass the elements to downstream until some one satisfied the until
	 * condition. The satisfied element will be put into the drop array if the
	 * array not null or not empty. If no satisfied element then Option.none()
	 * will be put into the drop array.
	 * 
	 * @param until
	 * @param drop
	 * @return
	 */
	public Canal<D> until(Filter<D> until, Option<D>[] drop)
	{
		return this.follow(new UntilOp<D>(until, drop));
	}

	/**
	 * Zip each element with element in another Canal into a
	 * {@code Tuple2<D,E>}.
	 * 
	 * @param that
	 * @return
	 */
	public <E> PairCanal<D, E> zip(Canal<E> that)
	{
		return this.follow(new ZipOp<D, E>(that)).toPair();
	}

	/**
	 * Zip each element with element in another Canal into a
	 * {@code Tuple2<Option<D>,Option<E>>}. Value {@code None} would be filled
	 * in case that the element was missing on the corresponding position.
	 * 
	 * @param that
	 * @return
	 */
	public <E> PairCanal<Option<D>, Option<E>> zipOuter(Canal<E> that)
	{
		return this.follow(new ZipOuterOp<D, E>(that)).toPair();
	}

	/**
	 * Zip each element in this Canal with its index number as a {@code Tuple2
	 * <D,Integer>}.
	 * 
	 * @return
	 */
	public PairCanal<D, Integer> zipWithIndex()
	{
		return this.follow(new ZipWithIndexOp<D>()).toPair();
	}

	/**
	 * Zip each element in this Canal with its index number as a {@code Tuple2
	 * <D,Long>}.
	 * 
	 * @return
	 */
	public PairCanal<D, Long> zipWithIndexLong()
	{
		return this.follow(new ZipWithIndexLongOp<D>()).toPair();
	}

	/**
	 * Zip each element in this Canal with a flag indicates its phase:<br/>
	 * <ul>
	 * <li>1 (0b01): head</li>
	 * <li>2 (0b10): tail</li>
	 * <li>3 (0b11): both</li>
	 * <li>0 (0b00): body</li>
	 * </ul>
	 * 
	 * @return
	 */
	public PairCanal<D, Integer> zipWithPhase()
	{
		return this.follow(new ZipWithPhaseOp<D>()).toPair();
	}
}
