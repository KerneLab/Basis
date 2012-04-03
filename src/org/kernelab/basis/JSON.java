package org.kernelab.basis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.JSON.Context;

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
public class JSON implements Map<String, Object>, Hierarchical
{

	public static class Context extends JSON
	{

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

					if (entry == null) {
						if (VAR_ENTRY_MATCHER.reset(buffer).lookingAt()) {
							entry = VAR_ENTRY_MATCHER.group(2);
							line = VAR_ENTRY_MATCHER.group(3);
							Tools.clearStringBuilder(buffer);
							buffer.append(line);
						}
					}

					if (entry != null) {

						if (VAR_EXIT_MATCHER.reset(buffer).lookingAt()) {

							line = VAR_EXIT_MATCHER.group(1);
							Tools.clearStringBuilder(buffer);
							buffer.append(line);

							Object object = JSON.Value(buffer.toString());

							if (object == JSON.NOT_A_VALUE) {
								object = JSON.Parse(buffer, null, Context.this);
							}

							if (object != JSON.NOT_A_VALUE) {
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
			try {
				reader.setDataFile(file).read();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			return this;
		}

		public Context read(File file, String charSetName)
		{
			try {
				reader.setDataFile(file, charSetName).read();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
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
			try {
				reader.setInputStream(is, charSetName).read();
			} catch (UnsupportedEncodingException e) {
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
				JSAN.this.remove(last);
			}

			public void set(Object object)
			{
				JSAN.this.set(last, object);
			}
		}

		public static final int						LAST	= -1;

		private static final Map<Integer, String>	INDEX	= new HashMap<Integer, String>();

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
			prototype(new TreeMap<String, Object>(new Comparator<String>() {

				public int compare(String a, String b)
				{
					return Integer.parseInt(a) - Integer.parseInt(b);
				}
			}));
		}

		public JSAN add(int index, Object object)
		{
			return splice(index, 0, object);
		}

		public JSAN add(Object object)
		{
			return add(LAST, object);
		}

		public JSAN addAll(Collection<? extends Object> collection)
		{
			return addAll(LAST, collection);
		}

		public JSAN addAll(int index, Collection<? extends Object> collection)
		{
			return splice(index, 0, collection);
		}

		public JSAN addAll(int index, JSAN jsan)
		{
			return splice(index, 0, jsan);
		}

		public JSAN addAll(int index, Object[] array)
		{
			return splice(index, 0, array);
		}

		public JSAN addAll(JSAN jsan)
		{
			return addAll(LAST, jsan);
		}

		public JSAN addAll(Object[] array)
		{
			return addAll(LAST, array);
		}

		@SuppressWarnings("unchecked")
		public <T> Collection<T> addTo(Collection<T> collection)
		{
			if (collection == null) {
				collection = new LinkedList<T>();
			}
			for (Object o : this) {
				collection.add((T) o);
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
			return new JSAN().clone(this);
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
			return this.size() == jsan.size() && this.containsAll(jsan) && jsan.containsAll(this);
		}

		public Object get(int index)
		{
			index = index(index);

			if (!has(index)) {
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

		public ArrayIterator iterator()
		{
			return new ArrayIterator(0);
		}

		public int lastIndexOf(Object object)
		{
			int index = -1;
			if (object == null) {
				int i = size();
				ArrayIterator iter = new ArrayIterator(size());
				while (iter.hasPrevious()) {
					i--;
					if (iter.previous() == null) {
						index = i;
						break;
					}
				}
			} else {
				int i = size();
				ArrayIterator iter = new ArrayIterator(size());
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

		protected Object put(int index, Object value)
		{
			return super.put(Index(index), value);
		}

		public Object remove(int index)
		{
			Object object = this.get(index);

			splice(index, 1);

			return object;
		}

		@Override
		public Object remove(Object object)
		{
			int index = indexOf(object);

			if (index == -1) {
				return null;
			} else {
				return this.remove(index);
			}
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
			if (jsan == null) {
				jsan = new JSAN();
			}

			start = bound(start);
			end = bound(end);

			for (int i = start; i < end; i++) {
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

			if (delta > 0) {
				for (i = trace; i >= tail; i--) {
					this.put(i + delta, this.get(i));
				}
			} else if (delta < 0) {
				for (i = tail; i <= trace; i++) {
					this.put(i + delta, this.get(i));
				}
				for (i = trace + delta + 1; i <= trace; i++) {
					super.remove(Index(i));
				}
			}

			i = 0;
			for (T object : collection) {
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

			if (delta > 0) {
				for (i = trace; i >= tail; i--) {
					this.put(i + delta, this.get(i));
				}
			} else if (delta < 0) {
				for (i = tail; i <= trace; i++) {
					this.put(i + delta, this.get(i));
				}
				for (i = trace + delta + 1; i <= trace; i++) {
					super.remove(Index(i));
				}
			}

			for (i = 0; i < fills; i++) {
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

			if (delta > 0) {
				for (i = trace; i >= tail; i--) {
					this.put(i + delta, this.get(i));
				}
			} else if (delta < 0) {
				for (i = tail; i <= trace; i++) {
					this.put(i + delta, this.get(i));
				}
				for (i = trace + delta + 1; i <= trace; i++) {
					super.remove(Index(i));
				}
			}

			for (i = 0; i < fills; i++) {
				this.put(index + i, objects[i]);
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
				array = (Object[]) java.lang.reflect.Array.newInstance(Object.class, size());
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

	public static class Quotation implements Hierarchical
	{
		public static final char	LINEAR_ATTRIBUTE		= '.';

		public static final char	NESTED_ATTRIBUTE_BEGIN	= '[';

		public static final char	NESTED_ATTRIBUTE_END	= ']';

		public static final char	NESTED_ATTRIBUTE_QUOTE	= '"';

		public static Object Quote(Context context, String quote)
		{
			if (context == null) {
				return null;
			}

			Object object = context;

			Map<String, Object> map = null;
			int nail = 0;

			char c = 0;
			int i = 0;
			String entry;

			i: for (i = 0; i < quote.length(); i++) {
				c = quote.charAt(i);

				if (c == LINEAR_ATTRIBUTE) {
					if (nail != i) {
						map = JSON.AsMap(object);
						if (map == null) {
							return null;
						} else {
							entry = quote.substring(nail, i).trim();
							object = map.get(entry);
							nail = i + 1;
						}
					} else {
						nail = i + 1;
					}
				}

				if (c == NESTED_ATTRIBUTE_BEGIN) {
					if (nail != i) {
						map = JSON.AsMap(object);
						if (map == null) {
							return null;
						} else {
							entry = quote.substring(nail, i).trim();
							object = map.get(entry);
							nail = i + 1;
						}
					} else {
						nail = i + 1;
					}
					i = JSON.DualMatchIndex(quote, NESTED_ATTRIBUTE_BEGIN, NESTED_ATTRIBUTE_END, i) - 1;
					continue i;
				}

				if (c == NESTED_ATTRIBUTE_QUOTE) {
					nail = i + 1;
					do {
						i = Tools.seekIndex(quote, NESTED_ATTRIBUTE_QUOTE, i + 1);
					} while (quote.charAt(i - 1) == JSON.ESCAPE_CHAR);
					break;
				}

				if (c == NESTED_ATTRIBUTE_END) {
					map = JSON.AsMap(object);
					if (map == null) {
						return null;
					} else {
						entry = Quote(context, quote.substring(nail, i)).toString();
						object = map.get(entry);
						nail = i + 1;
					}
				}
			}

			if (c == NESTED_ATTRIBUTE_QUOTE || Variable.isInteger(quote)) {
				object = quote.substring(nail, i);
			} else if (c != NESTED_ATTRIBUTE_END) {
				map = JSON.AsMap(object);
				if (map == null) {
					return null;
				} else {
					entry = quote.substring(nail, i).trim();
					object = map.get(entry);
				}
			}

			return object;
		}

		public static Object Quote(Context context, String quote, Object object)
		{
			JSON outer = null;
			String entry = null;

			int quoteLength = quote.length() - 1;
			if (quote.charAt(quoteLength) == NESTED_ATTRIBUTE_END) {
				int begin = JSON
						.ReverseDualMatchIndex(quote, NESTED_ATTRIBUTE_BEGIN, NESTED_ATTRIBUTE_END, quoteLength);
				outer = JSON.AsJSON(Quote(context, quote.substring(0, begin)));
				if (outer != null) {
					entry = Quote(context, quote.substring(begin + 1, quoteLength)).toString();
				}
			} else {
				int begin = quote.lastIndexOf(LINEAR_ATTRIBUTE);
				if (begin == -1) {
					outer = context;
					entry = quote;
				} else {
					outer = JSON.AsJSON(Quote(context, quote.substring(0, begin)));
					if (outer != null) {
						entry = quote.substring(begin + 1);
					}
				}
			}

			if (outer != null && entry != null) {
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

			do {
				entry = json.entry();

				if (outer.outer() != null) {
					buffer.insert(0, NESTED_ATTRIBUTE_END);
					if (!JSON.IsJSAN(outer)) {
						buffer.insert(0, NESTED_ATTRIBUTE_QUOTE);
					}
				}
				buffer.insert(0, entry);
				if (outer.outer() != null) {
					if (!JSON.IsJSAN(outer)) {
						buffer.insert(0, NESTED_ATTRIBUTE_QUOTE);
					}
					buffer.insert(0, NESTED_ATTRIBUTE_BEGIN);
				}

				json = outer;
				outer = json.outer();
			} while (outer != null);

			return buffer.toString();
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

	public static final Hierarchical AsHierarchical(Object o)
	{
		return Tools.as(o, Hierarchical.class);
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

		i: for (int i = Math.max(0, from); i < sequence.length(); i++) {

			char c = sequence.charAt(i);

			if (c == ESCAPE_CHAR) {
				c = sequence.charAt(i + 1);
				if (ESCAPING_CHAR.containsKey(c)) {
					i++;
				} else if (c == UNICODE_ESCAPING_CHAR) {
					i += UNICODE_ESCAPED_LENGTH + 1;
				}
				continue i;
			}

			if (c == QUOTE_CHAR) {
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

	public static final boolean IsQuotation(Object o)
	{
		return o instanceof Quotation;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String a = "[\"1\",{\"2\":\"\\\\\"},\"4\",[]]";
		Tools.debug(a);
		JSAN ja = JSON.Parse(a).toJSAN();
		Tools.debug(ja.toString());
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

		if (object == null) {
			if (string.startsWith(ARRAY_BEGIN_MARK) && string.endsWith(ARRAY_END_MARK)) {
				object = new JSAN();
			} else if (string.startsWith(OBJECT_BEGIN_MARK) && string.endsWith(OBJECT_END_MARK)) {
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

				if (inString) {

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
							json.insert(i, (char) Integer.parseInt(unicode, UNICODE_ESCAPE_RADIX));
						}
						continue i;

					} else if (c == QUOTE_CHAR) {
						inString = !inString;
					}

				} else {

					switch (c)
					{
						case OBJECT_BEGIN_CHAR:
							if (i != 0) {
								int match = DualMatchIndex(json, OBJECT_BEGIN_CHAR, OBJECT_END_CHAR, i);
								value = Parse(json.substring(i, match + 1), new JSON(), context);
								i = match;
								nail = NOT_BEGIN;
							} else {
								nail = FirstNonWhitespaceIndex(json, i + 1);
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
								i = DualMatchIndex(json, ARRAY_BEGIN_CHAR, ARRAY_END_CHAR, i);
							} else if (i != 0) {
								int match = DualMatchIndex(json, ARRAY_BEGIN_CHAR, ARRAY_END_CHAR, i);
								value = Parse(json.substring(i, match + 1), new JSAN(), context);
								i = match;
								nail = NOT_BEGIN;
							} else {
								nail = FirstNonWhitespaceIndex(json, i + 1);
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

		} catch (RuntimeException e) {
			throw new SyntaxErrorException(json, i);
		}

		return object;
	}

	public static Object Quote(Object o)
	{
		while (o instanceof Quotation) {
			o = ((Quotation) o).quote();
		}

		return o;
	}

	public static int ReverseDualMatchIndex(CharSequence sequence, char a, char b, int from)
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
			string = string.replaceFirst("^" + QUOTE_CHAR + "([\\d\\D]*)" + QUOTE_CHAR + "$", "$1");
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
			} else if (!string.startsWith(OBJECT_BEGIN_MARK) && !string.startsWith(ARRAY_BEGIN_MARK)) {
				value = new Quotation(string);
			} else {
				value = NOT_A_VALUE;
			}
		} catch (NumberFormatException e) {
			value = NOT_A_VALUE;
		}

		return value;
	}

	private Map<String, Object>	map;

	private JSON				outer;

	private String				entry;

	public JSON()
	{
		prototype(new LinkedHashMap<String, Object>());
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

	public void clear()
	{
		map.clear();
	}

	@Override
	public JSON clone()
	{
		return new JSON().clone(this);
	}

	@SuppressWarnings("unchecked")
	protected <T extends JSON> T clone(JSON source)
	{
		outer(source.outer()).entry(source.entry());

		String key = null;
		Object object = null;
		Hierarchical hirch = null;

		for (Entry<String, Object> entry : source.entrySet()) {
			key = entry.getKey();
			object = entry.getValue();

			if ((hirch = JSON.AsHierarchical(object)) != null) {
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
		return map.containsValue(value);
	}

	public Context context()
	{
		Context context = null;
		JSON outer = this;

		do {
			outer = outer.outer();
			if (IsContext(outer)) {
				context = (Context) outer;
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

	public Set<Entry<String, Object>> entrySet()
	{
		return map.entrySet();
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

	protected Map<String, Object> prototype()
	{
		return map;
	}

	protected JSON prototype(Map<String, Object> map)
	{
		if (map != null) {
			this.map = map;
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public Object put(String key, Object value)
	{
		if (value instanceof Map<?, ?> && !IsJSON(value)) {
			JSON json = new JSON();
			json.putAll((Map<? extends String, ? extends Object>) value);
			value = json;
		} else if (value instanceof Collection<?> && !IsJSAN(value)) {
			JSAN jsan = new JSAN();
			jsan.addAll((Collection<? extends Object>) value);
			value = jsan;
		}

		Hierarchical hirch = AsHierarchical(value);
		if (hirch != null) {
			if (hirch.context() == null) {
				hirch.outer(this).entry(key);
			}
		}

		map.put(key, value);

		return value;
	}

	public void putAll(Map<? extends String, ? extends Object> m)
	{
		map.putAll(m);
	}

	public Quotation quote()
	{
		return new Quotation(Quotation.Quote(this));
	}

	public Quotation quote(String entry)
	{
		String quote = Quotation.Quote(this);
		quote += Quotation.NESTED_ATTRIBUTE_BEGIN;
		if (JSON.IsJSAN(this)) {
			quote += Quotation.NESTED_ATTRIBUTE_QUOTE;
		}
		quote += entry;
		if (JSON.IsJSAN(this)) {
			quote += Quotation.NESTED_ATTRIBUTE_QUOTE;
		}
		quote += Quotation.NESTED_ATTRIBUTE_END;
		return new Quotation(quote);
	}

	public Object remove(Object key)
	{
		Object value = map.remove(key);

		Hierarchical hirch = AsHierarchical(value);
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
		if (json == null) {
			json = new JSON();
		}

		for (String key : this.keySet()) {
			json.put(this.attr(key).toString(), key);
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

	public Collection<Object> values()
	{
		return map.values();
	}
}
