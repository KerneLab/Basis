package org.kernelab.basis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.kernelab.basis.Canal.Option;
import org.kernelab.basis.Canal.Tuple2;
import org.kernelab.basis.JSON.Context;
import org.kernelab.basis.io.DataReader;
import org.kernelab.basis.io.StringBuilderWriter;

interface Hierarchical extends Copieable<Hierarchical>
{
	public Context context();

	public String entry();

	public Hierarchical entry(String entry);

	public JSON outer();

	public Hierarchical outer(JSON json);
}

/**
 * A light weighted class of JSON.
 * 
 * @author Dilly King
 */
public class JSON implements Map<String, Object>, Iterable<Object>, Serializable, Hierarchical
{
	protected static abstract class AbstractIterator<E> implements Iterator<E>
	{
		protected int			index;
		protected Iterator<E>[]	iters;
		protected Iterator<E>	iter;

		public AbstractIterator(Iterator<E>... iters)
		{
			this.iters = iters;
			this.index = -1;
			this.nextIter();
		}

		@Override
		public boolean hasNext()
		{
			while (iter != null)
			{
				if (iter.hasNext())
				{
					return true;
				}
				else if (nextIter() == null)
				{
					return false;
				}
			}
			return false;
		}

		protected Iterator<E> nextIter()
		{
			iter = null;

			if (iters != null)
			{
				do
				{
					index++;

					if (index < iters.length)
					{
						iter = iters[index];
					}
					else
					{
						break;
					}
				}
				while (iter == null);
			}

			return iter;
		}

		@Override
		public void remove()
		{
			if (iter != null)
			{
				iter.remove();
			}
		}
	}

	private static class ArrayLengthReverseComparator<T> implements Comparator<T[]>, Serializable
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -6259816417783534899L;

