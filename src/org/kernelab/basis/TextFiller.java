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
		TextFiller f = new TextFiller("Hello ?nam(e)?, this is ?number?.");
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
		for (String key : json.keySet()) {
			fillWith(key, json.attr(key));
		}

		return this;
	}

	public TextFiller fillWith(Map<String, Object> map)
	{
		for (Entry<String, Object> entry : map.entrySet()) {
			fillWith(entry.getKey(), entry.getValue());
		}

		return this;
	}

	public TextFiller fillWith(String target, Object object)
	{
		if (target != null && object != null) {

			Matcher matcher = Pattern.compile(
					"\\Q" + leftBoundary + target + rightBoundary + "\\E").matcher(
					this.result());

			this.shiftResult();

			String value = object.toString();

			while (matcher.find()) {
				matcher.appendReplacement(this.result(), value);
			}

			matcher.appendTail(this.result());
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
