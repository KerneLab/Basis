package org.kernelab.basis;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.kernelab.basis.JSON.Pair;

public class Canal<U, D> implements Iterable<D>
{
	protected static abstract class AbstractPond<U, D> implements Pond<U, D>
	{
		private Pond<?, U> up;

		public void close() throws Exception
		{
			if (upstream() != null)
			{
				upstream().close();
			}
		}

		public void end() throws Exception
		{
			this.close();
		}

		public abstract boolean hasNext();

		public abstract D next();

		@Override
		public void remove()
		{
		}

		public Pond<?, U> upstream()
		{
			return up;
		}

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
		protected final Canal<?, B> that;

		public CartesianOp(Canal<?, B> that)
		{
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

		public CartesianPond(Canal<?, B> that)
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
		protected final Canal<?, B> that;

		public Dam(Canal<?, B> that)
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

	public static class DisposableIterable<E> implements Iterable<E>
	{
		protected final Iterator<E> iter;

		public DisposableIterable(Iterator<E> iter)
		{
			this.iter = iter;
		}

		@Override
		public Iterator<E> iterator()
		{
			return iter;
		}
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
			return new Creek<E, E>()
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
		public FullJoiner(Canal<?, R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
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
		public FullJoinOp(Canal<?, R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
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
			return generator.produce();
		}
	}

	protected static class GeneratedSourcer<E> implements Sourcer<E>
	{
		protected final Producer<E> generator;

		public GeneratedSourcer(Producer<E> generator)
		{
			this.generator = generator;
		}

		@Override
		public Source<E> newPond()
		{
			return new GeneratedSource<E>(generator);
		}
	}

	protected static class GroupByOp<E, K, V> implements Converter<E, Tuple2<K, Canal<?, V>>>
	{
		protected final Mapper<E, K>	kop;

		protected final Mapper<E, V>	vop;

		public GroupByOp(Mapper<E, K> kop, Mapper<E, V> vop)
		{
			this.kop = kop;
			this.vop = vop;
		}

		@Override
		public Pond<E, Tuple2<K, Canal<?, V>>> newPond()
		{
			return new Grouper<E, K, V>(kop, vop);
		}
	}

	protected static class Grouper<E, K, V> extends AbstractPond<E, Tuple2<K, Canal<?, V>>>
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
		public void begin()
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
					sediment.put(key, list = new LinkedList<V>());
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
		public Tuple2<K, Canal<?, V>> next()
		{
			Entry<K, List<V>> entry = iter.next();
			return Tuple.<K, Canal<?, V>> of(entry.getKey(), Canal.of(entry.getValue()));
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
		public InnerJoiner(Canal<?, R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
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
		public InnerJoinOp(Canal<?, R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
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
		protected final Canal<?, E>		that;

		protected final Comparator<E>	cmp;

		public IntersectionOp(Canal<?, E> that, Comparator<E> cmp)
		{
			this.that = that;
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

		public IntersectionPond(Canal<?, E> that, Comparator<E> cmp)
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

	public static class JoinCanal<U, K, L, R> extends PairCanal<U, K, Tuple2<L, R>>
	{
		/**
		 * Map each joint in this Canal.
		 * 
		 * @param mapper
		 * @return
		 */
		public <W> Canal<Tuple2<K, Tuple2<L, R>>, W> mapJoint(final JointMapper<L, R, K, W> mapper)
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
		protected final Canal<?, R>		that;

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

		private final List<U>			emptyU	= new LinkedList<U>();

		private final List<V>			emptyV	= new LinkedList<V>();

		private boolean					isEmptyU;

		private boolean					isEmptyV;

		private K						k;

		private M						m;

		private N						n;

		private final M					missM;

		private final N					missN;

		private byte					hasM;

		private byte					hasN;

		public Joiner(Canal<?, R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
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
		public void begin()
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
		protected final Canal<?, R>		that;

		protected final Mapper<L, K>	kol;

		protected final Mapper<R, K>	kor;

		protected final Mapper<L, U>	vol;

		protected final Mapper<R, V>	vor;

		public JoinOp(Canal<?, R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
		{
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

	protected static class LeftJoiner<L, R, K, U, V> extends Joiner<L, R, K, U, V, U, Option<V>>
	{
		public LeftJoiner(Canal<?, R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
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
		public LeftJoinOp(Canal<?, R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
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
					return mapper.map(upstream().next());
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
			return mapper.map(o1).compareTo(mapper.map(o2));
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

	public static class PairCanal<U, K, V> extends Canal<U, Tuple2<K, V>>
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
		public <W> PairCanal<Tuple2<K, Canal<?, V>>, K, W> foldByKey(final Producer<W> initiator,
				final Reducer<V, W> folder)
		{
			return this.groupByKey().mapValues(new Mapper<Canal<?, V>, W>()
			{
				@Override
				public W map(Canal<?, V> el)
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
		public <W> JoinCanal<Tuple2<K, Tuple2<Option<V>, Option<W>>>, K, Option<V>, Option<W>> fullJoin(
				Canal<?, Tuple2<K, W>> that)
		{
			return fullJoin(that, new DefaultKop<Tuple2<K, V>, K>(), new DefaultKop<Tuple2<K, W>, K>())
					.<K, Tuple2<Option<V>, Option<W>>> toPair().toJoin();
		}

		/**
		 * Gather each value into correspondent group identified by same key.
		 * 
		 * @return
		 */
		public PairCanal<Tuple2<K, V>, K, Canal<?, V>> groupByKey()
		{
			return this.groupBy(new DefaultKop<Tuple2<K, V>, K>(), new DefaultVop<Tuple2<K, V>, V>());
		}

		/**
		 * Filter pairs whose value statisfy the pred.
		 * 
		 * @param pred
		 * @return
		 */
		public PairCanal<Tuple2<K, V>, K, V> having(final Filter<V> pred)
		{
			return this.filter(new Filter<Tuple2<K, V>>()
			{
				@Override
				public boolean filter(Tuple2<K, V> el)
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
		public <W> JoinCanal<Tuple2<K, Tuple2<V, W>>, K, V, W> join(Canal<?, Tuple2<K, W>> that)
		{
			return join(that, new DefaultKop<Tuple2<K, V>, K>(), new DefaultKop<Tuple2<K, W>, K>())
					.<K, Tuple2<V, W>> toPair().toJoin();
		}

		/**
		 * Pass each key of pair to downstream.
		 * 
		 * @return
		 */
		public Canal<Tuple2<K, V>, K> keys()
		{
			return this.map(new DefaultKop<Tuple2<K, V>, K>());
		}

		/**
		 * Left join with another Canal.
		 * 
		 * @param that
		 * @return
		 */
		public <W> JoinCanal<Tuple2<K, Tuple2<V, Option<W>>>, K, V, Option<W>> leftJoin(Canal<?, Tuple2<K, W>> that)
		{
			return leftJoin(that, new DefaultKop<Tuple2<K, V>, K>(), new DefaultKop<Tuple2<K, W>, K>())
					.<K, Tuple2<V, Option<W>>> toPair().toJoin();
		}

		/**
		 * Map each value in pair.
		 * 
		 * @param mapper
		 * @return
		 */
		public <W> PairCanal<Tuple2<K, V>, K, W> mapValues(final Mapper<V, W> mapper)
		{
			return this.map(new Mapper<Tuple2<K, V>, Tuple2<K, W>>()
			{
				@Override
				public Tuple2<K, W> map(Tuple2<K, V> el)
				{
					return Tuple.of(el._1, mapper.map(el._2));
				}
			}).toPair();
		}

		/**
		 * Reduce each value within same group.
		 * 
		 * @param reducer
		 * @return
		 */
		public PairCanal<Tuple2<K, Canal<?, V>>, K, V> reduceByKey(final Reducer<V, V> reducer)
		{
			return this.groupByKey().mapValues(new Mapper<Canal<?, V>, V>()
			{
				@Override
				public V map(Canal<?, V> el)
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
		public <W> JoinCanal<Tuple2<K, Tuple2<Option<V>, W>>, K, Option<V>, W> rightJoin(Canal<?, Tuple2<K, W>> that)
		{
			return rightJoin(that, new DefaultKop<Tuple2<K, V>, K>(), new DefaultKop<Tuple2<K, W>, K>())
					.<K, Tuple2<Option<V>, W>> toPair().toJoin();
		}

		@SuppressWarnings("unchecked")
		public <L, R> JoinCanal<Tuple2<K, V>, K, L, R> toJoin()
		{
			return (JoinCanal<Tuple2<K, V>, K, L, R>) new JoinCanal<Tuple2<K, V>, K, L, R>().setUpstream(this)
					.setOperator(new MapOp<Tuple2<K, V>, Tuple2<K, Tuple2<L, R>>>(
							new Mapper<Tuple2<K, V>, Tuple2<K, Tuple2<L, R>>>()
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
		public Canal<Tuple2<K, V>, V> values()
		{
			return this.map(new DefaultVop<Tuple2<K, V>, V>());
		}
	}

	protected static class PeekOp<E> implements Converter<E, E>
	{
		protected final Action<E> action;

		public PeekOp(Action<E> action)
		{
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
					action.action(el);
					return el;
				}
			};
		}
	}

	protected static interface Pond<I, O> extends CloseableIterator<O>
	{
		void begin() throws Exception;

		void close() throws Exception;

		void end() throws Exception;

		Pond<?, I> upstream();

		void upstream(Pond<?, I> up);
	}

	public static interface Producer<E>
	{
		E produce();
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
					try
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
				protected Collection<E> newSediment()
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
		public RightJoiner(Canal<?, R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
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
		public RightJoinOp(Canal<?, R> that, Mapper<L, K> kol, Mapper<R, K> kor, Mapper<L, U> vol, Mapper<R, V> vor)
		{
			super(that, kol, kor, vol, vor);
		}

		@Override
		public Pond<L, Tuple2<K, Tuple2<Option<U>, V>>> newPond()
		{
			return new RightJoiner<L, R, K, U, V>(that, kol, kor, vol, vor);
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
					return new LinkedList<E>();
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
					return new LinkedList<E>();
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
		public void begin() throws Exception
		{
		}

		public void close() throws Exception
		{
		}

		public void end() throws Exception
		{
			this.close();
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

	protected static class StratifyPond<E> extends AbstractPond<E, Canal<?, E>>
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
			List<E> dat = new LinkedList<E>();

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
		public Canal<?, E> next()
		{
			return Canal.of(res.next());
		}
	}

	protected static class StratifyWithOp<E> implements Converter<E, Canal<?, E>>
	{
		protected final Comparator<E> cmp;

		public StratifyWithOp(Comparator<E> cmp)
		{
			this.cmp = cmp;
		}

		@Override
		public Pond<E, Canal<?, E>> newPond()
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
		protected final Canal<?, E>		that;

		protected final Comparator<E>	cmp;

		public SubtractOp(Canal<?, E> that, Comparator<E> cmp)
		{
			this.that = that;
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

		public SubtractPond(Canal<?, E> that, Comparator<E> cmp)
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
			private int index = 0;

			@Override
			public boolean hasNext()
			{
				return index < size();
			}

			@Override
			public Object next()
			{
				return get(index++);
			}
		}

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

		public static <E1, E2, E3> Tuple3<E1, E2, E3> of(E1 _1, E2 _2, E3 _3)
		{
			return new Tuple3<E1, E2, E3>(_1, _2, _3);
		}

		protected void ensureIndex(int index)
		{
			if (index < 0 || index >= size())
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

		public abstract Object get(int i);

		public Iterator<Object> iterator()
		{
			return new TupleIterator();
		}

		public abstract int size();
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
		public int compareTo(Object o)
		{
			return Tools.compare(this._1, ((Tuple1<E1>) o)._1);
		}

		@Override
		public Object get(int i)
		{
			ensureIndex(i);
			return _1;
		}

		@Override
		public int size()
		{
			return 1;
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

		@SuppressWarnings("unchecked")
		@Override
		public int compareTo(Object o)
		{
			Tuple2<E1, E2> t = ((Tuple2<E1, E2>) o);
			return Tools.compare(this._1, t._1, this._2, t._2);
		}

		@Override
		public Object get(int i)
		{
			ensureIndex(i);
			if (i == size() - 1)
			{
				return _2;
			}
			else
			{
				return super.get(i);
			}
		}

		@Override
		public int size()
		{
			return 2;
		}

		@Override
		public String toString()
		{
			return "(" + _1 + ", " + _2 + ")";
		}
	}

	public static class Tuple3<E1, E2, E3> extends Tuple2<E1, E2>
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= 2128204413194342750L;

		public final E3				_3;

		public Tuple3(E1 _1, E2 _2, E3 _3)
		{
			super(_1, _2);
			this._3 = _3;
		}

		@SuppressWarnings("unchecked")
		@Override
		public int compareTo(Object o)
		{
			Tuple3<E1, E2, E3> t = ((Tuple3<E1, E2, E3>) o);
			return Tools.compare(this._1, t._1, this._2, t._2, this._3, t._3);
		}

		@Override
		public Object get(int i)
		{
			ensureIndex(i);
			if (i == size() - 1)
			{
				return _3;
			}
			else
			{
				return super.get(i);
			}
		}

		@Override
		public int size()
		{
			return 3;
		}

		@Override
		public String toString()
		{
			return "(" + _1 + ", " + _2 + ", " + _3 + ")";
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
		/**
		 * The index of current iterating element.<br />
		 * This index should {@code +1} just after {@code upstream().next()} is
		 * called and before any other action.
		 */
		protected long index = 0;

		public void begin()
		{
		}

		@Override
		public boolean hasNext()
		{
			return upstream().hasNext();
		}
	}

	protected static class ZipOp<A, B> implements Converter<A, Tuple2<A, B>>
	{
		protected final Canal<?, B> that;

		public ZipOp(Canal<?, B> that)
		{
			this.that = that;
		}

		@Override
		public Pond<A, Tuple2<A, B>> newPond()
		{
			return new ZipPond<A, B>(that);
		}
	}

	protected static class ZipPond<A, B> extends Dam<A, B, Tuple2<A, B>>
	{
		private Pond<?, B> there;

		public ZipPond(Canal<?, B> that)
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

	protected static class ZipWithIndexOp<E> implements Converter<E, Tuple2<E, Long>>
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
		List<Comparator<E>> list = new LinkedList<Comparator<E>>();
		Comparator<E> cmp = null;

		for (Object o : orders)
		{
			if (o instanceof Mapper<?, ?>)
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

	public static <E, K, V> Map<K, List<V>> group(Iterator<E> iter, Mapper<E, K> kop, Mapper<E, V> vop)
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
				map.put(k, new LinkedList<V>());
			}
			map.get(k).add(vop.map(el));
		}
		return map;
	}

	public static <E> Comparator<E> inverse(Comparator<E> cmp)
	{
		return new InverseComparator<E>(cmp);
	}

	public static <E> DisposableIterable<E> iterable(Iterator<E> iter)
	{
		return new DisposableIterable<E>(iter);
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

	public static <E> Canal<?, E> of(E[] array)
	{
		return of(array, 0);
	}

	public static <E> Canal<?, E> of(E[] array, int begin)
	{
		return of(array, begin, array.length);
	}

	public static <E> Canal<?, E> of(E[] array, int begin, int end)
	{
		return new Canal<E, E>().setOperator(new ArraySourcer<E>(array, begin, end));
	}

	@SuppressWarnings("unchecked")
	public static <E> Canal<?, E> of(Iterable<E> iter)
	{
		return iter instanceof Canal ? (Canal<?, E>) iter //
				: new Canal<E, E>().setOperator(new IterableSourcer<E>(iter));
	}

	public static PairCanal<?, String, Object> of(JSON json)
	{
		return new Canal<JSON.Pair, JSON.Pair>() //
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

	public static <K, V> PairCanal<?, K, V> of(Map<K, V> map)
	{
		return new Canal<Entry<K, V>, Entry<K, V>>() //
				.setOperator(new IterableSourcer<Entry<K, V>>(map.entrySet())) //
				.map(new Mapper<Entry<K, V>, Tuple2<K, V>>()
				{
					@Override
					public Tuple2<K, V> map(Entry<K, V> el)
					{
						return Tuple.of(el.getKey(), el.getValue());
					}
				}).toPair();
	}

	public static <E> Canal<?, E> of(Producer<E> spring)
	{
		return new Canal<E, E>().setOperator(new GeneratedSourcer<E>(spring));
	}

	public static <E> Option<E> option(E value)
	{
		return value != null ? some(value) : Canal.<E> none();
	}

	public static <E> Some<E> some(E value)
	{
		return new Some<E>(value);
	}

	public static <E> List<Iterable<E>> stratify(Iterable<E> data, Comparator<E> cmp)
	{
		List<Iterable<E>> res = new LinkedList<Iterable<E>>();

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
				lvl = new LinkedList<E>();
				res.add(lvl);
			}

			lvl.add(el);

			last = el;
		}

		return res;
	}

	private Canal<?, U>		upstream;

	private Operator<U, D>	operator;

	protected Pond<U, D> build()
	{
		return begin(build(null));
	}

	protected Pond<U, D> build(Pond<D, ?> down)
	{
		Pond<U, D> pond = this.newPond();

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
	 * Make Cartesian product result against another given {@code Canal<?,N>}.
	 * 
	 * @param that
	 * @return
	 */
	public <N> PairCanal<D, D, N> cartesian(Canal<?, N> that)
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
	public Canal<D, D> distinct()
	{
		return this.distinct(null);
	}

	/**
	 * Remove duplicate elements with a given {@link Comparator}.
	 * 
	 * @param cmp
	 * @return
	 */
	public Canal<D, D> distinct(Comparator<D> cmp)
	{
		return this.follow(new DistinctOp<D>(cmp));
	}

	@SuppressWarnings("unchecked")
	protected <T> T evaluate()
	{
		return ((Terminal<U, T>) this.build()).get();
	}

	/**
	 * Filter the elements.
	 * 
	 * @param filter
	 *            {@code (D data)->boolean} returns true if and only if the
	 *            element need to be passed to the downstream.
	 * @return
	 */
	public Canal<D, D> filter(Filter<D> filter)
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
	 *            {@code (D data)->boolean} returns true if and only if the
	 *            element need to be passed to the downstream.
	 * @return
	 */
	public Option<D> first(Filter<D> filter)
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
	public <V> Canal<D, V> flatMap(Mapper<D, Iterable<V>> mapper)
	{
		return this.follow(new FlatMapOp<D, V>(mapper));
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

	protected <N> Canal<D, N> follow(Operator<D, N> op)
	{
		return new Canal<D, N>().setUpstream(this).setOperator(op);
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
	protected <R, K, M, N> Canal<D, Tuple2<K, Tuple2<Option<M>, Option<N>>>> fullJoin(Canal<?, R> that,
			Mapper<D, K> kol, Mapper<R, K> kor)
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
	public <R, K, M, N> Canal<D, Tuple2<K, Tuple2<Option<M>, Option<N>>>> fullJoin(Canal<?, R> that, Mapper<D, K> kol,
			Mapper<R, K> kor, Mapper<D, M> vol, Mapper<R, N> vor)
	{
		return this.follow(new FullJoinOp<D, R, K, M, N>(that, kol, kor, vol, vor));
	}

	protected Operator<U, D> getOperator()
	{
		return operator;
	}

	protected Canal<?, U> getUpstream()
	{
		return upstream;
	}

	/**
	 * Gather each element into correspondent group identified by same key.
	 * 
	 * @param kop
	 *            {@code (D data)->K key} the kop of data.
	 * @return
	 */
	public <K> PairCanal<D, K, Canal<?, D>> groupBy(Mapper<D, K> kop)
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
	public <K, V> PairCanal<D, K, Canal<?, V>> groupBy(Mapper<D, K> kop, Mapper<D, V> vop)
	{
		return this.follow(new GroupByOp<D, K, V>(kop, vop)).toPair();
	}

	/**
	 * Pass the element that both in this and that Canal to the downstream.
	 * 
	 * @param that
	 * @return
	 */
	public Canal<D, D> intersection(Canal<?, D> that)
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
	public Canal<D, D> intersection(Canal<?, D> that, Comparator<D> cmp)
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
	protected <R, K, M, N> Canal<D, Tuple2<K, Tuple2<M, N>>> join(Canal<?, R> that, Mapper<D, K> kol, Mapper<R, K> kor)
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
	public <R, K, M, N> Canal<D, Tuple2<K, Tuple2<M, N>>> join(Canal<?, R> that, Mapper<D, K> kol, Mapper<R, K> kor,
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
	public <K> PairCanal<D, K, D> keyBy(final Mapper<D, K> kop)
	{
		return this.map(new Mapper<D, Tuple2<K, D>>()
		{
			@Override
			public Tuple2<K, D> map(D key)
			{
				return Tuple.of(kop.map(key), key);
			}
		}).toPair();
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
	protected <R, K, M, N> Canal<D, Tuple2<K, Tuple2<M, Option<N>>>> leftJoin(Canal<?, R> that, Mapper<D, K> kol,
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
	public <R, K, M, N> Canal<D, Tuple2<K, Tuple2<M, Option<N>>>> leftJoin(Canal<?, R> that, Mapper<D, K> kol,
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
	public Canal<D, D> limit(int limit)
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
	public <V> Canal<D, V> map(Mapper<D, V> mapper)
	{
		return this.follow(new MapOp<D, V>(mapper));
	}

	@SuppressWarnings("unchecked")
	protected Pond<U, D> newPond()
	{
		if (this.getOperator() instanceof Sourcer<?>)
		{
			return (Pond<U, D>) ((Sourcer<D>) this.getOperator()).newPond();
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
	public Canal<D, D> peek(Action<D> action)
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
		return this.follow(new ReduceOp<D>(reducer)).evaluate();
	}

	/**
	 * Reverse the elements' order in this Canal.
	 * 
	 * @return
	 */
	public Canal<D, D> reverse()
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
	protected <R, K, M, N> Canal<D, Tuple2<K, Tuple2<Option<M>, N>>> rightJoin(Canal<?, R> that, Mapper<D, K> kol,
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
	public <R, K, M, N> Canal<D, Tuple2<K, Tuple2<Option<M>, N>>> rightJoin(Canal<?, R> that, Mapper<D, K> kol,
			Mapper<R, K> kor, Mapper<D, M> vol, Mapper<R, N> vor)
	{
		return this.follow(new RightJoinOp<D, R, K, M, N>(that, kol, kor, vol, vor));
	}

	protected Canal<U, D> setOperator(Operator<U, D> operator)
	{
		this.operator = operator;
		return this;
	}

	protected Canal<U, D> setUpstream(Canal<?, U> upstream)
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
	public Canal<D, D> skip(int skip)
	{
		return this.follow(new SkipOp<D>(skip));
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
	public Canal<D, D> sortBy(Object... orders)
	{
		return this.follow(new SortByOp<D>(Canal.<D> comparatorsOfOrders(orders)));
	}

	/**
	 * Sort each element in this Canal by natural ascending order.
	 * 
	 * @return
	 */
	public Canal<D, D> sortWith()
	{
		return sortWith(null);
	}

	/**
	 * Sort each element in this Canal by natural order.
	 * 
	 * @param ascend
	 * @return
	 */
	public Canal<D, D> sortWith(boolean ascend)
	{
		return sortWith(null, ascend);
	}

	/**
	 * Sort each element in this Canal by a given Comparator in ascending order.
	 * 
	 * @param cmp
	 * @return
	 */
	public Canal<D, D> sortWith(Comparator<D> cmp)
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
	public Canal<D, D> sortWith(Comparator<D> cmp, boolean ascend)
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
	public Canal<D, Canal<?, D>> stratifyBy(Object... orders)
	{
		return this.stratifyWith(comparator(Canal.<D> comparatorsOfOrders(orders)));
	}

	/**
	 * Stratify each elements into levels according to the given Comparator.
	 * 
	 * @param cmp
	 * @return
	 */
	public Canal<D, Canal<?, D>> stratifyWith(Comparator<D> cmp)
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
	public Canal<D, Canal<?, D>> stratifyWith(Comparator<D> cmp, boolean ascend)
	{
		return this.stratifyWith(ascend ? cmp : inverse(cmp));
	}

	/**
	 * Pass the elements in this Canal but not in that Canal to the downstream.
	 * 
	 * @param that
	 * @return
	 */
	public Canal<D, D> subtract(Canal<?, D> that)
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
	public Canal<D, D> subtract(Canal<?, D> that, Comparator<D> cmp)
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
	public <K, V> PairCanal<U, K, V> toPair()
	{
		return (PairCanal<U, K, V>) new PairCanal<U, K, V>().setUpstream(this.getUpstream())
				.setOperator((Operator<U, Tuple2<K, V>>) this.getOperator());
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
	public Canal<D, D> union(Canal<?, D> that)
	{
		return this.follow(new UnionOp<D>(this, that));
	}

	/**
	 * Zip each element with element in another Canal into a
	 * {@code Tuple2<D,E>}.
	 * 
	 * @param that
	 * @return
	 */
	public <E> PairCanal<D, D, E> zip(Canal<?, E> that)
	{
		return this.follow(new ZipOp<D, E>(that)).toPair();
	}

	/**
	 * Zip each element in this Canal with its index number as a
	 * {@code Tuple2<D,Long>}.
	 * 
	 * @return
	 */
	public PairCanal<D, D, Long> zipWithIndex()
	{
		return this.follow(new ZipWithIndexOp<D>()).toPair();
	}
}