		@Override
		public int compare(T[] o1, T[] o2)
		{
			int c = o2.length - o1.length;
			if (c == 0)
			{
				c = o2.hashCode() - o1.hashCode();
			}
			return c;
		}
	}

	public static class Context extends JSON
	{
		private class ContextReader extends DataReader implements Serializable
		{
			/**
			 * 
			 */
			private static final long	serialVersionUID	= 1515152016778877444L;

			private String				entry				= null;

			private StringBuilder		buffer				= new StringBuilder();

			private Matcher				entryMatcher		= VAR_ENTRY_PATTERN.matcher("");

			private Matcher				exitMatcher			= VAR_EXIT_PATTERN.matcher("");

			private boolean				inComment			= false;

			@Override
			protected void readFinished()
			{
			}

			@Override
			protected void readLine(CharSequence line)
			{
				int from = 0;

				if (inComment)
				{
					int end = Tools.seekIndex(line, BLOCK_COMMENT_END, from);

					if (end > NOT_FOUND)
					{
						from = end + BLOCK_COMMENT_END.length();
						inComment = false;
					}
					else
					{
						from = line.length();
					}
				}

				if (!inComment)
				{
					inComment = FilterComments(buffer, line, from);
				}

				if (!inComment)
				{
					buffer.append(VAR_NEXT_LINE_CHAR);

					if (entry == null)
					{
						if (entryMatcher.reset(buffer).lookingAt())
						{
							entry = entryMatcher.group(2);
							line = entryMatcher.group(3);
							Tools.clearStringBuilder(buffer);
							buffer.append(line);
						}
					}

					if (entry != null)
					{
						if (exitMatcher.reset(buffer).lookingAt()
								&& JSON.DualMatchCount(buffer, OBJECT_BEGIN_CHAR, OBJECT_END_CHAR, 0) == 0)
						{
							line = exitMatcher.group(1);
							Tools.clearStringBuilder(buffer);
							buffer.append(line);

							Object object = JSON.ParseValueOf(buffer.toString());

							if (object == JSON.NOT_A_VALUE)
							{
								object = JSON.Parse(buffer);
							}

							if (object != JSON.NOT_A_VALUE)
							{
								Quotation.Quote(Context.this, entry, object);
								entry = null;
								Tools.clearStringBuilder(buffer);
							}
						}
					}
				}
			}

			@Override
			protected void readPrepare()
			{
				entry = null;
				inComment = false;
			}
		}

		/**
		 * 
		 */
		private static final long		serialVersionUID	= -7912039879626749853L;

		public static final String		VAR_DEFINE_MARK		= "var";

		public static final char		VAR_ASSIGN_CHAR		= '=';

		public static final String		VAR_ASSIGN_MARK		= String.valueOf(VAR_ASSIGN_CHAR);

		public static final char		VAR_NEXT_LINE_CHAR	= '\n';

		public static final char		VAR_END_CHAR		= ';';

		public static final String		VAR_END_MARK		= String.valueOf(VAR_END_CHAR);

		public static final Pattern		VAR_ENTRY_PATTERN	= Pattern
				.compile("^\\s*?(var\\s+)?\\s*?(\\w+)\\s*?=\\s*(.*);?\\s*$", Pattern.DOTALL);

		public static final Pattern		VAR_EXIT_PATTERN	= Pattern.compile("^\\s*(.*?)\\s*;\\s*$", Pattern.DOTALL);

		private transient DataReader	reader;

		public Context()
		{
			reader = new ContextReader();
		}

		@Override
		public Context context()
		{
			return this;
		}

		protected DataReader getReader()
		{
			return reader;
		}

		public Context read(File file) throws IOException
		{
			reader.setDataFile(file).read();
			return this;
		}

		public Context read(File file, String charsetName) throws IOException
		{
			reader.setDataFile(file, charsetName).read();
			return this;
		}

		public Context read(InputStream is) throws IOException
		{
			reader.setInputStream(is).read();
			return this;
		}

		public Context read(InputStream is, String charsetName) throws IOException
		{
			reader.setInputStream(is, charsetName).read();
			return this;
		}

		public Context read(Reader r)
		{
			reader.setReader(r).read();
			return this;
		}

		protected void setReader(DataReader reader)
		{
			this.reader = reader;
		}
	}

	protected static class EntrySet extends AbstractSet<Map.Entry<String, Object>>
	{
		protected static class EntryIterator extends AbstractIterator<Map.Entry<String, Object>>
		{
			public EntryIterator(Iterator<Map.Entry<String, Object>>... iters)
			{
				super(iters);
			}

			@Override
			public Map.Entry<String, Object> next()
			{
				return new InnerEntry(iter.next());
			}
		}

		protected Set<Map.Entry<String, Object>>[] sets;

		public EntrySet(Set<Map.Entry<String, Object>>... sets)
		{
			this.sets = sets;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Iterator<Map.Entry<String, Object>> iterator()
		{
			Iterator<Map.Entry<String, Object>>[] iters = null;

			if (sets != null)
			{
				iters = (Iterator<Map.Entry<String, Object>>[]) new Iterator<?>[sets.length];

				for (int i = 0; i < sets.length; i++)
				{
					if (sets[i] != null)
					{
						iters[i] = sets[i].iterator();
					}
				}
			}

			return new EntryIterator(iters);
		}

		@Override
		public int size()
		{
			int size = 0;
			if (sets != null)
			{
				for (Set<Map.Entry<String, Object>> set : sets)
				{
					if (set != null)
					{
						size += set.size();
					}
				}
			}
			return size;
		}
	}

	public static class Function implements Hierarchical, Serializable
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= -2638142033937727881L;

		public static final String	DEFINE_MARK			= "function";

		public static final char	DEFINE_FIRST_CHAR	= 'f';

		public static final Pattern	DEFINE_PATTERN		= Pattern.compile("^(function)\\s*?([^(]*?)(\\()([^)]*?)(\\))");

		public static final String	DEFAULT_NAME_PREFIX	= "_";

		private JSON				outer;

		private String				entry;

		private String				expression;

		private String				name;

		private List<String>		parameters;

		protected Function(Function f)
		{
			expression(f.expression()).outer(f.outer()).entry(f.entry());
		}

		public Function(String expression)
		{
			expression(expression);
		}

		@Override
		public Function clone()
		{
			return new Function(this);
		}

		public Context context()
		{
			return outer == null ? null : outer.context();
		}

		public String entry()
		{
			return entry;
		}

		public Function entry(String entry)
		{
			this.entry = entry;
			name(null);
			return this;
		}

		@Override
		public boolean equals(Object o)
		{
			boolean is = false;

			if (this == o)
			{
				is = true;
			}
			else
			{
				if (o instanceof Function)
				{
					is = this.expression().equals(((Function) o).expression());
				}
			}

			return is;
		}

		public String expression()
		{
			return expression;
		}

		protected Function expression(String expression)
		{
			Matcher matcher = DEFINE_PATTERN.matcher(expression);

			if (matcher.find())
			{
				this.expression = expression;

				name(matcher.group(2).trim());

				String[] params = matcher.group(4).split(",");

				List<String> list = new LinkedList<String>();

				for (String param : params)
				{
					list.add(param.trim());
				}

				parameters(list);
			}

			return this;
		}

		public String name()
		{
			if (name == null || name.length() == 0)
			{
				name = DEFAULT_NAME_PREFIX + (entry() == null ? "" : entry().trim());
			}
			return name;
		}

		protected Function name(String name)
		{
			this.name = name;
			return this;
		}

		public JSON outer()
		{
			return outer;
		}

		public Function outer(JSON json)
		{
			this.outer = json;
			return this;
		}

		public List<String> parameters()
		{
			return parameters;
		}

		protected Function parameters(List<String> parameters)
		{
			this.parameters = Collections.unmodifiableList(parameters);
			return this;
		}

		@Override
		public String toString()
		{
			return expression();
		}

		public String toString(String name)
		{
			return DEFINE_PATTERN.matcher(expression())
					.replaceFirst("$1 " + (name == null ? name() : name.trim()) + "$3$4$5");
		}
	}

	protected static class InnerEntry implements Map.Entry<String, Object>
	{
		protected final Map.Entry<String, Object> entry;

		public InnerEntry(Map.Entry<String, Object> entry)
		{
			this.entry = entry;
		}

		@Override
		public boolean equals(Object o)
		{
			return this.entry.equals(o);
		}

		@Override
		public String getKey()
		{
			return this.entry.getKey();
		}

		@Override
		public Object getValue()
		{
			return Quote(this.entry.getValue());
		}

		@Override
		public int hashCode()
		{
			return this.entry.hashCode();
		}

		@Override
		public Object setValue(Object value)
		{
			return this.entry.setValue(value);
		}

		@Override
		public String toString()
		{
			return this.entry.toString();
		}
	}

	/**
	 * A class to describe Array object using JSON format.
	 * 
	 * @author Dilly King
	 * 
	 */
	public static class JSAN extends JSON implements RandomAccess
	{
		private static class ArrayIndexComparator implements Comparator<String>, Serializable
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = -3834859681987586236L;

			public int compare(String a, String b)
			{
				Integer va = Index(a);
				Integer vb = Index(b);

				if (va != null && vb != null)
				{
					return va - vb;
				}
				else if (va == null && vb != null)
				{
					return 1;
				}
				else if (va != null && vb == null)
				{
					return -1;
				}
				else
				{
					return a.compareTo(b);
				}
			}
		}

		public static class GeneralValueComparator implements Comparator<Object>, Serializable
		{
			/**
			 * 
			 */
			private static final long			serialVersionUID	= -7397716884280949033L;

			private static Map<Class<?>, Short>	TYPE_RANK			= new HashMap<Class<?>, Short>();

			private static final short			BOOLEAN_RANK		= 0;

			private static final short			NUMBER_RANK			= 1;

			private static final short			CHARACTER_RANK		= 2;

			private static final short			STRING_RANK			= 3;

			private static final short			DATE_RANK			= 4;

			private static final short			CALENDAR_RANK		= 5;

			private static final short			HIERARCH_RANK		= 6;

			private static final short			NONE_RANK			= Short.MAX_VALUE;

			public static final int				ASCEND				= 1;

			public static final int				DESCEND				= -1;

			static
			{
				TYPE_RANK.put(Boolean.class, BOOLEAN_RANK);
				TYPE_RANK.put(Number.class, NUMBER_RANK);
				TYPE_RANK.put(Character.class, CHARACTER_RANK);
				TYPE_RANK.put(CharSequence.class, STRING_RANK);
				TYPE_RANK.put(java.util.Date.class, DATE_RANK);
				TYPE_RANK.put(java.util.Calendar.class, CALENDAR_RANK);
				TYPE_RANK.put(Hierarchical.class, HIERARCH_RANK);
			}

			protected static int CompareIndex(String a, String b)
			{
				int c = 0;

				Integer indexA = Index(a), indexB = Index(b);

				if (indexA == null && indexB == null)
				{
					c = a.compareTo(b);
				}
				else if (indexA != null && indexB == null)
				{
					c = -1;
				}
				else if (indexA == null && indexB != null)
				{
					c = 1;
				}
				else
				{
					c = indexA - indexB;
				}

				return c;
			}

			protected static short getTypeRank(Object value)
			{
				Short rank = null;

				if (value != null)
				{
					for (Entry<Class<?>, Short> entry : TYPE_RANK.entrySet())
					{
						if (Tools.superClass(value, entry.getKey()) != null)
						{
							rank = entry.getValue();
							break;
						}
					}
				}

				return rank == null ? NONE_RANK : rank;
			}

			private int order;

			public GeneralValueComparator()
			{
				this(ASCEND);
			}

			public GeneralValueComparator(int order)
			{
				this.order = order >= 0 ? ASCEND : DESCEND;
			}

			public int compare(Object a, Object b)
			{
				int c = 0;

				if (a == null && b == null)
				{
					c = 0;
				}
				else if (a == null && b != null)
				{
					c = 1;
				}
				else if (a != null && b == null)
				{
					c = -1;
				}
				else
				{
					Object x = a, y = b;

					if (IsQuotation(a))
					{
						x = ((Quotation) x).quote();
					}

					if (IsQuotation(b))
					{
						y = ((Quotation) y).quote();
					}

					short rankX = getTypeRank(x), rankY = getTypeRank(y);

					if (rankX == rankY)
					{
						switch (rankX)
						{
							case BOOLEAN_RANK:
								// false=0, true=1
								c = ((Boolean) x ? 1 : 0) - ((Boolean) y ? 1 : 0);
								break;

							case NUMBER_RANK:
								c = (int) Math.signum(((Number) x).doubleValue() - ((Number) y).doubleValue());
								break;

							case CHARACTER_RANK:
								c = (Character) x - (Character) y;
								break;

							case STRING_RANK:
								c = ((CharSequence) x).toString().compareTo(((CharSequence) y).toString());
								break;

							case DATE_RANK:
								c = (int) Math.signum(((java.util.Date) x).getTime() - ((java.util.Date) y).getTime());
								break;

							case CALENDAR_RANK:
								c = (int) Math.signum(((java.util.Calendar) x).getTimeInMillis()
										- ((java.util.Calendar) y).getTimeInMillis());
								break;

							case HIERARCH_RANK:
								// For Hierarchical values, compare the entry.
								// This means keep its order in the JSAN.
								if (!Tools.equals(x, y))
								{
									c = CompareIndex(((Hierarchical) a).entry(), ((Hierarchical) b).entry());
								}
								break;

							default:
								c = x.hashCode() - y.hashCode();
								break;
						}
					}
					else
					{
						c = rankX - rankY;
					}
				}

				return c * order;
			}
		}

		public static interface Reflector<T>
		{
			public JSAN reflect(JSAN jsan, T obj);
		}

		/**
		 * 
		 */
		private static final long	serialVersionUID	= 2156642568827950757L;

		public static final int		LAST				= -1;

		protected static final String Index(int i)
		{
			return String.valueOf(i);
		}

		protected static final Integer Index(Object o)
		{
			Integer index = null;

			if (o != null)
			{
				index = o instanceof Integer ? (Integer) o : Index(o.toString());
			}

			return index;
		}

		protected static final Integer Index(String key)
		{
			if (key == null)
			{
				return null;
			}

			try
			{
				int index = Integer.parseInt(key);

				if (index < 0)
				{
					return null;
				}
				else
				{
					return index;
				}
			}
			catch (NumberFormatException e)
			{
				return null;
			}
		}

		public static boolean IsTable(JSAN jsan)
		{
			int columns = -1;
			for (Object r : jsan)
			{
				if (r == null)
				{
					continue;
				}
				else if (!(r instanceof JSAN))
				{
					return false;
				}
				if (columns == -1)
				{
					columns = ((JSAN) r).size();
				}
				else if (columns != ((JSAN) r).size())
				{
					return false;
				}
			}
			return true;
		}

		public static JSAN Of(Iterable<?> itr)
		{
			if (itr == null)
			{
				return null;
			}
			else if (itr instanceof JSAN)
			{
				return (JSAN) itr;
			}
			else
			{
				return new JSAN().addAll(itr);
			}
		}

		public static <T> JSAN Of(T... arr)
		{
			if (arr == null)
			{
				return null;
			}
			else
			{
				return new JSAN().addAll(arr);
			}
		}

		public static JSAN Reflect(JSAN jsan, Map<Class<?>, Object> reflects, Object object)
		{
			if (object != null)
			{
				if (jsan == null)
				{
					jsan = new JSAN().reflects(reflects);
				}

				Object reflect = ReflectOf(object, reflects);

				if (reflect instanceof JSAN.Reflector)
				{
					jsan = JSAN.Reflect(jsan, object, (JSAN.Reflector<?>) reflect);
				}
				else if (reflect instanceof JSON.Reflector)
				{
					jsan = (JSAN) JSON.Reflect(jsan, object, (JSON.Reflector<?>) reflect);
				}
				else if (JSAN.IsJSAN(reflect))
				{
					jsan = JSAN.Reflect(jsan, object, (JSAN) reflect);
				}
				else if (JSON.IsJSON(reflect))
				{
					jsan = (JSAN) JSON.Reflect(jsan, object, (JSON) reflect);
				}
				else
				{
					jsan = (JSAN) JSAN.Reflect(jsan, object);
				}
			}

			return jsan;
		}

		public static JSAN Reflect(JSAN jsan, Object object)
		{
			JSAN reflect = null;

			Collection<String> fields = null;

			if (!(object instanceof Map) && !(object instanceof Iterable) && !IsArray(object))
			{
				fields = KeysOf(object);
			}

			if (fields != null)
			{
				reflect = new JSAN();

				for (String field : fields)
				{
					reflect.add(field);
				}
			}

			return JSAN.Reflect(jsan, object, reflect);
		}

		public static JSAN Reflect(JSAN jsan, Object object, Iterable<?> fields)
		{
			JSAN reflect = null;

			if (fields != null)
			{
				reflect = new JSAN();

				for (Object field : fields)
				{
					if (field != null)
					{
						reflect.add(field);
					}
				}
			}

			return JSAN.Reflect(jsan, object, reflect);
		}

		@SuppressWarnings("unchecked")
		public static JSAN Reflect(JSAN jsan, Object object, JSAN reflect)
		{
			if (object != null)
			{
				if (jsan == null)
				{
					jsan = new JSAN().reflects(object.getClass(), reflect);
				}

				if (IsJSAN(object))
				{
					JSAN obj = (JSAN) object;

					if (reflect == null)
					{
						jsan.putAll(obj);
					}
					else
					{
						for (Pair pair : reflect.pairs())
						{
							try
							{
								String key = pair.getValue().toString();

								if (obj.containsKey(key))
								{
									jsan.attr(pair.getKey(), obj.attr(key));
								}
							}
							catch (Exception e)
							{
							}
						}
					}
				}
				else if (IsJSON(object))
				{
					JSON obj = (JSON) object;

					if (reflect == null)
					{
						jsan.putAll(obj);
					}
					else
					{
						for (Pair pair : reflect.pairs())
						{
							try
							{
								String key = pair.getValue().toString();

								if (obj.containsKey(key))
								{
									jsan.attr(pair.getKey(), obj.attr(key));
								}
							}
							catch (Exception e)
							{
							}
						}
					}
				}
				else if (object instanceof Map)
				{
					Map<String, ?> map = (Map<String, ?>) object;

					if (reflect == null)
					{
						jsan.putAll(map);
					}
					else
					{
						for (Pair pair : reflect.pairs())
						{
							try
							{
								Object key = pair.getValue();

								if (map.containsKey(key))
								{
									jsan.attr(pair.getKey(), map.get(key));
								}
							}
							catch (Exception e)
							{
							}
						}
					}
				}
				else if (object instanceof Iterable)
				{
					Iterable<Object> obj = (Iterable<Object>) object;

					if (reflect == null)
					{
						int i = 0;

						for (Object o : obj)
						{
							jsan.attr(i++, o);
						}
					}
					else
					{
						Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();

						for (Pair pair : reflect.pairs())
						{
							try
							{
								Integer index = Index(pair.getValue().toString());

								if (index != null)
								{
									List<String> keys = map.get(index);

									if (keys == null)
									{
										keys = new LinkedList<String>();
										map.put(index, keys);
									}

									keys.add(pair.getKey());
								}
							}
							catch (Exception e)
							{
							}
						}

						int i = 0;

						for (Object o : obj)
						{
							if (map.isEmpty())
							{
								break;
							}
							else if (map.containsKey(i))
							{
								for (String k : map.get(i))
								{
									jsan.attr(k, o);
								}
								map.remove(i);
							}
							i++;
						}
					}
				}
				else if (IsArray(object))
				{
					int len = Array.getLength(object);

					if (reflect == null)
					{
						for (int i = 0; i < len; i++)
						{
							jsan.attr(i, Array.get(object, i));
						}
					}
					else
					{
						Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();

						for (Pair pair : reflect.pairs())
						{
							try
							{
								Integer index = Index(pair.getValue().toString());

								if (index != null)
								{
									List<String> keys = map.get(index);

									if (keys == null)
									{
										keys = new LinkedList<String>();
										map.put(index, keys);
									}

									keys.add(pair.getKey());
								}
							}
							catch (Exception e)
							{
							}
						}

						for (int i = 0; !map.isEmpty() && i < len; i++)
						{
							if (map.containsKey(i))
							{
								Object o = Array.get(object, i);

								for (String k : map.get(i))
								{
									jsan.attr(k, o);
								}
								map.remove(i);
							}
						}
					}
				}
				else if (reflect != null)
				{
					// Reflect Object using template
					int i = 0;

					for (Object field : reflect)
					{
						try
						{
							i = Access(jsan, i, object, field.toString(), null);
						}
						catch (Exception e)
						{
						}
					}
				}
				else
				{
					// Template is null.
					jsan = JSAN.Reflect(jsan, object);
				}
			}

			return jsan;
		}

		@SuppressWarnings("unchecked")
		public static <T> JSAN Reflect(JSAN jsan, Object object, JSAN.Reflector<T> reflector)
		{
			if (object != null && reflector != null)
			{
				if (jsan == null)
				{
					jsan = new JSAN();
				}

				try
				{
					jsan = (JSAN) reflector.reflect(jsan, (T) object);
				}
				catch (ClassCastException e)
				{
				}
			}

			return jsan;
		}

		public static JSAN Reflect(JSAN jsan, Object object, Map<String, ?> fields)
		{
			JSAN reflect = null;

			if (fields != null)
			{
				reflect = new JSAN();

				for (Entry<String, ?> entry : fields.entrySet())
				{
					if (entry.getKey() != null && entry.getValue() != null)
					{
						reflect.put(entry.getKey(), entry.getValue());
					}
				}
			}

			return JSAN.Reflect(jsan, object, reflect);
		}

		public static JSAN Reflect(JSAN jsan, Object object, String... fields)
		{
			JSAN reflect = null;

			if (fields != null)
			{
				reflect = new JSAN();

				for (String field : fields)
				{
					if (field != null)
					{
						reflect.add(field);
					}
				}
			}

			return JSAN.Reflect(jsan, object, reflect);
		}

		public static JSAN Reflect(Map<Class<?>, Object> reflects, Object object)
		{
			return JSAN.Reflect(null, reflects, object);
		}

		public static JSAN Reflect(Object object)
		{
			return JSAN.Reflect((JSAN) null, object);
		}

		public static JSAN Reflect(Object object, Iterable<?> fields)
		{
			return JSAN.Reflect(null, object, fields);
		}

		public static JSAN Reflect(Object object, Map<String, ?> reflect)
		{
			return JSAN.Reflect(null, object, reflect);
		}

		public static JSAN Reflect(Object object, String... fields)
		{
			return JSAN.Reflect(null, object, fields);
		}

		public static JSAN Sort(JSAN source, Set<Object> sorter, JSAN target)
		{
			if (source != null)
			{
				if (sorter == null)
				{
					sorter = new TreeSet<Object>();
				}
				else
				{
					sorter.clear();
				}

				if (target == null)
				{
					target = new JSAN().templates(source);
				}

				sorter.addAll(source.values());

				// Must clear here after addTo in case of target==source
				target.clear();

				target.splice(0, 0, sorter);
			}

			return target;
		}

		public static JSAN Unique(JSAN source, JSAN target)
		{
			return Sort(source, new LinkedHashSet<Object>(), target);
		}

		private Map<String, Object>	array;

		private int					length	= 0;

		public JSAN()
		{
			super();

			array(new TreeMap<String, Object>(new ArrayIndexComparator()));
		}

		public JSAN(Object... values)
		{
			this();
			addAll(LAST, values);
		}

		public JSAN add(int index, Object object)
		{
			return splice(index, 0, object);
		}

		public JSAN add(Object object)
		{
			this.put(length(), object);
			return this;
		}

		public JSAN addAll(boolean[] array)
		{
			for (boolean e : array)
			{
				this.add(e);
			}
			return this;
		}

		public JSAN addAll(byte[] array)
		{
			for (byte e : array)
			{
				this.add(e);
			}
			return this;
		}

		public JSAN addAll(char[] array)
		{
			for (char e : array)
			{
				this.add(e);
			}
			return this;
		}

		public JSAN addAll(double[] array)
		{
			for (double e : array)
			{
				this.add(e);
			}
			return this;
		}

		public JSAN addAll(float[] array)
		{
			for (float e : array)
			{
				this.add(e);
			}
			return this;
		}

		public JSAN addAll(int index, boolean[] array)
		{
			Object[] objects = new Object[array.length];
			int i = 0;
			for (boolean e : array)
			{
				objects[i++] = e;
			}
			addAll(index, objects);
			return this;
		}

		public JSAN addAll(int index, byte[] array)
		{
			Object[] objects = new Object[array.length];
			int i = 0;
			for (byte e : array)
			{
				objects[i++] = e;
			}
			addAll(index, objects);
			return this;
		}

		public JSAN addAll(int index, char[] array)
		{
			Object[] objects = new Object[array.length];
			int i = 0;
			for (char e : array)
			{
				objects[i++] = e;
			}
			addAll(index, objects);
			return this;
		}

		public JSAN addAll(int index, Collection<? extends Object> collection)
		{
			splice(index, 0, collection);
			return this;
		}

		public JSAN addAll(int index, double[] array)
		{
			Object[] objects = new Object[array.length];
			int i = 0;
			for (double e : array)
			{
				objects[i++] = e;
			}
			addAll(index, objects);
			return this;
		}

		public JSAN addAll(int index, float[] array)
		{
			Object[] objects = new Object[array.length];
			int i = 0;
			for (float e : array)
			{
				objects[i++] = e;
			}
			addAll(index, objects);
			return this;
		}

		public JSAN addAll(int index, int[] array)
		{
			Object[] objects = new Object[array.length];
			int i = 0;
			for (int e : array)
			{
				objects[i++] = e;
			}
			addAll(index, objects);
			return this;
		}

		public JSAN addAll(int index, JSAN jsan)
		{
			splice(index, 0, jsan);
			return this;
		}

		public JSAN addAll(int index, long[] array)
		{
			Object[] objects = new Object[array.length];
			int i = 0;
			for (long e : array)
			{
				objects[i++] = e;
			}
			addAll(index, objects);
			return this;
		}

		public JSAN addAll(int index, Object... array)
		{
			splice(index, 0, array);
			return this;
		}

		public JSAN addAll(int index, short[] array)
		{
			Object[] objects = new Object[array.length];
			int i = 0;
			for (short e : array)
			{
				objects[i++] = e;
			}
			addAll(index, objects);
			return this;
		}

		public JSAN addAll(int[] array)
		{
			for (int e : array)
			{
				this.add(e);
			}
			return this;
		}

		public JSAN addAll(Iterable<? extends Object> iterable)
		{
			for (Object e : iterable)
			{
				this.add(e);
			}
			return this;
		}

		public JSAN addAll(JSAN jsan)
		{
			for (Object e : jsan)
			{
				this.add(e);
			}
			return this;
		}

		public JSAN addAll(long[] array)
		{
			for (long e : array)
			{
				this.add(e);
			}
			return this;
		}

		public JSAN addAll(Object[] array)
		{
			for (Object e : array)
			{
				this.add(e);
			}
			return this;
		}

		public JSAN addAll(short[] array)
		{
			for (short e : array)
			{
				this.add(e);
			}
			return this;
		}

		public JSAN addLast(Object... values)
		{
			return addAll(length(), values);
		}

		@SuppressWarnings("unchecked")
		public <E extends Object, T extends Collection<E>> T addTo(T collection)
		{
			if (collection != null)
			{
				for (Object o : this)
				{
					try
					{
						collection.add((E) o);
					}
					catch (ClassCastException e)
					{
					}
				}
			}
			return collection;
		}

		public <E extends Object, T extends Collection<E>> T addTo(T collection, Class<E> cls)
		{
			if (cls == null)
			{
				return addTo(collection);
			}
			else
			{
				if (collection != null)
				{
					for (Object o : this)
					{
						try
						{
							collection.add(cls.cast(o));
						}
						catch (ClassCastException e)
						{
						}
					}
				}
				return collection;
			}
		}

		public <E extends Object, T extends Collection<E>> T addTo(T collection, Mapper<Object, E> mapper)
		{
			if (mapper == null)
			{
				return addTo(collection);
			}
			else
			{
				if (collection != null)
				{
					for (Object o : this)
					{
						try
						{
							collection.add(mapper.map(o));
						}
						catch (Exception e)
						{
						}
					}
				}
				return collection;
			}
		}

		public <E extends Object, T extends Collection<E>> T addTo(T collection, Transform<E> transform)
		{
			if (transform == null)
			{
				return addTo(collection);
			}
			else
			{
				if (collection != null)
				{
					for (Pair pair : this.pairs())
					{
						try
						{
							collection.add(transform.transform(this, pair.getKey(), pair.getValue()));
						}
						catch (Exception e)
						{
						}
					}
				}
				return collection;
			}
		}

		protected Map<String, Object> array()
		{
			return array;
		}

		protected JSAN array(Map<String, Object> array)
		{
			if (array != null)
			{
				this.array = array;
			}
			return this;
		}

		public JSAN asJSAN()
		{
			return this;
		}

		@SuppressWarnings("unchecked")
		public <E> E attr(int index)
		{
			try
			{
				return (E) this.get(index);
			}
			catch (ClassCastException e)
			{
				return null;
			}
		}

		public JSAN attr(int index, Object value)
		{
			this.put(index, value);
			return this;
		}

		@Override
		public <E> E attr(String key)
		{
			E element = null;

			if (key != null)
			{
				Integer index = Index(key.toString());

				if (index == null)
				{
					element = super.attr(key);
				}
				else
				{
					element = this.attr(index);
				}
			}

			return element;
		}

		@Override
		public JSAN attrAll(Map<String, ?> map)
		{
			this.putAll(map);
			return this;
		}

		public BigDecimal attrBigDecimal(int index)
		{
			return attrCast(index, BigDecimal.class);
		}

		public BigInteger attrBigInteger(int index)
		{
			return attrCast(index, BigInteger.class);
		}

		public Boolean attrBoolean(int index)
		{
			return attrCast(index, Boolean.class);
		}

		public Byte attrByte(int index)
		{
			return attrCast(index, Byte.class);
		}

		public Calendar attrCalendar(int index)
		{
			return attrCast(index, Calendar.class);
		}

		public <E> E attrCast(int index, Class<E> cls)
		{
			return CastAs(this.attr(index), cls);
		}

		public Character attrCharacter(int index)
		{
			return attrCast(index, Character.class);
		}

		public Date attrDate(int index)
		{
			return attrCast(index, Date.class);
		}

		public Double attrDouble(int index)
		{
			return attrCast(index, Double.class);
		}

		public Float attrFloat(int index)
		{
			return attrCast(index, Float.class);
		}

		public Function attrFunction(int index)
		{
			return attrCast(index, Function.class);
		}

		public <E> E attrIfAbsent(int index, Class<E> type, Mapper<Integer, E> map)
		{
			E val = this.attrCast(index, type);
			if (val == null)
			{
				try
				{
					this.attr(index, val = map.map(index));
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
			return val;
		}

		public <E> E attrIfPresent(int index, Class<E> type, StatefulMapper<Integer, E, E> map)
		{
			E val = this.attrCast(index, type);
			if (val != null)
			{
				try
				{
					this.attr(index, val = map.map(index, val));
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
			return val;
		}

		public Integer attrInteger(int index)
		{
			return attrCast(index, Integer.class);
		}

		public JSAN attrJSAN(int index)
		{
			return attrCast(index, JSAN.class);
		}

		public JSON attrJSON(int index)
		{
			return attrCast(index, JSON.class);
		}

		public Long attrLong(int index)
		{
			return attrCast(index, Long.class);
		}

		public <E> E attrMerge(int index, Class<E> type, StatefulMapper<Integer, E, E> map)
		{
			E val = this.attrCast(index, type);
			try
			{
				this.attr(index, val = map.map(index, val));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
			return val;
		}

		public Object attrObject(int index)
		{
			return attr(index);
		}

		public Short attrShort(int index)
		{
			return attrCast(index, Short.class);
		}

		public String attrString(int index)
		{
			return attrCast(index, String.class);
		}

		public Time attrTime(int index)
		{
			return attrCast(index, Time.class);
		}

		public Timestamp attrTimestamp(int index)
		{
			return attrCast(index, Timestamp.class);
		}

		protected int bound(int index)
		{
			return Tools.limitNumber(index(index) + (index < 0 ? 1 : 0), 0, length());
		}

		@Override
		public JSAN clean()
		{
			this.clear();
			return this;
		}

		@Override
		public void clear()
		{
			super.clear();
			array().clear();
			this.length(0);
		}

		@Override
		public JSAN clone()
		{
			return new JSAN().templates(this).clone(this);
		}

		public boolean containsAll(Iterable<? extends Object> iterable)
		{
			boolean contains = false;
			if (iterable != null)
			{
				contains = true;
				for (Object o : iterable)
				{
					if (!this.containsValue(o))
					{
						contains = false;
						break;
					}
				}
			}
			return contains;
		}

		@Override
		public boolean containsKey(Object key)
		{
			Integer index = Index(key);
			return index != null ? array().containsKey(index.toString()) : super.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value)
		{
			return array().containsValue(value) || object().containsValue(value);
		}

		public JSAN delete(Object value)
		{
			String key = keyOf(value);

			if (key != null)
			{
				this.remove(key);
			}

			return this;
		}

		public JSAN deleteAll(Iterable<? extends Object> iterable)
		{
			for (Object o : iterable)
			{
				this.deleteAll(o);
			}

			return this;
		}

		public JSAN deleteAll(Object value)
		{
			String key = null;

			while ((key = keyOf(value)) != null)
			{
				this.remove(key);
			}

			return this;
		}

		@Override
		public JSAN entry(String entry)
		{
			super.entry(entry);
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Set<Map.Entry<String, Object>> entrySet()
		{
			return new EntrySet(array().entrySet(), object().entrySet());
		}

		public boolean equalValues(JSAN jsan)
		{
			return this.size() == jsan.size() && this.containsAll(jsan) && jsan.containsAll(this);
		}

		public Object get(int index)
		{
			return Quote(this.got(index));
		}

		public Object get(Integer index)
		{
			return get((int) index);
		}

		@Override
		public Object get(Object key)
		{
			return Quote(this.got(key));
		}

		public Object got(int index)
		{
			if (index >= this.length())
			{
				return null;
			}
			return array().get(Index(bound(index)));
		}

		@Override
		public Object got(Object key)
		{
			Object element = null;

			if (key != null)
			{
				if (key instanceof Integer)
				{
					element = this.got(((Integer) key).intValue());
				}
				else
				{
					Integer index = Index(key);

					if (index != null)
					{
						element = this.got(index.intValue());
					}
					else
					{
						element = super.got(key);
					}
				}
			}

			return element;
		}

		public boolean has(int index)
		{
			return array().containsKey(Index(index));
		}

		@Override
		public boolean has(String entry)
		{
			Integer index = Index(entry);
			return index != null ? has(index) : super.has(entry);
		}

		protected int index(int index)
		{
			return index >= 0 ? index : index + length();
		}

		@Override
		public boolean isEmpty()
		{
			return array().isEmpty() && super.isEmpty();
		}

		public String keyOf(Object value)
		{
			String key = null;

			if (containsValue(value))
			{
				for (Pair pair : pairs())
				{
					if (Tools.equals(value, pair.getValue()))
					{
						key = pair.getKey();
						break;
					}
				}
			}

			return key;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Set<String> keySet()
		{
			return new KeySet(array().keySet(), object().keySet());
		}

		public String lastKeyOf(Object value)
		{
			String key = null;

			if (containsValue(value))
			{
				ValueIterator<Object> iter = new ValueIterator<Object>().reset(size());
				while (iter.hasPrevious())
				{
					iter.previous();

					if (Tools.equals(value, iter.get()))
					{
						key = iter.key();
						break;
					}
				}
			}

			return key;
		}

		public int length()
		{
			return length;
		}

		protected JSAN length(int length)
		{
			this.length = length;
			return this;
		}

		@Override
		public JSAN outer(JSON outer)
		{
			super.outer(outer);
			return this;
		}

		@Override
		public Set<Pair> pairs()
		{
			return pairs((Set<Pair>) null);
		}

		@Override
		public JSAN pairs(Iterable<? extends Object> pairs)
		{
			super.pairs(pairs);
			return this;
		}

		@Override
		public JSAN pairs(JSAN jsan)
		{
			super.pairs(jsan);
			return this;
		}

		@Override
		public JSAN pairs(JSON json)
		{
			super.pairs(json);
			return this;
		}

		@Override
		public JSAN pairs(Map<? extends String, ? extends Object> map)
		{
			super.pairs(map);
			return this;
		}

		@Override
		public JSAN pairs(Object... pairs)
		{
			super.pairs(pairs);
			return this;
		}

		@Override
		public Set<Pair> pairs(Set<Pair> result)
		{
			if (result == null)
			{
				result = new LinkedHashSet<Pair>();
			}

			for (String key : this.keySet())
			{
				result.add(new Pair(key));
			}

			return result;
		}

		@Override
		public JSAN projects(Class<?> cls, Iterable<?> fields)
		{
			super.projects(cls, fields);
			return this;
		}

		@Override
		public JSAN projects(Class<?> cls, JSAN project)
		{
			super.projects(cls, project);
			return this;
		}

		@Override
		public JSAN projects(Class<?> cls, JSON project)
		{
			super.projects(cls, project);
			return this;
		}

		@Override
		public JSAN projects(Class<?> cls, Map<String, Object> project)
		{
			super.projects(cls, project);
			return this;
		}

		@Override
		public JSAN projects(Class<?> cls, String... fields)
		{
			super.projects(cls, fields);
			return this;
		}

		@Override
		public <T> JSAN projects(Class<T> cls, JSON.Projector<T> projector)
		{
			super.projects(cls, projector);
			return this;
		}

		@Override
		public JSAN projects(JSON json)
		{
			super.projects(json);
			return this;
		}

		@Override
		public JSAN projects(Map<Class<?>, Object> projects)
		{
			super.projects(projects);
			return this;
		}

		@Override
		public JSAN projectsRemove(Class<?> cls)
		{
			super.projectsRemove(cls);
			return this;
		}

		@Override
		public JSAN projectStrict(boolean strict)
		{
			super.projectStrict(strict);
			return this;
		}

		@Override
		public JSAN projectStrict(JSON json)
		{
			super.projectStrict(json);
			return this;
		}

		public JSAN pushAll(Iterable<? extends Object> values)
		{
			for (Object o : values)
			{
				this.add(o);
			}
			return this;
		}

		public JSAN pushAll(Object... values)
		{
			for (Object o : values)
			{
				this.add(o);
			}
			return this;
		}

		public Object put(int index, Object value)
		{
			Object old = null;

			index = index(index);

			String key = Index(index);

			old = this.got(index);
			// Clear the old hierarchical relation.
			Hierarchical hirch = AsHierarchical(old);
			if (hirch != null && hirch.outer() == this)
			{
				hirch.outer(null).entry(null);
			}

			Transform<?> transform = this.transformOf(key, value);
			if (transform != null)
			{
				value = transform.transform(this, key, value);
			}

			value = ValueOf(value, reflects());

			hirch = AsHierarchical(value);
			if (hirch != null)
			{
				// Build up new hierarchical relation.
				JSON formalOuter = hirch.outer();
				String formalEntry = hirch.entry();
				JSON formalContext = hirch.context();

				hirch.outer(this).entry(key);

				// If in a Context.
				if (formalOuter != null && IsContext(formalContext) && IsJSON(hirch))
				{
					// The formal outer would quote the value at its new
					// location.
					if (formalContext == this.context())
					{
						// A quote would be made only in the same Context.
						formalOuter.put(formalEntry, AsJSON(hirch).quote());
					}
				}
			}

			array().put(key, value);

			if (index >= length())
			{
				length(index + 1);
			}

			return old;
		}

		@Override
		public Object put(String key, Object value)
		{
			Object old = null;

			if (key != null)
			{
				Integer index = Index(key);

				if (index == null)
				{
					old = super.put(key, value);
				}
				else
				{
					old = this.put(index, value);
				}
			}

			return old;
		}

		@Override
		public void putAll(Map<? extends String, ? extends Object> map)
		{
			for (Map.Entry<?, ?> entry : map.entrySet())
			{
				this.put((String) entry.getKey(), entry.getValue());
			}
		}

		@Override
		public JSAN reflect(Object object)
		{
			JSAN.Reflect(this, this.reflects(), object);
			return this;
		}

		@Override
		public JSAN reflect(Object object, Iterable<?> reflect)
		{
			JSAN.Reflect(this, object, reflect);
			return this;
		}

		public <T> JSAN reflect(Object object, JSAN.Reflector<T> reflector)
		{
			JSAN.Reflect(this, object, reflector);
			return this;
		}

		@Override
		public JSAN reflect(Object object, Map<String, ?> reflect)
		{
			JSAN.Reflect(this, object, reflect);
			return this;
		}

		@Override
		public JSAN reflect(Object object, String... fields)
		{
			JSAN.Reflect(this, object, fields);
			return this;
		}

		@Override
		public JSAN reflects(Class<?> cls, JSAN reflect)
		{
			super.reflects(cls, reflect);
			return this;
		}

		@Override
		public JSAN reflects(Class<?> cls, JSON reflect)
		{
			super.reflects(cls, reflect);
			return this;
		}

		@Override
		public <T> JSAN reflects(Class<T> cls, JSAN.Reflector<T> reflector)
		{
			super.reflects(cls, reflector);
			return this;
		}

		@Override
		public <T> JSAN reflects(Class<T> cls, JSON.Reflector<T> reflector)
		{
			super.reflects(cls, reflector);
			return this;
		}

		@Override
		public JSAN reflects(JSON json)
		{
			super.reflects(json);
			return this;
		}

		@Override
		public JSAN reflects(Map<Class<?>, Object> reflects)
		{
			super.reflects(reflects);
			return this;
		}

		@Override
		public JSAN reflectsJSAN(Class<?> cls, Iterable<?> fields)
		{
			super.reflectsJSAN(cls, fields);
			return this;
		}

		@Override
		public JSAN reflectsJSAN(Class<?> cls, JSAN fields)
		{
			super.reflectsJSAN(cls, fields);
			return this;
		}

		@Override
		public JSAN reflectsJSAN(Class<?> cls, JSON fields)
		{
			super.reflectsJSAN(cls, fields);
			return this;
		}

		@Override
		public JSAN reflectsJSAN(Class<?> cls, Map<String, ?> fields)
		{
			super.reflectsJSAN(cls, fields);
			return this;
		}

		@Override
		public JSAN reflectsJSAN(Class<?> cls, String... fields)
		{
			super.reflectsJSAN(cls, fields);
			return this;
		}

		@Override
		public JSAN reflectsJSON(Class<?> cls, Iterable<?> fields)
		{
			super.reflectsJSON(cls, fields);
			return this;
		}

		@Override
		public JSAN reflectsJSON(Class<?> cls, JSAN fields)
		{
			super.reflectsJSON(cls, fields);
			return this;
		}

		@Override
		public JSAN reflectsJSON(Class<?> cls, JSON fields)
		{
			super.reflectsJSON(cls, fields);
			return this;
		}

		@Override
		public JSAN reflectsJSON(Class<?> cls, Map<String, ?> fields)
		{
			super.reflectsJSON(cls, fields);
			return this;
		}

		@Override
		public JSAN reflectsJSON(Class<?> cls, String... fields)
		{
			super.reflectsJSON(cls, fields);
			return this;
		}

		@Override
		public JSAN reflectsRemove(Class<?> cls)
		{
			super.reflectsRemove(cls);
			return this;
		}

		@Override
		public Object remove(Object key)
		{
			Object value = null;

			if (key != null)
			{
				Integer index = Index(key.toString());

				if (index == null)
				{
					super.remove(key);
				}
				else
				{
					value = array().remove(key.toString());

					Hierarchical hirch = AsHierarchical(value);

					if (hirch != null)
					{
						if (hirch.outer() == this)
						{
							hirch.outer(null).entry(null);
						}
					}
				}
			}

			return value;
		}

		@Override
		public JSAN removeAll()
		{
			return clean();
		}

		@Override
		public JSAN removeAll(Iterable<? extends Object> keys)
		{
			if (keys != null)
			{
				for (Object o : keys)
				{
					this.remove(o);
				}
			}
			return this;
		}

		public JSAN retainAll(Collection<? extends Object> collection)
		{
			if (collection != null && !collection.isEmpty())
			{
				List<Object> temp = new LinkedList<Object>();
				for (Object o : this)
				{
					if (!collection.contains(o))
					{
						temp.add(o);
					}
				}
				this.deleteAll(temp);
			}
			return this;
		}

		public JSAN retainAll(JSAN jsan)
		{
			if (jsan != null && !jsan.isEmpty())
			{
				List<Object> temp = new LinkedList<Object>();
				for (Object o : this)
				{
					if (!jsan.hasVal(o))
					{
						temp.add(o);
					}
				}
				this.deleteAll(temp);
			}
			return this;
		}

		@Override
		public int size()
		{
			return array().size() + super.size();
		}

		public JSAN slice(int start)
		{
			return slice(start, LAST);
		}

		public JSAN slice(int start, int end)
		{
			return slice(null, start, end);
		}

		public JSAN slice(JSAN jsan, int start)
		{
			return slice(jsan, start, LAST);
		}

		public JSAN slice(JSAN jsan, int start, int end)
		{
			if (jsan == null)
			{
				jsan = new JSAN().templates(this);
			}
			else
			{
				jsan.clear();
			}

			start = bound(start);
			end = bound(end);

			ValueIterator<Object> iter = this.iterator(Index(start));

			while (iter.hasNext())
			{
				iter.next();

				if (iter.index() >= 0 && iter.index() < end)
				{
					jsan.attr(iter.index() - start, iter.get());
				}
				else
				{
					break;
				}
			}

			jsan.length(end - start);

			return jsan;
		}

		public JSAN sort()
		{
			return sort(this);
		}

		public JSAN sort(Comparator<Object> cmp)
		{
			return sort(cmp, this);
		}

		public JSAN sort(Comparator<Object> cmp, JSAN jsan)
		{
			if (cmp == null)
			{
				cmp = new GeneralValueComparator();
			}

			return sort(new TreeSet<Object>(cmp), jsan);
		}

		public JSAN sort(JSAN target)
		{
			return sort(new GeneralValueComparator(), target);
		}

		public JSAN sort(Set<Object> sorter)
		{
			return sort(sorter, this);
		}

		public JSAN sort(Set<Object> sorter, JSAN target)
		{
			return Sort(this, sorter, target);
		}

		public JSAN splice(int index, int cover, Collection<?> collection)
		{
			JSAN result = new JSAN().templates(this);

			int trace = length();
			index = bound(index);
			cover = Tools.limitNumber(cover, 0, trace - index);
			trace--;

			int fills = collection.size();
			int delta = fills - cover;

			ValueIterator<Object> iter = this.iterator(Index(index));

			// Clean
			while (iter.hasNext())
			{
				iter.next();

				if (iter.index() >= 0 && iter.index() < index + cover)
				{
					result.add(this.remove(iter.key()));
				}
				else
				{
					iter.previous();
					break;
				}
			}

			if (delta < 0)
			{
				// Shrink
				while (iter.hasNext())
				{
					iter.next();

					if (iter.index() >= 0)
					{
						this.put(iter.index() + delta, this.remove(iter.index()));
					}
					else
					{
						break;
					}
				}
				this.length(trace + delta + 1);
			}
			else if (delta > 0)
			{
				// Expand
				iter.reset(Index(length() + 1));

				int tail = index + cover;

				while (iter.hasPrevious())
				{
					iter.previous();

					if (iter.index() < tail)
					{
						break;
					}

					this.put(iter.index() + delta, this.remove(iter.index()));
				}
			}

			// Fill
			int i = 0;
			for (Object o : collection)
			{
				this.put(i + index, o);
				i++;
			}

			return result;
		}

		public JSAN splice(int index, int cover, JSAN jsan)
		{
			JSAN result = new JSAN().templates(this);

			int trace = length();
			index = bound(index);
			cover = Tools.limitNumber(cover, 0, trace - index);
			trace--;

			int fills = jsan.size();
			int delta = fills - cover;

			ValueIterator<Object> iter = this.iterator(Index(index));

			// Clean
			while (iter.hasNext())
			{
				iter.next();

				if (iter.index() >= 0 && iter.index() < index + cover)
				{
					result.add(this.remove(iter.key()));
				}
				else
				{
					iter.previous();
					break;
				}
			}

			if (delta < 0)
			{
				// Shrink
				while (iter.hasNext())
				{
					iter.next();

					if (iter.index() >= 0)
					{
						this.put(iter.index() + delta, this.remove(iter.index()));
					}
					else
					{
						break;
					}
				}
				this.length(trace + delta + 1);
			}
			else if (delta > 0)
			{
				// Expand
				iter.reset(Index(length() + 1));

				int tail = index + cover;

				while (iter.hasPrevious())
				{
					iter.previous();

					if (iter.index() < tail)
					{
						break;
					}

					this.put(iter.index() + delta, this.remove(iter.index()));
				}
			}

			// Fill
			int i = 0;
			for (Object o : jsan)
			{
				this.put(i + index, o);
				i++;
			}

			return result;
		}

		public JSAN splice(int index, int cover, Object... objects)
		{
			JSAN result = new JSAN().templates(this);

			int trace = length();
			index = bound(index);
			cover = Tools.limitNumber(cover, 0, trace - index);
			trace--;

			int fills = objects.length;
			int delta = fills - cover;

			ValueIterator<Object> iter = this.iterator(Index(index));

			// Clean
			while (iter.hasNext())
			{
				iter.next();

				if (iter.index() >= 0 && iter.index() < index + cover)
				{
					result.add(this.remove(iter.key()));
				}
				else
				{
					iter.previous();
					break;
				}
			}

			if (delta < 0)
			{
				// Shrink
				while (iter.hasNext())
				{
					iter.next();

					if (iter.index() >= 0)
					{
						this.put(iter.index() + delta, this.remove(iter.index()));
					}
					else
					{
						break;
					}
				}
				this.length(trace + delta + 1);
			}
			else if (delta > 0)
			{
				// Expand
				iter.reset(Index(length() + 1));

				int tail = index + cover;

				while (iter.hasPrevious())
				{
					iter.previous();

					if (iter.index() < tail)
					{
						break;
					}

					this.put(iter.index() + delta, this.remove(iter.index()));
				}
			}

			// Fill
			for (int i = 0; i < fills; i++)
			{
				this.put(i + index, objects[i]);
			}

			return result;
		}

		@Override
		public JSAN templates(JSON json)
		{
			super.templates(json);
			return this;
		}

		public Object[] toArray()
		{
			Object[] array = new Object[size()];

			int i = 0;
			for (Object o : this)
			{
				array[i++] = o;
			}

			return array;
		}

		@SuppressWarnings("unchecked")
		public <T> T[] toArray(T[] array)
		{
			if (array.length < size())
			{
				array = (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), size());
			}

			int i = 0;
			for (Object o : this)
			{
				array[i++] = (T) o;
			}

			for (; i < array.length;)
			{
				array[i++] = null;
			}

			return array;
		}

		@Override
		public JSAN toJSAN()
		{
			return this;
		}

		@Override
		public JSAN transforms(JSON json)
		{
			super.transforms(json);
			return this;
		}

		@Override
		public JSAN transforms(Map<Object, Transform<?>> transforms)
		{
			super.transforms(transforms);
			return this;
		}

		@Override
		public JSAN transforms(Object entry, Transform<?> transform)
		{
			super.transforms(entry, transform);
			return this;
		}

		@Override
		public JSAN transformsRemove(Object entry)
		{
			super.transformsRemove(entry);
			return this;
		}

		@Override
		protected JSAN transformsSingleton()
		{
			super.transformsSingleton();
			return this;
		}

		public JSAN unique()
		{
			return unique(this);
		}

		public JSAN unique(JSAN target)
		{
			return Unique(this, target);
		}

		@SuppressWarnings("unchecked")
		public <E> E val(int index)
		{
			try
			{
				return (E) attr(index);
			}
			catch (ClassCastException e)
			{
				return null;
			}
		}

		public <E> E val(int index, E defaultValue)
		{
			E val = val(index);
			return val == null ? defaultValue : val;
		}

		public BigDecimal valBigDecimal(int index)
		{
			return CastToBigDecimal(this.attr(index));
		}

		public BigDecimal valBigDecimal(int index, double defaultValue)
		{
			BigDecimal val = valBigDecimal(index);
			return val == null ? new BigDecimal(defaultValue) : val;
		}

		public BigDecimal valBigDecimal(int index, String defaultValue)
		{
			BigDecimal val = valBigDecimal(index);
			return val == null ? new BigDecimal(defaultValue) : val;
		}

		public BigInteger valBigInteger(int index)
		{
			return CastToBigInteger(this.attr(index));
		}

		public BigInteger valBigInteger(int index, long defaultValue)
		{
			BigInteger val = valBigInteger(index);
			return val == null ? new BigInteger(String.valueOf(defaultValue)) : val;
		}

		public BigInteger valBigInteger(int index, String defaultValue)
		{
			BigInteger val = valBigInteger(index);
			return val == null ? new BigInteger(defaultValue) : val;
		}

		public Boolean valBoolean(int index)
		{
			return CastToBoolean(this.attr(index));
		}

		public Boolean valBoolean(int index, Boolean defaultValue)
		{
			Boolean val = valBoolean(index);
			return val == null ? defaultValue : val;
		}

		public Byte valByte(int index)
		{
			return CastToByte(this.attr(index));
		}

		public Byte valByte(int index, Byte defaultValue)
		{
			Byte val = valByte(index);
			return val == null ? defaultValue : val;
		}

		public Calendar valCalendar(int index)
		{
			return CastToCalendar(this.attr(index));
		}

		public Calendar valCalendar(int index, Calendar defaultValue)
		{
			Calendar val = valCalendar(index);
			return val == null ? defaultValue : val;
		}

		public Calendar valCalendar(int index, long defaultValue)
		{
			Calendar val = valCalendar(index);

			if (val == null)
			{
				val = new GregorianCalendar();
				val.setTimeInMillis(defaultValue);
			}

			return val;
		}

		public <E> E valCast(int index, Class<E> cls)
		{
			return Tools.castTo(this.valObject(index), cls);
		}

		public <E> E valCast(int index, Class<E> cls, E defaultValue)
		{
			E val = valCast(index, cls);
			return val == null ? defaultValue : val;
		}

		public Character valCharacter(int index)
		{
			return CastToCharacter(this.attr(index));
		}

		public Character valCharacter(int index, Character defaultValue)
		{
			Character val = valCharacter(index);
			return val == null ? defaultValue : val;
		}

		public Date valDate(int index)
		{
			return CastToDate(this.attr(index));
		}

		public Date valDate(int index, Date defaultValue)
		{
			Date val = valDate(index);
			return val == null ? defaultValue : val;
		}

		public Date valDate(int index, long defaultValue)
		{
			Date val = valDate(index);
			return val == null ? new Date(defaultValue) : val;
		}

		public Double valDouble(int index)
		{
			return CastToDouble(this.attr(index));
		}

		public Double valDouble(int index, Double defaultValue)
		{
			Double val = valDouble(index);
			return val == null ? defaultValue : val;
		}

		public Float valFloat(int index)
		{
			return CastToFloat(this.attr(index));
		}

		public Float valFloat(int index, Float defaultValue)
		{
			Float val = valFloat(index);
			return val == null ? defaultValue : val;
		}

		public Function valFunction(int index)
		{
			return CastToFunction(this.attr(index));
		}

		public Function valFunction(int index, Function defaultValue)
		{
			Function val = valFunction(index);
			return val == null ? defaultValue : val;
		}

		public Function valFunction(int index, String defaultValue)
		{
			Function val = valFunction(index);
			return val == null ? new Function(defaultValue) : val;
		}

		public <E> E valIfAbsent(int index, Class<E> type, Mapper<Integer, E> map)
		{
			E val = this.valCast(index, type);
			if (val == null)
			{
				try
				{
					this.attr(index, val = map.map(index));
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
			return val;
		}

		public <E> E valIfPresent(int index, Class<E> type, StatefulMapper<Integer, E, E> map)
		{
			E val = this.valCast(index, type);
			if (val != null)
			{
				try
				{
					this.attr(index, val = map.map(index, val));
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
			return val;
		}

		public Integer valInteger(int index)
		{
			return CastToInteger(this.attr(index));
		}

		public Integer valInteger(int index, Integer defaultValue)
		{
			Integer val = valInteger(index);
			return val == null ? defaultValue : val;
		}

		public JSAN valJSAN(int index)
		{
			return CastToJSAN(this.attr(index));
		}

		public JSAN valJSAN(int index, boolean newIfNull)
		{
			JSAN val = valJSAN(index);
			return val == null && newIfNull ? new JSAN() : val;
		}

		public JSAN valJSAN(int index, JSAN defaultValue)
		{
			JSAN val = valJSAN(index);
			return val == null ? defaultValue : val;
		}

		public JSON valJSON(int index)
		{
			return CastToJSON(this.attr(index));
		}

		public JSON valJSON(int index, boolean newIfNull)
		{
			JSON val = valJSON(index);
			return val == null && newIfNull ? new JSON() : val;
		}

		public JSON valJSON(int index, JSON defaultValue)
		{
			JSON val = valJSON(index);
			return val == null ? defaultValue : val;
		}

		public Long valLong(int index)
		{
			return CastToLong(this.attr(index));
		}

		public Long valLong(int index, Long defaultValue)
		{
			Long val = valLong(index);
			return val == null ? defaultValue : val;
		}

		public <E> E valMerge(int index, Class<E> type, StatefulMapper<Integer, E, E> map)
		{
			E val = this.valCast(index, type);
			try
			{
				this.attr(index, val = map.map(index, val));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
			return val;
		}

		public Object valObject(int index)
		{
			return attr(index);
		}

		public Object valObject(int index, Object defaultValue)
		{
			Object val = valObject(index);
			return val == null ? defaultValue : val;
		}

		public Short valShort(int index)
		{
			return CastToShort(this.attr(index));
		}

		public Short valShort(int index, Short defaultValue)
		{
			Short val = valShort(index);
			return val == null ? defaultValue : val;
		}

		public String valString(int index)
		{
			return CastToString(this.attr(index));
		}

		public String valString(int index, String defaultValue)
		{
			String val = valString(index);
			return val == null ? defaultValue : val;
		}

		public Time valTime(int index)
		{
			return CastToTime(this.attr(index));
		}

		public Time valTime(int index, long defaultValue)
		{
			Time val = valTime(index);
			return val == null ? new Time(defaultValue) : val;
		}

		public Time valTime(int index, Time defaultValue)
		{
			Time val = valTime(index);
			return val == null ? defaultValue : val;
		}

		public Timestamp valTimestamp(int index)
		{
			return CastToTimestamp(this.attr(index));
		}

		public Timestamp valTimestamp(int index, long defaultValue)
		{
			Timestamp val = valTimestamp(index);
			return val == null ? new Timestamp(defaultValue) : val;
		}

		public Timestamp valTimestamp(int index, Timestamp defaultValue)
		{
			Timestamp val = valTimestamp(index);
			return val == null ? defaultValue : val;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Collection<Object> values()
		{
			return new ValuesCollection(array().values(), object().values());
		}
	}

	protected static class KeySet extends AbstractSet<String>
	{
		protected static class KeyIterator extends AbstractIterator<String>
		{
			public KeyIterator(Iterator<String>... iters)
			{
				super(iters);
			}

			@Override
			public String next()
			{
				return iter.next();
			}
		}

		protected Set<String>[] keys;

		public KeySet(Set<String>... keys)
		{
			this.keys = keys;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Iterator<String> iterator()
		{
			Iterator<String>[] iters = null;

			if (keys != null)
			{
				iters = (Iterator<String>[]) new Iterator<?>[keys.length];

				for (int i = 0; i < keys.length; i++)
				{
					if (keys[i] != null)
					{
						iters[i] = keys[i].iterator();
					}
				}
			}

			return new KeyIterator(iters);
		}

		@Override
		public int size()
		{
			int size = 0;
			if (keys != null)
			{
				for (Set<String> set : keys)
				{
					if (set != null)
					{
						size += set.size();
					}
				}
			}
			return size;
		}
	}

	public class Pair implements Entry<String, Object>
	{
		public final String key;

		public Pair(String key)
		{
			this.key = key;
		}

		@Override
		public boolean equals(Object o)
		{
			if (o instanceof Pair)
			{
				return Tools.equals(key, ((Pair) o).key);
			}
			else
			{
				return false;
			}
		}

		public String getKey()
		{
			return key;
		}

		public Object getValue()
		{
			return attr(key);
		}

		@Override
		public int hashCode()
		{
			return key == null ? 0 : key.hashCode();
		}

		public String key()
		{
			return key;
		}

		public Object setValue(Object value)
		{
			Object old = getValue();
			attr(key, value);
			return old;
		}

		public <E> E val()
		{
			return JSON.this.val(key);
		}

		public <E> E val(E defaultValue)
		{
			return JSON.this.val(key, defaultValue);
		}

		public BigDecimal valBigDecimal()
		{
			return JSON.this.valBigDecimal(key);
		}

		public BigDecimal valBigDecimal(double defaultValue)
		{
			return JSON.this.valBigDecimal(key, defaultValue);
		}

		public BigDecimal valBigDecimal(String defaultValue)
		{
			return JSON.this.valBigDecimal(key, defaultValue);
		}

		public Boolean valBoolean()
		{
			return JSON.this.valBoolean(key);
		}

		public Boolean valBoolean(Boolean defaultValue)
		{
			return JSON.this.valBoolean(key, defaultValue);
		}

		public Byte valByte()
		{
			return JSON.this.valByte(key);
		}

		public Byte valByte(Byte defaultValue)
		{
			return JSON.this.valByte(key, defaultValue);
		}

		public Calendar valCalendar()
		{
			return JSON.this.valCalendar(key);
		}

		public Calendar valCalendar(long defaultValue)
		{
			return JSON.this.valCalendar(key, defaultValue);
		}

		public <E> E valCast(Class<E> cls)
		{
			return JSON.this.valCast(key, cls);
		}

		public <E> E valCast(Class<E> cls, E defaultValue)
		{
			return JSON.this.valCast(key, cls, defaultValue);
		}

		public Character valCharacter()
		{
			return JSON.this.valCharacter(key);
		}

		public Character valCharacter(Character defaultValue)
		{
			return JSON.this.valCharacter(key, defaultValue);
		}

		public Date valDate()
		{
			return JSON.this.valDate(key);
		}

		public Date valDate(long defaultValue)
		{
			return JSON.this.valDate(key, defaultValue);
		}

		public Double valDouble()
		{
			return JSON.this.valDouble(key);
		}

		public Double valDouble(Double defaultValue)
		{
			return JSON.this.valDouble(key, defaultValue);
		}

		public Float valFloat()
		{
			return JSON.this.valFloat(key);
		}

		public Float valFloat(Float defaultValue)
		{
			return JSON.this.valFloat(key, defaultValue);
		}

		public Function valFunction()
		{
			return JSON.this.valFunction(key);
		}

		public Function valFunction(Function defaultValue)
		{
			return JSON.this.valFunction(key, defaultValue);
		}

		public Function valFunction(String defaultValue)
		{
			return JSON.this.valFunction(key, defaultValue);
		}

		public Integer valInteger()
		{
			return JSON.this.valInteger(key);
		}

		public Integer valInteger(Integer defaultValue)
		{
			return JSON.this.valInteger(key, defaultValue);
		}

		public JSAN valJSAN()
		{
			return JSON.this.valJSAN(key);
		}

		public JSAN valJSAN(boolean newIfNull)
		{
			return JSON.this.valJSAN(key, newIfNull);
		}

		public JSAN valJSAN(JSAN defaultValue)
		{
			return JSON.this.valJSAN(key, defaultValue);
		}

		public JSON valJSON()
		{
			return JSON.this.valJSON(key);
		}

		public JSON valJSON(boolean newIfNull)
		{
			return JSON.this.valJSON(key, newIfNull);
		}

		public JSON valJSON(JSON defaultValue)
		{
			return JSON.this.valJSON(key, defaultValue);
		}

		public Long valLong()
		{
			return JSON.this.valLong(key);
		}

		public Long valLong(Long defaultValue)
		{
			return JSON.this.valLong(key, defaultValue);
		}

		public Short valShort()
		{
			return JSON.this.valShort(key);
		}

		public Short valShort(Short defaultValue)
		{
			return JSON.this.valShort(key, defaultValue);
		}

		public String valString()
		{
			return JSON.this.valString(key);
		}

		public String valString(String defaultValue)
		{
			return JSON.this.valString(key, defaultValue);
		}

		public Time valTime()
		{
			return JSON.this.valTime(key);
		}

		public Time valTime(long defaultValue)
		{
			return JSON.this.valTime(key, defaultValue);
		}

		public Timestamp valTimestamp()
		{
			return JSON.this.valTimestamp(key);
		}

		public Timestamp valTimestamp(long defaultValue)
		{
			return JSON.this.valTimestamp(key, defaultValue);
		}
	}

	public static class Parser
	{
		public static final int	DISPOSED				= -1;

		public static final int	DONE					= 0;

		public static final int	IN_NONE					= 1;

		public static final int	IN_STRING				= 2;

		public static final int	IN_COMMENT				= 3;

		public static final int	IN_FUNCTION				= 4;

		/**
		 * The buffer will be recycled if the jail index is above the ratio of
		 * the buffer length. So if the ratio is 1 (or greater) that means never
		 * being recycled, 0 (or less) means always being recycled.
		 */
		public static double	DEFAULT_RECYCLE_RATIO	= 0.9;

		/**
		 * This is the default buffer size which decides how many characters
		 * will be read at most while parsing via a Reader.
		 */
		public static int		DEFAULT_READER_BUFFER	= 1000;

		private StringBuilder	buffer;

		private JSON			object;

		private String			entry;

		private Object			value;

		private int				arrayIndex;

		private int				status;

		private int				curr					= 0;

		private int				nail					= NOT_FOUND;

		private int				tail					= NOT_FOUND;

		private int				jail					= NOT_FOUND;

		private Parser			sub						= null;

		protected Parser commit(StringBuilder buffer)
		{
			if (value == NOT_A_VALUE && nail > NOT_FOUND)
			{
				value = ParseValueOf(buffer.subSequence(nail, tail + 1).toString());
			}

			if (value != NOT_A_VALUE)
			{
				if (entry == null && arrayIndex > NOT_FOUND)
				{
					entry = JSAN.Index(arrayIndex);
					arrayIndex++;
				}
				object.put(entry, value);
				value = NOT_A_VALUE;
				entry = null;
			}

			return this;
		}

		public Parser dispose()
		{
			if (sub != null)
			{
				sub.dispose();
				sub = null;
			}

			if (buffer != null)
			{
				Tools.clearStringBuilder(buffer);
				buffer = null;
			}

			entry = null;
			value = null;

			status(DISPOSED);

			return this;
		}

		protected Parser entry(StringBuilder buffer)
		{
			entry = RestoreString(buffer.subSequence(nail, tail + 1).toString());
			return this;
		}

		protected Parser follow()
		{
			if (sub != null)
			{
				curr = sub.curr;
				nail = sub.nail;
				tail = sub.tail;
				jail = sub.jail;
			}

			return this;
		}

		protected boolean isRoot()
		{
			return buffer != null;
		}

		protected int next(StringBuilder buffer)
		{
			curr++;

			int temp = (tail = (nail = FirstNonWhitespaceIndex(buffer, curr)));

			if (temp > NOT_FOUND)
			{
				curr = temp - 1;
			}

			return temp;
		}

		public Parser parse(CharSequence source)
		{
			if (buffer != source)
			{
				if (buffer == null)
				{
					if (source instanceof StringBuilder)
					{
						buffer = (StringBuilder) source;
					}
					else
					{
						buffer = new StringBuilder(source);
					}
				}
				else
				{
					this.recycle();
					buffer.append(source);
				}
			}

			object = null;

			this.parse(buffer, curr);

			return this;
		}

		public Parser parse(Reader reader, boolean closeAfterRead) throws IOException
		{
			return parse(reader, DEFAULT_READER_BUFFER, closeAfterRead);
		}

		public Parser parse(Reader reader, int buffers, boolean closeAfterRead) throws IOException
		{
			if (reader != null)
			{
				char[] chars = new char[Math.max(buffers, 1)];

				if (buffer == null)
				{
					buffer = new StringBuilder(chars.length);
				}
				else
				{
					this.recycle();
				}

				object = null;

				int length = -1;

				try
				{
					while ((length = reader.read(chars)) != -1 //
							|| (buffer.length() > 0 && curr < buffer.length()))
					{
						if (length > -1)
						{
							buffer.append(chars, 0, length);
						}

						parse(buffer);

						if (result() != null)
						{
							break;
						}
					}
				}
				finally
				{
					if (closeAfterRead)
					{
						reader.close();
					}
				}
			}

			return this;
		}

		protected int parse(StringBuilder buffer, int from)
		{
			curr = from;

			if (sub != null && sub.status() != DONE)
			{
				curr = sub.parse(buffer, curr);

				if (sub.status() == DONE)
				{
					this.pick().follow();
				}
			}

			if (curr < buffer.length() && (sub == null || sub.status() == DONE))
			{
				int temp = NOT_FOUND;

				char c;

				try
				{
					i: for (; curr < buffer.length(); curr++)
					{
						c = buffer.charAt(curr);

						j: switch (c)
						{
							case OBJECT_BEGIN_CHAR:
							case ARRAY_BEGIN_CHAR:
								if (sub == null)
								{
									sub = new Parser();
								}
								curr = sub.reset(c == OBJECT_BEGIN_CHAR ? new JSON() : new JSAN()) //
										.parse(buffer, curr + 1);
								if (sub.status() == DONE)
								{
									this.pick().follow();
									if (this.isRoot())
									{
										break i;
									}
									else
									{
										curr--;
									}
								}
								else
								{
									break i;
								}
								break j;

							case OBJECT_END_CHAR:
							case ARRAY_END_CHAR:
								commit(buffer).status(DONE);
								jail = curr;
								curr++;
								break i;

							case PAIR_CHAR:
								commit(buffer);
								if (next(buffer) <= NOT_FOUND)
								{
									break i;
								}
								break j;

							case ATTR_CHAR:
								entry(buffer);
								if (next(buffer) <= NOT_FOUND)
								{
									break i;
								}
								break j;

							case QUOTE_CHAR:
								nail = curr;
								temp = DualMatchIndex(buffer, QUOTE_CHAR, QUOTE_CHAR, curr);
								if (temp > NOT_FOUND)
								{
									status(IN_NONE);
									tail = (curr = temp);
								}
								else
								{
									status(IN_STRING);
									tail = buffer.length() - 1;
									break i;
								}
								break j;

							case COMMENT_CHAR:
								temp = EndOfComment(buffer, curr);
								if (temp > NOT_FOUND)
								{
									status(IN_NONE);
									if (nail == curr)
									{
										nail = temp + 1;
									}
									if (tail == curr)
									{
										tail = temp + 1;
									}
									curr = temp;
								}
								else
								{
									status(IN_COMMENT);
									break i;
								}
								break j;

							case Function.DEFINE_FIRST_CHAR:
								if (nail == curr && Function.DEFINE_MARK.equals( //
										buffer.subSequence(curr,
												Math.min(buffer.length(), curr + Function.DEFINE_MARK.length()) //
										).toString()))
								{
									temp = (tail = DualMatchIndex(buffer, OBJECT_BEGIN_CHAR, OBJECT_END_CHAR, curr));
									if (temp > NOT_FOUND)
									{
										status(IN_NONE);
										curr = temp;
									}
									else
									{
										status(IN_FUNCTION);
										break i;
									}
								}

							default:
								if (!Character.isWhitespace(c) && !Character.isSpaceChar(c))
								{
									if (nail <= NOT_FOUND)
									{
										nail = curr;
									}
									tail = curr;
								}
								break j;
						}
					}
				}
				catch (SyntaxErrorException e)
				{
					throw e;
				}
				catch (RuntimeException e)
				{
					throw new SyntaxErrorException(e, buffer, curr);
				}
			}

			return curr;
		}

		protected Parser pick()
		{
			if (sub != null)
			{
				value = sub.result();

				if (buffer != null)
				{
					object = sub.result();
				}
			}

			return this;
		}

		protected Parser recycle()
		{
			int pos = jail + 1;

			if (pos > 0 && pos > buffer.length() * DEFAULT_RECYCLE_RATIO)
			{
				buffer.delete(0, pos);
				curr -= pos;
				nail -= pos;
				tail -= pos;
				jail -= pos;
			}

			return this;
		}

		protected Parser reset(JSON object)
		{
			this.object = object;

			nail = NOT_FOUND;
			tail = NOT_FOUND;
			jail = NOT_FOUND;

			entry = null;
			value = NOT_A_VALUE;

			arrayIndex = IsJSAN(object) ? 0 : NOT_FOUND;

			status(IN_NONE);

			return this;
		}

		public JSON result()
		{
			return object;
		}

		protected int status()
		{
			return status;
		}

		protected Parser status(int status)
		{
			this.status = status;
			return this;
		}
	}

	public static class Path
	{
		protected static class ChildFinder implements Finder
		{
			protected final Extractor xtr;

			protected ChildFinder(Extractor xtr)
			{
				this.xtr = xtr;
			}

			@Override
			public Iterable<Object> find(final JSON data) throws Exception
			{
				Iterable<Object> keys = xtr.extract(data);

				if (keys == ALL_KEYS)
				{
					return data.values();
				}
				else
				{
					return Canal.of(keys).flatMap(new Mapper<Object, Iterable<Object>>()
					{
						@Override
						public Iterable<Object> map(Object key) throws Exception
						{
							return access(data, key);
						}
					});
				}
			}
		}

		protected static class DotFinder implements Finder
		{
			protected final Extractor xtr;

			protected DotFinder(Extractor xtr)
			{
				this.xtr = xtr;
			}

			@Override
			public Iterable<Object> find(final JSON data) throws Exception
			{
				Iterable<Object> keys = xtr.extract(data);

				if (keys == ALL_KEYS)
				{
					return data.values();
				}
				else
				{
					return Canal.of(keys).first().flatMap(new Mapper<Object, Iterable<Object>>()
					{
						@Override
						public Iterable<Object> map(Object key) throws Exception
						{
							return access(data, key);
						}
					});
				}
			}
		}

		protected static class EnumExtractor implements Extractor
		{
			protected final Object[] keys;

			protected EnumExtractor(Object[] keys)
			{
				this.keys = keys;
			}

			@Override
			public Iterable<Object> extract(JSON data) throws Exception
			{
				return Canal.of(keys);
			}
		}

		protected static interface Extractor
		{
			public Iterable<Object> extract(final JSON data) throws Exception;
		}

		protected static class FilterExtractor extends ScriptExtractor
		{
			protected FilterExtractor(String expr)
			{
				super(expr);
			}

			@Override
			public Iterable<Object> extract(JSON data) throws Exception
			{
				return Canal.of(data.asMap()).filter(new Filter<Tuple2<String, Object>>()
				{
					ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");

					@Override
					public boolean filter(Tuple2<String, Object> el) throws Exception
					{
						Object res = engine.eval(script(el._2));
						if (res == null || Boolean.FALSE.equals(res) || Tools.equals("", res)
								|| ((res instanceof Number) && ((Number) res).doubleValue() == 0.0))
						{
							return false;
						}
						else
						{
							return true;
						}
					}
				}).map(new Mapper<Tuple2<String, Object>, Object>()
				{
					@Override
					public Object map(Tuple2<String, Object> el) throws Exception
					{
						return el._1;
					}
				});
			}
		}

		protected static interface Finder
		{
			public Iterable<Object> find(final JSON data) throws Exception;
		}

		public static class IllegalPathException extends IllegalArgumentException
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public IllegalPathException(String msg)
			{
				super(msg);
			}

			public IllegalPathException(String msg, Throwable cause)
			{
				super(msg, cause);
			}
		}

		protected static class NameExtractor implements Extractor
		{
			protected final Iterable<Object> name;

			protected NameExtractor(String name)
			{
				this.name = WILDCARD.equals(name) ? ALL_KEYS : Option.<Object> some(name);
			}

			@Override
			public Iterable<Object> extract(JSON data) throws Exception
			{
				return name;
			}
		}

		protected static class RangeExtractor implements Extractor
		{
			protected final int	begin;

			protected final int	until;

			protected final int	step;

			protected RangeExtractor(Integer begin, Integer until, Integer step)
			{
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

				this.begin = a;
				this.until = b;
				this.step = c;
			}

			@Override
			public Iterable<Object> extract(JSON data) throws Exception
			{
				int len = data.size();
				int begin = this.begin, until = this.until;

				if (begin < 0)
				{
					begin += len;
				}
				if (until < 0)
				{
					until += len;
				}

				begin = Math.max(Math.min(begin, Math.max(len - 1, 0)), 0);
				until = Math.max(Math.min(Math.max(until, -1), len), -len);

				if (begin == until)
				{
					return Canal.none();
				}
				else if (step == 0)
				{
					return Canal.<Object> some(begin);
				}

				return Canal.of(Canal.range(begin, until, step)).map(new Mapper<Integer, Object>()
				{
					@Override
					public Object map(Integer el) throws Exception
					{
						return el;
					}
				});
			}
		}

		protected static class RecursiveFinder implements Finder
		{
			protected final Extractor xtr;

			protected RecursiveFinder(Extractor xtr)
			{
				this.xtr = xtr;
			}

			@Override
			public Iterable<Object> find(final JSON data) throws Exception
			{
				return recurs(data, xtr.extract(data));
			}

			protected Iterable<Object> recurs(final JSON data, final Iterable<Object> keys)
			{
				Iterable<Object> res = null;

				if (keys == ALL_KEYS)
				{
					res = data.values();
				}
				else
				{
					res = Canal.of(keys).flatMap(new Mapper<Object, Iterable<Object>>()
					{
						@Override
						public Iterable<Object> map(Object key) throws Exception
						{
							return access(data, key);
						}
					});
				}

				return Canal.of(res).union(Canal.of(data.values()).flatMap(new Mapper<Object, Iterable<Object>>()
				{
					@Override
					public Iterable<Object> map(Object obj) throws Exception
					{
						if (!JSON.IsJSON(obj))
						{
							return Option.none();
						}
						return recurs((JSON) obj, keys);
					}
				}));
			}
		}

		protected static class ScriptExtractor implements Extractor
		{
			protected static final String	SELF	= "@";

			protected static final String	REAL	= "_$_";

			protected final String			expr;

			protected final boolean			self;

			protected ScriptExtractor(String expr)
			{
				this.expr = expr.replace(SELF, REAL);
				this.self = expr.length() != this.expr.length();
			}

			protected Object eval(JSON data) throws ScriptException
			{
				return new ScriptEngineManager().getEngineByName("javascript").eval(script(data));
			}

			@Override
			public Iterable<Object> extract(JSON data) throws Exception
			{
				Object res = eval(data);
				if (res instanceof Number)
				{
					res = ((Number) res).intValue();
				}
				return Option.some(res);
			}

			protected String script(Object data)
			{
				return (self ? "var " + REAL + "=" + (data != null ? data.toString() : "null") + ";\n" : "") + expr;
			}
		}

		protected static class WildcardExtractor implements Extractor
		{
			@Override
			public Iterable<Object> extract(JSON data) throws Exception
			{
				return ALL_KEYS;
			}
		}

		public static final char				CHILD_CHAR	= '.';

		public static final String				WILDCARD	= "*";

		protected static final Iterable<Object>	ALL_KEYS	= Option.some(null);

		protected static final Set<Character>	NAME_BOUND	= new HashSet<Character>();

		static
		{
			NAME_BOUND.add('.');
			NAME_BOUND.add('[');
		}

		protected static Option<Object> access(final JSON data, final Object key)
		{
			return data.containsKey(key) ? Option.some(data.get(key)) : Option.none();
		}

		protected static int findNext(String text, int from, Set<Character> chars)
		{
			for (int i = from; i < text.length(); i++)
			{
				if (chars.contains(text.charAt(i)))
				{
					return i;
				}
			}
			return -1;
		}

		public static void main(String[] args)
		{
			JSON json = JSON.Parse("{\"a\":1, \"b\":true, \"c\":[1,2,3,\"xyz\"], \"d\":{\"a\":true}}");
			try
			{
				Path path = Path.parse("$.d.b");

				Tools.debug(path.find(json));

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		public static Path parse(String text)
		{
			return new Path(parseFinders(text));
		}

		protected static Extractor parseExtractor(String text, int begin, int until)
		{
			String expr = text.substring(begin + 1, until).trim();

			if (expr.isEmpty())
			{
				throw new IllegalPathException(
						"Missing expression in [] between " + begin + "th to " + until + "th char of " + text);
			}

			if (expr.startsWith("("))
			{ // script
				if (!expr.endsWith(")"))
				{
					throw new IllegalPathException("Missing ')' before " + until + "th char of " + text);
				}
				return new ScriptExtractor(expr.substring(1, expr.length() - 1));
			}
			else if (expr.startsWith("?("))
			{ // filter
				if (!expr.endsWith(")"))
				{
					throw new IllegalPathException("Missing ')' before " + until + "th char of " + text);
				}
				return new FilterExtractor(expr.substring(2, expr.length() - 1));
			}
			else if (expr.indexOf(':') >= 0)
			{ // range
				String[] pair = expr.split(":", -1);
				Integer a = parseInteger(pair[0].trim());
				Integer b = parseInteger(pair[1].trim());
				Integer c = pair.length > 2 ? parseInteger(pair[2].trim()) : null;
				return new RangeExtractor(a, b, c);
			}
			else if (expr.indexOf(',') >= 0)
			{ // enum
				return new EnumExtractor(Canal.of(expr.split(",", -1)).map(new Mapper<String, String>()
				{
					@Override
					public String map(String t) throws Exception
					{
						return restoreText(t.trim());
					}
				}).collect().toArray());
			}
			else
			{ // name
				return new NameExtractor(restoreText(expr.trim()));
			}
		}

		protected static List<Finder> parseFinders(String text)
		{
			String path = text.trim();
			if (!path.startsWith("$"))
			{
				throw new IllegalPathException(text + " must start with $");
			}

			List<Finder> res = new LinkedList<Finder>();

			char c = '\0', n = '\0';
			int j = 0, len = path.length();
			for (int i = 1; i < len; i++)
			{
				c = path.charAt(i);

				if (CHILD_CHAR == c)
				{
					i++;
					if (i < path.length())
					{
						n = path.charAt(i);

						if (CHILD_CHAR == n)
						{ // Recursive
							i++;
							j = findNext(path, i, NAME_BOUND);
							j = j >= 0 ? j : len;
							if (j == i)
							{
								throw new IllegalPathException("Missing name after " + (i - 1) + "th char of " + text);
							}
							res.add(new RecursiveFinder(new NameExtractor(path.substring(i, j))));
							i = j - 1;
						}
						else if (JSON.ARRAY_BEGIN_CHAR == n)
						{ // Recursive
							j = JSON.DualMatchIndex(path, JSON.ARRAY_BEGIN_CHAR, JSON.ARRAY_END_CHAR, i);
							if (j < 0)
							{
								throw new IllegalPathException("Missing a ']' after " + i + "th char of " + text);
							}
							res.add(new RecursiveFinder(parseExtractor(path, i, j)));
							i = j;
						}
						else
						{ // Dot
							j = findNext(path, i, NAME_BOUND);
							j = j >= 0 ? j : len;
							if (j == i)
							{
								throw new IllegalPathException("Missing name after " + i + "th char of " + text);
							}
							res.add(new DotFinder(new NameExtractor(path.substring(i, j))));
							i = j - 1;
						}
					}
				}
				else if (JSON.ARRAY_BEGIN_CHAR == c)
				{
					j = JSON.DualMatchIndex(path, JSON.ARRAY_BEGIN_CHAR, JSON.ARRAY_END_CHAR, i);
					if (j < 0)
					{
						throw new IllegalPathException("Missing a ']' after " + i + "th char of " + text);
					}
					res.add(new ChildFinder(parseExtractor(path, i, j)));
					i = j;
				}
			}

			return res;
		}

		protected static Integer parseInteger(String text)
		{
			if (text.isEmpty())
			{
				return null;
			}
			else
			{
				return Integer.valueOf(text);
			}
		}

		protected static String restoreText(String text)
		{
			if (text.length() > 1 //
					&& ((text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"') //
							|| (text.charAt(0) == '\'' && text.charAt(text.length() - 1) == '\'')))
			{
				return JSON.RestoreStringContent(text.substring(1, text.length() - 1));
			}
			else
			{
				return text;
			}
		}

		protected final List<Finder> finders;

		protected Path(List<Finder> finders)
		{
			this.finders = finders;
		}

		public Iterable<Object> find(final Object data)
		{
			Iterable<Object> last = Option.some(data);

			for (final Finder finder : finders)
			{
				last = Canal.of(last).filter(new Filter<Object>()
				{
					@Override
					public boolean filter(Object el) throws Exception
					{
						return JSON.IsJSON(el);
					}
				}).flatMap(new Mapper<Object, Iterable<Object>>()
				{
					@Override
					public Iterable<Object> map(Object el) throws Exception
					{
						return finder.find((JSON) el);
					}
				});
			}

			return last;
		}
	}

	public static interface Projector<T> extends Serializable
	{
		/**
		 * To generate a new instance according to given target class and source
		 * json. null can be returned which means using the default newInstance
		 * policy.
		 * 
		 * @param cls
		 * @param json
		 * @return
		 * @throws Exception
		 */
		public T instance(Class<T> cls, JSON json) throws Exception;

		/**
		 * Project the source json to the target object.
		 * 
		 * @param obj
		 * @param json
		 * @return
		 * @throws Exception
		 */
		public T project(T obj, JSON json) throws Exception;
	}

	public static class Quotation implements Hierarchical
	{
		public static final char	LINEAR_ATTRIBUTE		= '.';

		public static final char	NESTED_ATTRIBUTE_BEGIN	= '[';

		public static final char	NESTED_ATTRIBUTE_END	= ']';

		public static final char	NESTED_ATTRIBUTE_QUOTE	= '"';

		public static Object Quote(Context context, String quote, Object object)
		{
			JSON outer = null;
			String entry = null;

			StringBuilder buffer = new StringBuilder(quote.length());

			FilterComments(buffer, quote, 0);

			quote = buffer.toString();

			int quoteLength = quote.length() - 1;
			if (quote.charAt(quoteLength) == NESTED_ATTRIBUTE_END)
			{
				int begin = JSON.LastDualMatchIndex(quote, NESTED_ATTRIBUTE_BEGIN, NESTED_ATTRIBUTE_END, quoteLength);
				outer = JSON.AsJSON(Quote(context, quote.substring(0, begin)));
				if (outer != null)
				{
					entry = Quote(context, quote.substring(begin + 1, quoteLength)).toString();
				}
			}
			else
			{
				int begin = quote.lastIndexOf(LINEAR_ATTRIBUTE);
				if (begin == -1)
				{
					outer = context;
					entry = quote;
				}
				else
				{
					outer = JSON.AsJSON(Quote(context, quote.substring(0, begin)));
					if (outer != null)
					{
						entry = quote.substring(begin + 1);
					}
				}
			}

			if (outer != null && entry != null)
			{
				Object temp = object;
				object = outer.got(entry);
				outer.put(entry, temp);
			}

			return object;
		}

		public static String Quote(JSON json)
		{
			StringBuilder buffer = new StringBuilder();

			JSON outer = json.outer();
			String entry = null;

			while (outer != null)
			{
				entry = json.entry();

				if (outer.outer() != null)
				{
					buffer.insert(0, NESTED_ATTRIBUTE_END);
					if (!JSON.IsJSAN(outer))
					{
						buffer.insert(0, NESTED_ATTRIBUTE_QUOTE);
					}
				}
				buffer.insert(0, entry);
				if (outer.outer() != null)
				{
					if (!JSON.IsJSAN(outer))
					{
						buffer.insert(0, NESTED_ATTRIBUTE_QUOTE);
					}
					buffer.insert(0, NESTED_ATTRIBUTE_BEGIN);
				}

				json = outer;
				outer = json.outer();
			}

			return buffer.toString();
		}

		public static Object Quote(JSON context, String quote)
		{
			if (context == null)
			{
				return null;
			}

			Object object = context;

			Map<String, Object> map = null;
			int nail = 0;

			char c = 0;
			int i = 0;
			String entry;

			i: for (i = 0; i < quote.length(); i++)
			{
				c = quote.charAt(i);

				if (c == LINEAR_ATTRIBUTE)
				{
					if (nail != i)
					{
						map = JSON.AsMap(object);
						if (map == null)
						{
							return null;
						}
						else
						{
							entry = quote.substring(nail, i).trim();
							object = map.get(entry);
							nail = i + 1;
						}
					}
					else
					{
						nail = i + 1;
					}
				}

				if (c == NESTED_ATTRIBUTE_BEGIN)
				{
					if (nail != i)
					{
						map = JSON.AsMap(object);
						if (map == null)
						{
							return null;
						}
						else
						{
							entry = quote.substring(nail, i).trim();
							object = map.get(entry);
							nail = i + 1;
						}
					}
					else
					{
						nail = i + 1;
					}
					i = CheckNext(quote, i, JSON.DualMatchIndex(quote, NESTED_ATTRIBUTE_BEGIN, NESTED_ATTRIBUTE_END, i))
							- 1;
					continue i;
				}

				if (c == NESTED_ATTRIBUTE_QUOTE)
				{
					nail = i + 1;
					do
					{
						i = Tools.seekIndex(quote, NESTED_ATTRIBUTE_QUOTE, i + 1);
					}
					while (quote.charAt(i - 1) == JSON.ESCAPE_CHAR);
					break;
				}

				if (c == NESTED_ATTRIBUTE_END)
				{
					map = JSON.AsMap(object);
					if (map == null)
					{
						return null;
					}
					else
					{
						entry = Quote(context, quote.substring(nail, i)).toString();
						object = map.get(entry);
						nail = i + 1;
					}
				}
			}

			if (c == NESTED_ATTRIBUTE_QUOTE || Variable.isIntegerNumber(quote))
			{
				object = quote.substring(nail, i);
			}
			else if (c != NESTED_ATTRIBUTE_END)
			{
				map = JSON.AsMap(object);
				if (map == null)
				{
					return null;
				}
				else
				{
					entry = quote.substring(nail, i).trim();
					object = map.get(entry);
				}
			}

			return object;
		}

		private JSON	outer;

		private String	entry;

		private String	quote;

		protected Quotation(Quotation q)
		{
			quote(q.quote).outer(q.outer()).entry(q.entry());
		}

		public Quotation(String quote)
		{
			quote(quote);
		}

		@Override
		public Quotation clone()
		{
			return new Quotation(this).outer(null).entry(null);
		}

		public Context context()
		{
			return outer == null ? null : outer.context();
		}

		public String entry()
		{
			return entry;
		}

		public Quotation entry(String entry)
		{
			this.entry = entry;
			return this;
		}

		public JSON outer()
		{
			return outer;
		}

		public Quotation outer(JSON outer)
		{
			this.outer = outer;
			return this;
		}

		public Object quote()
		{
			return Quote(context(), quote);
		}

		protected Quotation quote(String quote)
		{
			this.quote = quote.trim();
			return this;
		}

		@Override
		public String toString()
		{
			return quote;
		}
	}

	public static interface Reflector<T> extends Serializable
	{
		public JSON reflect(JSON json, T obj);
	}

	public static class SyntaxErrorException extends RuntimeException
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= 5584855948726666241L;

		public static int			DEFAULT_VIEW		= 15;

		public static String FormatMessage(String hint, CharSequence msg, int index)
		{
			hint = hint == null ? "" : hint.trim();
			return hint + (hint.length() == 0 ? "" : " ") + "@" + index + "\n" //
					+ msg;
		}

		public static CharSequence LocateMessage(CharSequence source, int index, int view)
		{
			return source.subSequence(Math.max(index - view, 0), Math.min(index + view, source.length()));
		}

		public SyntaxErrorException(CharSequence source, int index)
		{
			this("", source, index);
		}

		public SyntaxErrorException(String msg)
		{
			super(msg);
		}

		public SyntaxErrorException(String hint, CharSequence source, int index)
		{
			super(FormatMessage(hint, LocateMessage(source, index, DEFAULT_VIEW), index));
		}

		public SyntaxErrorException(String hint, Throwable cause, CharSequence source, int index)
		{
			super(cause instanceof SyntaxErrorException //
					? cause.getMessage() //
					: FormatMessage(hint, LocateMessage(source, index, DEFAULT_VIEW), index), //
					cause);
		}

		public SyntaxErrorException(Throwable cause, CharSequence source, int index)
		{
			this(cause.getMessage(), cause, source, index);
		}
	}

	public static interface Transform<T> extends Serializable
	{
		/**
		 * To transform the value according to the given JSON object and the
		 * entry key.
		 * 
		 * @param json
		 *            The JSON which store the value.
		 * @param entry
		 *            The entry key which is associated with the value.
		 * @param value
		 *            The given value to be transformed.
		 * @return The transform result.
		 */
		public T transform(JSON json, String entry, Object value);
	}

	public class ValueIterator<T> implements Iterator<T>, Iterable<T>, Serializable
	{
		/**
		 * 
		 */
		private static final long		serialVersionUID	= 7218728241929428448L;

		private Class<T>				cls;

		private Transform<T>			tran;

		private LinkedList<String>		keys				= new LinkedList<String>();

		private ListIterator<String>	iter;

		private String					key;

		protected ValueIterator()
		{
			this.keys.addAll(keySet());
			this.iter = this.keys.listIterator();
		}

		protected ValueIterator(Class<T> cls)
		{
			this();
			this.cls = cls;
		}

		protected ValueIterator(Transform<T> tran)
		{
			this();
			this.tran = tran;
		}

		@SuppressWarnings("unchecked")
		protected T get()
		{
			Object o = JSON.this.attr(key);

			if (tran != null)
			{
				return tran.transform(JSON.this, key, o);
			}
			else if (cls == null || cls.isInstance(o))
			{
				return (T) o;
			}
			else
			{
				return null;
			}
		}

		public boolean hasNext()
		{
			return iter.hasNext();
		}

		public boolean hasPrevious()
		{
			return iter.hasPrevious();
		}

		protected int index()
		{
			int index = JSAN.LAST;

			try
			{
				index = Integer.parseInt(key);

				if (index < 0)
				{
					index = JSAN.LAST;
				}
			}
			catch (NumberFormatException e)
			{
			}

			return index;
		}

		protected ListIterator<String> iter()
		{
			return iter;
		}

		public Iterator<T> iterator()
		{
			return this;
		}

		protected String key()
		{
			return key;
		}

		protected LinkedList<String> keys()
		{
			return keys;
		}

		public T next()
		{
			key = iter.next();
			return get();
		}

		public T previous()
		{
			key = iter.previous();
			return get();
		}

		public void remove()
		{
			if (key != null)
			{
				JSON.this.remove(key);
				iter.remove();
				key = null;
			}
		}

		public ValueIterator<T> reset(int sequence)
		{
			iter = keys.listIterator(sequence);
			key = null;
			return this;
		}

		public ValueIterator<T> reset(String key)
		{
			int seq = 0;

			try
			{
				int index = Integer.parseInt(key);

				if (index < 0)
				{
					throw new NumberFormatException();
				}

				for (String k : keys)
				{
					try
					{
						if (index <= Integer.parseInt(k))
						{
							break;
						}
					}
					catch (NumberFormatException e)
					{
						break;
					}
					seq++;
				}
			}
			catch (NumberFormatException e)
			{
				for (String k : keys)
				{
					if (Tools.equals(key, k))
					{
						break;
					}
					seq++;
				}
			}

			return reset(seq);
		}
	}

	public static class ValuesCollection extends AbstractCollection<Object>
	{
		protected class ValuesIterator extends AbstractIterator<Object>
		{
			public ValuesIterator(Iterator<Object>... iters)
			{
				super(iters);
			}

			@Override
			public Object next()
			{
				return Quote(iter.next());
			}
		}

		protected Collection<Object>[] vals;

		public ValuesCollection(Collection<Object>... vals)
		{
			this.vals = vals;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Iterator<Object> iterator()
		{
			Iterator<Object>[] iters = null;

			if (vals != null)
			{
				iters = (Iterator<Object>[]) new Iterator<?>[vals.length];

				for (int i = 0; i < vals.length; i++)
				{
					if (vals[i] != null)
					{
						iters[i] = vals[i].iterator();
					}
				}
			}

			return new ValuesIterator(iters);
		}

		@Override
		public int size()
		{
			int size = 0;
			if (vals != null)
			{
				for (Collection<Object> val : vals)
				{
					if (val != null)
					{
						size += val.size();
					}
				}
			}
			return size;
		}
	}

	/**
	 * 
	 */
	private static final long					serialVersionUID			= 6090747632739206720L;

	public static final char					OBJECT_BEGIN_CHAR			= '{';
	public static final String					OBJECT_BEGIN_MARK			= String.valueOf(OBJECT_BEGIN_CHAR);

	public static final char					OBJECT_END_CHAR				= '}';
	public static final String					OBJECT_END_MARK				= String.valueOf(OBJECT_END_CHAR);

	public static final char					ARRAY_BEGIN_CHAR			= '[';
	public static final String					ARRAY_BEGIN_MARK			= String.valueOf(ARRAY_BEGIN_CHAR);

	public static final char					ARRAY_END_CHAR				= ']';
	public static final String					ARRAY_END_MARK				= String.valueOf(ARRAY_END_CHAR);

	public static final char					PAIR_CHAR					= ',';
	public static final String					PAIR_MARK					= String.valueOf(PAIR_CHAR);

	public static final char					ATTR_CHAR					= ':';
	public static final String					ATTR_MARK					= String.valueOf(ATTR_CHAR);

	public static final char					QUOTE_CHAR					= '"';
	public static final String					QUOTE_MARK					= String.valueOf(QUOTE_CHAR);

	public static final char					ESCAPE_CHAR					= '\\';
	public static final String					ESCAPE_MARK					= String.valueOf(ESCAPE_CHAR);

	public static final char					COMMENT_CHAR				= '/';
	public static final char					LINE_COMMENT_CHAR			= COMMENT_CHAR;

	public static final char					BLOCK_COMMENT_CHAR			= '*';

	public static final String					BLOCK_COMMENT_END			= BLOCK_COMMENT_CHAR + "" + COMMENT_CHAR;

	public static final int						NOT_FOUND					= -1;

	public static final Object					NOT_A_VALUE					= new String("NOT A VALUE");

	public static final String					NULL_STRING					= "null";

	public static final String					TRUE_STRING					= "true";

	public static final String					FALSE_STRING				= "false";

	public static final char					UNICODE_ESCAPING_CHAR		= 'u';

	public static final String					UNICODE_ESCAPING_MARK		= "\\u";

	public static final int						UNICODE_ESCAPED_LENGTH		= 4;

	public static final int						UNICODE_ESCAPE_RADIX		= 16;

	protected static Map<Character, Character>	ESCAPING_CHAR				= new HashMap<Character, Character>();

	protected static Map<Character, String>		ESCAPED_CHAR				= new HashMap<Character, String>();

	public static final String					DEFAULT_DATETIME_FORMAT		= "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	public static final String					DEFAULT_DATETIME_REGEX		= "^\\d{4}-\\d{2}-\\d{2}((T| )\\d{2}:\\d{2}(?::(\\d{2})(?:\\.(\\d{3}|\\d{6}||\\d{9}))?)?)?(Z|[+-]\\d{2}(?::?\\d{2})?)?$";

	public static final Pattern					DEFAULT_DATETIME_PATTERN	= Pattern.compile(DEFAULT_DATETIME_REGEX);

	protected static final DateFormat			DEFAULT_DATE_FORMAT			= Tools
			.getDateFormat(DEFAULT_DATETIME_FORMAT);

	public static String						DEFAULT_LINE_INDENT			= "\t";

	public static String						LINE_WRAP					= "\n";

	static
	{
		ESCAPING_CHAR.put('"', '"');
		ESCAPING_CHAR.put('\\', '\\');
		ESCAPING_CHAR.put('/', '/');
		ESCAPING_CHAR.put('b', '\b');
		ESCAPING_CHAR.put('f', '\f');
		ESCAPING_CHAR.put('n', '\n');
		ESCAPING_CHAR.put('r', '\r');
		ESCAPING_CHAR.put('t', '\t');
		ESCAPING_CHAR = Collections.unmodifiableMap(ESCAPING_CHAR);

		ESCAPED_CHAR.put('"', "\\\"");
		ESCAPED_CHAR.put('\\', "\\\\");
		ESCAPED_CHAR.put('/', "\\/");
		ESCAPED_CHAR.put('\b', "\\b");
		ESCAPED_CHAR.put('\f', "\\f");
		ESCAPED_CHAR.put('\n', "\\n");
		ESCAPED_CHAR.put('\r', "\\r");
		ESCAPED_CHAR.put('\t', "\\t");
		ESCAPED_CHAR = Collections.unmodifiableMap(ESCAPED_CHAR);
	}

	protected static int Access(JSAN jsan, int idx, Object object, String name, Field field) throws Exception
	{
		if (object == null || (name == null && field == null))
		{
			return idx;
		}

		if (name == null)
		{
			name = field.getName();
		}

		if (JSON.IsJSON(object))
		{
			jsan.attr(idx, ((JSON) object).attr(name));
			return idx + 1;
		}
		else if (object instanceof Map)
		{
			jsan.attr(idx, ((Map<?, ?>) object).get(name));
			return idx + 1;
		}
		else
		{
			Member acs = Tools.accessor(object.getClass(), name, field);
			if (acs != null)
			{
				jsan.attr(idx, Tools.access(object, acs));
				return idx + 1;
			}
			return idx;
		}
	}

	protected static void Access(JSON json, String key, Object object, String name, Field field) throws Exception
	{
		if (object == null || (name == null && field == null))
		{
			return;
		}

		if (name == null)
		{
			name = field.getName();
		}

		if (JSON.IsJSON(object))
		{
			json.attr(key, ((JSON) object).attr(name));
		}
		else if (object instanceof Map)
		{
			json.attr(key, ((Map<?, ?>) object).get(name));
		}
		else
		{
			Member acs = Tools.accessor(object.getClass(), name, field);
			if (acs != null)
			{
				json.attr(key, Tools.access(object, acs));
			}
		}
	}

	public static <T> Object Access(T object, String name, Field field) throws Exception
	{
		if (object == null || (name == null && field == null))
		{
			return null;
		}

		if (name == null)
		{
			name = field.getName();
		}

		if (JSON.IsJSON(object))
		{
			return ((JSON) object).attr(name);
		}
		else if (object instanceof Map)
		{
			return ((Map<?, ?>) object).get(name);
		}
		else
		{
			return Tools.access(object, name, field);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> void Access(T object, String name, Field field, Object value) throws Exception
	{
		if (object == null || (name == null && field == null))
		{
			return;
		}

		if (name == null)
		{
			name = field.getName();
		}

		if (JSON.IsJSON(object))
		{
			((JSON) object).attr(name, value);
		}
		else if (object instanceof Map)
		{
			((Map<String, Object>) object).put(name, value);
		}
		else
		{
			Tools.access(object, name, field, value);
		}
	}

	public static final Context AsContext(Object o)
	{
		return Tools.as(o, Context.class);
	}

	public static final Function AsFunction(Object o)
	{
		return Tools.as(o, Function.class);
	}

	public static final Hierarchical AsHierarchical(Object o)
	{
		return Tools.as(o, Hierarchical.class);
	}

	@SuppressWarnings("unchecked")
	public static final Iterable<Object> AsIterable(Object o)
	{
		Iterable<Object> iter = null;
		if (o instanceof Iterable<?>)
		{
			iter = (Iterable<Object>) o;
		}
		return iter;
	}

	public static final JSAN AsJSAN(Object o)
	{
		return Tools.as(o, JSAN.class);
	}

	public static final JSON AsJSON(Object o)
	{
		return Tools.as(o, JSON.class);
	}

	@SuppressWarnings("unchecked")
	public static final Map<String, Object> AsMap(Object o)
	{
		Map<String, Object> map = null;
		if (o instanceof Map<?, ?>)
		{
			map = (Map<String, Object>) o;
		}
		return map;
	}

	public static final Quotation AsQuotation(Object o)
	{
		return Tools.as(o, Quotation.class);
	}

	public static <T> T CastAs(Object obj, Class<T> cls)
	{
		if (obj != null && cls != null)
		{
			try
			{
				return cls.cast(obj);
			}
			catch (ClassCastException e)
			{
			}
		}
		return null;
	}

	@SuppressWarnings("unused")
	public static <T> Object CastToArray(Object object, Class<T> type, Map<Class<?>, Object> projects) throws Exception
	{
		Object array = null;

		if (object instanceof Iterable)
		{
			int length = 0;

			if (object instanceof JSON)
			{
				length = ((JSON) object).size();
			}
			else if (object instanceof Collection)
			{
				length = ((Collection<?>) object).size();
			}
			else
			{
				for (Object o : (Iterable<?>) object)
				{
					length++;
				}
			}

			array = Array.newInstance(type, length);

			length = 0;

			for (Object o : (Iterable<?>) object)
			{
				try
				{
					Array.set(array, length, ProjectTo(o, type, null, projects));
				}
				catch (Exception ex)
				{
					if (object instanceof JSON && ((JSON) object).projectStrict())
					{
						Tools.throwOriginalException(ex);
					}
				}
				length++;
			}
		}

		return array;
	}

	public static BigDecimal CastToBigDecimal(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return BigDecimal.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					return new BigDecimal(CastToString(obj));
				}
				catch (NumberFormatException ex)
				{
				}
			}
		}
		return null;
	}

	public static BigInteger CastToBigInteger(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return BigInteger.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					return new BigInteger(CastToString(obj));
				}
				catch (NumberFormatException ex)
				{
				}
			}
		}
		return null;
	}

	public static Boolean CastToBoolean(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return Boolean.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				String str = CastToString(obj);
				if (TRUE_STRING.equals(str) || FALSE_STRING.equals(str))
				{
					return Boolean.valueOf(str);
				}
			}
		}
		return null;
	}

	public static Byte CastToByte(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return Byte.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					return Byte.valueOf(CastToString(obj));
				}
				catch (NumberFormatException ex)
				{
				}
			}
		}
		return null;
	}

	public static Calendar CastToCalendar(Object obj)
	{
		if (obj != null)
		{
			Long millis = CastToTimeInMillis(obj);

			if (millis != null)
			{
				Calendar val = new GregorianCalendar();
				val.setTimeInMillis(millis);
				return val;
			}
		}
		return null;
	}

	public static Character CastToCharacter(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return Character.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					return CastToString(obj).charAt(0);
				}
				catch (Exception ex)
				{
				}
			}
		}
		return null;
	}

	public static Date CastToDate(Object obj)
	{
		if (obj != null)
		{
			Long millis = CastToTimeInMillis(obj);

			if (millis != null)
			{
				return new Date(millis);
			}
		}
		return null;
	}

	public static Double CastToDouble(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return Double.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					return Double.valueOf(CastToString(obj));
				}
				catch (NumberFormatException ex)
				{
				}
			}
		}
		return null;
	}

	public static Float CastToFloat(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return Float.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					return Float.valueOf(CastToString(obj));
				}
				catch (NumberFormatException ex)
				{
				}
			}
		}
		return null;
	}

	public static Function CastToFunction(Object obj)
	{
		return CastAs(obj, Function.class);
	}

	public static Integer CastToInteger(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return Integer.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					return Integer.valueOf(CastToString(obj));
				}
				catch (NumberFormatException ex)
				{
				}
			}
		}
		return null;
	}

	public static JSAN CastToJSAN(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return JSAN.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					return (JSAN) JSAN.Parse(CastToString(obj));
				}
				catch (Exception ex)
				{
				}
			}
		}
		return null;
	}

	public static JSON CastToJSON(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return JSON.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					return JSON.Parse(CastToString(obj));
				}
				catch (Exception ex)
				{
				}
			}
		}
		return null;
	}

	public static Long CastToLong(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return Long.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					return Long.valueOf(CastToString(obj));
				}
				catch (NumberFormatException ex)
				{
				}
			}
		}
		return null;
	}

	public static Short CastToShort(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return Short.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					return Short.valueOf(CastToString(obj));
				}
				catch (NumberFormatException ex)
				{
				}
			}
		}
		return null;
	}

	public static String CastToString(Object obj)
	{
		if (obj != null)
		{
			try
			{
				return String.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					if (obj instanceof java.util.Date)
					{
						return String.valueOf(((java.util.Date) obj).getTime());
					}
					else if (obj instanceof java.util.Calendar)
					{
						return String.valueOf(((java.util.Calendar) obj).getTimeInMillis());
					}
					else if (obj instanceof java.sql.Clob)
					{
						return Tools.readerToStringBuilder(((java.sql.Clob) obj).getCharacterStream()).toString();
					}
					else
					{
						return obj.toString();
					}
				}
				catch (Exception ex)
				{
				}
			}
		}
		return null;
	}

	public static Time CastToTime(Object obj)
	{
		if (obj != null)
		{
			Long millis = CastToTimeInMillis(obj);

			if (millis != null)
			{
				return new Time(millis);
			}
		}
		return null;
	}

	public static Long CastToTimeInMillis(Object obj)
	{
		if (obj == null)
		{
			return null;
		}

		Long l = CastToLong(obj);
		if (l != null)
		{
			return l;
		}

		String ts = obj.toString();
		if (ts == null)
		{
			return null;
		}

		String format = FindDatetimePattern(ts);
		if (format == null)
		{
			return null;
		}

		return Tools.getCalendar(ts, format).getTimeInMillis();
	}

	public static Timestamp CastToTimestamp(Object obj)
	{
		if (obj != null)
		{
			Long millis = CastToTimeInMillis(obj);

			if (millis != null)
			{
				return new Timestamp(millis);
			}
		}
		return null;
	}

	public static int CheckNext(CharSequence source, int now, int next)
	{
		if (next < 0)
		{
			throw new SyntaxErrorException(source, now);
		}
		else
		{
			return next;
		}
	}

	public static int DualMatchCount(CharSequence seq, char a, char b, int from)
	{
		int match = 0;

		int length = seq.length();

		boolean inString = false;

		char c;

		for (int i = Math.max(0, from); i < length; i++)
		{
			c = seq.charAt(i);

			if (c == ESCAPE_CHAR)
			{
				i++;
				if (i < length)
				{
					c = seq.charAt(i);
					if (c == UNICODE_ESCAPING_CHAR)
					{
						i += UNICODE_ESCAPED_LENGTH;
						continue;
					}
					else if (ESCAPING_CHAR.containsKey(c))
					{
						continue;
					}
					else
					{
						i--;
					}
				}
				else
				{
					break;
				}
			}

			if (c == QUOTE_CHAR)
			{
				inString = !inString;
			}

			if (inString && (a != QUOTE_CHAR || b != QUOTE_CHAR))
			{
				continue;
			}

			if (c == COMMENT_CHAR && !inString)
			{
				i = EndOfComment(seq, i);
				if (i == NOT_FOUND)
				{
					i = length - 1;
				}
				continue;
			}

			if (c == a)
			{
				if (a == b && match < 0)
				{
					match++;
				}
				else
				{
					match--;
				}
			}
			else if (c == b)
			{
				match++;
			}
		}

		return match;
	}

	public static int DualMatchIndex(CharSequence seq, char a, char b, int from)
	{
		int index = NOT_FOUND;

		int match = 0;

		int length = seq.length();

		boolean inString = false;

		char c;

		for (int i = Math.max(0, from); i < length; i++)
		{
			c = seq.charAt(i);

			if (c == ESCAPE_CHAR)
			{
				i++;
				if (i < length)
				{
					c = seq.charAt(i);
					if (c == UNICODE_ESCAPING_CHAR)
					{
						i += UNICODE_ESCAPED_LENGTH;
						continue;
					}
					else if (ESCAPING_CHAR.containsKey(c))
					{
						continue;
					}
					else
					{
						i--;
					}
				}
				else
				{
					break;
				}
			}

			if (c == QUOTE_CHAR)
			{
				inString = !inString;
			}

			if (inString && (a != QUOTE_CHAR || b != QUOTE_CHAR))
			{
				continue;
			}

			if (c == COMMENT_CHAR && !inString)
			{
				i = EndOfComment(seq, i);
				if (i == NOT_FOUND)
				{
					i = length - 1;
				}
				continue;
			}

			if (c == a)
			{
				if (a == b && match < 0)
				{
					match++;
					if (match == 0)
					{
						index = i;
						break;
					}
				}
				else
				{
					match--;
				}
			}
			else if (c == b)
			{
				match++;
				if (match == 0)
				{
					index = i;
					break;
				}
			}
		}

		return index;
	}

	public static int EndOfComment(CharSequence seq, int start)
	{
		return EndOfComment(seq, start, start + 2);
	}

	public static int EndOfComment(CharSequence seq, int start, int from)
	{
		int end = NOT_FOUND;

		int length = seq.length();

		char c = seq.charAt(start);

		if (c == COMMENT_CHAR)
		{
			if (++start < length)
			{
				c = seq.charAt(start);

				switch (c)
				{
					case LINE_COMMENT_CHAR:
						end = Tools.seekIndex(seq, '\n', from);
						if (end == NOT_FOUND)
						{
							end = Tools.seekIndex(seq, '\r', from);
						}
						break;

					case BLOCK_COMMENT_CHAR:
						end = Tools.seekIndex(seq, BLOCK_COMMENT_END, from);
						if (end != NOT_FOUND)
						{
							end += BLOCK_COMMENT_END.length() - 1;
						}
						break;
				}
			}
		}

		return end;
	}

	public static String EscapeChar(char c)
	{
		return UNICODE_ESCAPING_MARK + String.format("%04x", (int) c);
	}

	public static String EscapeString(String string)
	{
		char c;
		int last = 0;
		String esc;
		StringBuilder buf = null;

		for (int i = 0; i < string.length(); i++)
		{
			c = string.charAt(i);
			if (NeedToEscape(c))
			{
				if (buf == null)
				{
					buf = new StringBuilder((int) Math.ceil(string.length() * 1.5));
				}

				if (last < i)
				{
					buf.append(string, last, i);
				}

				esc = ESCAPED_CHAR.get(c);
				buf.append(esc != null ? esc : EscapeChar(c));

				last = i + 1;
			}
		}

		if (last == 0)
		{
			return string;
		}
		else
		{
			if (last < string.length())
			{
				buf.append(string, last, string.length());
			}
			return buf.toString();
		}
	}

	public static <T> Map<String, Field> FieldsOf(Class<T> cls, Map<String, Field> fields)
	{
		if (cls != null)
		{
			if (fields == null)
			{
				fields = new LinkedHashMap<String, Field>();
			}

			if (Object.class == cls)
			{
				return fields;
			}

			fields = FieldsOf(cls.getSuperclass(), fields);

			for (Field field : cls.getDeclaredFields())
			{
				if (!Modifier.isStatic(field.getModifiers()))
				{
					fields.put(field.getName(), field);
				}
			}
		}
		return fields;
	}

	public static <T> Map<String, Field> FieldsOf(T object)
	{
		if (object == null)
		{
			return null;
		}
		else
		{
			return FieldsOf(object.getClass(), null);
		}
	}

	public static boolean FilterComments(StringBuilder buffer, CharSequence seq, int from)
	{
		boolean inComment = false;

		if (seq == null)
		{
			return inComment;
		}
		else
		{
			int length = seq.length();

			if (buffer == null)
			{
				buffer = new StringBuilder(length);
			}

			boolean inString = false;

			char c, l = 0;

			for (int i = from; i < length; i++)
			{
				c = seq.charAt(i);

				if (c == QUOTE_CHAR && l != ESCAPE_CHAR)
				{
					inString = !inString;
				}
				else if (c == COMMENT_CHAR && !inString)
				{
					int end = EndOfComment(seq, i);

					if (end == NOT_FOUND)
					{
						end = length - 1;

						if (i + 1 < length && seq.charAt(i + 1) == BLOCK_COMMENT_CHAR)
						{
							inComment = true;
						}
					}

					i = end;
					continue;
				}

				buffer.append(c);
				l = c;
			}

			return inComment;
		}
	}

	/**
	 * Find a date time string pattern based on default date time pattern.
	 * 
	 * @param datetime
	 * @return
	 */
	public static String FindDatetimePattern(String datetime)
	{
		Matcher m = DEFAULT_DATETIME_PATTERN.matcher(datetime);
		if (!m.matches())
		{
			return null;
		}

		String format = DEFAULT_DATETIME_FORMAT;

		if (m.group(1) == null)
		{
			format = format.replaceFirst("'T'.+SSS", "");
		}
		else
		{
			if (" ".equals(m.group(2)))
			{
				format = format.replace("'T'", " ");
			}

			if (m.group(3) == null)
			{
				format = format.replace(":ss", "");
			}

			if (m.group(4) == null)
			{
				format = format.replace(".SSS", "");
			}
			else
			{
				format = format.replace(".SSS", "." + Tools.repeat('S', m.group(4).length()));
			}
		}

		if (m.group(5) == null)
		{
			format = format.replace("'Z'", "");
		}
		else if (!"Z".equals(m.group(5)))
		{
			format = format.replace("'Z'", "X");
		}

		return format;
	}

	public static final int FirstNonWhitespaceIndex(CharSequence seq, int from)
	{
		int index = NOT_FOUND;

		int length = seq.length();
		char c;
		int code;

		for (int i = Math.max(0, from); i < length; i++)
		{
			code = (c = seq.charAt(i));

			if (c == COMMENT_CHAR)
			{
				i = EndOfComment(seq, i);
				if (i == NOT_FOUND)
				{
					i = length - 1;
				}
				continue;
			}

			if (!Character.isSpaceChar(code) && !Character.isWhitespace(code))
			{
				index = i;
				break;
			}
		}

		return index;
	}

	protected static boolean IsAllPrimaryData(JSON json)
	{
		for (Object value : json.values())
		{
			if (value instanceof JSON)
			{
				return false;
			}
		}
		return true;
	}

	public static final boolean IsArray(Object o)
	{
		return o != null && o.getClass().isArray();
	}

	public static final boolean IsContext(Object o)
	{
		return o instanceof Context;
	}

	public static final boolean IsFunction(Object o)
	{
		return o instanceof Function;
	}

	public static final boolean IsHierarchical(Object o)
	{
		return o instanceof Hierarchical;
	}

	public static final boolean IsJSAN(Object o)
	{
		return o instanceof JSAN;
	}

	public static final boolean IsJSON(Object o)
	{
		return o instanceof JSON;
	}

	public static final boolean IsPlainJSON(Object o)
	{
		return (o instanceof JSON) && !(o instanceof JSAN);
	}

	public static final boolean IsQuotation(Object o)
	{
		return o instanceof Quotation;
	}

	public static <S, C> boolean IsSubClassOf(Class<S> sub, Class<C> cls)
	{
		try
		{
			sub.asSubclass(cls);
			return true;
		}
		catch (ClassCastException e)
		{
			return false;
		}
	}

	public static boolean IsSubTypeOf(Type sub, Type cls)
	{
		try
		{
			((Class<?>) sub).asSubclass((Class<?>) cls);
			return true;
		}
		catch (ClassCastException e)
		{
			return false;
		}
	}

	public static Collection<String> KeysOf(Class<?> cls, Collection<String> keys)
	{
		if (cls != null)
		{
			if (keys == null)
			{
				keys = new LinkedHashSet<String>();
			}

			keys = KeysOf(cls.getSuperclass(), keys);

			for (Field field : cls.getDeclaredFields())
			{
				if (!Modifier.isStatic(field.getModifiers()))
				{
					keys.add(field.getName());
				}
			}
		}

		return keys;
	}

	public static <T> Collection<String> KeysOf(T object)
	{
		Collection<String> keys = null;

		if (object != null)
		{
			keys = KeysOf(object.getClass(), keys);
		}

		return keys;
	}

	public static int LastDualMatchIndex(CharSequence seq, char a, char b, int from)
	{
		int index = NOT_FOUND;
		int match = 0;

		boolean inString = false;

		i: for (int i = Math.min(seq.length() - 1, from); i >= 0; i--)
		{
			char c = seq.charAt(i);

			if (c == QUOTE_CHAR && i > 0 && seq.charAt(i - 1) != ESCAPE_CHAR)
			{
				inString = !inString;
			}
			if (inString)
			{
				continue i;
			}

			if (c == b)
			{
				match++;
			}
			else if (c == a)
			{
				match--;
				if (match == 0)
				{
					index = i;
					break;
				}
			}
		}

		return index;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Tools.debug(JSON.FindDatetimePattern("2022-10-23T22:06+08:00"));
	}

	public static boolean NeedToEscape(char c)
	{
		return ESCAPED_CHAR.containsKey(c) || Character.isISOControl(c);
	}

	public static JSON Of(Map<String, ?> map)
	{
		if (map == null)
		{
			return null;
		}
		else if (map instanceof JSON)
		{
			return (JSON) map;
		}
		else
		{
			return new JSON().attrAll(map);
		}
	}

	public static void Output(Writer out, Object object, int indents, String indent)
	{
		if (out == null)
		{
			return;
		}

		try
		{
			String newLine = System.getProperty("line.separator", "\n");

			if (!(object instanceof JSON))
			{
				Tools.repeat(out, indent, indents);
				out.write(JSON.SerializeValueOf(object));
				return;
			}

			JSON json = (JSON) object;
			if (json instanceof JSAN)
			{
				if (IsAllPrimaryData(json))
				{
					Tools.repeat(out, indent, indents);
					JSON.Serialize(json, out, -1);
				}
				else
				{
					Tools.repeat(out, indent, indents);
					boolean first = true;
					out.write('[' + newLine);
					for (Object value : json)
					{
						if (first)
						{
							first = false;
						}
						else
						{
							out.write(',' + newLine);
						}
						Output(out, value, indents + 1, indent);
					}
					out.write(newLine);
					Tools.repeat(out, indent, indents);
					out.write(']');
				}
			}
			else
			{
				Tools.repeat(out, indent, indents);
				boolean first = true;
				out.write('{' + newLine);
				for (Pair pair : json.pairs())
				{
					if (first)
					{
						first = false;
					}
					else
					{
						out.write(',' + newLine);
					}
					Tools.repeat(out, indent, indents + 1);
					out.write(JSON.SerializeValueOf(pair.getKey()));
					out.write(':');
					Output(out, pair.getValue(), 0, "  ");
				}
				out.write(newLine);
				Tools.repeat(out, indent, indents);
				out.write('}');
			}
		}
		catch (Exception e)
		{
		}
		finally
		{
			try
			{
				out.flush();
			}
			catch (Exception e)
			{
			}
		}
	}

	public static JSON Parse(CharSequence source)
	{
		return new Parser().parse(source).dispose().result();
	}

	public static JSON Parse(File file)
	{
		return Parse(file, Charset.defaultCharset());
	}

	public static JSON Parse(File file, Charset charset)
	{
		JSON json = null;

		if (file.isFile())
		{
			if (charset == null)
			{
				charset = Charset.defaultCharset();
			}

			Reader reader = null;
			try
			{
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
				json = Parse(reader);
			}
			catch (Exception e)
			{
			}
			finally
			{
				if (reader != null)
				{
					try
					{
						reader.close();
					}
					catch (IOException e)
					{
					}
				}
			}
		}

		return json;
	}

	public static JSON Parse(File file, String charsetName)
	{
		return Parse(file, Charset.forName(charsetName));
	}

	public static JSON Parse(Reader reader)
	{
		JSON json = null;

		if (reader != null)
		{
			Parser parser = new Parser();
			try
			{
				json = parser.parse(reader, true).result();
			}
			catch (IOException e)
			{
			}
			finally
			{
				if (parser != null)
				{
					parser.dispose();
				}
			}
		}

		return json;
	}

	public static Object ParseValueOf(String string)
	{
		if (string == null)
		{
			return NOT_A_VALUE;
		}

		string = string.trim();

		try
		{
			if (string.length() == 0)
			{
				return NOT_A_VALUE;
			}
			else if (string.startsWith(QUOTE_MARK) && string.endsWith(QUOTE_MARK))
			{
				return RestoreString(string);
			}
			else if (Variable.isIntegerNumber(string))
			{
				try
				{
					return Integer.valueOf(string);
				}
				catch (NumberFormatException e)
				{
					return Long.valueOf(string);
				}
			}
			else if (Variable.isFloatNumber(string))
			{
				return new BigDecimal(string);
			}
			else if (TRUE_STRING.equals(string) || FALSE_STRING.equals(string))
			{
				return Boolean.parseBoolean(string);
			}
			else if (NULL_STRING.equals(string))
			{
				return null;
			}
			else if (string.startsWith(Function.DEFINE_MARK))
			{
				return new Function(string);
			}
			else if (!string.startsWith(OBJECT_BEGIN_MARK) && !string.startsWith(ARRAY_BEGIN_MARK))
			{
				return new Quotation(string);
			}
			else
			{
				return NOT_A_VALUE;
			}
		}
		catch (NumberFormatException e)
		{
			return NOT_A_VALUE;
		}
	}

	public static Path Path(String text)
	{
		return JSON.Path.parse(text);
	}

	/**
	 * Generate a new instance according to the source json against target
	 * class.
	 * 
	 * @param cls
	 * @param json
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T Project(Class<T> cls, JSON json)
	{
		Object project = ProjectOf(cls, json.projects());
		if (project instanceof Projector)
		{
			try
			{
				T obj = ((Projector<T>) project).instance(cls, json);
				if (obj != null)
				{
					return obj;
				}
			}
			catch (Exception e)
			{
				if (json.projectStrict())
				{
					Tools.throwAsRuntimeException(Tools.getOriginalThrowable(e));
				}
			}
		}

		TreeMap<Class<?>[], Constructor<?>> cons = new TreeMap<Class<?>[], Constructor<?>>(
				new ArrayLengthReverseComparator<Class<?>>());

		for (Constructor<?> c : cls.getConstructors())
		{
			if (Modifier.isPublic(c.getModifiers()))
			{
				cons.put(c.getParameterTypes(), c);
			}
		}

		Object[] params = null;

		for (Entry<Class<?>[], Constructor<?>> entry : cons.entrySet())
		{
			Class<?>[] types = entry.getKey();
			Constructor<?> con = entry.getValue();

			if (params == null)
			{
				params = new Object[types.length];

				int i = 0;
				for (Object o : json.values())
				{
					if (i >= params.length)
					{
						break;
					}
					params[i] = o;
					i++;
				}
			}

			if (Tools.suitableParameters(types, params))
			{
				Object[] param = new Object[types.length];
				System.arraycopy(params, 0, param, 0, param.length);
				try
				{
					return (T) con.newInstance(param);
				}
				catch (Exception e)
				{
				}
			}
		}

		try
		{
			return cls.newInstance();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * The default project procedure.
	 * 
	 * @param object
	 *            target object
	 * @param json
	 *            source json
	 * @return
	 */
	public static <T> T Project(T object, JSON json)
	{
		Map<String, Object> project = null;

		Map<String, Field> fields = null;

		if (!(object instanceof Map) && !(object instanceof Iterable) && !IsArray(object))
		{
			fields = FieldsOf(object);
		}

		if (fields != null)
		{
			project = new LinkedHashMap<String, Object>();

			for (String key : fields.keySet())
			{
				project.put(key, key);
			}
		}

		try
		{
			return Project(object, json, project);
		}
		catch (Exception e)
		{
			if (json.projectStrict())
			{
				Tools.throwAsRuntimeException(Tools.getOriginalThrowable(e));
			}
			return object;
		}
	}

	public static <T> T Project(T object, JSON json, JSON.Projector<T> projector) throws Exception
	{
		if (object != null && json != null && projector != null)
		{
			object = projector.project(object, json);
		}
		return object;
	}

	@SuppressWarnings("unchecked")
	public static <T> T Project(T object, JSON json, Map<String, ? extends Object> project) throws Exception
	{
		if (object != null)
		{
			if (IsJSON(object))
			{
				JSON obj = (JSON) object;

				if (project == null)
				{
					obj.putAll(json);
				}
				else
				{
					for (Entry<String, ? extends Object> entry : project.entrySet())
					{
						try
						{
							String key = entry.getValue() != null ? entry.getValue().toString() : null;

							if (json.has(key))
							{
								obj.attr(entry.getKey(), json.attr(key));
							}
						}
						catch (Exception e)
						{
							if (obj.projectStrict())
							{
								Tools.throwOriginalException(e);
							}
						}
					}
				}
			}
			else if (object instanceof Map)
			{
				object = (T) ProjectTo(json, object.getClass(), object, json.projects());
			}
			else if (object instanceof Collection)
			{
				object = (T) ProjectTo(json, object.getClass(), object, json.projects());
			}
			else if (IsArray(object))
			{
				object = (T) ProjectTo(json, object.getClass(), object, json.projects());
			}
			else
			{
				if (project != null)
				{
					Class<?> cls = object.getClass();
					Field field = null;
					Object value = null;
					Type type = null;

					for (Entry<String, ? extends Object> entry : project.entrySet())
					{
						try
						{
							String name = entry.getKey();
							String key = entry.getValue() != null ? entry.getValue().toString() : null;

							if (json.has(key))
							{
								field = Tools.fieldOf(cls, name);
								try
								{
									/*
									 * For the value which could be a
									 * "container", like Map, Collection or a
									 * normal Object.
									 */
									value = Access(object, name, field);
								}
								catch (Exception e)
								{
									value = null;
								}

								type = null;
								if (field != null)
								{
									type = field.getType();
									if (IsSubTypeOf(type, Collection.class) || IsSubTypeOf(type, Map.class))
									{ // value is container, use generic type
										type = field.getGenericType();
									}
								}
								else if (value != null)
								{
									type = value.getClass();
								}
								else
								{ // The value read by "getter" was null.
									Member m = Tools.accessor(cls, name);
									type = Tools.getAccessorType(m);
								}

								Object result = null;
								if (type != null || value != null)
								{
									try
									{
										Object obj = json.attr(key);
										if (obj instanceof JSON)
										{
											JSON j = (JSON) obj;
											if (j.projects() == null)
											{
												j.projects(json);
											}
										}
										result = ProjectTo(obj, type, value, json.projects());
									}
									catch (Exception e)
									{
										if (json.projectStrict())
										{
											Tools.throwOriginalException(e);
										}
										else
										{
											result = json.attr(key);
										}
									}
								}
								else
								{
									result = json.attr(key);
								}

								try
								{
									Access(object, name, field, result);
								}
								catch (IllegalAccessException e)
								{
								}
							}
						}
						catch (Exception e)
						{
							if (json.projectStrict())
							{
								Tools.throwOriginalException(e);
							}
						}
					}
				}
			}
		}
		return object;
	}

	@SuppressWarnings("unchecked")
	public static <T> T Project(T object, Map<Class<?>, Object> projects, JSON json)
	{
		if (json != null)
		{
			Object project = ProjectOf(object, projects);
			json.projects(projects);
			try
			{
				if (project instanceof JSON.Projector)
				{
					object = Project(object, json, (JSON.Projector<T>) project);
				}
				else if (project instanceof Map)
				{
					object = Project(object, json, (Map<String, Object>) project);
				}
				else
				{
					object = Project(object, json);
				}
			}
			catch (Exception e)
			{
				if (json.projectStrict())
				{
					Tools.throwAsRuntimeException(Tools.getOriginalThrowable(e));
				}
			}
		}
		return object;
	}

	public static Object ProjectOf(Class<?> cls, Map<Class<?>, Object> projects)
	{
		if (cls != null && projects != null)
		{
			Object project = null;

			for (Map.Entry<Class<?>, Object> entry : projects.entrySet())
			{
				Class<?> c = entry.getKey();

				if (c != null && IsSubClassOf(cls, c) && (project = entry.getValue()) != null)
				{
					return project;
				}
			}

			try
			{
				return projects.get(null);
			}
			catch (NullPointerException e)
			{
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	public static Object ProjectOf(Object object, Map<Class<?>, Object> projects)
	{
		return object == null ? null : ProjectOf(object.getClass(), projects);
	}

	@SuppressWarnings("unchecked")
	public static <T> Object ProjectTo(Object obj, Type type, Object val, Map<Class<?>, Object> projects)
			throws Exception
	{
		Object project = ProjectOf(type, projects);

		Class<T> cls = type instanceof Class ? (Class<T>) type : null;

		ParameterizedType paramedType = type instanceof ParameterizedType ? (ParameterizedType) type : null;

		if (cls != null && cls.isInstance(obj) && paramedType == null)
		{
			val = obj;
		}
		else if (String.class == type)
		{
			val = CastToString(obj);
		}
		else if (Integer.TYPE == type || Integer.class == type)
		{
			val = CastToInteger(obj);
		}
		else if (Double.TYPE == type || Double.class == type)
		{
			val = CastToDouble(obj);
		}
		else if (Boolean.TYPE == type || Boolean.class == type)
		{
			val = CastToBoolean(obj);
		}
		else if (Character.TYPE == type || Character.class == type)
		{
			val = CastToCharacter(obj);
		}
		else if (BigDecimal.class == type)
		{
			val = CastToBigDecimal(obj);
		}
		else if (JSON.class == type)
		{
			val = CastAs(obj, JSON.class);
		}
		else if (JSAN.class == type)
		{
			val = CastAs(obj, JSAN.class);
		}
		else if (Function.class == type)
		{
			val = CastToFunction(obj);
		}
		else if (Long.TYPE == type || Long.class == type)
		{
			val = CastToLong(obj);
		}
		else if (Calendar.class == type)
		{
			val = CastToCalendar(obj);
		}
		else if (java.util.Date.class == type || Date.class == type)
		{
			val = CastToDate(obj);
		}
		else if (Timestamp.class == type)
		{
			val = CastToTimestamp(obj);
		}
		else if (Time.class == type)
		{
			val = CastToTime(obj);
		}
		else if (Byte.TYPE == type || Byte.class == type)
		{
			val = CastToByte(obj);
		}
		else if (Float.TYPE == type || Float.class == type)
		{
			val = CastToFloat(obj);
		}
		else if (Short.TYPE == type || Short.class == type)
		{
			val = CastToShort(obj);
		}
		else if (project instanceof JSON.Projector)
		{
			val = ((JSON.Projector<T>) project).project((T) val, (JSON) obj);
		}
		else if (paramedType != null && IsSubTypeOf(paramedType.getRawType(), Map.class) && JSON.IsJSON(obj))
		{ // Target class is a Map.
			Map<Object, Object> map = (Map<Object, Object>) (val == null ? new LinkedHashMap<Object, Object>() : val);

			Type eleType = paramedType.getActualTypeArguments()[1];
			Class<?> eleClass = null;
			try
			{
				if (eleType instanceof Class)
				{
					eleClass = (Class<?>) eleType;
				}
				else if (eleType instanceof WildcardType)
				{
					WildcardType w = (WildcardType) eleType;
					Type[] t = w.getUpperBounds();
					if (t != null && t.length > 0 && t[0] != Object.class)
					{
						eleClass = (Class<?>) t[0];
					}
					else
					{
						t = w.getLowerBounds();
						if (t != null && t.length > 0)
						{
							eleClass = (Class<?>) t[0];
						}
					}
				}
				if (eleClass != null)
				{
					project = ProjectOf(eleClass, projects);
				}
			}
			catch (Exception e)
			{
			}

			map.clear();

			JSON json = (JSON) obj;

			if (project == null)
			{
				Object o;
				for (Pair pair : json.pairs())
				{
					try
					{
						o = pair.getValue();
						if (o != null && eleClass != null && !eleClass.isInstance(o))
						{
							if (o instanceof JSON)
							{
								JSON j = (JSON) o;
								if (j.projects() == null)
								{
									j.projects(json);
								}
							}
							o = ProjectTo(o, eleClass, null, json.projects());
						}
						map.put(pair.getKey(), o);
					}
					catch (Exception e)
					{
						if (json.projectStrict())
						{
							Tools.throwOriginalException(e);
						}
					}
				}
			}
			else if (project != null && eleClass != null && IsPlainJSON(json))
			{
				Object v;
				for (Pair pair : json.pairs())
				{
					try
					{
						v = pair.getValue();
						if (v != null && !eleClass.isInstance(v))
						{
							JSON j = (JSON) v;
							if (j.projects() == null)
							{
								j.projects(json);
							}
							v = Project(Project(eleClass, (JSON) v), projects, j);
						}
						map.put(pair.getKey(), v);
					}
					catch (Exception e)
					{
						if (json.projectStrict())
						{
							Tools.throwOriginalException(e);
						}
					}
				}
			}
			else
			{
				for (Entry<String, Object> entry : ((Map<String, Object>) project).entrySet())
				{
					if (entry.getKey() != null && entry.getValue() != null)
					{
						try
						{
							String key = entry.getValue() != null ? entry.getValue().toString() : null;

							if (json.has(key))
							{
								map.put(entry.getKey(), json.attr(key));
							}
						}
						catch (Exception e)
						{
							if (json.projectStrict())
							{
								Tools.throwOriginalException(e);
							}
						}
					}
				}
			}

			val = map;
		}
		else if (paramedType != null && IsSubTypeOf(paramedType.getRawType(), Collection.class) && JSON.IsJSON(obj))
		{ // Target class is a Collection.
			Collection<Object> coll = (Collection<Object>) (val == null ? new LinkedList<Object>() : val);

			Type eleType = paramedType.getActualTypeArguments()[0];
			Class<?> eleClass = null;
			try
			{
				if (eleType instanceof Class)
				{
					eleClass = (Class<?>) eleType;
				}
				else if (eleType instanceof WildcardType)
				{
					WildcardType w = (WildcardType) eleType;
					Type[] t = w.getUpperBounds();
					if (t != null && t.length > 0 && t[0] != Object.class)
					{
						eleClass = (Class<?>) t[0];
					}
					else
					{
						t = w.getLowerBounds();
						if (t != null && t.length > 0)
						{
							eleClass = (Class<?>) t[0];
						}
					}
				}
				if (eleClass != null)
				{
					project = ProjectOf(eleClass, projects);
				}
			}
			catch (Exception e)
			{
			}

			coll.clear();

			JSON json = (JSON) obj;

			if (project == null)
			{
				for (Object o : json.values())
				{
					try
					{
						if (o != null && eleClass != null && !eleClass.isInstance(o))
						{
							if (o instanceof JSON)
							{
								JSON j = (JSON) o;
								if (j.projects() == null)
								{
									j.projects(json);
								}
							}
							o = ProjectTo(o, eleClass, null, json.projects());
						}
						coll.add(o);
					}
					catch (Exception e)
					{
						if (json.projectStrict())
						{
							Tools.throwOriginalException(e);
						}
					}
				}
			}
			else if (project != null && eleClass != null && IsJSAN(json))
			{
				for (JSON o : ((JSAN) json).iterator(JSON.class))
				{
					if (o == null)
					{
						coll.add(null);
					}
					else
					{
						try
						{
							if (o.projects() == null)
							{
								o.projects(json);
							}
							coll.add(Project(Project(eleClass, o), projects, o));
						}
						catch (Exception e)
						{
							if (json.projectStrict())
							{
								Tools.throwOriginalException(e);
							}
						}
					}
				}
			}
			else
			{
				for (Object v : ((Map<String, Object>) project).values())
				{
					if (v == null)
					{
						coll.add(null);
					}
					else
					{
						try
						{
							String key = v.toString();

							if (json.has(key))
							{
								coll.add(json.attr(key));
							}
						}
						catch (Exception e)
						{
							if (json.projectStrict())
							{
								Tools.throwOriginalException(e);
							}
						}
					}
				}
			}

			val = coll;
		}
		else if (cls != null && cls.isArray())
		{
			val = CastToArray(obj, cls.getComponentType(), projects);
		}
		else if (cls != null && JSON.IsJSON(obj))
		{ // Target class is a normal Object.
			JSON json = (JSON) obj;
			val = Project(val == null ? Project(cls, json) : val, json.projects(), json);
		}
		else
		{
			val = obj;
		}

		return val;
	}

	public static Object Quote(Object o)
	{
		while (o instanceof Quotation)
		{
			o = ((Quotation) o).quote();
		}
		return o;
	}

	public static JSON Reflect(JSON json, Map<Class<?>, Object> reflects, Object object)
	{
		if (object != null)
		{
			Object reflect = ReflectOf(object, reflects);

			if (reflect instanceof JSAN.Reflector)
			{
				json = JSAN.Reflect((JSAN) (json == null ? new JSAN() : json).reflects(reflects), object,
						(JSAN.Reflector<?>) reflect);
			}
			else if (reflect instanceof JSON.Reflector)
			{
				json = JSON.Reflect((json == null ? new JSON() : json).reflects(reflects), object,
						(JSON.Reflector<?>) reflect);
			}
			else if (JSAN.IsJSAN(reflect))
			{
				json = JSAN.Reflect((JSAN) (json == null ? new JSAN() : json).reflects(reflects), object,
						(JSAN) reflect);
			}
			else if (JSON.IsJSON(reflect))
			{
				json = JSON.Reflect((json == null ? new JSON() : json).reflects(reflects), object, (JSON) reflect);
			}
			else
			{
				json = JSON.Reflect((json == null ? new JSON() : json).reflects(reflects), object);
			}
		}

		return json;
	}

	/**
	 * The default reflect procedure.
	 * 
	 * @param json
	 *            target json
	 * @param object
	 *            source object
	 * @return
	 */
	public static JSON Reflect(JSON json, Object object)
	{
		JSON reflect = null;

		Collection<String> fields = null;

		if (!(object instanceof Map) && !(object instanceof Iterable) && !IsArray(object))
		{
			fields = KeysOf(object);
		}

		if (fields != null)
		{
			reflect = new JSON();

			for (String field : fields)
			{
				reflect.put(field, field);
			}
		}

		return JSON.Reflect(json, object, reflect);
	}

	public static JSON Reflect(JSON json, Object object, Iterable<?> fields)
	{
		JSON reflect = null;

		if (fields != null)
		{
			reflect = new JSON();

			for (Object field : fields)
			{
				if (field != null)
				{
					reflect.put(field.toString(), field.toString());
				}
			}
		}

		return JSON.Reflect(json, object, reflect);
	}

	@SuppressWarnings("unchecked")
	public static JSON Reflect(JSON json, Object object, JSON reflect)
	{
		if (object != null)
		{
			if (json == null)
			{
				json = new JSON().reflects(object.getClass(), reflect);
			}

			if (IsJSON(object))
			{
				JSON obj = (JSON) object;

				if (reflect == null)
				{
					json.putAll(obj);
				}
				else
				{
					for (Pair pair : reflect.pairs())
					{
						try
						{
							String key = pair.getValue().toString();

							if (obj.containsKey(key))
							{
								json.attr(pair.getKey(), obj.attr(key));
							}
						}
						catch (Exception e)
						{
						}
					}
				}
			}
			else if (object instanceof Map)
			{
				Map<String, ?> map = (Map<String, ?>) object;

				if (reflect == null)
				{
					json.putAll(map);
				}
				else
				{
					for (Pair pair : reflect.pairs())
					{
						try
						{
							Object key = pair.getValue();

							if (map.containsKey(key))
							{
								json.attr(pair.getKey(), map.get(key));
							}
						}
						catch (Exception e)
						{
						}
					}
				}
			}
			else if (object instanceof Iterable)
			{
				JSAN temp = JSAN.Reflect(new JSAN().templates(json), object);

				if (reflect == null)
				{
					json.putAll(temp);
				}
				else
				{
					for (Pair pair : reflect.pairs())
					{
						try
						{
							String key = pair.getValue().toString();

							if (temp.containsKey(key))
							{
								json.attr(pair.getKey(), temp.attr(key));
							}
						}
						catch (Exception e)
						{
						}
					}
				}
			}
			else if (IsArray(object))
			{
				JSAN temp = JSAN.Reflect(new JSAN().templates(json), object);

				if (reflect == null)
				{
					json.putAll(temp);
				}
				else
				{
					for (Pair pair : reflect.pairs())
					{
						try
						{
							String key = pair.getValue().toString();

							if (temp.containsKey(key))
							{
								json.attr(pair.getKey(), temp.attr(key));
							}
						}
						catch (Exception e)
						{
						}
					}
				}
			}
			else if (reflect != null)
			{
				// Reflect Object using template
				for (Pair pair : reflect.pairs())
				{
					try
					{
						if (pair.getKey() != null)
						{
							Access(json, pair.getKey(), object, pair.getValue().toString(), null);
						}
					}
					catch (Exception e)
					{
					}
				}
			}
			else
			{
				// Template is null.
				json = JSON.Reflect(json, object);
			}
		}

		return json;
	}

	@SuppressWarnings("unchecked")
	public static <T> JSON Reflect(JSON json, Object object, JSON.Reflector<T> reflector)
	{
		if (object != null && reflector != null)
		{
			if (json == null)
			{
				json = new JSON();
			}

			try
			{
				json = reflector.reflect(json, (T) object);
			}
			catch (ClassCastException e)
			{
			}
		}

		return json;
	}

	public static JSON Reflect(JSON json, Object object, Map<String, ?> fields)
	{
		JSON reflect = null;

		if (fields != null)
		{
			reflect = new JSON();

			for (Entry<String, ?> entry : fields.entrySet())
			{
				if (entry.getKey() != null && entry.getValue() != null)
				{
					reflect.put(entry.getKey(), entry.getValue());
				}
			}
		}

		return JSON.Reflect(json, object, reflect);
	}

	public static JSON Reflect(JSON json, Object object, String... fields)
	{
		JSON reflect = null;

		if (fields != null)
		{
			reflect = new JSON();

			for (String field : fields)
			{
				if (field != null)
				{
					reflect.put(field, field);
				}
			}
		}

		return JSON.Reflect(json, object, reflect);
	}

	public static JSON Reflect(Map<Class<?>, Object> reflects, Object object)
	{
		return JSON.Reflect(null, reflects, object);
	}

	public static JSON Reflect(Object object)
	{
		return JSON.Reflect((JSON) null, object);
	}

	public static JSON Reflect(Object object, Iterable<?> fields)
	{
		return JSON.Reflect(null, object, fields);
	}

	public static JSON Reflect(Object object, JSON reflect)
	{
		return JSON.Reflect(null, object, reflect);
	}

	public static <T> JSON Reflect(Object object, JSON.Reflector<T> reflector)
	{
		return JSON.Reflect(null, object, reflector);
	}

	public static JSON Reflect(Object object, Map<String, ?> fields)
	{
		return JSON.Reflect(null, object, fields);
	}

	public static JSON Reflect(Object object, String... fields)
	{
		return JSON.Reflect(null, object, fields);
	}

	public static Object ReflectOf(Object object, Map<Class<?>, Object> reflects)
	{
		if (object != null && reflects != null)
		{
			for (Map.Entry<Class<?>, Object> entry : reflects.entrySet())
			{
				Class<?> cls = entry.getKey();

				if (cls != null && cls.isInstance(object) && entry.getValue() != null)
				{
					return entry.getValue();
				}
			}

			try
			{
				return reflects.get(null);
			}
			catch (NullPointerException e)
			{
				return null;
			}
		}

		return null;
	}

	public static String RestoreString(String text)
	{
		if (text == null)
		{
			return null;
		}

		text = text.trim();

		if (NULL_STRING.equals(text))
		{
			return null;
		}
		else if (text.length() >= 2 && text.charAt(0) == QUOTE_CHAR && text.charAt(text.length() - 1) == QUOTE_CHAR)
		{
			return RestoreStringContent(text.substring(1, text.length() - 1));
		}
		else
		{
			return RestoreStringContent(text);
		}
	}

	public static String RestoreStringContent(String text)
	{
		if (text.length() == 0)
		{
			return text;
		}

		char c;
		int last = 0;
		StringBuilder buf = null;

		for (int i = 0; i < text.length(); i++)
		{
			c = text.charAt(i);

			if (c == ESCAPE_CHAR)
			{
				if (buf == null)
				{
					buf = new StringBuilder(text.length());
				}

				buf.append(text, last, i);
				i++;
				c = text.charAt(i);

				if (c == UNICODE_ESCAPING_CHAR)
				{
					i++;
					String unicode = text.substring(i, i + UNICODE_ESCAPED_LENGTH);
					buf.append((char) Integer.parseInt(unicode, UNICODE_ESCAPE_RADIX));
					i += UNICODE_ESCAPED_LENGTH - 1;
				}
				else if (ESCAPING_CHAR.containsKey(c))
				{
					buf.append(ESCAPING_CHAR.get(c));
				}

				last = i + 1;
			}
		}

		if (last == 0)
		{
			return text;
		}
		else
		{
			if (last < text.length())
			{
				buf.append(text, last, text.length());
			}
			return buf.toString();
		}
	}

	public static StringBuilder Serialize(JSON json, StringBuilder buffer, int indents)
	{
		return Serialize(json, buffer, indents, null);
	}

	public static StringBuilder Serialize(JSON json, StringBuilder buffer, int indents, String indent)
	{
		if (json != null)
		{
			StringBuilderWriter writer = new StringBuilderWriter(buffer);

			try
			{
				Serialize(json, writer, indents, indent);
			}
			catch (IOException e)
			{
			}
		}
		return buffer;
	}

	public static Writer Serialize(JSON json, Writer out, int indents) throws IOException
	{
		return Serialize(json, out, indents, null);
	}

	public static Writer Serialize(JSON json, Writer out, int indents, String indent) throws IOException
	{
		if (out == null)
		{
			out = new StringWriter();
		}

		indent = indent == null ? DEFAULT_LINE_INDENT : indent;

		boolean isJSAN = IsJSAN(json);
		boolean isFirst = true;

		if (!JSON.IsContext(json))
		{
			if (isJSAN)
			{
				out.write(ARRAY_BEGIN_CHAR);
			}
			else
			{
				out.write(OBJECT_BEGIN_CHAR);
			}
		}

		int inner = indents;
		if (inner > -1)
		{
			if (!JSON.IsContext(json))
			{
				inner++;
			}
		}

		String key;
		Object object;
		CharSequence value;

		for (Map.Entry<String, Object> entry : json.entrySet())
		{
			key = entry.getKey();
			object = entry.getValue();

			if (!JSON.IsContext(json))
			{
				if (isFirst)
				{
					isFirst = false;
				}
				else
				{
					out.append(PAIR_CHAR);
				}
				if (indents > -1)
				{
					out.append(LINE_WRAP);
					Tools.repeat(out, indent, indents + 1);
				}
			}

			if (!isJSAN)
			{
				if (key == null)
				{
					out.append(NULL_STRING);
				}
				else
				{
					if (JSON.IsContext(json))
					{
						out.append(Context.VAR_DEFINE_MARK);
						out.append(' ');
					}
					else
					{
						out.append(QUOTE_CHAR);
					}

					out.append(EscapeString(key));

					if (JSON.IsContext(json))
					{
						out.append(Context.VAR_ASSIGN_CHAR);
					}
					else
					{
						out.append(QUOTE_CHAR);
						out.append(ATTR_CHAR);
					}
				}
			}

			if (IsJSON(object))
			{
				Serialize((JSON) object, out, inner, indent);
				value = null;
			}
			else
			{
				value = SerializeValueOf(object);
			}

			if (value != null)
			{
				out.append(value);
			}

			if (JSON.IsContext(json))
			{
				out.append(Context.VAR_END_CHAR);
				out.append(LINE_WRAP);
			}
		}

		if (!JSON.IsContext(json))
		{
			if (isJSAN)
			{
				if (indents > -1)
				{
					out.append(LINE_WRAP);
					Tools.repeat(out, indent, indents);
				}
				out.append(ARRAY_END_CHAR);
			}
			else
			{
				if (indents > -1)
				{
					out.append(LINE_WRAP);
					Tools.repeat(out, indent, indents);
				}
				out.append(OBJECT_END_CHAR);
			}
		}

		return out;
	}

	public static String SerializeValueOf(Object value)
	{
		if (value == null)
		{
			return NULL_STRING;
		}
		else if (value instanceof CharSequence || value instanceof Character)
		{
			return QUOTE_CHAR + EscapeString(value.toString()) + QUOTE_CHAR;
		}
		else if (value instanceof BigDecimal)
		{
			return ((BigDecimal) value).toPlainString();
		}
		else if (value instanceof Double || value instanceof Float)
		{
			return new BigDecimal(value.toString()).toPlainString();
		}
		else if (value instanceof java.util.Date)
		{
			return String.valueOf(((java.util.Date) value).getTime());
		}
		else if (value instanceof java.util.Calendar)
		{
			return String.valueOf(((java.util.Calendar) value).getTimeInMillis());
		}
		else
		{
			return value.toString();
		}
	}

	public static Object ValueOf(Object object)
	{
		return ValueOf(object, null);
	}

	public static Object ValueOf(Object object, Map<Class<?>, Object> reflects)
	{
		if (ReflectOf(object, reflects) != null)
		{
			if (IsArray(object) || (object instanceof Iterable && !IsPlainJSON(object)))
			{
				return JSAN.Reflect(reflects, object);
			}
			else
			{
				return JSON.Reflect(reflects, object);
			}
		}
		else
		{
			if (object == null //
					|| object instanceof String || object instanceof Boolean || object instanceof Number //
					|| object instanceof JSON || object instanceof java.util.Date || object instanceof Calendar //
					|| object instanceof Character || object instanceof Function || object instanceof Quotation)
			{
				return object;
			}
			else if (object instanceof CharSequence)
			{
				return object.toString();
			}
			else if (object instanceof java.sql.Clob)
			{
				try
				{
					return Tools.readerToStringBuilder(((java.sql.Clob) object).getCharacterStream()).toString();
				}
				catch (Exception e)
				{
				}
			}
			else if (IsArray(object) || object instanceof Iterable)
			{
				return JSAN.Reflect(reflects, object);
			}
			else
			{
				return JSON.Reflect(reflects, object);
			}
		}
		return null;
	}

	private Map<String, Object>					object;

	private JSON								outer;

	private String								entry;

	private transient Map<Class<?>, Object>		reflects;

	private transient Map<Class<?>, Object>		projects;

	private transient boolean					projectStrict	= false;

	private transient Map<Object, Transform<?>>	transforms;

	public JSON()
	{
		object(new LinkedHashMap<String, Object>());
	}

	public Iterable<Object> asIterable()
	{
		return (Iterable<Object>) this;
	}

	public JSON asJSON()
	{
		return this;
	}

	@SuppressWarnings("unchecked")
	public <V extends Object> Map<String, V> asMap()
	{
		return (Map<String, V>) this;
	}

	@SuppressWarnings("unchecked")
	public <E> E attr(String key)
	{
		try
		{
			return (E) this.get(key);
		}
		catch (ClassCastException e)
		{
			return null;
		}
	}

	public JSON attr(String key, Object value)
	{
		this.put(key, value);
		return this;
	}

	public JSON attrAll(Map<String, ?> map)
	{
		this.putAll(map);
		return this;
	}

	public BigDecimal attrBigDecimal(String key)
	{
		return attrCast(key, BigDecimal.class);
	}

	public BigInteger attrBigInteger(String key)
	{
		return attrCast(key, BigInteger.class);
	}

	public Boolean attrBoolean(String key)
	{
		return attrCast(key, Boolean.class);
	}

	public Byte attrByte(String key)
	{
		return attrCast(key, Byte.class);
	}

	public Calendar attrCalendar(String key)
	{
		return attrCast(key, Calendar.class);
	}

	public <E> E attrCast(String key, Class<E> cls)
	{
		return CastAs(this.valObject(key), cls);
	}

	public Character attrCharacter(String key)
	{
		return attrCast(key, Character.class);
	}

	public Date attrDate(String key)
	{
		return attrCast(key, Date.class);
	}

	public Double attrDouble(String key)
	{
		return attrCast(key, Double.class);
	}

	public Float attrFloat(String key)
	{
		return attrCast(key, Float.class);
	}

	public Function attrFunction(String key)
	{
		return attrCast(key, Function.class);
	}

	public <E> E attrIfAbsent(String key, Class<E> type, Mapper<String, E> map)
	{
		E val = this.attrCast(key, type);
		if (val == null)
		{
			try
			{
				this.attr(key, val = map.map(key));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		return val;
	}

	public <E> E attrIfPresent(String key, Class<E> type, StatefulMapper<String, E, E> map)
	{
		E val = this.attrCast(key, type);
		if (val != null)
		{
			try
			{
				this.attr(key, val = map.map(key, val));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		return val;
	}

	public Integer attrInteger(String key)
	{
		return attrCast(key, Integer.class);
	}

	public JSAN attrJSAN(String key)
	{
		return attrCast(key, JSAN.class);
	}

	public JSON attrJSON(String key)
	{
		return attrCast(key, JSON.class);
	}

	public Long attrLong(String key)
	{
		return attrCast(key, Long.class);
	}

	public <E> E attrMerge(String key, Class<E> type, StatefulMapper<String, E, E> map)
	{
		E val = this.attrCast(key, type);
		try
		{
			this.attr(key, val = map.map(key, val));
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		return val;
	}

	public Object attrObject(String key)
	{
		return attr(key);
	}

	public Short attrShort(String key)
	{
		return attrCast(key, Short.class);
	}

	public String attrString(String key)
	{
		return attrCast(key, String.class);
	}

	public Time attrTime(String key)
	{
		return attrCast(key, Time.class);
	}

	public Timestamp attrTimestamp(String key)
	{
		return attrCast(key, Timestamp.class);
	}

	public JSON clean()
	{
		this.clear();
		return this;
	}

	@Override
	public void clear()
	{
		rescind();
		object().clear();
	}

	@Override
	public JSON clone()
	{
		return new JSON().templates(this).clone(this);
	}

	@SuppressWarnings("unchecked")
	protected <T extends JSON> T clone(JSON source)
	{
		outer(source.outer()).entry(source.entry());

		String key = null;
		Object object = null;
		Hierarchical hirch = null;

		for (Map.Entry<String, Object> entry : source.entrySet())
		{
			key = entry.getKey();
			object = entry.getValue();

			if ((hirch = JSON.AsHierarchical(object)) != null)
			{
				object = hirch.clone().outer(this).entry(key);
			}

			this.put(key, object);
		}

		return (T) this;
	}

	@Override
	public boolean containsKey(Object key)
	{
		return object().containsKey(key != null ? key.toString() : null);
	}

	@Override
	public boolean containsValue(Object value)
	{
		return object().containsValue(value);
	}

	@Override
	public Context context()
	{
		JSON outer = this;

		while (outer.outer() != null)
		{
			outer = outer.outer();

			if (outer == this)
			{ // Avoid loop reference.
				break;
			}
		}

		return AsContext(outer);
	}

	@Override
	public String entry()
	{
		return entry;
	}

	@Override
	public JSON entry(String entry)
	{
		this.entry = entry;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Map.Entry<String, Object>> entrySet()
	{
		return new EntrySet(object().entrySet());
	}

	@Override
	public boolean equals(Object o)
	{
		boolean is = false;

		if (this == o)
		{
			is = true;
		}
		else if (o instanceof JSON)
		{
			JSON that = (JSON) o;

			if (this.size() == that.size())
			{
				is = true;

				for (String key : this.keySet())
				{
					if (!Tools.equals(this.valObject(key), that.valObject(key)))
					{
						is = false;
						break;
					}
				}
			}
		}

		return is;
	}

	@Override
	protected void finalize() throws Throwable
	{
		outer(null).entry(null).clear();
		super.finalize();
	}

	public Iterable<Object> find(JSON.Path path)
	{
		return path.find(this);
	}

	public Iterable<Object> find(String path)
	{
		return this.find(Path(path));
	}

	public JSON flatten()
	{
		return this.flatten(null, new JSON());
	}

	protected JSON flatten(String path, JSON res)
	{
		if (IsJSAN(this))
		{
			return this;
		}
		Object val = null;
		for (String key : this.keySet())
		{
			val = this.val(key);
			if (IsPlainJSON(val))
			{
				((JSON) val).flatten((path != null ? path + '.' : "") + key, res);
			}
			else
			{
				res.attr((path != null ? path + '.' : "") + key, val);
			}
		}
		return res;
	}

	@Override
	public Object get(Object key)
	{
		return Quote(this.got(key));
	}

	public Object got(Object key)
	{
		return object().get(key);
	}

	public boolean has(String entry)
	{
		try
		{
			return object().containsKey(entry);
		}
		catch (NullPointerException e)
		{
			return false;
		}
	}

	public boolean hasVal(Object value)
	{
		boolean has = false;

		for (String key : this.keySet())
		{
			if (Tools.equals(value, this.valObject(key)))
			{
				has = true;
				break;
			}
		}

		return has;
	}

	public boolean hasVal(Object value, Comparator<Object> cmp)
	{
		boolean has = false;

		if (cmp == null)
		{
			has = hasVal(value);
		}
		else
		{
			for (String key : this.keySet())
			{
				if (cmp.compare(value, this.valObject(key)) == 0)
				{
					has = true;
					break;
				}
			}
		}

		return has;
	}

	@Override
	public boolean isEmpty()
	{
		return object().isEmpty();
	}

	@Override
	public ValueIterator<Object> iterator()
	{
		return new ValueIterator<Object>();
	}

	/**
	 * Fetch the iterator which would iterate all the elements in this JSAN that
	 * could be cast to the given class.
	 * 
	 * @param cls
	 *            The target class, null means iterate all.
	 * @return An iterator.
	 */
	public <T> ValueIterator<T> iterator(Class<T> cls)
	{
		return new ValueIterator<T>(cls);
	}

	/**
	 * Fetch the iterator which would iterate all the elements in this JSAN
	 * start with the given key.
	 * 
	 * @param key
	 *            From where the iteration would be started.
	 * @return An iterator.
	 */
	public ValueIterator<Object> iterator(String key)
	{
		return iterator().reset(key);
	}

	/**
	 * Fetch the iterator which would iterate all the elements in this JSAN that
	 * has been transformed to the target class with the given Transform.
	 * 
	 * @param tran
	 *            The transform which would transform each element.
	 * @return An iterator.
	 */
	public <T> ValueIterator<T> iterator(Transform<T> tran)
	{
		return new ValueIterator<T>(tran);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<String> keySet()
	{
		return new KeySet(object().keySet());
	}

	protected Map<String, Object> object()
	{
		return object;
	}

	protected JSON object(Map<String, Object> object)
	{
		if (object != null)
		{
			this.object = object;
		}
		return this;
	}

	@Override
	public JSON outer()
	{
		return outer;
	}

	@Override
	public JSON outer(JSON outer)
	{
		this.outer = outer;
		return this;
	}

	public Set<Pair> pairs()
	{
		return pairs((Set<Pair>) null);
	}

	public JSON pairs(Iterable<? extends Object> pairs)
	{
		Object key = null;
		Object value = null;
		int index = 0;
		for (Object o : pairs)
		{
			if (index % 2 == 0)
			{
				key = o;
			}
			else
			{
				value = o;
				if (key != null)
				{
					this.attr(key.toString(), value);
				}
				key = null;
				value = null;
			}
			index++;
		}
		if (key != null)
		{
			this.attr(key.toString(), value);
		}
		return this;
	}

	public JSON pairs(JSAN jsan)
	{
		return pairs((Iterable<? extends Object>) jsan);
	}

	public JSON pairs(JSON json)
	{
		return pairs((Map<? extends String, ? extends Object>) json);
	}

	public JSON pairs(Map<? extends String, ? extends Object> map)
	{
		this.putAll(map);
		return this;
	}

	public JSON pairs(Object... pairs)
	{
		Object key = null;
		for (int i = 0, n = pairs.length - 1; i < n; i += 2)
		{
			key = pairs[i];
			if (key != null)
			{
				this.attr(key.toString(), pairs[i + 1]);
			}
		}
		return this;
	}

	public Set<Pair> pairs(Set<Pair> result)
	{
		if (result == null)
		{
			result = new LinkedHashSet<Pair>();
		}

		for (String key : this.keySet())
		{
			result.add(new Pair(key));
		}

		return result;
	}

	public <E> E project(Class<E> cls)
	{
		E object = Project(cls, this);

		if (object != null)
		{
			object = this.project(object);
		}

		return object;
	}

	public <E> E project(E object)
	{
		return Project(object, projects(), this);
	}

	public Map<Class<?>, Object> projects()
	{
		return projects;
	}

	public JSON projects(Class<?> cls, Iterable<?> fields)
	{
		if (cls != null && fields != null)
		{
			Map<String, Object> project = new LinkedHashMap<String, Object>();

			for (Object name : fields)
			{
				if (name != null)
				{
					project.put(name.toString(), name);
				}
			}

			projects(cls, project);
		}
		return this;
	}

	public JSON projects(Class<?> cls, JSAN project)
	{
		projects(cls, (Iterable<?>) project);
		return this;
	}

	public JSON projects(Class<?> cls, JSON project)
	{
		if (cls != null && project != null)
		{
			Map<String, Object> map = new LinkedHashMap<String, Object>();

			for (Pair pair : project.pairs())
			{
				if (pair.getKey() != null && pair.getValue() != null)
				{
					map.put(pair.getKey(), pair.getValue());
				}
			}

			projects(cls, map);
		}
		return this;
	}

	public JSON projects(Class<?> cls, Map<String, Object> project)
	{
		if (cls != null && project != null)
		{
			projectsSingleton();
			projects().put(cls, project);
		}
		return this;
	}

	public JSON projects(Class<?> cls, String... fields)
	{
		if (cls != null && fields != null)
		{
			Map<String, Object> project = new LinkedHashMap<String, Object>();

			for (String name : fields)
			{
				if (name != null)
				{
					project.put(name, name);
				}
			}

			projects(cls, project);
		}
		return this;
	}

	public <T> JSON projects(Class<T> cls, JSON.Projector<T> projector)
	{
		if (cls != null && projector != null)
		{
			projectsSingleton();
			projects().put(cls, projector);
		}
		return this;
	}

	public JSON projects(JSON json)
	{
		if (json != null)
		{
			projects(json.projects());
			projectStrict(json.projectStrict());
		}
		return this;
	}

	public JSON projects(Map<Class<?>, Object> projects)
	{
		this.projects = projects;
		return this;
	}

	public JSON projectsRemove(Class<?> cls)
	{
		if (cls != null && projects() != null)
		{
			projects().remove(cls);
		}
		return this;
	}

	protected JSON projectsSingleton()
	{
		if (projects() == null)
		{
			projects(new LinkedHashMap<Class<?>, Object>());
		}
		return this;
	}

	public boolean projectStrict()
	{
		return projectStrict;
	}

	public JSON projectStrict(boolean strict)
	{
		this.projectStrict = strict;
		return this;
	}

	public JSON projectStrict(JSON json)
	{
		if (json != null)
		{
			projectStrict(json.projectStrict());
		}
		return this;
	}

	@Override
	public Object put(String key, Object value)
	{
		Object old = null;

		if (key != null)
		{
			old = this.got(key);
			// Clear the old hierarchical relation.
			Hierarchical hirch = AsHierarchical(old);
			if (hirch != null && hirch.outer() == this)
			{
				hirch.outer(null).entry(null);
			}

			Transform<?> transform = this.transformOf(key, value);
			if (transform != null)
			{
				value = transform.transform(this, key, value);
			}

			value = ValueOf(value, reflects());

			hirch = AsHierarchical(value);
			if (hirch != null)
			{
				// Build up new hierarchical relation.
				JSON formalOuter = hirch.outer();
				String formalEntry = hirch.entry();
				JSON formalContext = hirch.context();

				hirch.outer(this).entry(key);

				// If in a Context.
				if (formalOuter != null && IsContext(formalContext) && IsJSON(hirch))
				{ // The formal outer would quote the
					// value at its new location.
					if (formalContext == this.context())
					{ // A quote would be
						// made only in the
						// same Context.
						formalOuter.put(formalEntry, AsJSON(hirch).quote());
					}
				}
			}

			object().put(key, value);
		}

		return old;
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> map)
	{
		for (Map.Entry<?, ?> entry : map.entrySet())
		{
			this.put((String) entry.getKey(), entry.getValue());
		}
	}

	@SuppressWarnings("unchecked")
	public <E extends Object, T extends Map<String, E>> T putTo(T map)
	{
		if (map != null)
		{
			for (Pair pair : this.pairs())
			{
				try
				{
					map.put(pair.getKey(), (E) pair.getValue());
				}
				catch (ClassCastException e)
				{
				}
			}
		}
		return map;
	}

	public <E extends Object, T extends Map<String, E>> T putTo(T map, Class<E> cls)
	{
		if (cls == null)
		{
			return putTo(map);
		}
		else
		{
			if (map != null)
			{
				for (Pair pair : this.pairs())
				{
					try
					{
						map.put(pair.getKey(), cls.cast(pair.getValue()));
					}
					catch (ClassCastException e)
					{
					}
				}
			}
			return map;
		}
	}

	public <E extends Object, T extends Map<String, E>> T putTo(T map, Mapper<Object, E> mapper)
	{
		if (mapper == null)
		{
			return putTo(map);
		}
		else
		{
			if (map != null)
			{
				for (Pair pair : this.pairs())
				{
					try
					{
						map.put(pair.getKey(), mapper.map(pair.getValue()));
					}
					catch (Exception e)
					{
					}
				}
			}
			return map;
		}
	}

	public <E extends Object, T extends Map<String, E>> T putTo(T map, Transform<E> transform)
	{
		if (transform == null)
		{
			return putTo(map);
		}
		else
		{
			if (map != null)
			{
				for (Pair pair : this.pairs())
				{
					try
					{
						map.put(pair.getKey(), transform.transform(this, pair.getKey(), pair.getValue()));
					}
					catch (Exception e)
					{
					}
				}
			}
			return map;
		}
	}

	public Quotation quote()
	{
		return new Quotation(Quotation.Quote(this)).outer(this.outer()).entry(this.entry());
	}

	public Quotation quote(String entry)
	{
		String quote = Quotation.Quote(this);
		quote += Quotation.NESTED_ATTRIBUTE_BEGIN;
		if (!JSON.IsJSAN(this))
		{
			quote += Quotation.NESTED_ATTRIBUTE_QUOTE;
		}
		quote += entry;
		if (!JSON.IsJSAN(this))
		{
			quote += Quotation.NESTED_ATTRIBUTE_QUOTE;
		}
		quote += Quotation.NESTED_ATTRIBUTE_END;
		return new Quotation(quote).outer(this).entry(entry);
	}

	public JSON reflect(Object object)
	{
		JSON.Reflect(this, this.reflects(), object);
		return this;
	}

	public JSON reflect(Object object, Iterable<?> fields)
	{
		JSON.Reflect(this, object, fields);
		return this;
	}

	public JSON reflect(Object object, JSON reflect)
	{
		JSON.Reflect(this, object, reflect);
		return this;
	}

	public <T> JSON reflect(Object object, JSON.Reflector<T> reflector)
	{
		JSON.Reflect(this, object, reflector);
		return this;
	}

	public JSON reflect(Object object, Map<String, ?> reflect)
	{
		JSON.Reflect(this, object, reflect);
		return this;
	}

	public JSON reflect(Object object, String... fields)
	{
		JSON.Reflect(this, object, fields);
		return this;
	}

	public Object reflectOfClass(Class<?> cls)
	{
		Object reflect = null;

		if (cls != null && reflects() != null)
		{
			reflect = reflects().get(cls);
		}

		return reflect;
	}

	public Object reflectOfObject(Object object)
	{
		Object reflect = null;

		if (object != null && reflects() != null)
		{
			for (Class<?> cls : reflects().keySet())
			{
				if (cls != null && cls.isInstance(object) && (reflect = reflectOfClass(cls)) != null)
				{
					break;
				}
			}
		}

		return reflect;
	}

	public Map<Class<?>, Object> reflects()
	{
		return reflects;
	}

	public JSON reflects(Class<?> cls, JSAN reflect)
	{
		if (cls != null && reflect != null)
		{
			reflectsSingleton();
			reflects().put(cls, reflect);
		}
		return this;
	}

	public JSON reflects(Class<?> cls, JSON reflect)
	{
		if (cls != null && reflect != null)
		{
			reflectsSingleton();
			reflects().put(cls, reflect);
		}
		return this;
	}

	public <T> JSON reflects(Class<T> cls, JSAN.Reflector<T> reflector)
	{
		if (cls != null && reflector != null)
		{
			reflectsSingleton();
			reflects().put(cls, reflector);
		}
		return this;
	}

	public <T> JSON reflects(Class<T> cls, JSON.Reflector<T> reflector)
	{
		if (cls != null && reflector != null)
		{
			reflectsSingleton();
			reflects().put(cls, reflector);
		}
		return this;
	}

	public JSON reflects(JSON json)
	{
		if (json != null)
		{
			reflects(json.reflects());
		}
		return this;
	}

	public JSON reflects(Map<Class<?>, Object> reflects)
	{
		this.reflects = reflects;
		return this;
	}

	public JSON reflectsJSAN(Class<?> cls, Iterable<?> fields)
	{
		if (cls != null && fields != null)
		{
			JSAN reflect = new JSAN();

			for (Object field : fields)
			{
				if (field != null)
				{
					reflect.add(field);
				}
			}
			reflects(cls, reflect);
		}
		return this;
	}

	public JSON reflectsJSAN(Class<?> cls, JSAN fields)
	{
		return reflectsJSAN(cls, (Iterable<?>) fields);
	}

	public JSON reflectsJSAN(Class<?> cls, JSON fields)
	{
		if (cls != null && fields != null)
		{
			JSAN reflect = new JSAN();

			for (Pair pair : fields.pairs())
			{
				if (pair.getKey() != null && pair.getValue() != null)
				{
					reflect.put(pair.getKey(), pair.getValue());
				}
			}
			reflects(cls, reflect);
		}
		return this;
	}

	public JSON reflectsJSAN(Class<?> cls, Map<String, ?> fields)
	{
		if (cls != null && fields != null)
		{
			JSAN reflect = new JSAN();

			for (Entry<String, ?> entry : fields.entrySet())
			{
				if (entry.getKey() != null && entry.getValue() != null)
				{
					reflect.put(entry.getKey(), entry.getValue());
				}
			}
			reflects(cls, reflect);
		}
		return this;
	}

	public JSON reflectsJSAN(Class<?> cls, String... fields)
	{
		if (cls != null && fields != null)
		{
			JSAN reflect = new JSAN();

			for (String field : fields)
			{
				if (field != null)
				{
					reflect.add(field);
				}
			}

			reflects(cls, reflect);
		}
		return this;
	}

	public JSON reflectsJSON(Class<?> cls, Iterable<?> fields)
	{
		if (cls != null && fields != null)
		{
			JSON reflect = new JSON();

			for (Object field : fields)
			{
				if (field != null)
				{
					reflect.put(field.toString(), field);
				}
			}
			reflects(cls, reflect);
		}
		return this;
	}

	public JSON reflectsJSON(Class<?> cls, JSAN fields)
	{
		return reflectsJSON(cls, (Iterable<?>) fields);
	}

	public JSON reflectsJSON(Class<?> cls, JSON fields)
	{
		if (cls != null && fields != null)
		{
			JSON reflect = new JSON();

			for (Pair pair : fields.pairs())
			{
				if (pair.getKey() != null && pair.getValue() != null)
				{
					reflect.put(pair.getKey(), pair.getValue());
				}
			}
			reflects(cls, reflect);
		}
		return this;
	}

	public JSON reflectsJSON(Class<?> cls, Map<String, ?> fields)
	{
		if (cls != null && fields != null)
		{
			JSON reflect = new JSON();

			for (Entry<String, ?> entry : fields.entrySet())
			{
				if (entry.getKey() != null && entry.getValue() != null)
				{
					reflect.put(entry.getKey(), entry.getValue());
				}
			}
			reflects(cls, reflect);
		}
		return this;
	}

	public JSON reflectsJSON(Class<?> cls, String... fields)
	{
		if (cls != null && fields != null)
		{
			JSON reflect = new JSON();

			for (String field : fields)
			{
				if (field != null)
				{
					reflect.put(field, field);
				}
			}

			reflects(cls, reflect);
		}
		return this;
	}

	public JSON reflectsRemove(Class<?> cls)
	{
		if (cls != null && reflects() != null)
		{
			reflects().remove(cls);
		}
		return this;
	}

	protected JSON reflectsSingleton()
	{
		if (reflects() == null)
		{
			reflects(new LinkedHashMap<Class<?>, Object>());
		}
		return this;
	}

	@Override
	public Object remove(Object key)
	{
		Object value = object().remove(key);

		Hierarchical hirch = AsHierarchical(value);

		if (hirch != null)
		{
			if (hirch.outer() == this)
			{
				hirch.outer(null).entry(null);
			}
		}

		return value;
	}

	public JSON removeAll()
	{
		return clean();
	}

	public JSON removeAll(Iterable<? extends Object> keys)
	{
		for (Object key : keys)
		{
			this.remove(key);
		}
		return this;
	}

	public JSON removeAll(Map<? extends Object, ? extends Object> map)
	{
		return this.removeAll(map.keySet());
	}

	public JSON removeAll(String... keys)
	{
		for (String key : keys)
		{
			this.remove(key);
		}
		return this;
	}

	protected JSON rescind()
	{
		Hierarchical hirch = null;
		for (Object o : values())
		{
			if ((hirch = AsHierarchical(o)) != null)
			{
				if (hirch.outer() == this)
				{
					hirch.outer(null).entry(null);
				}
			}
		}
		return this;
	}

	@Override
	public int size()
	{
		return object().size();
	}

	public JSON swap()
	{
		return swap(null);
	}

	public JSON swap(JSON json)
	{
		if (json == null)
		{
			json = new JSON().templates(this);
		}

		for (String key : this.keySet())
		{
			json.put(String.valueOf(this.valObject(key)), key);
		}

		return json;
	}

	public JSON templates(JSON json)
	{
		return this.transforms(json) //
				.reflects(json) //
				.projects(json) //
				.projectStrict(json);
	}

	public JSAN toJSAN()
	{
		JSAN jsan = new JSAN().templates(this);
		jsan.addAll(this.values());
		return jsan;
	}

	@Override
	public String toString()
	{
		return Serialize(this, new StringBuilder(), -1).toString();
	}

	public String toString(int indent)
	{
		return Serialize(this, new StringBuilder(), indent).insert(0, Tools.repeat(DEFAULT_LINE_INDENT, indent))
				.toString();
	}

	public Transform<?> transformOf(String entry, Object value)
	{
		Transform<?> transform = null;

		if (transforms() != null && !transforms().isEmpty())
		{
			if (entry != null)
			{
				transform = transforms().get(entry);
			}

			if (transform == null && value != null)
			{
				for (Entry<Object, Transform<?>> e : transforms().entrySet())
				{
					if (e.getKey() instanceof Class)
					{
						if (((Class<?>) e.getKey()).isInstance(value))
						{
							transform = e.getValue();
							break;
						}
					}
				}
			}
		}

		return transform;
	}

	public Map<Object, Transform<?>> transforms()
	{
		return transforms;
	}

	public JSON transforms(JSON json)
	{
		if (json != null)
		{
			transforms(json.transforms());
		}
		return this;
	}

	public JSON transforms(Map<Object, Transform<?>> transforms)
	{
		this.transforms = transforms;
		return this;
	}

	public JSON transforms(Object entry, Transform<?> transform)
	{
		if (transform != null && (entry instanceof String || entry instanceof Class))
		{
			transformsSingleton();
			transforms().put(entry, transform);
		}
		return this;
	}

	public JSON transformsRemove(Object entry)
	{
		if (entry != null && transforms() != null)
		{
			transforms().remove(entry);
		}
		return this;
	}

	protected JSON transformsSingleton()
	{
		if (transforms() == null)
		{
			transforms(new LinkedHashMap<Object, Transform<?>>());
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public <E> E val(String key)
	{
		try
		{
			return (E) this.attr(key);
		}
		catch (ClassCastException e)
		{
			return null;
		}
	}

	public <E> E val(String key, E defaultValue)
	{
		E val = val(key);
		return val == null ? defaultValue : val;
	}

	public BigDecimal valBigDecimal(String key)
	{
		return CastToBigDecimal(this.attr(key));
	}

	public BigDecimal valBigDecimal(String key, double defaultValue)
	{
		BigDecimal val = valBigDecimal(key);
		return val == null ? new BigDecimal(defaultValue) : val;
	}

	public BigDecimal valBigDecimal(String key, String defaultValue)
	{
		BigDecimal val = valBigDecimal(key);
		return val == null ? new BigDecimal(defaultValue) : val;
	}

	public BigInteger valBigInteger(String key)
	{
		return CastToBigInteger(this.attr(key));
	}

	public BigInteger valBigInteger(String key, long defaultValue)
	{
		BigInteger val = valBigInteger(key);
		return val == null ? new BigInteger(String.valueOf(defaultValue)) : val;
	}

	public BigInteger valBigInteger(String key, String defaultValue)
	{
		BigInteger val = valBigInteger(key);
		return val == null ? new BigInteger(defaultValue) : val;
	}

	public Boolean valBoolean(String key)
	{
		return CastToBoolean(this.attr(key));
	}

	public Boolean valBoolean(String key, Boolean defaultValue)
	{
		Boolean val = valBoolean(key);
		return val == null ? defaultValue : val;
	}

	public Byte valByte(String key)
	{
		return CastToByte(this.attr(key));
	}

	public Byte valByte(String key, Byte defaultValue)
	{
		Byte val = valByte(key);
		return val == null ? defaultValue : val;
	}

	public Calendar valCalendar(String key)
	{
		return CastToCalendar(this.attr(key));
	}

	public Calendar valCalendar(String key, Calendar defaultValue)
	{
		Calendar val = valCalendar(key);
		return val == null ? defaultValue : val;
	}

	public Calendar valCalendar(String key, long defaultValue)
	{
		Calendar val = valCalendar(key);

		if (val == null)
		{
			val = new GregorianCalendar();
			val.setTimeInMillis(defaultValue);
		}

		return val;
	}

	public <E> E valCast(String key, Class<E> cls)
	{
		return Tools.castTo(this.valObject(key), cls);
	}

	public <E> E valCast(String key, Class<E> cls, E defaultValue)
	{
		E val = valCast(key, cls);
		return val == null ? defaultValue : val;
	}

	public Character valCharacter(String key)
	{
		return CastToCharacter(this.attr(key));
	}

	public Character valCharacter(String key, Character defaultValue)
	{
		Character val = valCharacter(key);
		return val == null ? defaultValue : val;
	}

	public Date valDate(String key)
	{
		return CastToDate(this.attr(key));
	}

	public Date valDate(String key, Date defaultValue)
	{
		Date val = valDate(key);
		return val == null ? defaultValue : val;
	}

	public Date valDate(String key, long defaultValue)
	{
		Date val = valDate(key);
		return val == null ? new Date(defaultValue) : val;
	}

	public Double valDouble(String key)
	{
		return CastToDouble(this.attr(key));
	}

	public Double valDouble(String key, Double defaultValue)
	{
		Double val = valDouble(key);
		return val == null ? defaultValue : val;
	}

	public Float valFloat(String key)
	{
		return CastToFloat(this.attr(key));
	}

	public Float valFloat(String key, Float defaultValue)
	{
		Float val = valFloat(key);
		return val == null ? defaultValue : val;
	}

	public Function valFunction(String key)
	{
		return CastToFunction(this.attr(key));
	}

	public Function valFunction(String key, Function defaultValue)
	{
		Function val = valFunction(key);
		return val == null ? defaultValue : val;
	}

	public Function valFunction(String key, String defaultValue)
	{
		Function val = valFunction(key);
		return val == null ? new Function(defaultValue) : val;
	}

	public <E> E valIfAbsent(String key, Class<E> type, Mapper<String, E> map)
	{
		E val = this.valCast(key, type);
		if (val == null)
		{
			try
			{
				this.attr(key, val = map.map(key));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		return val;
	}

	public <E> E valIfPresent(String key, Class<E> type, StatefulMapper<String, E, E> map)
	{
		E val = this.valCast(key, type);
		if (val != null)
		{
			try
			{
				this.attr(key, val = map.map(key, val));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		return val;
	}

	public Integer valInteger(String key)
	{
		return CastToInteger(this.attr(key));
	}

	public Integer valInteger(String key, Integer defaultValue)
	{
		Integer val = valInteger(key);
		return val == null ? defaultValue : val;
	}

	public JSAN valJSAN(String key)
	{
		return CastToJSAN(this.attr(key));
	}

	public JSAN valJSAN(String key, boolean newIfNull)
	{
		JSAN val = valJSAN(key);
		return val == null && newIfNull ? new JSAN() : val;
	}

	public JSAN valJSAN(String key, JSAN defaultValue)
	{
		JSAN val = valJSAN(key);
		return val == null ? defaultValue : val;
	}

	public JSON valJSON(String key)
	{
		return CastToJSON(this.attr(key));
	}

	public JSON valJSON(String key, boolean newIfNull)
	{
		JSON val = valJSON(key);
		return val == null && newIfNull ? new JSON() : val;
	}

	public JSON valJSON(String key, JSON defaultValue)
	{
		JSON val = valJSON(key);
		return val == null ? defaultValue : val;
	}

	public Long valLong(String key)
	{
		return CastToLong(this.attr(key));
	}

	public Long valLong(String key, Long defaultValue)
	{
		Long val = valLong(key);
		return val == null ? defaultValue : val;
	}

	public <E> E valMerge(String key, Class<E> type, StatefulMapper<String, E, E> map)
	{
		E val = this.valCast(key, type);
		try
		{
			this.attr(key, val = map.map(key, val));
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		return val;
	}

	public Object valObject(String key)
	{
		return attr(key);
	}

	public Object valObject(String key, Object defaultValue)
	{
		Object val = valObject(key);
		return val == null ? defaultValue : val;
	}

	public Collection<Object> vals()
	{
		return vals(new LinkedList<Object>());
	}

	public Collection<Object> vals(Collection<Object> collection)
	{
		if (collection == null)
		{
			collection = new LinkedList<Object>();
		}

		for (String key : keySet())
		{
			collection.add(val(key));
		}

		return collection;
	}

	public Short valShort(String key)
	{
		return CastToShort(this.attr(key));
	}

	public Short valShort(String key, Short defaultValue)
	{
		Short val = valShort(key);
		return val == null ? defaultValue : val;
	}

	public String valString(String key)
	{
		return CastToString(this.attr(key));
	}

	public String valString(String key, String defaultValue)
	{
		String val = valString(key);
		return val == null ? defaultValue : val;
	}

	public Time valTime(String key)
	{
		return CastToTime(this.attr(key));
	}

	public Time valTime(String key, long defaultValue)
	{
		Time val = valTime(key);
		return val == null ? new Time(defaultValue) : val;
	}

	public Time valTime(String key, Time defaultValue)
	{
		Time val = valTime(key);
		return val == null ? defaultValue : val;
	}

	public Timestamp valTimestamp(String key)
	{
		return CastToTimestamp(this.attr(key));
	}

	public Timestamp valTimestamp(String key, long defaultValue)
	{
		Timestamp val = valTimestamp(key);
		return val == null ? new Timestamp(defaultValue) : val;
	}

	public Timestamp valTimestamp(String key, Timestamp defaultValue)
	{
		Timestamp val = valTimestamp(key);
		return val == null ? defaultValue : val;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<Object> values()
	{
		return new ValuesCollection(object().values());
	}
}
