package org.kernelab.basis;

import java.util.Map;

public class JSONQuotation implements JSONHierarchical
{
	public static final char	LINEAR_ATTRIBUTE		= '.';

	public static final char	NESTED_ATTRIBUTE_BEGIN	= '[';

	public static final char	NESTED_ATTRIBUTE_END	= ']';

	public static final char	NESTED_ATTRIBUTE_QUOTE	= '"';

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

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

	public static Object Quote(JSONContext context, String quote)
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
				i = JSON.DualMatchIndex(quote, NESTED_ATTRIBUTE_BEGIN,
						NESTED_ATTRIBUTE_END, i) - 1;
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

	public static Object Quote(JSONContext context, String quote, Object object)
	{
		JSON outer = null;
		String entry = null;

		int quoteLength = quote.length() - 1;
		if (quote.charAt(quoteLength) == NESTED_ATTRIBUTE_END) {
			int begin = JSON.ReverseDualMatchIndex(quote, NESTED_ATTRIBUTE_BEGIN,
					NESTED_ATTRIBUTE_END, quoteLength);
			outer = JSON.AsJSON(Quote(context, quote.substring(0, begin)));
			if (outer != null) {
				entry = Quote(context, quote.substring(begin + 1, quoteLength))
						.toString();
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

	private JSON	outer;

	private String	entry;

	private String	quote;

	public JSONQuotation(String quote)
	{
		quote(quote);
	}

	public JSONContext context()
	{
		return outer == null ? null : outer.context();
	}

	public String entry()
	{
		return entry;
	}

	public JSONQuotation entry(String entry)
	{
		this.entry = entry;
		return this;
	}

	public JSON outer()
	{
		return outer;
	}

	public JSONQuotation outer(JSON outer)
	{
		this.outer = outer;
		return this;
	}

	public Object quote()
	{
		return Quote(context(), quote);
	}

	protected JSONQuotation quote(String quote)
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
