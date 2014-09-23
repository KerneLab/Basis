package org.kernelab.basis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class JSON implements Map<String, Object>, Serializable, Hierarchical
{
	private static class ArrayLengthReverseComparator<T> implements Comparator<T[]>, Serializable
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= -6259816417783534899L;

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

		public static final Pattern		VAR_ENTRY_PATTERN	= Pattern.compile(
																	"^\\s*?(var\\s+)?\\s*?(\\w+)\\s*?=\\s*(.*);?\\s*$",
																	Pattern.DOTALL);

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
			return DEFINE_PATTERN.matcher(expression()).replaceFirst(
					"$1 " + (name == null ? name() : name.trim()) + "$3$4$5");
		}
	}

	/**
	 * A class to describe Array object using JSON format.
	 * 
	 * @author Dilly King
	 * 
	 */
	public static class JSAN extends JSON implements Iterable<Object>
	{
		private static class ArrayIndexComparator implements Comparator<String>, Serializable
		{
			/**
			 * 
			 */
			private static final long	serialVersionUID	= -3834859681987586236L;

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

		protected class ArrayIterator implements Iterator<Object>, Serializable
		{
			/**
			 * 
			 */
			private static final long		serialVersionUID	= 7218728241929428448L;

			private LinkedList<String>		keys				= new LinkedList<String>();

			private ListIterator<String>	iter;

			private String					key;

			protected ArrayIterator()
			{
				keys.addAll(keySet());
				iter = keys.listIterator();
			}

			public ArrayIterator(int sequence)
			{
				this();
				reset(sequence);
			}

			public ArrayIterator(String key)
			{
				this();
				reset(key);
			}

			protected Object get()
			{
				return JSAN.this.attr(key());
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
				int index = LAST;

				try
				{
					index = Integer.parseInt(key);

					if (index < 0)
					{
						index = LAST;
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

			protected String key()
			{
				return key;
			}

			protected LinkedList<String> keys()
			{
				return keys;
			}

			public Object next()
			{
				key = iter.next();
				return JSAN.this.attr(key);
			}

			public Object previous()
			{
				key = iter.previous();
				return JSAN.this.attr(key);
			}

			public void remove()
			{
				if (key != null)
				{
					JSAN.this.remove(key);
					iter.remove();
					key = null;
				}
			}

			protected ArrayIterator reset(int sequence)
			{
				iter = keys.listIterator(sequence);
				key = null;
				return this;
			}

			protected ArrayIterator reset(String key)
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

				reset(seq);

				return this;
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

			private int	order;

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
		private static final long					serialVersionUID	= 2156642568827950757L;

		public static final int						LAST				= -1;

		private static final Map<Integer, String>	INDEX				= new WeakHashMap<Integer, String>();

		protected static final String Index(int i)
		{
			String index = INDEX.get(i);

			if (index == null)
			{
				index = String.valueOf(i);
				INDEX.put(i, index);
			}

			return index;
		}

		protected static final Integer Index(Object o)
		{
			Integer index = null;

			if (o != null)
			{
				index = Index(o.toString());
			}

			return index;
		}

		protected static final Integer Index(String key)
		{
			Integer index = null;

			if (key != null)
			{
				try
				{
					index = Integer.parseInt(key);

					if (index < 0)
					{
						index = null;
					}
				}
				catch (NumberFormatException e)
				{
				}
			}

			return index;
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
			if (jsan == null)
			{
				jsan = new JSAN();
			}

			if (object != null)
			{
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
					Map<String, ?> obj = (Map<String, ?>) object;

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
								Object key = pair.getValue();

								if (obj.containsKey(key))
								{
									jsan.attr(pair.getKey(), obj.get(key));
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
							jsan.attr(i, Access(object, field.toString()));
						}
						catch (Exception e)
						{
						}
						i++;
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
					target = new JSAN().reflects(source).transformers(source);
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
			return splice(index, 0, collection);
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
			return splice(index, 0, jsan);
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
			return splice(index, 0, array);
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

		@SuppressWarnings("unchecked")
		public <E> E attr(int index)
		{
			E value = null;

			try
			{
				value = (E) Quote(this.get(index));
			}
			catch (ClassCastException e)
			{
			}

			return value;
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

		public Boolean attrBoolean(int index)
		{
			return attrCast(index, Boolean.class);
		}

		public Byte attrByte(int index)
		{
			return attrCast(index, Byte.class);
		}

		public <E> E attrCast(int index, Class<E> cls)
		{
			return CastTo(this.attr(index), cls);
		}

		public Character attrCharacter(int index)
		{
			return attrCast(index, Character.class);
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

		public Short attrShort(int index)
		{
			return attrCast(index, Short.class);
		}

		public String attrString(int index)
		{
			return attrCast(index, String.class);
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
			return new JSAN().reflects(this).transformers(this).clone(this);
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
			return index != null ? array().containsKey(key) : super.containsKey(key);
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

		@Override
		public Set<Map.Entry<String, Object>> entrySet()
		{
			Set<Map.Entry<String, Object>> set = new LinkedHashSet<Map.Entry<String, Object>>();

			set.addAll(array().entrySet());
			set.addAll(super.entrySet());

			return set;
		}

		public boolean equalValues(JSAN jsan)
		{
			return this.size() == jsan.size() && this.containsAll(jsan) && jsan.containsAll(this);
		}

		public Object get(int index)
		{
			return array().get(Index(bound(index)));
		}

		public Object get(Integer index)
		{
			return get((int) index);
		}

		@Override
		public Object get(Object key)
		{
			Object element = null;

			if (key != null)
			{
				if (key instanceof Integer)
				{
					element = this.get((Integer) key);
				}
				else
				{
					Integer index = Index(key.toString());

					if (index == null)
					{
						element = super.get(key);
					}
					else
					{
						element = this.get(Integer.parseInt(key.toString()));
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

		public ArrayIterator iterator()
		{
			return new ArrayIterator("0");
		}

		public ArrayIterator iterator(int sequence)
		{
			return new ArrayIterator(sequence);
		}

		public ArrayIterator iterator(String key)
		{
			return new ArrayIterator(key);
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

		@Override
		public Set<String> keySet()
		{
			Set<String> set = new LinkedHashSet<String>();

			set.addAll(array().keySet());

			set.addAll(super.keySet());

			return set;
		}

		public String lastKeyOf(Object value)
		{
			String key = null;

			if (containsValue(value))
			{
				ArrayIterator iter = new ArrayIterator(size());
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
		public JSAN projects(Class<?> cls, JSON project)
		{
			super.projects(cls, project);
			return this;
		}

		@Override
		public JSAN projects(Class<?> cls, Map<Field, Object> project)
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

			old = this.get(index);
			// Clear the old hierarchical relation.
			Hierarchical hirch = AsHierarchical(old);
			if (hirch != null && hirch.outer() == this)
			{
				hirch.outer(null).entry(null);
			}

			value = ValueOf(value, reflects());

			Transformer transformer = this.transformerOf(key);
			if (transformer != null)
			{
				value = transformer.transform(this, key, value);
			}

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
				jsan = new JSAN().reflects(this).transformers(this);
			}
			else
			{
				jsan.clear();
			}

			start = bound(start);
			end = bound(end);

			ArrayIterator iter = this.iterator(Index(start));

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
			JSAN result = new JSAN().reflects(this).transformers(this);

			int trace = length();
			index = bound(index);
			cover = Tools.limitNumber(cover, 0, trace - index);
			trace--;

			int fills = collection.size();
			int delta = fills - cover;

			ArrayIterator iter = this.iterator(Index(index));

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
			JSAN result = new JSAN().reflects(this).transformers(this);

			int trace = length();
			index = bound(index);
			cover = Tools.limitNumber(cover, 0, trace - index);
			trace--;

			int fills = jsan.size();
			int delta = fills - cover;

			ArrayIterator iter = this.iterator(Index(index));

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
			JSAN result = new JSAN().reflects(this).transformers(this);

			int trace = length();
			index = bound(index);
			cover = Tools.limitNumber(cover, 0, trace - index);
			trace--;

			int fills = objects.length;
			int delta = fills - cover;

			ArrayIterator iter = this.iterator(Index(index));

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

		public Object[] toArray(Object[] array)
		{
			if (array.length < size())
			{
				array = (Object[]) java.lang.reflect.Array.newInstance(Object.class, size());
			}

			int i = 0;
			for (Object o : this)
			{
				array[i++] = o;
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
		public JSAN transformer(String entry, Transformer transformer)
		{
			super.transformer(entry, transformer);
			return this;
		}

		@Override
		public JSAN transformers(JSON json)
		{
			super.transformers(json);
			return this;
		}

		@Override
		public JSAN transformers(Map<String, Transformer> transformers)
		{
			super.transformers(transformers);
			return this;
		}

		@Override
		public JSAN transformersRemove(String entry)
		{
			super.transformersRemove(entry);
			return this;
		}

		@Override
		protected JSAN transformersSingleton()
		{
			super.transformersSingleton();
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
			E val = null;

			Object obj = attr(index);

			try
			{
				val = (E) obj;
			}
			catch (ClassCastException e)
			{
			}

			return val;
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
			return this.attrCast(index, cls);
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
			return attrJSAN(index);
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
			return attrJSON(index);
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

		public Timestamp valTimestamp(int index)
		{
			return CastToTimestamp(this.attr(index));
		}

		public Timestamp valTimestamp(int index, long defaultValue)
		{
			Timestamp val = valTimestamp(index);
			return val == null ? new Timestamp(defaultValue) : val;
		}

		@Override
		public Collection<Object> values()
		{
			Collection<Object> values = new LinkedList<Object>();

			values.addAll(array().values());
			values.addAll(super.values());

			return values;
		}
	}

	public class Pair implements Entry<String, Object>
	{
		private String	key;

		public Pair(String key)
		{
			this.key = key;
		}

		public String getKey()
		{
			return key;
		}

		public Object getValue()
		{
			return attr(key);
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

		public static void main(String[] args) throws Exception
		{
			File file = new File("./dat/test.json");

			BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

			JSON j = null;

			Parser p = new Parser();

			OutputStreamWriter w = new OutputStreamWriter(System.out);

			do
			{
				j = p.parse(r, false).result();

				if (j != null)
				{
					JSON.Serialize(j, w, 0);
				}

				w.flush();

			} while (j != null);

			w.close();

			r.close();
		}

		private StringBuilder	buffer;

		private JSON			object;

		private String			entry;

		private Object			value;

		private int				arrayIndex;

		private int				status;

		private int				curr	= 0;

		private int				nail	= NOT_FOUND;

		private int				tail	= NOT_FOUND;

		private int				jail	= NOT_FOUND;

		private Parser			sub		= null;

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

				if (closeAfterRead)
				{
					reader.close();
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
								if (nail == curr
										&& Function.DEFINE_MARK.equals( //
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

	public static interface Projector<T>
	{
		public T project(T obj, JSON json);
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
				object = outer.get(entry);
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
					i = CheckNext(quote, i, JSON.DualMatchIndex(quote, NESTED_ATTRIBUTE_BEGIN, NESTED_ATTRIBUTE_END, i)) - 1;
					continue i;
				}

				if (c == NESTED_ATTRIBUTE_QUOTE)
				{
					nail = i + 1;
					do
					{
						i = Tools.seekIndex(quote, NESTED_ATTRIBUTE_QUOTE, i + 1);
					} while (quote.charAt(i - 1) == JSON.ESCAPE_CHAR);
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

	public static interface Reflector<T>
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

	public static interface Transformer
	{
		public Object transform(JSON json, String entry, Object value);
	}

	/**
	 * 
	 */
	private static final long						serialVersionUID		= 6090747632739206720L;
	public static final char						OBJECT_BEGIN_CHAR		= '{';

	public static final String						OBJECT_BEGIN_MARK		= String.valueOf(OBJECT_BEGIN_CHAR);
	public static final char						OBJECT_END_CHAR			= '}';

	public static final String						OBJECT_END_MARK			= String.valueOf(OBJECT_END_CHAR);
	public static final char						ARRAY_BEGIN_CHAR		= '[';

	public static final String						ARRAY_BEGIN_MARK		= String.valueOf(ARRAY_BEGIN_CHAR);
	public static final char						ARRAY_END_CHAR			= ']';

	public static final String						ARRAY_END_MARK			= String.valueOf(ARRAY_END_CHAR);
	public static final char						PAIR_CHAR				= ',';

	public static final String						PAIR_MARK				= String.valueOf(PAIR_CHAR);
	public static final char						ATTR_CHAR				= ':';

	public static final String						ATTR_MARK				= String.valueOf(ATTR_CHAR);
	public static final char						QUOTE_CHAR				= '"';

	public static final String						QUOTE_MARK				= String.valueOf(QUOTE_CHAR);
	public static final char						ESCAPE_CHAR				= '\\';

	public static final String						ESCAPE_MARK				= String.valueOf(ESCAPE_CHAR);

	public static final char						COMMENT_CHAR			= '/';

	public static final char						LINE_COMMENT_CHAR		= COMMENT_CHAR;

	public static final char						BLOCK_COMMENT_CHAR		= '*';

	public static final String						BLOCK_COMMENT_END		= BLOCK_COMMENT_CHAR + "" + COMMENT_CHAR;

	public static final int							NOT_FOUND				= -1;

	public static final Object						NOT_A_VALUE				= new String("NOT A VALUE");

	public static final String						NULL_STRING				= "null";

	public static final String						TRUE_STRING				= "true";

	public static final String						FALSE_STRING			= "false";

	public static final char						UNICODE_ESCAPING_CHAR	= 'u';

	public static final int							UNICODE_ESCAPED_LENGTH	= 4;

	public static final int							UNICODE_ESCAPE_RADIX	= 16;

	public static final Map<Character, Character>	ESCAPING_CHAR			= new HashMap<Character, Character>();

	public static final Map<Character, String>		ESCAPED_CHAR			= new HashMap<Character, String>();

	public static String							DEFAULT_LINE_INDENT		= "\t";

	public static String							LINE_WRAP				= "\n";

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

		ESCAPED_CHAR.put('"', "\\\"");
		ESCAPED_CHAR.put('\\', "\\\\");
		ESCAPED_CHAR.put('/', "\\/");
		ESCAPED_CHAR.put('\b', "\\b");
		ESCAPED_CHAR.put('\f', "\\f");
		ESCAPED_CHAR.put('\n', "\\n");
		ESCAPED_CHAR.put('\r', "\\r");
		ESCAPED_CHAR.put('\t', "\\t");
	}

	public static Object Access(Object object, String fieldName) throws Exception
	{
		Object value = null;

		if (object != null && fieldName != null)
		{
			if (JSON.IsJSON(object))
			{
				value = ((JSON) object).attr(fieldName);
			}
			else if (object instanceof Map)
			{
				value = ((Map<?, ?>) object).get(fieldName);
			}
			else
			{
				Class<?> cls = object.getClass();

				String methodName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

				Method method = null;

				try
				{
					method = cls.getMethod("get" + methodName);
				}
				catch (NoSuchMethodException e)
				{
					try
					{
						method = cls.getMethod("is" + methodName);
					}
					catch (NoSuchMethodException ex)
					{
						try
						{
							method = cls.getMethod(fieldName);
						}
						catch (NoSuchMethodException err)
						{
							value = cls.getField(fieldName).get(object);
						}
					}
				}

				if (method != null && method.getParameterTypes().length == 0)
				{
					value = method.invoke(object);
				}
			}
		}

		return value;
	}

	@SuppressWarnings("unchecked")
	public static void Access(Object object, String fieldName, Object value) throws Exception
	{
		if (object != null && fieldName != null)
		{
			if (JSON.IsJSON(object))
			{
				((JSON) object).attr(fieldName, value);
			}
			else if (object instanceof Map)
			{
				((Map<String, Object>) object).put(fieldName, value);
			}
			else
			{
				Class<?> cls = object.getClass();

				Field field = FieldOf(cls, fieldName);

				if (field != null)
				{
					String methodName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

					Method method = null;

					try
					{
						method = cls.getMethod("set" + methodName, field.getType());
					}
					catch (NoSuchMethodException e)
					{
						try
						{
							method = cls.getMethod(fieldName, field.getType());
						}
						catch (NoSuchMethodException ex)
						{
							field.set(object, value);
						}
					}

					if (method != null && method.getParameterTypes().length == 1)
					{
						method.invoke(object, value);
					}
				}
			}
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

	public static <T> T CastTo(Object obj, Class<T> cls)
	{
		T val = null;

		if (obj != null && cls != null)
		{
			try
			{
				val = cls.cast(obj);
			}
			catch (ClassCastException e)
			{
			}
		}

		return val;
	}

	@SuppressWarnings("unused")
	public static <T> Object CastToArray(Object object, Class<T> type, Map<Class<?>, Object> projects)
	{
		Object array = null;

		if (object instanceof Iterable)
		{
			int length = 0;

			if (object instanceof JSAN)
			{
				length = ((JSAN) object).size();
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
				}
				length++;
			}
		}

		return array;
	}

	public static BigDecimal CastToBigDecimal(Object obj)
	{
		BigDecimal val = null;

		try
		{
			val = BigDecimal.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = new BigDecimal(CastToString(obj));
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
	}

	public static Boolean CastToBoolean(Object obj)
	{
		Boolean val = null;

		try
		{
			val = Boolean.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			String str = CastToString(obj);
			if (TRUE_STRING.equals(str) || FALSE_STRING.equals(str))
			{
				val = Boolean.valueOf(str);
			}
		}

		return val;
	}

	public static Byte CastToByte(Object obj)
	{
		Byte val = null;

		try
		{
			val = Byte.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = Byte.valueOf(CastToString(obj));
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
	}

	public static Calendar CastToCalendar(Object obj)
	{
		Calendar val = null;

		Long lon = CastToLong(obj);

		if (lon != null)
		{
			val = new GregorianCalendar();
			val.setTimeInMillis(lon);
		}

		return val;
	}

	public static Character CastToCharacter(Object obj)
	{
		Character val = null;

		try
		{
			val = Character.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = CastToString(obj).charAt(0);
			}
			catch (StringIndexOutOfBoundsException ex)
			{
			}
		}

		return val;
	}

	public static Date CastToDate(Object obj)
	{
		Date val = null;

		Long lon = CastToLong(obj);

		if (lon != null)
		{
			val = new Date(lon);
		}

		return val;
	}

	public static Double CastToDouble(Object obj)
	{
		Double val = null;

		try
		{
			val = Double.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = Double.valueOf(CastToString(obj));
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
	}

	public static Float CastToFloat(Object obj)
	{
		Float val = null;

		try
		{
			val = Float.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = Float.valueOf(CastToString(obj));
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
	}

	public static Function CastToFunction(Object obj)
	{
		return CastTo(obj, Function.class);
	}

	public static Integer CastToInteger(Object obj)
	{
		Integer val = null;

		try
		{
			val = Integer.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = Integer.valueOf(CastToString(obj));
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
	}

	public static JSAN CastToJSAN(Object obj)
	{
		return CastTo(obj, JSAN.class);
	}

	public static JSON CastToJSON(Object obj)
	{
		return CastTo(obj, JSON.class);
	}

	public static Long CastToLong(Object obj)
	{
		Long val = null;

		try
		{
			val = Long.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = Long.valueOf(CastToString(obj));
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
	}

	public static Short CastToShort(Object obj)
	{
		Short val = null;

		try
		{
			val = Short.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = Short.valueOf(CastToString(obj));
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
	}

	public static String CastToString(Object obj)
	{
		String val = null;

		try
		{
			val = String.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				if (obj instanceof java.util.Date)
				{
					val = String.valueOf(((java.util.Date) obj).getTime());
				}
				else if (obj instanceof java.util.Calendar)
				{
					val = String.valueOf(((java.util.Calendar) obj).getTimeInMillis());
				}
				else
				{
					val = obj.toString();
				}
			}
			catch (Exception ex)
			{
			}
		}

		return val;
	}

	public static Time CastToTime(Object obj)
	{
		Time val = null;

		Long lon = CastToLong(obj);

		if (lon != null)
		{
			val = new Time(lon);
		}

		return val;
	}

	public static Timestamp CastToTimestamp(Object obj)
	{
		Timestamp val = null;

		Long lon = CastToLong(obj);

		if (lon != null)
		{
			val = new Timestamp(lon);
		}

		return val;
	}

	public static boolean CharacterNeedToEscape(char c)
	{
		return ESCAPED_CHAR.containsKey(c);
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

	public static String EscapeString(String string)
	{
		StringBuilder buffer = new StringBuilder(string);

		char c;
		String escape;
		for (int i = 0; i < buffer.length(); i++)
		{
			c = buffer.charAt(i);
			if (CharacterNeedToEscape(c))
			{
				buffer.deleteCharAt(i);
				escape = ESCAPED_CHAR.get(c);
				buffer.insert(i, escape);
				i += escape.length() - 1;
			}
		}

		return buffer.toString();
	}

	public static Field FieldOf(Class<?> cls, String name)
	{
		Field field = null;

		if (cls != null)
		{
			try
			{
				field = cls.getDeclaredField(name);
			}
			catch (Exception e)
			{
			}

			if (field == null)
			{
				field = FieldOf(cls.getSuperclass(), name);
			}
		}

		return field;
	}

	public static <T> Collection<Field> FieldsOf(Class<T> cls, Collection<Field> fields)
	{
		if (cls != null)
		{
			if (fields == null)
			{
				fields = new LinkedHashSet<Field>();
			}

			fields = FieldsOf(cls.getSuperclass(), fields);

			for (Field field : cls.getDeclaredFields())
			{
				fields.add(field);
			}
		}

		return fields;
	}

	public static <T> Collection<Field> FieldsOf(T object)
	{
		Collection<Field> fields = null;

		if (object != null)
		{
			fields = FieldsOf(object.getClass(), fields);
		}

		return fields;
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

			if (!Character.isWhitespace(code) && !Character.isSpaceChar(code))
			{
				index = i;
				break;
			}
		}

		return index;
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
		boolean is = true;

		try
		{
			sub.asSubclass(cls);
		}
		catch (ClassCastException e)
		{
			is = false;
		}

		return is;
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
				keys.add(field.getName());
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
		String s = "{\"k\":1}";

		JSON j = Parse(s);

		Tools.debug(j);
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
				json = parser.parse(reader, true).dispose().result();
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
		string = string.trim();

		Object value = null;

		try
		{
			if (string == null || string.length() == 0)
			{
				value = NOT_A_VALUE;
			}
			else if (string.startsWith(QUOTE_MARK) && string.endsWith(QUOTE_MARK))
			{
				value = RestoreString(string);
			}
			else if (Variable.isIntegerNumber(string))
			{
				try
				{
					value = Integer.valueOf(string);
				}
				catch (NumberFormatException e)
				{
					value = Long.valueOf(string);
				}
			}
			else if (Variable.isFloatNumber(string))
			{
				value = Double.parseDouble(string);
			}
			else if (TRUE_STRING.equals(string) || FALSE_STRING.equals(string))
			{
				value = Boolean.parseBoolean(string);
			}
			else if (NULL_STRING.equals(string))
			{
				value = null;
			}
			else if (string.startsWith(Function.DEFINE_MARK))
			{
				value = new Function(string);
			}
			else if (!string.startsWith(OBJECT_BEGIN_MARK) && !string.startsWith(ARRAY_BEGIN_MARK))
			{
				value = new Quotation(string);
			}
			else
			{
				value = NOT_A_VALUE;
			}
		}
		catch (NumberFormatException e)
		{
			value = NOT_A_VALUE;
		}

		return value;
	}

	@SuppressWarnings("unchecked")
	public static <T> T Project(Class<T> cls, JSON json)
	{
		T object = null;

		TreeMap<Class<?>[], Constructor<?>> cons = new TreeMap<Class<?>[], Constructor<?>>(
				new ArrayLengthReverseComparator<Class<?>>());

		for (Constructor<?> c : cls.getConstructors())
		{
			cons.put(c.getParameterTypes(), c);
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
					object = (T) con.newInstance(param);
					break;
				}
				catch (IllegalArgumentException e)
				{
				}
				catch (InstantiationException e)
				{
				}
				catch (IllegalAccessException e)
				{
				}
				catch (InvocationTargetException e)
				{
				}
			}
		}

		return object;
	}

	public static <T> T Project(T object, JSON json)
	{
		Map<Field, Object> project = null;

		Collection<Field> fields = null;

		if (!(object instanceof Map) && !(object instanceof Iterable) && !IsArray(object))
		{
			fields = FieldsOf(object);
		}

		if (fields != null)
		{
			project = new LinkedHashMap<Field, Object>();

			for (Field field : fields)
			{
				project.put(field, field.getName());
			}
		}

		return Project(object, json, project);
	}

	public static <T> T Project(T object, JSON json, JSON.Projector<T> projector)
	{
		if (object != null && json != null && projector != null)
		{
			object = projector.project(object, json);
		}

		return object;
	}

	@SuppressWarnings("unchecked")
	public static <T> T Project(T object, JSON json, Map<Field, Object> project)
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
					for (Entry<Field, Object> entry : project.entrySet())
					{
						try
						{
							String key = entry.getValue().toString();

							if (json.has(key))
							{
								obj.attr(entry.getKey().getName(), json.attr(key));
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
					for (Entry<Field, Object> entry : project.entrySet())
					{
						try
						{
							Field field = entry.getKey();

							String name = field.getName();

							String key = entry.getValue().toString();

							if (json.has(key))
							{
								Object value = Access(object, name);

								value = ProjectTo(json.attr(key), field.getType(), value, json.projects());

								Access(object, name, value);
							}
						}
						catch (Exception e)
						{
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

			try
			{
				if (project instanceof JSON.Projector)
				{
					object = Project(object, json, (JSON.Projector<T>) project);
				}
				else if (project instanceof Map)
				{
					object = Project(object, json, (Map<Field, Object>) project);
				}
				else
				{
					object = Project(object, json);
				}
			}
			catch (Exception e)
			{
			}
		}

		return object;
	}

	public static Object ProjectOf(Object object, Map<Class<?>, Object> projects)
	{
		Object project = null;

		if (object != null && projects != null)
		{
			for (Map.Entry<Class<?>, Object> entry : projects.entrySet())
			{
				Class<?> cls = entry.getKey();

				if (cls != null && cls.isInstance(object) && (project = entry.getValue()) != null)
				{
					break;
				}
			}
		}

		return project;
	}

	@SuppressWarnings("unchecked")
	public static <T> Object ProjectTo(Object obj, Class<T> cls, Object val, Map<Class<?>, Object> projects)
	{
		Object project = ProjectOf(cls, projects);

		if (cls.isInstance(obj))
		{
			val = obj;
		}
		else if (String.class == cls)
		{
			val = CastToString(obj);
		}
		else if (Integer.TYPE == cls || Integer.class == cls)
		{
			val = CastToInteger(obj);
		}
		else if (Double.TYPE == cls || Double.class == cls)
		{
			val = CastToDouble(obj);
		}
		else if (Boolean.TYPE == cls || Boolean.class == cls)
		{
			val = CastToBoolean(obj);
		}
		else if (Character.TYPE == cls || Character.class == cls)
		{
			val = CastToCharacter(obj);
		}
		else if (BigDecimal.class == cls)
		{
			val = CastToBigDecimal(obj);
		}
		else if (JSON.class == cls)
		{
			val = CastToJSON(obj);
		}
		else if (JSAN.class == cls)
		{
			val = CastToJSAN(obj);
		}
		else if (Function.class == cls)
		{
			val = CastToFunction(obj);
		}
		else if (Long.TYPE == cls || Long.class == cls)
		{
			val = CastToLong(obj);
		}
		else if (Calendar.class == cls)
		{
			val = CastToCalendar(obj);
		}
		else if (java.util.Date.class == cls || Date.class == cls)
		{
			val = CastToDate(obj);
		}
		else if (Timestamp.class == cls)
		{
			val = CastToTimestamp(obj);
		}
		else if (Time.class == cls)
		{
			val = CastToTime(obj);
		}
		else if (Byte.TYPE == cls || Byte.class == cls)
		{
			val = CastToByte(obj);
		}
		else if (Float.TYPE == cls || Float.class == cls)
		{
			val = CastToFloat(obj);
		}
		else if (Short.TYPE == cls || Short.class == cls)
		{
			val = CastToShort(obj);
		}
		else if (project instanceof JSON.Projector)
		{
			val = ((JSON.Projector<T>) project).project((T) val, (JSON) obj);
		}
		else if (IsSubClassOf(cls, Map.class))
		{
			// Target class is a Map.
			try
			{
				Map<Object, Object> map = (Map<Object, Object>) (val == null ? cls.newInstance() : val);

				map.clear();

				JSON json = (JSON) obj;

				if (project == null)
				{
					for (Pair pair : json.pairs())
					{
						map.put(pair.getKey(), pair.getValue());
					}
				}
				else
				{
					for (Entry<Field, Object> entry : ((Map<Field, Object>) project).entrySet())
					{
						try
						{
							String key = entry.getValue().toString();

							if (json.has(key))
							{
								map.put(entry.getKey().getName(), json.attr(key));
							}
						}
						catch (Exception e)
						{
						}
					}
				}
			}
			catch (Exception e)
			{
			}
		}
		else if (IsSubClassOf(cls, Collection.class))
		{
			// Target class is a Collection.
			try
			{
				Collection<Object> col = (Collection<Object>) (val == null ? cls.newInstance() : val);

				col.clear();

				JSON json = (JSON) obj;

				if (project == null)
				{
					for (Pair pair : json.pairs())
					{
						col.add(pair.getValue());
					}
				}
				else
				{
					for (Entry<Field, Object> entry : ((Map<Field, Object>) project).entrySet())
					{
						try
						{
							String key = entry.getValue().toString();

							if (json.has(key))
							{
								col.add(json.attr(key));
							}
						}
						catch (Exception e)
						{
						}
					}
				}
			}
			catch (Exception e)
			{
			}
		}
		else if (cls.isArray())
		{
			val = CastToArray(obj, cls.getComponentType(), projects);
		}
		else
		{
			// Target class is a normal Object.
			try
			{
				JSON json = (JSON) obj;
				val = Project(val == null ? cls.newInstance() : val, json.projects(), json);
			}
			catch (Exception ex)
			{
			}
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
				json = JSAN.Reflect((JSAN) (json == null ? new JSAN().reflects(reflects) : json), object,
						(JSAN.Reflector<?>) reflect);
			}
			else if (reflect instanceof JSON.Reflector)
			{
				json = JSON.Reflect((json == null ? new JSON().reflects(reflects) : json), object,
						(JSON.Reflector<?>) reflect);
			}
			else if (JSAN.IsJSAN(reflect))
			{
				json = JSAN.Reflect((JSAN) (json == null ? new JSAN().reflects(reflects) : json), object,
						(JSAN) reflect);
			}
			else if (JSON.IsJSON(reflect))
			{
				json = JSON.Reflect((json == null ? new JSON().reflects(reflects) : json), object, (JSON) reflect);
			}
			else
			{
				json = JSON.Reflect((json == null ? new JSON().reflects(reflects) : json), object);
			}
		}

		return json;
	}

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
		if (json == null)
		{
			json = new JSON();
		}

		if (object != null)
		{
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
				Map<String, ?> obj = (Map<String, ?>) object;

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
							Object key = pair.getValue();

							if (obj.containsKey(key))
							{
								json.attr(pair.getKey(), obj.get(key));
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
				JSAN temp = JSAN.Reflect(new JSAN().reflects(json).transformers(json), object);

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
				JSAN temp = JSAN.Reflect(new JSAN().reflects(json).transformers(json), object);

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
							json.attr(pair.getKey(), Access(object, pair.getValue().toString()));
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
		Object reflect = null;

		if (object != null && reflects != null)
		{
			for (Map.Entry<Class<?>, Object> entry : reflects.entrySet())
			{
				Class<?> cls = entry.getKey();

				if (cls != null && cls.isInstance(object) && (reflect = entry.getValue()) != null)
				{
					break;
				}
			}
		}

		return reflect;
	}

	public static String RestoreString(String string)
	{
		if (string != null)
		{
			string = string.trim();

			if (string.equals(NULL_STRING))
			{
				string = null;
			}
			else
			{
				string = string.replaceFirst("^" + QUOTE_CHAR + "([\\d\\D]*)" + QUOTE_CHAR + "$", "$1");

				StringBuilder buffer = new StringBuilder(string.length());

				char c;

				for (int i = 0; i < string.length(); i++)
				{
					c = string.charAt(i);

					if (c == ESCAPE_CHAR)
					{
						i++;

						c = string.charAt(i);

						if (c == UNICODE_ESCAPING_CHAR)
						{
							i++;

							String unicode = string.substring(i, i + UNICODE_ESCAPED_LENGTH);

							c = (char) Integer.parseInt(unicode, UNICODE_ESCAPE_RADIX);

							i += UNICODE_ESCAPED_LENGTH - 1;
						}
						else if (ESCAPING_CHAR.containsKey(c))
						{
							c = ESCAPING_CHAR.get(c);
						}
					}

					buffer.append(c);
				}

				string = buffer.toString();
			}
		}

		return string;
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

	public static Writer Serialize(JSON json, Writer writer, int indents) throws IOException
	{
		return Serialize(json, writer, indents, null);
	}

	public static Writer Serialize(JSON json, Writer writer, int indents, String indent) throws IOException
	{
		if (writer == null)
		{
			writer = new StringWriter();
		}

		indent = indent == null ? DEFAULT_LINE_INDENT : indent;

		boolean isJSAN = IsJSAN(json);
		boolean isFirst = true;

		if (!JSON.IsContext(json))
		{
			if (isJSAN)
			{
				writer.write(ARRAY_BEGIN_CHAR);
			}
			else
			{
				writer.write(OBJECT_BEGIN_CHAR);
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
					writer.append(PAIR_CHAR);
				}
				if (indents > -1)
				{
					writer.append(LINE_WRAP);
					Tools.repeat(writer, indent, indents + 1);
				}
			}

			if (!isJSAN)
			{
				if (key == null)
				{
					writer.append(NULL_STRING);
				}
				else
				{
					if (JSON.IsContext(json))
					{
						writer.append(Context.VAR_DEFINE_MARK);
						writer.append(' ');
					}
					else
					{
						writer.append(QUOTE_CHAR);
					}

					writer.append(EscapeString(entry.getKey()));

					if (JSON.IsContext(json))
					{
						writer.append(Context.VAR_ASSIGN_CHAR);
					}
					else
					{
						writer.append(QUOTE_CHAR);
						writer.append(ATTR_CHAR);
					}
				}
			}

			if (object == null)
			{
				value = NULL_STRING;
			}
			else if (IsJSON(object))
			{
				Serialize((JSON) object, writer, inner, indent);
				value = null;
			}
			else if (object instanceof CharSequence || object instanceof Character)
			{
				value = QUOTE_CHAR + EscapeString(object.toString()) + QUOTE_CHAR;
			}
			else if (object instanceof java.util.Date)
			{
				value = String.valueOf(((java.util.Date) object).getTime());
			}
			else if (object instanceof java.util.Calendar)
			{
				value = String.valueOf(((java.util.Calendar) object).getTimeInMillis());
			}
			else
			{
				value = object.toString();
			}

			if (value != null)
			{
				writer.append(value);
			}

			if (JSON.IsContext(json))
			{
				writer.append(Context.VAR_END_CHAR);
				writer.append(LINE_WRAP);
			}
		}

		if (!JSON.IsContext(json))
		{
			if (isJSAN)
			{
				if (indents > -1)
				{
					writer.append(LINE_WRAP);
					Tools.repeat(writer, indent, indents);
				}
				writer.append(ARRAY_END_CHAR);
			}
			else
			{
				if (indents > -1)
				{
					writer.append(LINE_WRAP);
					Tools.repeat(writer, indent, indents);
				}
				writer.append(OBJECT_END_CHAR);
			}
		}

		return writer;
	}

	public static Object ValueOf(Object object)
	{
		return ValueOf(object, null);
	}

	public static Object ValueOf(Object object, Map<Class<?>, Object> reflects)
	{
		Object result = null;

		if (ReflectOf(object, reflects) != null)
		{
			if (IsArray(object) || object instanceof Iterable)
			{
				result = JSAN.Reflect(reflects, object);
			}
			else
			{
				result = JSON.Reflect(reflects, object);
			}
		}
		else
		{
			if (object == null || object instanceof String || object instanceof Boolean || object instanceof Number
					|| object instanceof Character || object instanceof JSON || object instanceof Function
					|| object instanceof Quotation || object instanceof BigDecimal || object instanceof java.util.Date
					|| object instanceof java.util.Calendar)
			{
				result = object;
			}
			else if (object instanceof CharSequence)
			{
				result = object.toString();
			}
			else if (object instanceof java.sql.Clob)
			{
				try
				{
					result = Tools.readerToStringBuilder(((java.sql.Clob) object).getCharacterStream()).toString();
				}
				catch (Exception e)
				{
				}
			}
			else if (IsArray(object) || object instanceof Iterable)
			{
				result = JSAN.Reflect(reflects, object);
			}
			else
			{
				result = JSON.Reflect(reflects, object);
			}
		}
		return result;
	}

	private Map<String, Object>					object;

	private JSON								outer;

	private String								entry;

	private transient Map<Class<?>, Object>		reflects;

	private transient Map<Class<?>, Object>		projects;

	private transient Map<String, Transformer>	transformers;

	public JSON()
	{
		object(new LinkedHashMap<String, Object>());
	}

	@SuppressWarnings("unchecked")
	public <E> E attr(String key)
	{
		E value = null;

		try
		{
			value = (E) Quote(this.get(key));
		}
		catch (ClassCastException e)
		{
		}

		return value;
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

	public Boolean attrBoolean(String key)
	{
		return attrCast(key, Boolean.class);
	}

	public Byte attrByte(String key)
	{
		return attrCast(key, Byte.class);
	}

	public <E> E attrCast(String key, Class<E> cls)
	{
		return CastTo(this.attr(key), cls);
	}

	public Character attrCharacter(String key)
	{
		return attrCast(key, Character.class);
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

	public Short attrShort(String key)
	{
		return attrCast(key, Short.class);
	}

	public String attrString(String key)
	{
		return attrCast(key, String.class);
	}

	public JSON clean()
	{
		this.clear();
		return this;
	}

	public void clear()
	{
		rescind();
		object().clear();
	}

	@Override
	public JSON clone()
	{
		return new JSON().reflects(this).transformers(this).clone(this);
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

	public boolean containsKey(Object key)
	{
		return object().containsKey(key);
	}

	public boolean containsValue(Object value)
	{
		return object().containsValue(value);
	}

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

	public String entry()
	{
		return entry;
	}

	public JSON entry(String entry)
	{
		this.entry = entry;
		return this;
	}

	public Set<Map.Entry<String, Object>> entrySet()
	{
		return object().entrySet();
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
					if (!Tools.equals(this.val(key), that.val(key)))
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

	public Object get(Object key)
	{
		return object().get(key);
	}

	public boolean has(String entry)
	{
		return object().containsKey(entry);
	}

	public boolean hasVal(Object value)
	{
		boolean has = false;

		for (String key : this.keySet())
		{
			if (Tools.equals(value, this.val(key)))
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
				if (cmp.compare(value, this.val(key)) == 0)
				{
					has = true;
					break;
				}
			}
		}

		return has;
	}

	public boolean isEmpty()
	{
		return object().isEmpty();
	}

	public Set<String> keySet()
	{
		return object().keySet();
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

	public JSON outer()
	{
		return outer;
	}

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

	public JSON pairs(Map<? extends String, ? extends Object> map)
	{
		this.putAll(map);
		return this;
	}

	public JSON pairs(Object... pairs)
	{
		for (int i = 0; i < pairs.length; i += 2)
		{
			Object key = pairs[i];
			Object value = null;
			try
			{
				value = pairs[i + 1];
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
			}

			if (key != null)
			{
				this.attr(key.toString(), value);
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
		E object = null;

		object = Project(cls, this);

		if (object == null)
		{
			try
			{
				object = cls.newInstance();
			}
			catch (Exception e)
			{
			}
		}

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
			Map<Field, Object> project = new LinkedHashMap<Field, Object>();

			for (Object name : fields)
			{
				if (name != null)
				{
					Field field = FieldOf(cls, name.toString());

					if (field != null)
					{
						project.put(field, name);
					}
				}
			}

			projects(cls, project);
		}
		return this;
	}

	public JSON projects(Class<?> cls, JSON project)
	{
		if (cls != null && project != null)
		{
			Map<Field, Object> map = new LinkedHashMap<Field, Object>();

			for (Pair pair : project.pairs())
			{
				Field field = FieldOf(cls, pair.getKey());

				if (field != null && pair.getValue() != null)
				{
					map.put(field, pair.getValue());
				}
			}

			projects(cls, map);
		}
		return this;
	}

	public JSON projects(Class<?> cls, Map<Field, Object> project)
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
			Map<Field, Object> project = new LinkedHashMap<Field, Object>();

			for (String name : fields)
			{
				if (name != null)
				{
					Field field = FieldOf(cls, name);

					if (field != null)
					{
						project.put(field, name);
					}
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

	public Object put(String key, Object value)
	{
		Object old = null;

		if (key != null)
		{
			old = this.get(key);
			// Clear the old hierarchical relation.
			Hierarchical hirch = AsHierarchical(old);
			if (hirch != null && hirch.outer() == this)
			{
				hirch.outer(null).entry(null);
			}

			value = ValueOf(value, reflects());

			Transformer transformer = this.transformerOf(key);
			if (transformer != null)
			{
				value = transformer.transform(this, key, value);
			}

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

	public <E extends Object, T extends Map<String, E>> T putTo(T map, Mapper<Object, E> mapper)
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
			json = new JSON().reflects(this).transformers(this);
		}

		for (String key : this.keySet())
		{
			json.put(this.attr(key).toString(), key);
		}

		return json;
	}

	public JSAN toJSAN()
	{
		JSAN jsan = new JSAN().reflects(this).transformers(this);
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

	public JSON transformer(String entry, Transformer transformer)
	{
		if (entry != null && transformer != null)
		{
			transformersSingleton();
			transformers().put(entry, transformer);
		}
		return this;
	}

	public Transformer transformerOf(String entry)
	{
		Transformer transformer = null;

		if (entry != null && transformers() != null)
		{
			transformer = transformers().get(entry);
		}

		return transformer;
	}

	public Map<String, Transformer> transformers()
	{
		return transformers;
	}

	public JSON transformers(JSON json)
	{
		if (json != null)
		{
			transformers(json.transformers());
		}
		return this;
	}

	public JSON transformers(Map<String, Transformer> transformers)
	{
		this.transformers = transformers;
		return this;
	}

	public JSON transformersRemove(String entry)
	{
		if (entry != null && transformers() != null)
		{
			transformers().remove(entry);
		}
		return this;
	}

	protected JSON transformersSingleton()
	{
		if (transformers() == null)
		{
			transformers(new LinkedHashMap<String, Transformer>());
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public <E> E val(String key)
	{
		E val = null;

		Object obj = this.attr(key);

		try
		{
			val = (E) obj;
		}
		catch (ClassCastException e)
		{
		}

		return val;
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
		return this.attrCast(key, cls);
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
		return attrJSAN(key);
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
		return attrJSON(key);
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

	public Timestamp valTimestamp(String key)
	{
		return CastToTimestamp(this.attr(key));
	}

	public Timestamp valTimestamp(String key, long defaultValue)
	{
		Timestamp val = valTimestamp(key);
		return val == null ? new Timestamp(defaultValue) : val;
	}

	public Collection<Object> values()
	{
		return object().values();
	}
}
