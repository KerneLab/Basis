package org.kernelab.basis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
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

		public Context read(File file)
		{
			try
			{
				reader.setDataFile(file).read();
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			return this;
		}

		public Context read(File file, String charSetName)
		{
			try
			{
				reader.setDataFile(file, charSetName).read();
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			return this;
		}

		public Context read(InputStream is)
		{
			reader.setInputStream(is).read();
			return this;
		}

		public Context read(InputStream is, String charSetName)
		{
			try
			{
				reader.setInputStream(is, charSetName).read();
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
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

	/**
	 * A class to describe Array object using JSON format.
	 * 
	 * @author Dilly King
	 * 
	 */
	public static class JSAN extends JSON implements Iterable<Object>
	{
		private class ArrayIterator implements ListIterator<Object>
		{
			private int	cursor;

			private int	last;

			public ArrayIterator(int index)
			{
				cursor = index;
				last = cursor - 1;
			}

			public void add(Object object)
			{
				JSAN.this.add(last, object);
			}

			public boolean hasNext()
			{
				return cursor < JSAN.this.size();
			}

			public boolean hasPrevious()
			{
				return cursor > 0;
			}

			public Object next()
			{
				last = cursor++;
				return JSAN.this.attr(last);
			}

			public int nextIndex()
			{
				return cursor;
			}

			public Object previous()
			{
				last = --cursor;
				return JSAN.this.attr(last);
			}

			public int previousIndex()
			{
				return cursor - 1;
			}

			public void remove()
			{
				JSAN.this.removeByIndex(last);
			}

			public void set(Object object)
			{
				JSAN.this.set(last, object);
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

		public static final String Index(int i)
		{
			String index = INDEX.get(i);

			if (index == null)
			{
				index = String.valueOf(i);
				INDEX.put(i, index);
			}

			return index;
		}

		@SuppressWarnings("unchecked")
		public static JSAN Reflect(JSAN jsan, Object object)
		{
			Object template = null;

			if (object != null)
			{
				if (jsan != null)
				{
					template = jsan.templateOfObject(object);
				}

				if (template == null)
				{
					Collection<String> fields = FieldsOf(object);

					if (fields != null)
					{
						List<String> temp = new LinkedList<String>();

						for (String field : FieldsOf(object))
						{
							temp.add(field);
						}

						template = temp;
					}
				}
			}

			return JSAN.Reflect(jsan, object, (Iterable<String>) template);
		}

		@SuppressWarnings("unchecked")
		public static JSAN Reflect(JSAN jsan, Object object, Iterable<?> template)
		{
			if (object != null)
			{
				if (jsan == null)
				{
					jsan = new JSAN();
				}

				if (IsJSAN(object))
				{
					JSAN obj = (JSAN) object;
					if (jsan == object)
					{
						jsan = (JSAN) object;
					}
					else if (template == null)
					{
						jsan.addAll(obj);
					}
					else
					{
						for (Object o : template)
						{
							try
							{
								if (obj.containsKey(o.toString()))
								{
									jsan.add(obj.attr(o.toString()));
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
						jsan.addAll(obj.values());
					}
					else
					{
						for (Object o : template)
						{
							try
							{
								if (obj.containsKey(o.toString()))
								{
									jsan.add(obj.attr(o.toString()));
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
						jsan.addAll(obj.values());
					}
					else
					{
						for (Object o : template)
						{
							try
							{
								if (obj.containsKey(o.toString()))
								{
									jsan.add(obj.get(o.toString()));
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
						jsan.pairs(obj);
					}
					else
					{
						JSAN temp = new JSAN().templates(jsan).pairs(obj);
						for (Object o : template)
						{
							try
							{
								if (temp.containsKey(o.toString()))
								{
									jsan.add(temp.attr(o.toString()));
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
					String type = object.getClass().getComponentType().getCanonicalName();
					JSAN temp = new JSAN().templates(jsan);
					if ("boolean".equals(type))
					{
						temp.addAll((boolean[]) object);
					}
					else if ("char".equals(type))
					{
						temp.addAll((char[]) object);
					}
					else if ("byte".equals(type))
					{
						temp.addAll((byte[]) object);
					}
					else if ("short".equals(type))
					{
						temp.addAll((short[]) object);
					}
					else if ("int".equals(type))
					{
						temp.addAll((int[]) object);
					}
					else if ("long".equals(type))
					{
						temp.addAll((long[]) object);
					}
					else if ("float".equals(type))
					{
						temp.addAll((float[]) object);
					}
					else if ("double".equals(type))
					{
						temp.addAll((double[]) object);
					}
					else
					{
						temp.addAll((Object[]) object);
					}

					if (template == null)
					{
						jsan.addAll(temp);
					}
					else
					{
						for (Object o : template)
						{
							try
							{
								if (temp.containsKey(o.toString()))
								{
									jsan.add(temp.attr(o.toString()));
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
								jsan.add(method.invoke(object));
							}
						}
						catch (Exception e)
						{
						}
					}
				}
			}
			else
			{ // Template is null.
				jsan = JSAN.Reflect(jsan, object);
			}

			return jsan;
		}

		@SuppressWarnings("unchecked")
		public static <T> JSAN Reflect(JSAN jsan, Object object, JSAN.Reflector<T> reflector)
		{
			try
			{
				jsan = reflector.reflect(jsan, (T) object);
			}
			catch (ClassCastException e)
			{
			}
			return jsan;
		}

		public static JSAN Reflect(JSAN jsan, Object object, Map<String, ?> template)
		{
			Iterable<?> temp = null;
			if (template != null)
			{
				temp = template.values();
			}
			return JSAN.Reflect(jsan, object, temp);
		}

		@SuppressWarnings("unchecked")
		public static JSAN Reflect(JSAN jsan, Object object, Object template)
		{
			if (template instanceof Map)
			{
				jsan = JSAN.Reflect(jsan, object, (Map<String, String>) template);
			}
			else if (template instanceof Iterable)
			{
				jsan = JSAN.Reflect(jsan, object, (Iterable<String>) template);
			}
			else if (template instanceof String[])
			{
				jsan = JSAN.Reflect(jsan, object, (String[]) template);
			}
			else if (template instanceof JSAN.Reflector)
			{
				jsan = JSAN.Reflect(jsan, object, (JSAN.Reflector<?>) template);
			}
			else if (object instanceof Map || object instanceof Iterable || IsArray(object))
			{
				jsan = JSAN.Reflect(jsan, object, (Iterable<Object>) template);
			}
			else
			{
				jsan = JSAN.Reflect(jsan, object);
			}
			return jsan;
		}

		public static JSAN Reflect(JSAN jsan, Object object, String... fields)
		{
			List<String> template = null;

			if (fields != null)
			{
				template = new LinkedList<String>();

				for (String field : fields)
				{
					template.add(field);
				}
			}
			return JSAN.Reflect(jsan, object, template);
		}

		public static JSON Reflect(Map<Class<?>, Object> templates, Object object)
		{
			JSON json = null;

			if (object != null)
			{
				Object template = null;

				if (templates != null)
				{
					for (Class<?> cls : templates.keySet())
					{
						if (cls != null && cls.isInstance(object) && (template = templates.get(cls)) != null)
						{
							break;
						}
					}
				}

				if (template instanceof Map)
				{
					json = JSON.Reflect(new JSON().templates(templates), object, template);
				}
				else if (template instanceof Iterable)
				{
					json = JSAN.Reflect(new JSAN().templates(templates), object, template);
				}
				else if (template instanceof JSON.Reflector)
				{
					json = JSON.Reflect(new JSON().templates(templates), object, template);
				}
				else if (template instanceof JSAN.Reflector)
				{
					json = JSAN.Reflect(new JSAN().templates(templates), object, template);
				}
				else
				{
					json = JSAN.Reflect(new JSAN().templates(templates), object);
				}
			}

			return json;
		}

		public static JSAN Reflect(Object object)
		{
			return JSAN.Reflect((JSAN) null, object);
		}

		public static JSAN Reflect(Object object, Iterable<?> fields)
		{
			return JSAN.Reflect((JSAN) null, object, fields);
		}

		public static <T> JSAN Reflect(Object object, JSAN.Reflector<T> reflector)
		{
			return JSAN.Reflect((JSAN) null, object, reflector);
		}

		public static JSAN Reflect(Object object, Map<String, ?> template)
		{
			return JSAN.Reflect((JSAN) null, object, template);
		}

		public static JSAN Reflect(Object object, String... fields)
		{
			return JSAN.Reflect((JSAN) null, object, fields);
		}

		public JSAN()
		{
			prototype(new TreeMap<String, Object>(new Comparator<String>() {

				public int compare(String a, String b)
				{
					return Integer.parseInt(a) - Integer.parseInt(b);
				}
			}));
		}

		public JSAN(Object... values)
		{
			this();
			addAll(values);
		}

		public JSAN add(int index, Object object)
		{
			return splice(index, 0, object);
		}

		public JSAN add(Object object)
		{
			return add(LAST, object);
		}

		public JSAN addAll(boolean[] array)
		{
			return addAll(size(), array);
		}

		public JSAN addAll(byte[] array)
		{
			return addAll(size(), array);
		}

		public JSAN addAll(char[] array)
		{
			return addAll(size(), array);
		}

		public JSAN addAll(Collection<? extends Object> collection)
		{
			return addAll(LAST, collection);
		}

		public JSAN addAll(double[] array)
		{
			return addAll(size(), array);
		}

		public JSAN addAll(float[] array)
		{
			return addAll(size(), array);
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

		public <E> JSAN addAll(int index, E... array)
		{
			return splice(index, 0, array);
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
			return addAll(size(), array);
		}

		public JSAN addAll(JSAN jsan)
		{
			return addAll(LAST, jsan);
		}

		public JSAN addAll(long[] array)
		{
			return addAll(size(), array);
		}

		public JSAN addAll(Object[] array)
		{
			return addAll(LAST, array);
		}

		public JSAN addAll(short[] array)
		{
			return addAll(size(), array);
		}

		@SuppressWarnings("unchecked")
		public <E, T extends Collection<E>> T addTo(T collection)
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

		@SuppressWarnings("unchecked")
		public <E> E attr(int index)
		{
			return (E) Quote(this.get(index));
		}

		public JSAN attr(int index, Object value)
		{
			this.set(index, value);
			return this;
		}

		public Boolean attrBoolean(int index)
		{
			return attrCast(index, Boolean.class);
		}

		@SuppressWarnings("unchecked")
		public <E> E attrCast(int index, Class<E> cls)
		{
			return (E) attr(index);
		}

		public Double attrDouble(int index)
		{
			return attrCast(index, Double.class);
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

		public String attrString(int index)
		{
			return attrCast(index, String.class);
		}

		protected int bound(int index)
		{
			return Tools.limitNumber(index(index) + (index < 0 ? 1 : 0), 0, this.size());
		}

		@Override
		public JSAN clone()
		{
			return new JSAN().templates(this).clone(this);
		}

		public boolean contains(Object o)
		{
			return super.containsValue(o);
		}

		public boolean containsAll(Iterable<Object> iterable)
		{
			boolean contains = false;
			if (iterable != null)
			{
				contains = true;
				for (Object o : iterable)
				{
					if (!this.contains(o))
					{
						contains = false;
						break;
					}
				}
			}
			return contains;
		}

		@Override
		public JSAN entry(String entry)
		{
			super.entry(entry);
			return this;
		}

		public boolean equalValues(JSAN jsan)
		{
			return this.size() == jsan.size() && this.containsAll(jsan) && jsan.containsAll(this);
		}

		public Object get(int index)
		{
			index = index(index);

			if (!has(index))
			{
				throw new IndexOutOfBoundsException(Index(index));
			}

			return super.get(Index(index));
		}

		public boolean has(int index)
		{
			return index >= 0 && index < this.size();
		}

		protected int index(int index)
		{
			return index >= 0 ? index : index + this.size();
		}

		public int indexOf(Object object)
		{
			int index = -1;

			int i = 0;

			for (Object o : this)
			{
				if (Tools.equals(object, o))
				{
					index = i;
					break;
				}
				i++;
			}

			return index;
		}

		public ArrayIterator iterator()
		{
			return new ArrayIterator(0);
		}

		public int lastIndexOf(Object object)
		{
			int index = -1;
			if (object == null)
			{
				int i = size();
				ArrayIterator iter = new ArrayIterator(size());
				while (iter.hasPrevious())
				{
					i--;
					if (iter.previous() == null)
					{
						index = i;
						break;
					}
				}
			}
			else
			{
				int i = size();
				ArrayIterator iter = new ArrayIterator(size());
				while (iter.hasPrevious())
				{
					i--;
					if (object.equals(iter.previous()))
					{
						index = i;
						break;
					}
				}
			}
			return index;
		}

		@Override
		public JSAN outer(JSON outer)
		{
			super.outer(outer);
			return this;
		}

		@Override
		public JSAN pairs(Iterable<? extends Object> pairs)
		{
			for (Object o : pairs)
			{
				this.add(o);
			}
			return this;
		}

		@Override
		public JSAN pairs(Map<? extends String, ? extends Object> map)
		{
			for (Map.Entry<?, ?> entry : map.entrySet())
			{
				this.add(entry.getKey());
				this.add(entry.getValue());
			}
			return this;
		}

		@Override
		public JSAN pairs(Object... pairs)
		{
			this.addAll(pairs);
			return this;
		}

		@Override
		protected JSAN prototype(Map<String, Object> map)
		{
			super.prototype(map);
			return this;
		}

		protected Object put(int index, Object value)
		{
			return super.put(Index(index), value);
		}

		@Override
		public JSAN reflect(Object object)
		{
			return JSAN.Reflect(this, object);
		}

		@Override
		public JSAN reflect(Object object, Iterable<?> template)
		{
			return JSAN.Reflect(this, object, template);
		}

		public <T> JSAN reflect(Object object, JSAN.Reflector<T> reflector)
		{
			return JSAN.Reflect(this, object, reflector);
		}

		@Override
		public JSAN reflect(Object object, Map<String, ?> template)
		{
			return JSAN.Reflect(this, object, template.values());
		}

		@Override
		public JSAN reflect(Object object, String... fields)
		{
			return JSAN.Reflect(this, object, fields);
		}

		@Override
		public Object remove(Object object)
		{
			int index = indexOf(object);

			if (index == -1)
			{
				return null;
			}
			else
			{
				return this.removeByIndex(index);
			}
		}

		@Override
		public JSAN removeAll()
		{
			super.removeAll();
			return this;
		}

		@Override
		public JSAN removeAll(Iterable<? extends Object> iterable)
		{
			if (iterable != null)
			{
				for (Object o : iterable)
				{
					if (super.containsValue(o))
					{
						this.remove(o);
					}
				}
			}
			return this;
		}

		public JSAN removeAllByIndex(int... indexes)
		{
			for (int index : indexes)
			{
				this.removeByIndex(index);
			}
			return this;
		}

		public JSAN removeAllByIndex(Iterable<Integer> indexes)
		{
			for (Integer index : indexes)
			{
				if (index != null)
				{
					this.removeByIndex(index);
				}
			}
			return this;
		}

		public Object removeByIndex(int index)
		{
			Object object = this.attr(index);

			splice(index, 1);

			return object;
		}

		public JSAN retainAll(Collection<Object> collection)
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
				this.removeAll(temp);
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
				this.removeAll(temp);
			}
			return this;
		}

		public Object set(int index, Object object)
		{
			Object old = this.get(index);

			super.put(Index(index), object);

			return old;
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

			start = bound(start);
			end = bound(end);

			for (int i = start; i < end; i++)
			{
				jsan.add(this.get(i));
			}

			return jsan;
		}

		public <T extends Object> JSAN splice(int index, int cover, Collection<T> collection)
		{
			int trace = this.size();
			index = bound(index);
			cover = Tools.limitNumber(cover, 0, trace - index);
			trace--;

			int fills = collection.size();
			int delta = fills - cover;
			int tail = index + cover;

			int i = 0;

			if (delta > 0)
			{
				for (i = trace; i >= tail; i--)
				{
					this.put(i + delta, this.get(i));
				}
			}
			else if (delta < 0)
			{
				for (i = tail; i <= trace; i++)
				{
					this.put(i + delta, this.get(i));
				}
				for (i = trace + delta + 1; i <= trace; i++)
				{
					super.remove(Index(i));
				}
			}

			i = 0;
			for (T object : collection)
			{
				this.put(index + i++, object);
			}

			return this;
		}

		public JSAN splice(int index, int cover, JSAN jsan)
		{
			int trace = this.size();
			index = bound(index);
			cover = Tools.limitNumber(cover, 0, trace - index);
			trace--;

			int fills = jsan.size();
			int delta = fills - cover;
			int tail = index + cover;

			int i = 0;

			if (delta > 0)
			{
				for (i = trace; i >= tail; i--)
				{
					this.put(i + delta, this.get(i));
				}
			}
			else if (delta < 0)
			{
				for (i = tail; i <= trace; i++)
				{
					this.put(i + delta, this.get(i));
				}
				for (i = trace + delta + 1; i <= trace; i++)
				{
					super.remove(Index(i));
				}
			}

			for (i = 0; i < fills; i++)
			{
				this.put(index + i, jsan.get(i));
			}

			return this;
		}

		public <T extends Object> JSAN splice(int index, int cover, T... objects)
		{
			int trace = this.size();
			index = bound(index);
			cover = Tools.limitNumber(cover, 0, trace - index);
			trace--;

			int fills = objects.length;
			int delta = fills - cover;
			int tail = index + cover;

			int i = 0;

			if (delta > 0)
			{
				for (i = trace; i >= tail; i--)
				{
					this.put(i + delta, this.get(i));
				}
			}
			else if (delta < 0)
			{
				for (i = tail; i <= trace; i++)
				{
					this.put(i + delta, this.get(i));
				}
				for (i = trace + delta + 1; i <= trace; i++)
				{
					super.remove(Index(i));
				}
			}

			for (i = 0; i < fills; i++)
			{
				this.put(index + i, objects[i]);
			}

			return this;
		}

		@Override
		public JSAN template(Class<?> cls, Iterable<?> template)
		{
			super.template(cls, template);
			return this;
		}

		@Override
		public JSAN template(Class<?> cls, Map<String, ?> template)
		{
			super.template(cls, template);
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
		public JSAN templateJSAN(Class<?> cls, Iterable<?> template)
		{
			super.templateJSAN(cls, template);
			return this;
		}

		@Override
		public JSAN templateJSAN(Class<?> cls, Map<String, ?> template)
		{
			super.templateJSAN(cls, template);
			return this;
		}

		@Override
		public JSAN templateJSAN(Class<?> cls, String... fields)
		{
			super.templateJSAN(cls, fields);
			return this;
		}

		@Override
		public JSAN templateJSON(Class<?> cls, Iterable<?> template)
		{
			super.templateJSON(cls, template);
			return this;
		}

		@Override
		public JSAN templateJSON(Class<?> cls, Map<String, ?> template)
		{
			super.templateJSON(cls, template);
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
					value = Integer.valueOf(string);
				}
				catch (NumberFormatException e)
				{
					value = Long.valueOf(string);
				}
			}
			else if (Variable.isDouble(string))
			{
				value = Double.valueOf(string);
			}
			else if (TRUE_STRING.equals(string) || FALSE_STRING.equals(string))
			{
				value = Boolean.valueOf(string);
			}
			else if (NULL_STRING.equals(string))
			{
				value = null;
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

	public static JSON Reflect(JSON json, Object object)
	{
		Object template = null;

		if (object != null)
		{
			if (json != null)
			{
				template = json.templateOfObject(object);
			}

			if (template == null)
			{
				Collection<String> fields = FieldsOf(object);

				if (fields != null)
				{
					Map<String, String> temp = new LinkedHashMap<String, String>();

					for (String field : fields)
					{
						temp.put(field, field);
					}

					template = temp;
				}
			}
		}

		return JSON.Reflect(json, object, template);
	}

	public static JSON Reflect(JSON json, Object object, Iterable<?> fields)
	{
		Map<String, String> template = null;

		if (fields != null)
		{
			template = new LinkedHashMap<String, String>();

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
	public static <T> JSON Reflect(JSON json, Object object, JSON.Reflector<T> reflector)
	{
		try
		{
			json = reflector.reflect(json, (T) object);
		}
		catch (ClassCastException e)
		{
		}
		return json;
	}

	@SuppressWarnings("unchecked")
	public static JSON Reflect(JSON json, Object object, Map<String, ?> template)
	{
		if (object != null)
		{
			if (json == null)
			{
				json = new JSON();
			}

			if (IsJSON(object))
			{
				JSON obj = (JSON) object;
				if (json == object)
				{
					json = obj;
				}
				else if (template == null)
				{
					json.putAll(obj);
				}
				else
				{
					for (Map.Entry<String, ?> entry : template.entrySet())
					{
						try
						{
							if (obj.containsKey(entry.getValue()))
							{
								json.put(entry.getKey(), obj.attr(entry.getValue().toString()));
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
					for (Map.Entry<String, ?> entry : template.entrySet())
					{
						if (obj.containsKey(entry.getValue()))
						{
							json.put(entry.getKey(), obj.get(entry.getValue()));
						}
					}
				}
			}
			else if (object instanceof Iterable)
			{
				Iterable<Object> obj = (Iterable<Object>) object;
				JSAN jsan = new JSAN().templates(json);
				jsan.pairs(obj);
				if (template == null)
				{
					json.putAll(jsan);
				}
				else
				{
					for (Map.Entry<String, ?> entry : template.entrySet())
					{
						try
						{
							if (jsan.containsKey(entry.getValue().toString()))
							{
								json.put(entry.getKey(), jsan.attr(entry.getValue().toString()));
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
				json.putAll(JSAN.Reflect(new JSAN().templates(json), object, template));
			}
			else if (template != null)
			{
				// Reflect Object using template
				Class<?> cls = object.getClass();
				for (Map.Entry<String, ?> entry : template.entrySet())
				{
					try
					{
						if (entry.getKey() != null)
						{
							String name = entry.getValue().toString();

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
									json.attr(entry.getKey(), method.invoke(object));
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
			{ // Template is null.
				json = JSON.Reflect(json, object);
			}
		}

		return json;
	}

	@SuppressWarnings("unchecked")
	public static JSON Reflect(JSON json, Object object, Object template)
	{
		if (template instanceof Map)
		{
			json = JSON.Reflect(json, object, (Map<String, String>) template);
		}
		else if (template instanceof Iterable)
		{
			json = JSON.Reflect(json, object, (Iterable<String>) template);
		}
		else if (template instanceof String[])
		{
			json = JSON.Reflect(json, object, (String[]) template);
		}
		else if (template instanceof JSON.Reflector)
		{
			json = JSON.Reflect(json, object, (JSON.Reflector<?>) template);
		}
		else if (object instanceof Map || object instanceof Iterable || IsArray(object))
		{
			json = JSON.Reflect(json, object, (Map<String, Object>) template);
		}
		else
		{
			json = JSON.Reflect(json, object);
		}
		return json;
	}

	public static JSON Reflect(JSON json, Object object, String... fields)
	{
		Map<String, String> template = null;

		if (fields != null)
		{
			template = new LinkedHashMap<String, String>();

			for (String field : fields)
			{
				template.put(field, field);
			}
		}
		return JSON.Reflect(json, object, template);
	}

	public static JSON Reflect(Map<Class<?>, Object> templates, Object object)
	{
		JSON json = null;

		if (object != null)
		{
			Object template = null;

			if (templates != null)
			{
				for (Class<?> cls : templates.keySet())
				{
					if (cls != null && cls.isInstance(object) && (template = templates.get(cls)) != null)
					{
						break;
					}
				}
			}

			if (template instanceof Map)
			{
				json = JSON.Reflect(new JSON().templates(templates), object, template);
			}
			else if (template instanceof Iterable)
			{
				json = JSAN.Reflect(new JSAN().templates(templates), object, template);
			}
			else if (template instanceof JSON.Reflector)
			{
				json = JSON.Reflect(new JSON().templates(templates), object, template);
			}
			else if (template instanceof JSAN.Reflector)
			{
				json = JSAN.Reflect(new JSAN().templates(templates), object, template);
			}
			else
			{
				json = JSON.Reflect(new JSON().templates(templates), object);
			}
		}

		return json;
	}

	public static JSON Reflect(Object object)
	{
		return JSON.Reflect((JSON) null, object);
	}

	public static JSON Reflect(Object object, Iterable<?> fields)
	{
		return JSON.Reflect((JSON) null, object, fields);
	}

	public static <T> JSON Reflect(Object object, JSON.Reflector<T> reflector)
	{
		return JSON.Reflect((JSON) null, object, reflector);
	}

	public static JSON Reflect(Object object, Map<String, ?> template)
	{
		return JSON.Reflect((JSON) null, object, template);
	}

	public static JSON Reflect(Object object, String... fields)
	{
		return JSON.Reflect((JSON) null, object, fields);
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
				|| object instanceof Character || object instanceof JSON || object instanceof Quotation)
		{
			result = object;
		}
		else if (object instanceof CharSequence)
		{
			result = object.toString();
		}
		else if (object instanceof java.util.Calendar)
		{
			result = ((java.util.Calendar) object).getTimeInMillis();
		}
		else if (object instanceof java.util.Date)
		{
			result = ((java.util.Date) object).getTime();
		}
		else
		{
			result = JSON.Reflect(templates, object);
		}

		return result;
	}

	private Map<String, Object>				map;

	private JSON							outer;

	private String							entry;

	private transient Map<Class<?>, Object>	templates;

	public JSON()
	{
		prototype(new LinkedHashMap<String, Object>());
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

	public Boolean attrBoolean(String key)
	{
		Boolean value = null;

		Object obj = this.attr(key);

		try
		{
			value = Boolean.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			String str = obj.toString();
			if (TRUE_STRING.equals(str) || FALSE_STRING.equals(str))
			{
				value = Boolean.valueOf(str);
			}
		}

		return value;
	}

	public Byte attrByte(String key)
	{
		Byte value = null;

		Object obj = this.attr(key);

		try
		{
			value = Byte.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				value = Byte.valueOf(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return value;
	}

	@SuppressWarnings("unchecked")
	public <E> E attrCast(String key, Class<E> cls)
	{
		Object value = this.attr(key);

		if (!cls.isInstance(value))
		{
			value = null;
		}

		return (E) value;
	}

	public Character attrCharacter(String key)
	{
		Character value = null;

		Object obj = this.attr(key);

		try
		{
			value = Character.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				value = obj.toString().charAt(0);
			}
			catch (StringIndexOutOfBoundsException ex)
			{
			}
		}

		return value;
	}

	public Double attrDouble(String key)
	{
		Double value = null;

		Object obj = this.attr(key);

		try
		{
			value = Double.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				value = Double.valueOf(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return value;
	}

	public Float attrFloat(String key)
	{
		Float value = null;

		Object obj = this.attr(key);

		try
		{
			value = Float.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				value = Float.valueOf(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return value;
	}

	public Integer attrInteger(String key)
	{
		Integer value = null;

		Object obj = this.attr(key);

		try
		{
			value = Integer.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				value = Integer.valueOf(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return value;
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
		Long value = null;

		Object obj = this.attr(key);

		try
		{
			value = Long.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				value = Long.valueOf(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return value;
	}

	public Short attrShort(String key)
	{
		Short value = null;

		Object obj = this.attr(key);

		try
		{
			value = Short.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			try
			{
				value = Short.valueOf(obj.toString());
			}
			catch (NumberFormatException ex)
			{
			}
		}

		return value;
	}

	public String attrString(String key)
	{
		String value = null;

		Object obj = this.attr(key);

		try
		{
			value = String.class.cast(obj);
		}
		catch (ClassCastException e)
		{
			value = obj.toString();
		}

		return value;
	}

	public void clear()
	{
		rescind();
		map.clear();
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
		return map.containsKey(key);
	}

	public boolean containsValue(Object value)
	{
		boolean contains = false;

		for (Pair p : this.pairs())
		{
			if (Tools.equals(value, p.getValue()))
			{
				contains = true;
				break;
			}
		}

		return contains;
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
		return map.entrySet();
	}

	@Override
	protected void finalize() throws Throwable
	{
		outer(null).entry(null).clear();
		super.finalize();
	}

	public Object get(Object key)
	{
		return map.get(key);
	}

	public boolean has(String entry)
	{
		return map.containsKey(entry);
	}

	public boolean isEmpty()
	{
		return map.isEmpty();
	}

	public Set<String> keySet()
	{
		return map.keySet();
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

	protected Map<String, Object> prototype()
	{
		return map;
	}

	protected JSON prototype(Map<String, Object> map)
	{
		if (map != null)
		{
			this.map = map;
		}
		return this;
	}

	public Object put(String key, Object value)
	{
		if (key != null)
		{
			Object old = this.get(key);
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

			map.put(key, value);
		}

		return value;
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
		return JSON.Reflect(this, object);
	}

	public JSON reflect(Object object, Iterable<?> fields)
	{
		return JSON.Reflect(this, object, fields);
	}

	public <T> JSON reflect(Object object, JSON.Reflector<T> reflector)
	{
		return JSON.Reflect(this, object, reflector);
	}

	public JSON reflect(Object object, Map<String, ?> template)
	{
		return JSON.Reflect(this, object, template);
	}

	public JSON reflect(Object object, String... fields)
	{
		return JSON.Reflect(this, object, fields);
	}

	public Object remove(Object key)
	{
		Object value = map.remove(key);

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
		this.clear();
		return this;
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
		return map.size();
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

	public JSON template(Class<?> cls, Iterable<?> template)
	{
		if (cls != null && template != null)
		{
			templatesSingleton();
			templates().put(cls, template);
		}
		return this;
	}

	public JSON template(Class<?> cls, Map<String, ?> template)
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

	public JSON templateJSAN(Class<?> cls, Iterable<?> template)
	{
		return this.template(cls, template);
	}

	public JSON templateJSAN(Class<?> cls, Map<String, ?> template)
	{
		if (cls != null && template != null)
		{
			this.template(cls, template.values());
		}
		return this;
	}

	public JSON templateJSAN(Class<?> cls, String... fields)
	{
		if (cls != null && fields != null)
		{
			List<String> template = new LinkedList<String>();

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

	public JSON templateJSON(Class<?> cls, Iterable<?> template)
	{
		if (cls != null && template != null)
		{
			Map<String, Object> map = new LinkedHashMap<String, Object>();

			for (Object field : template)
			{
				if (field != null)
				{
					map.put(field.toString(), field);
				}
			}

			this.template(cls, map);
		}
		return this;
	}

	public JSON templateJSON(Class<?> cls, Map<String, ?> template)
	{
		return this.template(cls, template);
	}

	public JSON templateJSON(Class<?> cls, String... fields)
	{
		if (cls != null && fields != null)
		{
			Map<String, Object> template = new LinkedHashMap<String, Object>();

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
		JSAN jsan = null;

		if (IsJSAN(this))
		{
			jsan = (JSAN) this;
		}
		else if (IsJSON(this))
		{
			jsan = new JSAN().templates(this);
			jsan.addAll(this.values());
		}

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

	public Boolean valBoolean(String key)
	{
		return attrBoolean(key);
	}

	public Boolean valBoolean(String key, Boolean defaultValue)
	{
		Boolean val = valBoolean(key);
		return val == null ? defaultValue : val;
	}

	public Byte valByte(String key)
	{
		return attrByte(key);
	}

	public Byte valByte(String key, Byte defaultValue)
	{
		Byte val = valByte(key);
		return val == null ? defaultValue : val;
	}

	public Character valCharacter(String key)
	{
		return attrCharacter(key);
	}

	public Character valCharacter(String key, Character defaultValue)
	{
		Character val = valCharacter(key);
		return val == null ? defaultValue : val;
	}

	public Double valDouble(String key)
	{
		return attrDouble(key);
	}

	public Double valDouble(String key, Double defaultValue)
	{
		Double val = valDouble(key);
		return val == null ? defaultValue : val;
	}

	public Float valFloat(String key)
	{
		return attrFloat(key);
	}

	public Float valFloat(String key, Float defaultValue)
	{
		Float val = valFloat(key);
		return val == null ? defaultValue : val;
	}

	public Integer valInteger(String key)
	{
		return attrInteger(key);
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

	public JSAN valJSAN(String key, JSAN defaultValue)
	{
		JSAN val = valJSAN(key);
		return val == null ? defaultValue : val;
	}

	public JSON valJSON(String key)
	{
		return attrJSON(key);
	}

	public JSON valJSON(String key, JSON defaultValue)
	{
		JSON val = valJSON(key);
		return val == null ? defaultValue : val;
	}

	public Long valLong(String key)
	{
		return attrLong(key);
	}

	public Long valLong(String key, Long defaultValue)
	{
		Long val = valLong(key);
		return val == null ? defaultValue : val;
	}

	public Short valShort(String key)
	{
		return attrShort(key);
	}

	public Short valShort(String key, Short defaultValue)
	{
		Short val = valShort(key);
		return val == null ? defaultValue : val;
	}

	public String valString(String key)
	{
		return attrString(key);
	}

	public String valString(String key, String defaultValue)
	{
		String val = valString(key);
		return val == null ? defaultValue : val;
	}

	public Collection<Object> values()
	{
		return map.values();
	}
}
