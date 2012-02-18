package org.kernelab.basis;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * To fill a text template with a Map.
 * 
 * @author Dilly King
 * @version 2012.01.13.3
 */
public class TextFiller
{
	public static char	DEFAULT_BOUNDARY	= '?';

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		TextFiller f = new TextFiller("Hello ?nam(e)?, this is ?number?.");
		JSON json = JSON.Parse("{\"nam(e)\":\"Dilly\",\"number\":3}");
		Tools.debug(f.reset().fillWith(json).getResult());
	}

	private CharSequence	template;

	private char			leftBoundary;

	private char			rightBoundary;

	private StringBuffer	backup;

	private StringBuffer	result;

	public TextFiller()
	{
		this(DEFAULT_BOUNDARY);
	}

	public TextFiller(char boundary)
	{
		this(boundary, boundary);
	}

	public TextFiller(char leftBoundary, char rightBoundary)
	{
		this(null, leftBoundary, rightBoundary);
	}

	public TextFiller(CharSequence template)
	{
		this(template, DEFAULT_BOUNDARY);
	}

	public TextFiller(CharSequence template, char boundary)
	{
		this(template, boundary, boundary);
	}

	public TextFiller(CharSequence template, char leftBoundary, char rightBoundary)
	{
		this.setTemplate(template).setBoundary(leftBoundary, rightBoundary);
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
					this.getResult());

			this.shiftResult();

			String value = object.toString();

			while (matcher.find()) {
				matcher.appendReplacement(this.getResult(), value);
			}

			matcher.appendTail(this.getResult());
		}

		return this;
	}

	protected StringBuffer getBackup()
	{
		return backup;
	}

	public char getLeftBoundary()
	{
		return leftBoundary;
	}

	public StringBuffer getResult()
	{
		return result;
	}

	public char getRightBoundary()
	{
		return rightBoundary;
	}

	public CharSequence getTemplate()
	{
		return template;
	}

	public TextFiller reset()
	{
		return reset(null);
	}

	public TextFiller reset(CharSequence template)
	{
		if (template != null) {
			this.setTemplate(template);
		}
		if (this.getResult() == null) {
			this.setResult(new StringBuffer());
		} else {
			Tools.clearStringBuffer(this.getResult());
		}
		this.getResult().append(this.getTemplate());
		if (this.getBackup() == null) {
			this.setBackup(new StringBuffer());
		} else {
			Tools.clearStringBuffer(this.getBackup());
		}
		return this;
	}

	protected TextFiller setBackup(StringBuffer backup)
	{
		this.backup = backup;
		return this;
	}

	public TextFiller setBoundary(char leftBoundary, char rightBoundary)
	{
		return this.setLeftBoundary(leftBoundary).setRightBoundary(rightBoundary);
	}

	public TextFiller setLeftBoundary(char leftBoundary)
	{
		this.leftBoundary = leftBoundary;
		return this;
	}

	protected TextFiller setResult(StringBuffer result)
	{
		this.result = result;
		return this;
	}

	public TextFiller setRightBoundary(char rightBoundary)
	{
		this.rightBoundary = rightBoundary;
		return this;
	}

	public TextFiller setTemplate(CharSequence template)
	{
		this.template = template;
		return this;
	}

	public TextFiller shiftResult()
	{
		StringBuffer temp = this.getResult();
		this.setResult(Tools.clearStringBuffer(this.getBackup()));
		this.setBackup(temp);
		return this;
	}

	@Override
	public String toString()
	{
		return this.getResult().toString();
	}
}
