package org.kernelab.basis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
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
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface Hierarchical extends Copieable<Hierarchical>
{
	public JSON context();

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
	public static class Context extends JSON
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= -7912039879626749853L;

		public static final String	VAR_DEFINE_MARK		= "var";

		public static final char	VAR_ASSIGN_CHAR		= '=';

		public static final String	VAR_ASSIGN_MARK		= String.valueOf(VAR_ASSIGN_CHAR);

		public static final char	VAR_END_CHAR		= ';';

		public static final String	VAR_END_MARK		= String.valueOf(VAR_END_CHAR);

		public static final Matcher	VAR_ENTRY_MATCHER	= Pattern.compile("^\\s*?(var\\s+)?\\s*?(\\S+)\\s*?=\\s*(.*)$")
																.matcher("");

		public static final Matcher	VAR_EXIT_MATCHER	= Pattern.compile("^\\s*(.*?)\\s*;\\s*$").matcher("");

		private DataReader			reader;

		public Context()
		{
			reader = new DataReader() {

				private String			entry	= null;

				private StringBuilder	buffer	= new StringBuilder();

				@Override
				protected void readFinished()
				{

				}

				@Override
				protected void readLine(CharSequence line)
				{
					buffer.append(line);

					if (entry == null)
					{
						if (VAR_ENTRY_MATCHER.reset(buffer).lookingAt())
						{
							entry = VAR_ENTRY_MATCHER.group(2);
							line = VAR_ENTRY_MATCHER.group(3);
							Tools.clearStringBuilder(buffer);
							buffer.append(line);
						}
					}

					if (entry != null)
					{
						if (VAR_EXIT_MATCHER.reset(buffer).lookingAt())
						{

							line = VAR_EXIT_MATCHER.group(1);
							Tools.clearStringBuilder(buffer);
							buffer.append(line);

							Object object = JSON.ParseValueOf(buffer.toString());

							if (object == JSON.NOT_A_VALUE)
							{
								object = JSON.Parse(buffer, null, Context.this);
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

				@Override
				protected void readPrepare()
				{
					entry = null;
				}
			};
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

	public static class Function implements Hierarchical
	{
		public static final String	DEFINE_MARK			= "function";

		public static final char	DEFINE_FIRST_CHAR	= 'f';

		public static final String	DEFINE_REGEX		= "^(function)\\s*?([^(]*?)(\\()";

		public static final String	DEFAULT_NAME_PREFIX	= "_";

		private JSON				outer;

		private String				entry;

		private String				expression;

		private String				name;

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

		public JSON context()
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

		public String expression()
		{
			return expression;
		}

		protected Function expression(String expression)
		{
			this.expression = expression;
			return this;
		}

		public String name()
		{
			if (name == null)
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

		@Override
		public String toString()
		{
			return expression();
		}

		public String toString(String name)
		{
			return expression().replaceFirst(DEFINE_REGEX, "$1 " + (name == null ? name() : name.trim()) + "$3");
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
		protected class ArrayIterator implements Iterator<Object>
		{
			private LinkedList<String>		keys	= new LinkedList<String>();

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

		public static JSAN Reflect(JSAN jsan, Map<Class<?>, Object> templates, Object object)
		{
			if (object != null)
			{
				if (jsan == null)
				{
					jsan = new JSAN().templates(templates);
				}

				Object template = null;

				if (templates != null)
				{
					for (Map.Entry<Class<?>, Object> entry : templates.entrySet())
					{
						Class<?> cls = entry.getKey();

						if (cls != null && cls.isInstance(object) && (template = entry.getValue()) != null)
						{
							break;
						}
					}
				}

				if (template instanceof JSAN.Reflector)
				{
					jsan = JSAN.Reflect(jsan, object, (JSAN.Reflector<?>) template);
				}
				else if (template instanceof JSON.Reflector)
				{
					jsan = (JSAN) JSON.Reflect(jsan, object, (JSON.Reflector<?>) template);
				}
				else if (JSAN.IsJSAN(template))
				{
					jsan = JSAN.Reflect(jsan, object, (JSAN) template);
				}
				else if (JSON.IsJSON(template))
				{
					jsan = (JSAN) JSON.Reflect(jsan, object, (JSON) template);
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
			JSAN template = null;

			Collection<String> fields = FieldsOf(object);

			if (fields != null)
			{
				template = new JSAN();

				for (String field : fields)
				{
					template.add(field);
				}
			}

			return JSAN.Reflect(jsan, object, template);
		}

		public static JSAN Reflect(JSAN jsan, Object object, Iterable<?> fields)
		{
			JSAN template = null;

			if (fields != null)
			{
				template = new JSAN();

				for (Object field : fields)
				{
					if (field != null)
					{
						template.add(field);
					}
				}
			}

			return JSAN.Reflect(jsan, object, template);
		}

		@SuppressWarnings("unchecked")
		public static JSAN Reflect(JSAN jsan, Object object, JSAN template)
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

					if (template == null)
					{
						jsan.putAll(obj);
					}
					else
					{
						for (Pair pair : template.pairs())
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

					if (template == null)
					{
						jsan.putAll(obj);
					}
					else
					{
						for (Pair pair : template.pairs())
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

					if (template == null)
					{
						jsan.putAll(obj);
					}
					else
					{
						for (Pair pair : template.pairs())
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

					if (template == null)
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

						for (Pair pair : template.pairs())
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

					if (template == null)
					{
						for (int i = 0; i < len; i++)
						{
							jsan.attr(i, Array.get(object, i));
						}
					}
					else
					{
						Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();

						for (Pair pair : template.pairs())
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
				else if (template != null)
				{
					// Reflect Object using template
					Class<?> cls = object.getClass();

					int i = 0;

					for (Object obj : template)
					{
						try
						{
							String field = obj.toString();

							String methodName = field.substring(0, 1).toUpperCase() + field.substring(1);

							Method method = null;

							try
							{
								method = cls.getMethod("get" + methodName);
							}
							catch (NoSuchMethodException e)
							{
								method = cls.getMethod("is" + methodName);
							}

							if (method != null && method.getParameterTypes().length == 0)
							{
								jsan.attr(i, method.invoke(object));
							}
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
			JSAN template = null;

			if (fields != null)
			{
				template = new JSAN();

				for (Entry<String, ?> entry : fields.entrySet())
				{
					if (entry.getKey() != null && entry.getValue() != null)
					{
						template.put(entry.getKey(), entry.getValue());
					}
				}
			}

			return JSAN.Reflect(jsan, object, template);
		}

		public static JSAN Reflect(JSAN jsan, Object object, String... fields)
		{
			JSAN template = null;

			if (fields != null)
			{
				template = new JSAN();

				for (String field : fields)
				{
					if (field != null)
					{
						template.add(field);
					}
				}
			}

			return JSAN.Reflect(jsan, object, template);
		}

		public static JSAN Reflect(Map<Class<?>, Object> templates, Object object)
		{
			return JSAN.Reflect(null, templates, object);
		}

		public static JSAN Reflect(Object object)
		{
			return JSAN.Reflect((JSAN) null, object);
		}

		public static JSAN Reflect(Object object, Iterable<?> fields)
		{
			return JSAN.Reflect(null, object, fields);
		}

		public static JSAN Reflect(Object object, Map<String, ?> template)
		{
			return JSAN.Reflect(null, object, template);
		}

		public static JSAN Reflect(Object object, String... fields)
		{
			return JSAN.Reflect(null, object, fields);
		}

		private Map<String, Object>	array;

		private int					length	= 0;

		public JSAN()
		{
			super();

			array(new TreeMap<String, Object>(new Comparator<String>() {

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

			}));
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
					collection.add((E) o);
				}
			}
			return collection;
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

		@SuppressWarnings("unchecked")
		public <E> E attrCast(int index, Class<E> cls)
		{
			Object val = this.attr(index);

			if (!cls.isInstance(val))
			{
				val = null;
			}

			return (E) val;
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
			return new JSAN().templates(this).clone(this);
		}

		public boolean contains(Object value)
		{
			return this.containsValue(value);
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
			return array().containsValue(value) || super.containsValue(value);
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

			value = ValueOf(value, templates());

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
				{ // The formal outer would quote the value at its new
					// location.
					if (formalContext == this.context())
					{ // A quote would be made only in the same Context.
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
			JSAN.Reflect(this, this.templates(), object);
			return this;
		}

		@Override
		public JSAN reflect(Object object, Iterable<?> template)
		{
			JSAN.Reflect(this, object, template);
			return this;
		}

		public <T> JSAN reflect(Object object, JSAN.Reflector<T> reflector)
		{
			JSAN.Reflect(this, object, reflector);
			return this;
		}

		@Override
		public JSAN reflect(Object object, Map<String, ?> template)
		{
			JSAN.Reflect(this, object, template);
			return this;
		}

		@Override
		public JSAN reflect(Object object, String... fields)
		{
			JSAN.Reflect(this, object, fields);
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
					if (!jsan.contains(o))
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

		public JSAN splice(int index, int cover, Collection<?> collection)
		{
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
					this.remove(iter.key());
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

			return this;
		}

		public JSAN splice(int index, int cover, JSAN jsan)
		{
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
					this.remove(iter.key());
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

			return this;
		}

		public JSAN splice(int index, int cover, Object... objects)
		{
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
					this.remove(iter.key());
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

			return this;
		}

		@Override
		public <T> JSAN template(Class<T> cls, JSAN.Reflector<T> reflector)
		{
			super.template(cls, reflector);
			return this;
		}

		@Override
		public <T> JSAN template(Class<T> cls, JSON.Reflector<T> reflector)
		{
			super.template(cls, reflector);
			return this;
		}

		@Override
		public JSAN templateJSAN(Class<?> cls, Iterable<?> fields)
		{
			super.templateJSAN(cls, fields);
			return this;
		}

		@Override
		public JSAN templateJSAN(Class<?> cls, Map<String, ?> fields)
		{
			super.templateJSAN(cls, fields);
			return this;
		}

		@Override
		public JSAN templateJSAN(Class<?> cls, String... fields)
		{
			super.templateJSAN(cls, fields);
			return this;
		}

		@Override
		public JSAN templateJSON(Class<?> cls, Iterable<?> fields)
		{
			super.templateJSON(cls, fields);
			return this;
		}

		@Override
		public JSAN templateJSON(Class<?> cls, Map<String, ?> fields)
		{
			super.templateJSON(cls, fields);
			return this;
		}

		@Override
		public JSAN templateJSON(Class<?> cls, String... fields)
		{
			super.templateJSON(cls, fields);
			return this;
		}

		@Override
		public JSAN templates(JSON json)
		{
			super.templates(json);
			return this;
		}

		@Override
		public JSAN templates(Map<Class<?>, Object> templates)
		{
			super.templates(templates);
			return this;
		}

		@Override
		public JSAN templatesRemove(Class<?> cls)
		{
			super.templatesRemove(cls);
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

		public <E> E val(int index)
		{
			return val(index, null);
		}

		public <E> E val(int index, E defaultValue)
		{
			E val = attr(index);
			return val == null ? defaultValue : val;
		}

		public BigDecimal valBigDecimal(int index)
		{
			BigDecimal val = null;

			Object obj = this.attr(index);

			try
			{
				val = BigDecimal.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					val = new BigDecimal(obj.toString());
				}
				catch (NumberFormatException ex)
				{
				}
			}

			return val;
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
			Boolean val = null;

			Object obj = this.attr(index);

			try
			{
				val = Boolean.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					val = Boolean.parseBoolean(obj.toString());
				}
				catch (NumberFormatException ex)
				{
				}
			}

			return val;
		}

		public Boolean valBoolean(int index, Boolean defaultValue)
		{
			Boolean val = valBoolean(index);
			return val == null ? defaultValue : val;
		}

		public Byte valByte(int index)
		{
			Byte val = null;

			Object obj = this.attr(index);

			try
			{
				val = Byte.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					val = Byte.parseByte(obj.toString());
				}
				catch (NumberFormatException ex)
				{
				}
			}

			return val;
		}

		public Byte valByte(int index, Byte defaultValue)
		{
			Byte val = valByte(index);
			return val == null ? defaultValue : val;
		}

		public Calendar valCalendar(int index)
		{
			Calendar val = null;

			Long obj = this.valLong(index);

			if (obj != null)
			{
				val = new GregorianCalendar();
				val.setTimeInMillis(obj);
			}

			return val;
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

		public Character valCharacter(int index)
		{
			Character val = null;

			Object obj = this.attr(index);

			try
			{
				val = Character.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					val = obj.toString().charAt(0);
				}
				catch (StringIndexOutOfBoundsException ex)
				{
				}
			}

			return val;
		}

		public Character valCharacter(int index, Character defaultValue)
		{
			Character val = valCharacter(index);
			return val == null ? defaultValue : val;
		}

		public Date valDate(int index)
		{
			Date value = null;

			Long obj = this.valLong(index);

			if (obj != null)
			{
				value = new Date(obj);
			}

			return value;
		}

		public Date valDate(int index, long defaultValue)
		{
			Date val = valDate(index);
			return val == null ? new Date(defaultValue) : val;
		}

		public Double valDouble(int index)
		{
			Double val = null;

			Object obj = this.attr(index);

			try
			{
				val = Double.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					val = Double.parseDouble(obj.toString());
				}
				catch (NumberFormatException ex)
				{
				}
			}

			return val;
		}

		public Double valDouble(int index, Double defaultValue)
		{
			Double val = valDouble(index);
			return val == null ? defaultValue : val;
		}

		public Float valFloat(int index)
		{
			Float val = null;

			Object obj = this.attr(index);

			try
			{
				val = Float.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					val = Float.parseFloat(obj.toString());
				}
				catch (NumberFormatException ex)
				{
				}
			}

			return val;
		}

		public Float valFloat(int index, Float defaultValue)
		{
			Float val = valFloat(index);
			return val == null ? defaultValue : val;
		}

		public Function valFunction(int index)
		{
			Function val = null;

			Object obj = this.attr(index);

			try
			{
				val = Function.class.cast(obj);
			}
			catch (ClassCastException e)
			{
			}

			return val;
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
			Integer val = null;

			Object obj = this.attr(index);

			try
			{
				val = Integer.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					val = Integer.parseInt(obj.toString());
				}
				catch (NumberFormatException ex)
				{
				}
			}

			return val;
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
			Long val = null;

			Object obj = this.attr(index);

			try
			{
				val = Long.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					val = Long.parseLong(obj.toString());
				}
				catch (NumberFormatException ex)
				{
				}
			}

			return val;
		}

		public Long valLong(int index, Long defaultValue)
		{
			Long val = valLong(index);
			return val == null ? defaultValue : val;
		}

		public Short valShort(int index)
		{
			Short val = null;

			Object obj = this.attr(index);

			try
			{
				val = Short.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				try
				{
					val = Short.parseShort(obj.toString());
				}
				catch (NumberFormatException ex)
				{
				}
			}

			return val;
		}

		public Short valShort(int index, Short defaultValue)
		{
			Short val = valShort(index);
			return val == null ? defaultValue : val;
		}

		public String valString(int index)
		{
			String val = null;

			Object obj = this.attr(index);

			try
			{
				val = String.class.cast(obj);
			}
			catch (ClassCastException e)
			{
				val = obj.toString();
			}

			return val;
		}

		public String valString(int index, String defaultValue)
		{
			String val = valString(index);
			return val == null ? defaultValue : val;
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

		public Object setValue(Object value)
		{
			Object old = getValue();
			attr(key, value);
			return old;
		}
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

			int quoteLength = quote.length() - 1;
			if (quote.charAt(quoteLength) == NESTED_ATTRIBUTE_END)
			{
				int begin = JSON
						.ReverseDualMatchIndex(quote, NESTED_ATTRIBUTE_BEGIN, NESTED_ATTRIBUTE_END, quoteLength);
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
					i = JSON.DualMatchIndex(quote, NESTED_ATTRIBUTE_BEGIN, NESTED_ATTRIBUTE_END, i) - 1;
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

			if (c == NESTED_ATTRIBUTE_QUOTE || Variable.isInteger(quote))
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

		public JSON context()
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

		public SyntaxErrorException(CharSequence source, int index)
		{
			super("Near\n" + source.subSequence(Math.max(index - 30, 0), Math.min(index + 30, source.length())));
		}
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

	public static final int							NOT_BEGIN				= -1;

	public static final Object						NOT_A_VALUE				= new String("NOT A VALUE");

	public static final String						NULL_STRING				= "null";

	public static final String						TRUE_STRING				= "true";

	public static final String						FALSE_STRING			= "false";

	public static final char						UNICODE_ESCAPING_CHAR	= 'u';

	public static final int							UNICODE_ESCAPED_LENGTH	= 4;

	public static final int							UNICODE_ESCAPE_RADIX	= 16;

	public static final Map<Character, Character>	ESCAPING_CHAR			= new HashMap<Character, Character>();

	public static final Map<Character, String>		ESCAPED_CHAR			= new HashMap<Character, String>();

	public static String							LINE_INDENT				= "\t";

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

	public static boolean CharacterNeedToEscape(char c)
	{
		return ESCAPED_CHAR.containsKey(c);
	}

	public static int DualMatchIndex(CharSequence sequence, char a, char b, int from)
	{
		int index = -1;
		int match = 0;

		boolean inString = false;

		i: for (int i = Math.max(0, from); i < sequence.length(); i++)
		{
			char c = sequence.charAt(i);

			if (c == ESCAPE_CHAR)
			{
				c = sequence.charAt(i + 1);
				if (ESCAPING_CHAR.containsKey(c))
				{
					i++;
				}
				else if (c == UNICODE_ESCAPING_CHAR)
				{
					i += UNICODE_ESCAPED_LENGTH + 1;
				}
				continue i;
			}

			if (c == QUOTE_CHAR)
			{
				inString = !inString;
			}
			if (inString)
			{
				continue i;
			}

			if (c == a)
			{
				match++;
			}
			else if (c == b)
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

	public static Collection<String> FieldsOf(Class<?> cls, Collection<String> fields)
	{
		if (cls != null)
		{
			if (fields == null)
			{
				fields = new LinkedHashSet<String>();
			}

			fields = FieldsOf(cls.getSuperclass(), fields);

			for (Field field : cls.getDeclaredFields())
			{
				fields.add(field.getName());
			}
		}

		return fields;
	}

	public static <T> Collection<String> FieldsOf(T object)
	{
		Collection<String> fields = null;

		if (object != null && !(object instanceof Map) && !(object instanceof Iterable) && !IsArray(object))
		{
			fields = FieldsOf(object.getClass(), fields);
		}

		return fields;
	}

	public static final int FirstNonWhitespaceIndex(CharSequence sequence, int from)
	{
		int index = -1;

		for (int i = from; i < sequence.length(); i++)
		{
			if (!Character.isWhitespace(sequence.charAt(i)))
			{
				index = i;
				break;
			}
		}

		return index;
	}

	public static final int FirstWhitespaceIndex(CharSequence sequence, int from)
	{
		int index = -1;

		for (int i = from; i < sequence.length(); i++)
		{
			if (Character.isWhitespace(sequence.charAt(i)))
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

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		JSON j = new JSON();
		j.attr("k", "i");
		Tools.debug(j.valInteger("k", 2));
	}

	public static JSON Parse(CharSequence source)
	{
		return Parse(source, null);
	}

	public static JSON Parse(CharSequence source, JSON object)
	{
		return Parse(source, object, null);
	}

	public static JSON Parse(CharSequence source, JSON object, Context context)
	{
		String string = source.toString().trim();

		if (!(string.startsWith(OBJECT_BEGIN_MARK) && string.endsWith(OBJECT_END_MARK))
				&& !(string.startsWith(ARRAY_BEGIN_MARK) && string.endsWith(ARRAY_END_MARK)))
		{
			return null;
		}

		StringBuilder json = new StringBuilder(string);

		if (object == null)
		{
			if (string.startsWith(ARRAY_BEGIN_MARK) && string.endsWith(ARRAY_END_MARK))
			{
				object = new JSAN();
			}
			else if (string.startsWith(OBJECT_BEGIN_MARK) && string.endsWith(OBJECT_END_MARK))
			{
				object = new JSON();
			}
			else
			{
				return null;
			}
		}

		int arrayIndex = NOT_BEGIN;
		int nail = NOT_BEGIN;

		String entry = null;
		Object value = null;

		boolean inString = false;

		int i = 0;
		char c;

		try
		{
			i: for (i = 0; i < json.length(); i++)
			{
				c = json.charAt(i);

				if (inString)
				{
					if (c == ESCAPE_CHAR)
					{
						json.deleteCharAt(i);
						c = json.charAt(i);
						if (ESCAPING_CHAR.containsKey(c))
						{
							json.deleteCharAt(i);
							json.insert(i, ESCAPING_CHAR.get(c));
						}
						else if (c == UNICODE_ESCAPING_CHAR)
						{
							json.deleteCharAt(i);
							String unicode = json.substring(i, i + UNICODE_ESCAPED_LENGTH);
							json.delete(i, i + UNICODE_ESCAPED_LENGTH);
							json.insert(i, (char) Integer.parseInt(unicode, UNICODE_ESCAPE_RADIX));
						}
						continue i;

					}
					else if (c == QUOTE_CHAR)
					{
						inString = !inString;
					}
				}
				else
				{
					switch (c)
					{
						case OBJECT_BEGIN_CHAR:
							if (i != 0)
							{
								int match = DualMatchIndex(json, OBJECT_BEGIN_CHAR, OBJECT_END_CHAR, i);
								value = Parse(json.substring(i, match + 1), new JSON(), context);
								i = match;
								nail = NOT_BEGIN;
							}
							else
							{
								nail = FirstNonWhitespaceIndex(json, i + 1);
							}
							break;

						case OBJECT_END_CHAR:
							if (entry != null)
							{
								if (nail != NOT_BEGIN)
								{
									value = ParseValueOf(json.substring(nail, i));
								}
								object.put(entry, value);
							}
							break i;

						case ARRAY_BEGIN_CHAR:
							if (nail != NOT_BEGIN && nail != i)
							{
								i = DualMatchIndex(json, ARRAY_BEGIN_CHAR, ARRAY_END_CHAR, i);
							}
							else if (i != 0)
							{
								int match = DualMatchIndex(json, ARRAY_BEGIN_CHAR, ARRAY_END_CHAR, i);
								value = Parse(json.substring(i, match + 1), new JSAN(), context);
								i = match;
								nail = NOT_BEGIN;
							}
							else
							{
								nail = FirstNonWhitespaceIndex(json, i + 1);
								arrayIndex++;
							}
							break;

						case ARRAY_END_CHAR:
							if (nail != NOT_BEGIN && nail != i)
							{
								value = ParseValueOf(json.substring(nail, i));
							}
							if (value != null)
							{
								object.put(JSAN.Index(arrayIndex), value);
							}
							break i;

						case PAIR_CHAR:
							if (nail != NOT_BEGIN)
							{
								value = ParseValueOf(json.substring(nail, i));
							}
							if (arrayIndex > NOT_BEGIN)
							{
								entry = JSAN.Index(arrayIndex);
								arrayIndex++;
							}
							object.put(entry, value);
							nail = FirstNonWhitespaceIndex(json, i + 1);
							entry = null;
							value = null;
							break;

						case ATTR_CHAR:
							entry = TrimQuotes(json.substring(nail, i));
							nail = FirstNonWhitespaceIndex(json, i + 1);
							break;

						case QUOTE_CHAR:
							inString = !inString;
							break;

						case Function.DEFINE_FIRST_CHAR:
							if (nail == i
									&& Function.DEFINE_MARK.equals(json.substring(i,
											Math.min(json.length(), i + Function.DEFINE_MARK.length()))))
							{
								i = DualMatchIndex(json, OBJECT_BEGIN_CHAR, OBJECT_END_CHAR, i);
							}
							break;
					}
				}
			}

		}
		catch (RuntimeException e)
		{
			throw new SyntaxErrorException(json, i);
		}

		return object;
	}

	public static JSON Parse(Reader reader)
	{
		return Parse(Tools.readerToStringBuilder(reader));
	}

	public static Object ParseValueOf(String string)
	{
		string = string.trim();

		Object value = null;

		try
		{
			if (string.startsWith(QUOTE_MARK) && string.endsWith(QUOTE_MARK))
			{
				value = TrimQuotes(string);
			}
			else if (Variable.isInteger(string))
			{
				try
				{
					value = Integer.parseInt(string);
				}
				catch (NumberFormatException e)
				{
					value = Long.parseLong(string);
				}
			}
			else if (Variable.isDouble(string))
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

	public static Object Quote(Object o)
	{
		while (o instanceof Quotation)
		{
			o = ((Quotation) o).quote();
		}

		return o;
	}

	public static JSON Reflect(JSON json, Map<Class<?>, Object> templates, Object object)
	{
		if (object != null)
		{
			Object template = null;

			if (templates != null)
			{
				for (Map.Entry<Class<?>, Object> entry : templates.entrySet())
				{
					Class<?> cls = entry.getKey();

					if (cls != null && cls.isInstance(object) && (template = entry.getValue()) != null)
					{
						break;
					}
				}
			}

			if (template instanceof JSAN.Reflector)
			{
				json = JSAN.Reflect((JSAN) (json == null ? new JSAN().templates(templates) : json), object,
						(JSAN.Reflector<?>) template);
			}
			else if (template instanceof JSON.Reflector)
			{
				json = JSON.Reflect((json == null ? new JSON().templates(templates) : json), object,
						(JSON.Reflector<?>) template);
			}
			else if (JSAN.IsJSAN(template))
			{
				json = JSAN.Reflect((JSAN) (json == null ? new JSAN().templates(templates) : json), object,
						(JSAN) template);
			}
			else if (JSON.IsJSON(template))
			{
				json = JSON.Reflect((json == null ? new JSON().templates(templates) : json), object, (JSON) template);
			}
			else
			{
				json = JSON.Reflect((json == null ? new JSON().templates(templates) : json), object);
			}
		}

		return json;
	}

	public static JSON Reflect(JSON json, Object object)
	{
		JSON template = null;

		Collection<String> fields = FieldsOf(object);

		if (fields != null)
		{
			template = new JSON();

			for (String field : fields)
			{
				template.put(field, field);
			}
		}

		return JSON.Reflect(json, object, template);
	}

	public static JSON Reflect(JSON json, Object object, Iterable<?> fields)
	{
		JSON template = null;

		if (fields != null)
		{
			template = new JSON();

			for (Object field : fields)
			{
				if (field != null)
				{
					template.put(field.toString(), field.toString());
				}
			}
		}

		return JSON.Reflect(json, object, template);
	}

	@SuppressWarnings("unchecked")
	public static JSON Reflect(JSON json, Object object, JSON template)
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

				if (template == null)
				{
					json.putAll(obj);
				}
				else
				{
					for (Pair pair : template.pairs())
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

				if (template == null)
				{
					json.putAll(obj);
				}
				else
				{
					for (Pair pair : template.pairs())
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
				JSAN temp = JSAN.Reflect(new JSAN().templates(json), object);

				if (template == null)
				{
					json.putAll(temp);
				}
				else
				{
					for (Pair pair : template.pairs())
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

				if (template == null)
				{
					json.putAll(temp);
				}
				else
				{
					for (Pair pair : template.pairs())
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
			else if (template != null)
			{
				// Reflect Object using template
				Class<?> cls = object.getClass();

				for (Pair pair : template.pairs())
				{
					try
					{
						if (pair.getKey() != null)
						{
							String name = pair.getValue().toString();

							if (name.length() > 0)
							{
								String methodName = name.substring(0, 1).toUpperCase() + name.substring(1);

								Method method = null;

								try
								{
									method = cls.getMethod("get" + methodName);
								}
								catch (NoSuchMethodException e)
								{
									method = cls.getMethod("is" + methodName);
								}

								if (method != null && method.getParameterTypes().length == 0)
								{
									json.attr(pair.getKey(), method.invoke(object));
								}
							}
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
		JSON template = null;

		if (fields != null)
		{
			template = new JSON();

			for (Entry<String, ?> entry : fields.entrySet())
			{
				if (entry.getKey() != null && entry.getValue() != null)
				{
					template.put(entry.getKey(), entry.getValue());
				}
			}
		}

		return JSON.Reflect(json, object, template);
	}

	public static JSON Reflect(JSON json, Object object, String... fields)
	{
		JSON template = null;

		if (fields != null)
		{
			template = new JSON();

			for (String field : fields)
			{
				if (field != null)
				{
					template.put(field, field);
				}
			}
		}

		return JSON.Reflect(json, object, template);
	}

	public static JSON Reflect(Map<Class<?>, Object> templates, Object object)
	{
		return JSON.Reflect(null, templates, object);
	}

	public static JSON Reflect(Object object)
	{
		return JSON.Reflect((JSON) null, object);
	}

	public static JSON Reflect(Object object, Iterable<?> fields)
	{
		return JSON.Reflect(null, object, fields);
	}

	public static JSON Reflect(Object object, JSON template)
	{
		return JSON.Reflect(null, object, template);
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

	public static int ReverseDualMatchIndex(CharSequence sequence, char a, char b, int from)
	{
		int index = -1;
		int match = 0;

		boolean inString = false;

		i: for (int i = Math.min(sequence.length() - 1, from); i >= 0; i--)
		{

			char c = sequence.charAt(i);

			if (c == QUOTE_CHAR && i > 0 && sequence.charAt(i - 1) != ESCAPE_CHAR)
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

	public static StringBuilder Serialize(JSON json, StringBuilder buffer, int indent)
	{
		if (buffer == null)
		{
			buffer = new StringBuilder();
		}

		int inner = indent;
		if (inner > -1)
		{
			if (!JSON.IsContext(json))
			{
				inner++;
			}
		}

		boolean isJSAN = IsJSAN(json);
		boolean isFirst = true;

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
					buffer.append(PAIR_CHAR);
				}
				if (indent > -1)
				{
					buffer.append(LINE_WRAP);
					Tools.repeat(buffer, LINE_INDENT, indent + 1);
				}
			}

			if (!isJSAN)
			{
				if (key == null)
				{
					buffer.append(NULL_STRING);
				}
				else
				{
					if (JSON.IsContext(json))
					{
						buffer.append(Context.VAR_DEFINE_MARK);
						buffer.append(' ');
					}
					else
					{
						buffer.append(QUOTE_CHAR);
					}

					buffer.append(EscapeString(entry.getKey()));

					if (JSON.IsContext(json))
					{
						buffer.append(Context.VAR_ASSIGN_CHAR);
					}
					else
					{
						buffer.append(QUOTE_CHAR);
						buffer.append(ATTR_CHAR);
					}
				}
			}

			if (object == null)
			{
				value = NULL_STRING;
			}
			else if (IsJSON(object))
			{
				value = Serialize((JSON) object, null, inner);
			}
			else if (object instanceof CharSequence || object instanceof Character)
			{
				value = QUOTE_CHAR + EscapeString(object.toString()) + QUOTE_CHAR;
			}
			else
			{
				value = object.toString();
			}

			buffer.append(value);

			if (JSON.IsContext(json))
			{
				buffer.append(Context.VAR_END_CHAR);
				buffer.append(LINE_WRAP);
			}
		}

		if (!JSON.IsContext(json))
		{
			if (isJSAN)
			{
				buffer.insert(0, ARRAY_BEGIN_CHAR);
				if (indent > -1)
				{
					buffer.append(LINE_WRAP);
					Tools.repeat(buffer, LINE_INDENT, indent);
				}
				buffer.append(ARRAY_END_CHAR);
			}
			else
			{
				buffer.insert(0, OBJECT_BEGIN_CHAR);
				if (indent > -1)
				{
					buffer.append(LINE_WRAP);
					Tools.repeat(buffer, LINE_INDENT, indent);
				}
				buffer.append(OBJECT_END_CHAR);
			}
		}

		return buffer;
	}

	public static String TrimQuotes(String string)
	{
		string = string.trim();
		if (string.equals(NULL_STRING))
		{
			string = null;
		}
		else
		{
			string = string.replaceFirst("^" + QUOTE_CHAR + "([\\d\\D]*)" + QUOTE_CHAR + "$", "$1");
		}
		return string;
	}

	public static Object ValueOf(Object object)
	{
		return ValueOf(object, null);
	}

	public static Object ValueOf(Object object, Map<Class<?>, Object> templates)
	{
		Object result = null;

		if (object == null || object instanceof String || object instanceof Boolean || object instanceof Number
				|| object instanceof Character || object instanceof JSON || object instanceof Function
				|| object instanceof Quotation)
		{
			result = object;
		}
		else if (object instanceof CharSequence)
		{
			result = object.toString();
		}
		else if (object instanceof BigDecimal)
		{
			result = (BigDecimal) object;
		}
		else if (object instanceof java.util.Calendar)
		{
			result = ((java.util.Calendar) object).getTimeInMillis();
		}
		else if (object instanceof java.util.Date)
		{
			result = ((java.util.Date) object).getTime();
		}
		else if (IsArray(object) || object instanceof Iterable)
		{
			result = JSAN.Reflect(templates, object);
		}
		else
		{
			result = JSON.Reflect(templates, object);
		}

		return result;
	}

	private Map<String, Object>				object;

	private JSON							outer;

	private String							entry;

	private transient Map<Class<?>, Object>	templates;

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

	@SuppressWarnings("unchecked")
	public <E> E attrCast(String key, Class<E> cls)
	{
		Object val = this.attr(key);

		if (!cls.isInstance(val))
		{
			val = null;
		}

		return (E) val;
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

	public boolean containsKey(Object key)
	{
		return object().containsKey(key);
	}

	public boolean containsValue(Object value)
	{
		return object().containsValue(value);
	}

	public JSON context()
	{
		JSON context = this;
		JSON outer = this;

		do
		{
			outer = outer.outer();
			if (IsContext(outer))
			{
				context = outer;
				break;
			}
		} while (outer != null);

		return context;
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

			value = ValueOf(value, templates());

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
				{ // The formal outer would quote the value at its new location.
					if (formalContext == this.context())
					{ // A quote would be made only in the same Context.
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
		JSON.Reflect(this, this.templates(), object);
		return this;
	}

	public JSON reflect(Object object, Iterable<?> fields)
	{
		JSON.Reflect(this, object, fields);
		return this;
	}

	public JSON reflect(Object object, JSON template)
	{
		JSON.Reflect(this, object, template);
		return this;
	}

	public <T> JSON reflect(Object object, JSON.Reflector<T> reflector)
	{
		JSON.Reflect(this, object, reflector);
		return this;
	}

	public JSON reflect(Object object, Map<String, ?> template)
	{
		JSON.Reflect(this, object, template);
		return this;
	}

	public JSON reflect(Object object, String... fields)
	{
		JSON.Reflect(this, object, fields);
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
			json = new JSON().templates(this);
		}

		for (String key : this.keySet())
		{
			json.put(this.attr(key).toString(), key);
		}

		return json;
	}

	public JSON template(Class<?> cls, JSAN template)
	{
		if (cls != null && template != null)
		{
			templatesSingleton();
			templates().put(cls, template);
		}
		return this;
	}

	public JSON template(Class<?> cls, JSON template)
	{
		if (cls != null && template != null)
		{
			templatesSingleton();
			templates().put(cls, template);
		}
		return this;
	}

	public <T> JSON template(Class<T> cls, JSAN.Reflector<T> reflector)
	{
		if (cls != null && reflector != null)
		{
			templatesSingleton();
			templates().put(cls, reflector);
		}
		return this;
	}

	public <T> JSON template(Class<T> cls, JSON.Reflector<T> reflector)
	{
		if (cls != null && reflector != null)
		{
			templatesSingleton();
			templates().put(cls, reflector);
		}
		return this;
	}

	public JSON templateJSAN(Class<?> cls, Iterable<?> fields)
	{
		if (cls != null && fields != null)
		{
			JSAN template = new JSAN();

			for (Object field : fields)
			{
				if (field != null)
				{
					template.add(field);
				}
			}
			template(cls, template);
		}
		return this;
	}

	public JSON templateJSAN(Class<?> cls, Map<String, ?> fields)
	{
		if (cls != null && fields != null)
		{
			JSAN template = new JSAN();

			for (Entry<String, ?> entry : fields.entrySet())
			{
				if (entry.getKey() != null && entry.getValue() != null)
				{
					template.put(entry.getKey(), entry.getValue());
				}
			}
			template(cls, template);
		}
		return this;
	}

	public JSON templateJSAN(Class<?> cls, String... fields)
	{
		if (cls != null && fields != null)
		{
			JSAN template = new JSAN();

			for (String field : fields)
			{
				if (field != null)
				{
					template.add(field);
				}
			}

			template(cls, template);
		}
		return this;
	}

	public JSON templateJSON(Class<?> cls, Iterable<?> fields)
	{
		if (cls != null && fields != null)
		{
			JSON template = new JSON();

			for (Object field : fields)
			{
				if (field != null)
				{
					template.put(field.toString(), field);
				}
			}
			template(cls, template);
		}
		return this;
	}

	public JSON templateJSON(Class<?> cls, Map<String, ?> fields)
	{
		if (cls != null && fields != null)
		{
			JSON template = new JSON();

			for (Entry<String, ?> entry : fields.entrySet())
			{
				if (entry.getKey() != null && entry.getValue() != null)
				{
					template.put(entry.getKey(), entry.getValue());
				}
			}
			template(cls, template);
		}
		return this;
	}

	public JSON templateJSON(Class<?> cls, String... fields)
	{
		if (cls != null && fields != null)
		{
			JSON template = new JSON();

			for (String field : fields)
			{
				if (field != null)
				{
					template.put(field, field);
				}
			}

			template(cls, template);
		}
		return this;
	}

	public Object templateOfClass(Class<?> cls)
	{
		Object template = null;

		if (cls != null && templates() != null)
		{
			template = templates().get(cls);
		}

		return template;
	}

	public Object templateOfObject(Object object)
	{
		Object template = null;

		if (object != null && templates() != null)
		{
			for (Class<?> cls : templates().keySet())
			{
				if (cls != null && cls.isInstance(object) && (template = templateOfClass(cls)) != null)
				{
					break;
				}
			}
		}

		return template;
	}

	public Map<Class<?>, Object> templates()
	{
		return templates;
	}

	public JSON templates(JSON json)
	{
		if (json != null)
		{
			templates(json.templates());
		}
		return this;
	}

	public JSON templates(Map<Class<?>, Object> templates)
	{
		this.templates = templates;
		return this;
	}

	public JSON templatesRemove(Class<?> cls)
	{
		if (cls != null)
		{
			templatesSingleton();
			templates().remove(cls);
		}
		return this;
	}

	protected JSON templatesSingleton()
	{
		if (templates() == null)
		{
			templates(new LinkedHashMap<Class<?>, Object>());
		}
		return this;
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
		return Serialize(this, null, -1).toString();
	}

	public String toString(int indent)
	{
		return Serialize(this, null, indent).insert(0, Tools.repeat(LINE_INDENT, indent)).toString();
	}

	public <E> E val(String key)
	{
		return val(key, null);
	}

	public <E> E val(String key, E defaultValue)
	{
		E val = attr(key);
		return val == null ? defaultValue : val;
	}

	public BigDecimal valBigDecimal(String key)
	{
		BigDecimal val = null;

		Object obj = this.attr(key);

		try
		{
			val = BigDecimal.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = new BigDecimal(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
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
		Boolean val = null;

		Object obj = this.attr(key);

		try
		{
			val = Boolean.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			String str = obj.toString();
			if (TRUE_STRING.equals(str) || FALSE_STRING.equals(str))
			{
				val = Boolean.parseBoolean(str);
			}
		}

		return val;
	}

	public Boolean valBoolean(String key, Boolean defaultValue)
	{
		Boolean val = valBoolean(key);
		return val == null ? defaultValue : val;
	}

	public Byte valByte(String key)
	{
		Byte val = null;

		Object obj = this.attr(key);

		try
		{
			val = Byte.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = Byte.parseByte(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
	}

	public Byte valByte(String key, Byte defaultValue)
	{
		Byte val = valByte(key);
		return val == null ? defaultValue : val;
	}

	public Calendar valCalendar(String key)
	{
		Calendar val = null;

		Long obj = this.valLong(key);

		if (obj != null)
		{
			val = new GregorianCalendar();
			val.setTimeInMillis(obj);
		}

		return val;
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

	public Character valCharacter(String key)
	{
		Character val = null;

		Object obj = this.attr(key);

		try
		{
			val = Character.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = obj.toString().charAt(0);
			}
			catch (StringIndexOutOfBoundsException ex)
			{
			}
		}

		return val;
	}

	public Character valCharacter(String key, Character defaultValue)
	{
		Character val = valCharacter(key);
		return val == null ? defaultValue : val;
	}

	public Date valDate(String key)
	{
		Date val = null;

		Long obj = this.valLong(key);

		if (obj != null)
		{
			val = new Date(obj);
		}

		return val;
	}

	public Date valDate(String key, long defaultValue)
	{
		Date val = valDate(key);
		return val == null ? new Date(defaultValue) : val;
	}

	public Double valDouble(String key)
	{
		Double val = null;

		Object obj = this.attr(key);

		try
		{
			val = Double.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = Double.parseDouble(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
	}

	public Double valDouble(String key, Double defaultValue)
	{
		Double val = valDouble(key);
		return val == null ? defaultValue : val;
	}

	public Float valFloat(String key)
	{
		Float val = null;

		Object obj = this.attr(key);

		try
		{
			val = Float.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = Float.parseFloat(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
	}

	public Float valFloat(String key, Float defaultValue)
	{
		Float val = valFloat(key);
		return val == null ? defaultValue : val;
	}

	public Function valFunction(String key)
	{
		Function val = null;

		Object obj = this.attr(key);

		try
		{
			val = Function.class.cast(obj);
		}
		catch (ClassCastException e)
		{
		}

		return val;
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
		Integer val = null;

		Object obj = this.attr(key);

		try
		{
			val = Integer.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = Integer.parseInt(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
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
		Long val = null;

		Object obj = this.attr(key);

		try
		{
			val = Long.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = Long.parseLong(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
	}

	public Long valLong(String key, Long defaultValue)
	{
		Long val = valLong(key);
		return val == null ? defaultValue : val;
	}

	public Short valShort(String key)
	{
		Short val = null;

		Object obj = this.attr(key);

		try
		{
			val = Short.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				val = Short.parseShort(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return val;
	}

	public Short valShort(String key, Short defaultValue)
	{
		Short val = valShort(key);
		return val == null ? defaultValue : val;
	}

	public String valString(String key)
	{
		String val = null;

		Object obj = this.attr(key);

		try
		{
			val = String.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			val = obj.toString();
		}

		return val;
	}

	public String valString(String key, String defaultValue)
	{
		String val = valString(key);
		return val == null ? defaultValue : val;
	}

	public Collection<Object> values()
	{
		return object().values();
	}
}
