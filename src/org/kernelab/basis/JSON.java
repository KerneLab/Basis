package org.kernelab.basis;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * A light weighted class of JSON.
 * 
 * @author Dilly King
 */
public class JSON extends LinkedHashMap<String, Object> implements JSONHierarchical
{

	/**
	 * A class to describe Array object using JSON format.
	 * 
	 * @author Dilly King
	 * 
	 */
	public static class JSAN extends JSON implements Iterable<Object>
	{
		private class JSANListIterator implements ListIterator<Object>
		{
			private int	cursor;

			private int	last;

			public JSANListIterator(int index)
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
				JSAN.this.remove(last);
			}

			public void set(Object object)
			{
				JSAN.this.set(last, object);
			}
		}

		private static final Map<Integer, String>	INDEX				= new HashMap<Integer, String>();

		/**
                 * 
                 */
		private static final long					serialVersionUID	= -4343125403853941129L;

		public static final String Index(int i)
		{
			String index = INDEX.get(i);
			if (index == null) {
				index = String.valueOf(i);
				INDEX.put(i, index);
			}
			return index;
		}

		public JSAN()
		{
			super();
		}

		protected JSAN(JSAN jsan)
		{
			super(jsan);
		}

		public JSAN add(int index, Object object)
		{
			return splice(index, 0, object);
		}

		public JSAN add(Object object)
		{
			return splice(this.size(), 0, object);
		}

		public JSAN addAll(Iterable<? extends Object> iterable)
		{
			return splice(this.size(), 0, iterable);
		}

		public JSAN addAll(Object[] array)
		{
			return splice(this.size(), 0, array);
		}

