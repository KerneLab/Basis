package org.kernelab.basis;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * To fill a text template with a Map.
 * 
 * @author Dilly King
 */
public class TextFiller
{
	public static String	DEFAULT_BOUNDARY	= "?";

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		TextFiller f = new TextFiller("Hello ???nam(e)??, this is ?number?.");
		JSON json = JSON.Parse("{\"nam(e)\":\"Dilly\",\"number\":3}");
		Tools.debug(f.reset().fillWith(json).result());
	}

	private CharSequence	template;

	private String			leftBoundary;

	private String			rightBoundary;

	private StringBuffer	backup;

	private StringBuffer	result;

	public TextFiller()
	{
		this(DEFAULT_BOUNDARY);
	}

	public TextFiller(CharSequence template)
	{
		this(template, DEFAULT_BOUNDARY);
	}

	public TextFiller(CharSequence template, String boundary)
	{
		this(template, boundary, boundary);
	}

	public TextFiller(CharSequence template, String leftBoundary, String rightBoundary)
	{
		this.template(template).boundary(leftBoundary, rightBoundary);
	}

	protected StringBuffer backup()
	{
		return backup;
	}

	protected TextFiller backup(StringBuffer backup)
	{
		this.backup = backup;
		return this;
	}

	public TextFiller boundary(String leftBoundary, String rightBoundary)
	{
		return this.leftBoundary(leftBoundary).rightBoundary(rightBoundary);
	}

	public TextFiller fillWith(JSON json)
	{
		return fillWith(json, leftBoundary, rightBoundary);
	}

	public TextFiller fillWith(JSON json, String boundary)
	{
		return fillWith(json, boundary, boundary);
	}

	public TextFiller fillWith(JSON json, String leftBoundary, String rightBoundary)
	{
		for (String key : json.keySet()) {
			fillWith(key, json.attr(key), leftBoundary, rightBoundary);
		}

		return this;
	}

	public TextFiller fillWith(Map<String, Object> map)
	{
		return fillWith(map, leftBoundary, rightBoundary);
	}

	public TextFiller fillWith(Map<String, Object> map, String boundary)
	{
		return fillWith(map, boundary, boundary);
	}

	public TextFiller fillWith(Map<String, Object> map, String leftBoundary, String rightBoundary)
	{
		for (Entry<String, Object> entry : map.entrySet()) {
			fillWith(entry.getKey(), entry.getValue(), leftBoundary, rightBoundary);
		}

		return this;
	}

	@SuppressWarnings("unchecked")
	public TextFiller fillWith(String target, Iterable<?> iterable, String leftBoundary, String rightBoundary)
	{
		if (target != null && iterable != null) {

			Matcher matcher = Pattern.compile(
					Pattern.quote(leftBoundary + target + rightBoundary) + "(.*?)"
							+ Pattern.quote(leftBoundary + target + rightBoundary), Pattern.DOTALL).matcher(
					this.result());

			this.shiftResult();

			while (matcher.find()) {

				TextFiller filler = new TextFiller(matcher.group(1), leftBoundary, rightBoundary);

				StringBuilder buffer = new StringBuilder();

				for (Object o : iterable) {

					if (o instanceof JSON) {
						filler.reset().fillWith((JSON) o);
					} else if (o instanceof Map<?, ?>) {
						filler.reset().fillWith((Map<String, Object>) o);
					}

					buffer.append(filler.result());
				}

				matcher.appendReplacement(this.result(), buffer.toString());
			}

			matcher.appendTail(this.result());
		}

		return this;
	}

	public TextFiller fillWith(String target, Object object)
	{
		return fillWith(target, object, leftBoundary, rightBoundary);
	}

	public TextFiller fillWith(String target, Object object, String boundary)
	{
		return fillWith(target, object, boundary, boundary);
	}

	public TextFiller fillWith(String target, Object object, String leftBoundary, String rightBoundary)
	{
		if (target != null && object != null) {

			if (object instanceof Iterable<?>) {

				return this.fillWith(target, (Iterable<?>) object, leftBoundary, rightBoundary);

			} else {

				Matcher matcher = Pattern.compile(Pattern.quote(leftBoundary + target + rightBoundary)).matcher(
						this.result());

				this.shiftResult();

				String value = object.toString();

				while (matcher.find()) {
					matcher.appendReplacement(this.result(), value);
				}

				matcher.appendTail(this.result());
			}
		}

		return this;
	}

	public String leftBoundary()
	{
		return leftBoundary;
	}

	public TextFiller leftBoundary(String leftBoundary)
	{
		this.leftBoundary = leftBoundary;
		return this;
	}

	public TextFiller reset()
	{
		return reset(null);
	}

	public TextFiller reset(CharSequence template)
	{
		if (template != null) {
			this.template(template);
		}
		if (this.result() == null) {
			this.result(new StringBuffer());
		} else {
			Tools.clearStringBuffer(this.result());
		}
		this.result().append(this.template());
		if (this.backup() == null) {
			this.backup(new StringBuffer());
		} else {
			Tools.clearStringBuffer(this.backup());
		}
		return this;
	}

	public StringBuffer result()
	{
		return result;
	}

	protected TextFiller result(StringBuffer result)
	{
		this.result = result;
		return this;
	}

	public String rightBoundary()
	{
		return rightBoundary;
	}

	public TextFiller rightBoundary(String rightBoundary)
	{
		this.rightBoundary = rightBoundary;
		return this;
	}

	public TextFiller shiftResult()
	{
		StringBuffer temp = this.result();
		this.result(Tools.clearStringBuffer(this.backup()));
		this.backup(temp);
		return this;
	}

	public CharSequence template()
	{
		return template;
	}

	public TextFiller template(CharSequence template)
	{
		this.template = template;
		return this;
	}

	@Override
	public String toString()
	{
		return this.result().toString();
	}
}