		public Collection<Object> addTo(Collection<Object> collection)
		{
			if (collection == null) {
				collection = new LinkedList<Object>();
			}
			for (Object o : this) {
				collection.add(o);
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

		@Override
		public JSAN clone()
		{
			return new JSAN(this);
		}

		public boolean contains(Object o)
		{
			return super.containsValue(o);
		}

		public boolean containsAll(Iterable<Object> iterable)
		{
			boolean contains = false;
			if (iterable != null) {
				contains = true;
				for (Object o : iterable) {
					if (!this.contains(o)) {
						contains = false;
						break;
					}
				}
			}
			return contains;
		}

		public boolean equalValues(JSAN jsan)
		{
			return this.size() == jsan.size() && this.containsAll(jsan)
					&& jsan.containsAll(this);
		}

		public Object get(int index)
		{
			if (!has(index)) {
				throw new NoSuchElementException();
			}

			return super.get(Index(index));
		}

		public boolean has(int index)
		{
			return index >= 0 && index < this.size();
		}

		public int indexOf(Object object)
		{
			int index = -1;
			if (object == null) {
				int i = 0;
				for (Object o : this) {
					if (o == null) {
						index = i;
						break;
					}
					i++;
				}
			} else {
				int i = 0;
				for (Object o : this) {
					if (object.equals(o)) {
						index = i;
						break;
					}
					i++;
				}
			}
			return index;
		}

		public JSANListIterator iterator()
		{
			return new JSANListIterator(0);
		}

		public int lastIndexOf(Object object)
		{
			int index = -1;
			if (object == null) {
				int i = size();
				JSANListIterator iter = new JSANListIterator(size());
				while (iter.hasPrevious()) {
					i--;
					if (iter.previous() == null) {
						index = i;
						break;
					}
				}
			} else {
				int i = size();
				JSANListIterator iter = new JSANListIterator(size());
				while (iter.hasPrevious()) {
					i--;
					if (object.equals(iter.previous())) {
						index = i;
						break;
					}
				}
			}
			return index;
		}

		public Object remove(int index)
		{
			if (!has(index)) {
				throw new NoSuchElementException();
			}

			Object object = this.get(index);

			for (int i = index + 1; i < this.size(); i++) {
				super.put(Index(i - 1), this.get(i));
			}

			super.remove(Index(this.size() - 1));

			return object;
		}

		@Override
		public Object remove(Object object)
		{
			return this.remove(indexOf(object));
		}

		@Override
		public JSAN removeAll(Iterable<? extends Object> iterable)
		{
			if (iterable != null) {
				for (Object o : iterable) {
					if (super.containsValue(o)) {
						this.remove(o);
					}
				}
			}
			return this;
		}

		public JSAN retainAll(Collection<Object> collection)
		{
			if (collection != null && !collection.isEmpty()) {
				List<Object> temp = new LinkedList<Object>();
				for (Object o : this) {
					if (!collection.contains(o)) {
						temp.add(o);
					}
				}
				this.removeAll(temp);
			}
			return this;
		}

		public JSAN retainAll(JSAN jsan)
		{
			if (jsan != null && !jsan.isEmpty()) {
				List<Object> temp = new LinkedList<Object>();
				for (Object o : this) {
					if (!jsan.contains(o)) {
						temp.add(o);
					}
				}
				this.removeAll(temp);
			}
			return this;
		}

		public Object set(int index, Object object)
		{
			if (!has(index)) {
				throw new NoSuchElementException();
			}

			Object old = this.get(index);
			super.put(Index(index), object);

			return old;
		}

		public JSAN splice(int index, int cover, Collection<Object> collection)
		{
			index = Tools.limitNumber(index, 0, this.size());
			cover = Tools.limitNumber(cover, 0, this.size());

			int delta = collection.size() - cover;
			int total = this.size();

			List<Object> buffer = new LinkedList<Object>();

			for (int i = index + cover; i < total; i++) {
				buffer.add(this.get(i));
			}

			int j = index + cover;
			for (Object object : buffer) {
				super.put(Index(delta + j++), object);
			}

			for (int i = total + delta; i < total; i++) {
				super.remove(Index(i));
			}

			for (Object object : collection) {
				super.put(Index(index++), object);
			}

			return this;
		}

		public JSAN splice(int index, int cover, JSAN jsan)
		{
			index = Tools.limitNumber(index, 0, this.size());
			cover = Tools.limitNumber(cover, 0, this.size());

			int delta = jsan.size() - cover;
			int total = this.size();

			List<Object> buffer = new LinkedList<Object>();

			for (int i = index + cover; i < total; i++) {
				buffer.add(this.get(i));
			}

			int j = index + cover;
			for (Object object : buffer) {
				super.put(Index(delta + j++), object);
			}

			for (int i = total + delta; i < total; i++) {
				super.remove(Index(i));
			}

			for (Object object : jsan) {
				super.put(Index(index++), object);
			}

			return this;
		}

		public JSAN splice(int index, int cover, Object... objects)
		{
			index = Tools.limitNumber(index, 0, this.size());
			cover = Tools.limitNumber(cover, 0, this.size());

			int delta = objects.length - cover;
			int total = this.size();

			List<Object> buffer = new LinkedList<Object>();

			for (int i = index + cover; i < total; i++) {
				buffer.add(this.get(i));
			}

			int j = index + cover;
			for (Object object : buffer) {
				super.put(Index(delta + j++), object);
			}

			for (int i = total + delta; i < total; i++) {
				super.remove(Index(i));
			}

			for (Object object : objects) {
				super.put(Index(index++), object);
			}

			return this;
		}

		public Object[] toArray()
		{
			Object[] array = new Object[size()];

			int i = 0;
			for (Object o : this) {
				array[i++] = o;
			}

			return array;
		}

		public Object[] toArray(Object[] array)
		{
			if (array.length < size()) {
				array = (Object[]) java.lang.reflect.Array.newInstance(Object.class,
						size());
			}

			int i = 0;
			for (Object o : this) {
				array[i++] = o;
			}

			for (; i < array.length;) {
				array[i++] = null;
			}

			return array;
		}
	}

	public static class JSONSyntaxException extends RuntimeException
	{

		/**
		 * 
		 */
		private static final long	serialVersionUID	= 5584855948726666241L;

		public JSONSyntaxException(CharSequence source, int index)
		{
			super("Near\n"
					+ source.subSequence(Math.max(index - 30, 0),
							Math.min(index + 30, source.length())));
		}
	}

	/**
	 * 
	 */
	private static final long						serialVersionUID		= -4977780486670612620L;

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

	public static final Object						NOT_A_VALUE				= new Object();

	public static final String						NULL_STRING				= "null";

	public static final String						TRUE_STRING				= "true";

	public static final String						FALSE_STRING			= "false";

	public static final char						UNICODE_ESCAPING_CHAR	= 'u';

	public static final int							UNICODE_ESCAPED_LENGTH	= 4;

	public static final int							UNICODE_ESCAPE_RADIX	= 16;

	public static final Map<Character, Character>	ESCAPING_CHAR			= new HashMap<Character, Character>();

	public static final Map<Character, String>		ESCAPED_CHAR			= new HashMap<Character, String>();

	static {
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

	public static final JSONHierarchical AsHierarchical(Object o)
	{
		return Tools.as(o, JSONHierarchical.class);
	}

	@SuppressWarnings("unchecked")
	public static final Iterable<Object> AsIterable(Object o)
	{
		Iterable<Object> iter = null;
		if (o instanceof Iterable<?>) {
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
		if (o instanceof Map<?, ?>) {
			map = (Map<String, Object>) o;
		}
		return map;
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

		i: for (int i = Math.max(0, from); i < sequence.length(); i++) {

			char c = sequence.charAt(i);

			if (c == QUOTE_CHAR && i > 0 && sequence.charAt(i - 1) != ESCAPE_CHAR) {
				inString = !inString;
			}
			if (inString) {
				continue i;
			}

			if (c == a) {
				match++;
			} else if (c == b) {
				match--;
				if (match == 0) {
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
		for (int i = 0; i < buffer.length(); i++) {
			c = buffer.charAt(i);
			if (CharacterNeedToEscape(c)) {
				buffer.deleteCharAt(i);
				escape = ESCAPED_CHAR.get(c);
				buffer.insert(i, escape);
				i += escape.length() - 1;
			}
		}

		return buffer.toString();
	}

	public static final int FirstNonWhitespaceIndex(CharSequence sequence, int from)
	{
		int index = -1;

		for (int i = from; i < sequence.length(); i++) {
			if (!Character.isWhitespace(sequence.charAt(i))) {
				index = i;
				break;
			}
		}

		return index;
	}

	public static final int FirstWhitespaceIndex(CharSequence sequence, int from)
	{
		int index = -1;

		for (int i = from; i < sequence.length(); i++) {
			if (Character.isWhitespace(sequence.charAt(i))) {
				index = i;
				break;
			}
		}

		return index;
	}

	public static final boolean IsContext(Object o)
	{
		return o instanceof JSONContext;
	}

	public static final boolean IsHierarchical(Object o)
	{
		return o instanceof JSONHierarchical;
	}

	public static final boolean IsJSAN(Object o)
	{
		return o instanceof JSAN;
	}

	public static final boolean IsJSON(Object o)
	{
		return o instanceof JSON;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String s = "{1,[\"2]}";
		JSON json = JSON.Parse(s);
		Tools.debug(json.toString());
	}

	public static JSON Parse(CharSequence source)
	{
		return Parse(source, null);
	}

	public static JSON Parse(CharSequence source, JSON object)
	{
		return Parse(source, object, null);
	}

	public static JSON Parse(CharSequence source, JSON object, JSONContext context)
	{
		String string = source.toString().trim();

		if (!(string.startsWith(OBJECT_BEGIN_MARK) && string.endsWith(OBJECT_END_MARK))
				&& !(string.startsWith(ARRAY_BEGIN_MARK) && string
						.endsWith(ARRAY_END_MARK)))
		{
			return null;
		}

		StringBuilder json = new StringBuilder(string);

		if (object == null) {
			if (string.startsWith(ARRAY_BEGIN_MARK) && string.endsWith(ARRAY_END_MARK)) {
				object = new JSAN();
			} else if (string.startsWith(OBJECT_BEGIN_MARK)
					&& string.endsWith(OBJECT_END_MARK))
			{
				object = new JSON();
			} else {
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

		try {

			i: for (i = 0; i < json.length(); i++) {

				c = json.charAt(i);

				if (c == ESCAPE_CHAR) {
					json.deleteCharAt(i);
					c = json.charAt(i);
					if (ESCAPING_CHAR.containsKey(c)) {
						json.deleteCharAt(i);
						json.insert(i, ESCAPING_CHAR.get(c));
					} else if (c == UNICODE_ESCAPING_CHAR) {
						json.deleteCharAt(i);
						String unicode = json.substring(i, i + UNICODE_ESCAPED_LENGTH);
						json.delete(i, i + UNICODE_ESCAPED_LENGTH);
						json.insert(i,
								(char) Integer.parseInt(unicode, UNICODE_ESCAPE_RADIX));
					}
					continue i;
				}

				if (!inString) {

					switch (c)
					{
						case OBJECT_BEGIN_CHAR:
							if (i != 0) {
								int match = DualMatchIndex(json, OBJECT_BEGIN_CHAR,
										OBJECT_END_CHAR, i);
								value = Parse(json.substring(i, match + 1), new JSON(),
										context);
								i = match;
								nail = NOT_BEGIN;
							} else {
								nail = i + 1;
							}
							break;

						case OBJECT_END_CHAR:
							if (entry != null) {
								if (nail != NOT_BEGIN) {
									value = Value(json.substring(nail, i));
								}
								object.put(entry, value);
							}
							break i;

						case ARRAY_BEGIN_CHAR:
							if (nail != NOT_BEGIN && nail != i) {
								i = DualMatchIndex(json, ARRAY_BEGIN_CHAR,
										ARRAY_END_CHAR, i);
							} else if (i != 0) {
								int match = DualMatchIndex(json, ARRAY_BEGIN_CHAR,
										ARRAY_END_CHAR, i);
								value = Parse(json.substring(i, match + 1), new JSAN(),
										context);
								i = match;
								nail = NOT_BEGIN;
							} else {
								nail = i + 1;
								arrayIndex++;
							}
							break;

						case ARRAY_END_CHAR:
							if (nail != NOT_BEGIN && nail != i) {
								value = Value(json.substring(nail, i));
							}
							if (value != null) {
								object.put(JSAN.Index(arrayIndex), value);
							}
							break i;

						case PAIR_CHAR:
							if (nail != NOT_BEGIN) {
								value = Value(json.substring(nail, i));
							}
							if (arrayIndex > NOT_BEGIN) {
								entry = JSAN.Index(arrayIndex);
								arrayIndex++;
							}
							object.put(entry, value);
							nail = i + 1;
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

				} else if (c == QUOTE_CHAR) {
					inString = !inString;
				}
			}

		} catch (RuntimeException e) {
			throw new JSONSyntaxException(json, i);
		}

		return object;
	}

	public static Object Quote(Object o)
	{
		while (o instanceof JSONQuotation) {
			o = ((JSONQuotation) o).quote();
		}

		return o;
	}

	public static int ReverseDualMatchIndex(CharSequence sequence, char a, char b,
			int from)
	{
		int index = -1;
		int match = 0;

		boolean inString = false;

		i: for (int i = Math.min(sequence.length() - 1, from); i >= 0; i--) {

			char c = sequence.charAt(i);

			if (c == QUOTE_CHAR && i > 0 && sequence.charAt(i - 1) != ESCAPE_CHAR) {
				inString = !inString;
			}
			if (inString) {
				continue i;
			}

			if (c == b) {
				match++;
			} else if (c == a) {
				match--;
				if (match == 0) {
					index = i;
					break;
				}
			}
		}

		return index;
	}

	public static String Serialize(JSON json)
	{
		StringBuilder buffer = new StringBuilder();

		boolean isJSAN = IsJSAN(json);
		boolean isFirst = true;

		String key;
		Object object;
		String value;

		for (Entry<String, Object> entry : json.entrySet()) {

			key = entry.getKey();
			object = entry.getValue();

			if (isFirst) {
				isFirst = false;
			} else {
				buffer.append(PAIR_CHAR);
			}

			if (!isJSAN) {
				if (key == null) {
					buffer.append(NULL_STRING);
				} else {
					buffer.append(QUOTE_CHAR);
					buffer.append(EscapeString(entry.getKey()));
					buffer.append(QUOTE_CHAR);
					buffer.append(ATTR_CHAR);
				}
			}

			if (object == null) {
				value = NULL_STRING;
			} else if (IsJSON(object)) {
				value = Serialize((JSON) object);
			} else if (object instanceof String) {
				value = QUOTE_CHAR + EscapeString(object.toString()) + QUOTE_CHAR;
			} else {
				value = object.toString();
			}

			buffer.append(value);
		}

		if (isJSAN) {
			buffer.insert(0, ARRAY_BEGIN_CHAR);
			buffer.append(ARRAY_END_CHAR);
		} else {
			buffer.insert(0, OBJECT_BEGIN_CHAR);
			buffer.append(OBJECT_END_CHAR);
		}

		return buffer.toString();
	}

	public static String TrimQuotes(String string)
	{
		string = string.trim();
		if (string.equals(NULL_STRING)) {
			string = null;
		} else {
			string = string.replaceFirst("^" + QUOTE_CHAR + "([\\d\\D]*)" + QUOTE_CHAR
					+ "$", "$1");
		}
		return string;
	}

	public static Object Value(String string)
	{
		string = string.trim();

		Object value = null;

		try {
			if (string.startsWith(QUOTE_MARK) && string.endsWith(QUOTE_MARK)) {
				value = TrimQuotes(string);
			} else if (Variable.isInteger(string)) {
				value = Integer.valueOf(string);
			} else if (Variable.isDouble(string)) {
				value = Double.valueOf(string);
			} else if (string.equals(TRUE_STRING) || string.equals(FALSE_STRING)) {
				value = Boolean.valueOf(string);
			} else if (string.equals(NULL_STRING)) {
				value = null;
			} else if (!string.startsWith(OBJECT_BEGIN_MARK)
					&& !string.startsWith(ARRAY_BEGIN_MARK))
			{
				value = new JSONQuotation(string);
			} else {
				value = NOT_A_VALUE;
			}
		} catch (NumberFormatException e) {
			value = NOT_A_VALUE;
		}

		return value;
	}

	private JSON	outer;

	private String	entry;

	public JSON()
	{
		super();
	}

	protected JSON(JSON json)
	{
		super(json);
	}

	@SuppressWarnings("unchecked")
	public <E> E attr(String key)
	{
		return (E) Quote(this.get(key));
	}

	public JSON attr(String key, Object value)
	{
		this.put(key, value);
		return this;
	}

	public Boolean attrBoolean(String key)
	{
		return attrCast(key, Boolean.class);
	}

	@SuppressWarnings("unchecked")
	public <E> E attrCast(String key, Class<E> cls)
	{
		return (E) this.attr(key);
	}

	public Double attrDouble(String key)
	{
		return attrCast(key, Double.class);
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

	public String attrString(String key)
	{
		return attrCast(key, String.class);
	}

	@Override
	public JSON clone()
	{
		return new JSON(this);
	}

	public JSONContext context()
	{
		JSONContext context = null;
		JSON outer = this;

		do {
			outer = outer.outer();
			if (IsContext(outer)) {
				context = (JSONContext) outer;
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

	public JSON outer()
	{
		return outer;
	}

	public JSON outer(JSON outer)
	{
		this.outer = outer;
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object put(String key, Object value)
	{
		if (value instanceof Map<?, ?> && !IsJSON(value)) {
			JSON json = new JSON();
			json.putAll((Map<? extends String, ? extends Object>) value);
			value = json;
		} else if (value instanceof Iterable<?> && !IsJSAN(value)) {
			JSAN jsan = new JSAN();
			jsan.addAll((Iterable<? extends Object>) value);
			value = jsan;
		}

		JSONHierarchical hirch = AsHierarchical(value);
		if (hirch != null) {
			if (hirch.context() == null) {
				hirch.outer(this).entry(key);
			}
		}

		super.put(key, value);

		return value;
	}

	public JSONQuotation quote()
	{
		return new JSONQuotation(JSONQuotation.Quote(this));
	}

	public JSONQuotation quote(String entry)
	{
		String quote = JSONQuotation.Quote(this);
		quote += JSONQuotation.NESTED_ATTRIBUTE_BEGIN;
		if (JSON.IsJSAN(this)) {
			quote += JSONQuotation.NESTED_ATTRIBUTE_QUOTE;
		}
		quote += entry;
		if (JSON.IsJSAN(this)) {
			quote += JSONQuotation.NESTED_ATTRIBUTE_QUOTE;
		}
		quote += JSONQuotation.NESTED_ATTRIBUTE_END;
		return new JSONQuotation(quote);
	}

	@Override
	public Object remove(Object key)
	{
		Object value = super.remove(key);

		JSONHierarchical hirch = AsHierarchical(value);
		if (hirch != null) {
			if (context() == hirch.context()) {
				hirch.outer(null).entry(null);
			}
		}

		return value;
	}

	public JSON removeAll(Iterable<? extends Object> keys)
	{
		for (Object key : keys) {
			this.remove(key);
		}
		return this;
	}

	public JSON removeAll(Map<? extends Object, ? extends Object> map)
	{
		return this.removeAll(map.keySet());
	}

	public JSON swap(JSON json)
	{
		if (json == null) {
			json = new JSON();
		}

		for (Entry<String, Object> entry : this.entrySet()) {
			json.put(entry.getValue().toString(), entry.getKey());
		}

		return json;
	}

	public JSAN toJSAN()
	{
		JSAN jsan = null;

		if (IsJSAN(this)) {
			jsan = (JSAN) this;
		} else if (IsJSON(this)) {
			jsan = new JSAN();
			jsan.addAll(this.values());
		}

		return jsan;
	}

	@Override
	public String toString()
	{
		return Serialize(this);
	}
}
