package org.kernelab.basis;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.io.ExtensionLoader;

public class Arbiter
{
	public static interface ConditionInterpreter
	{
		/**
		 * To identify whether this interpreter could interpret the condition.
		 * 
		 * @param condition
		 * @param value
		 * @param key
		 * @param object
		 * @return true if this interpreter could interpret the condition,
		 *         otherwise false.
		 */
		public boolean identify(String condition, Object value, String key, JSON object);

		/**
		 * To judge whether this condition is true or false according to the
		 * given value and other information.
		 * 
		 * @param condition
		 * @param value
		 * @param key
		 * @param object
		 * @return true if the condition is true, otherwise false.
		 */
		public boolean judge(String condition, Object value, String key, JSON object);
	}

	public static class DefinedConditionInterpreter implements ConditionInterpreter
	{
		public boolean identify(String condition, Object value, String key, JSON object)
		{
			return Tools.equals("=#", condition);
		}

		public boolean judge(String condition, Object value, String key, JSON object)
		{
			return object.has(key);
		}
	}

	public static class EqualConditionInterpreter implements ConditionInterpreter
	{
		public static final Pattern	PATTERN	= Pattern.compile("==([\\d\\D]+)");

		public boolean identify(String condition, Object value, String key, JSON object)
		{
			return (value instanceof Number || value instanceof String || value instanceof Boolean)
					&& PATTERN.matcher(condition).matches();
		}

		public boolean judge(String condition, Object value, String key, JSON object)
		{
			boolean judge = false;

			Matcher matcher = PATTERN.matcher(condition);

			if (matcher.matches())
			{
				String s = value.toString();
				if (value instanceof Number)
				{
					judge = Double.valueOf(s).compareTo(Double.valueOf(matcher.group(1))) == 0;
				}
				else
				{
					judge = s.compareTo(matcher.group(1)) == 0;
				}
			}

			return judge;
		}
	}

	public static class GreaterConditionInterpreter implements ConditionInterpreter
	{
		public static final Pattern	PATTERN	= Pattern.compile(">([\\d\\D]+)");

		public boolean identify(String condition, Object value, String key, JSON object)
		{
			return (value instanceof Number || value instanceof String) && PATTERN.matcher(condition).matches();
		}

		public boolean judge(String condition, Object value, String key, JSON object)
		{
			boolean judge = false;

			Matcher matcher = PATTERN.matcher(condition);

			if (matcher.matches())
			{
				String s = value.toString();
				if (value instanceof Number)
				{
					judge = Double.valueOf(s).compareTo(Double.valueOf(matcher.group(1))) > 0;
				}
				else
				{
					judge = s.compareTo(matcher.group(1)) > 0;
				}
			}

			return judge;
		}
	}

	public static class GreaterEqualConditionInterpreter implements ConditionInterpreter
	{
		public static final Pattern	PATTERN	= Pattern.compile(">=([\\d\\D]+)");

		public boolean identify(String condition, Object value, String key, JSON object)
		{
			return (value instanceof Number || value instanceof String) && PATTERN.matcher(condition).matches();
		}

		public boolean judge(String condition, Object value, String key, JSON object)
		{
			boolean judge = false;

			Matcher matcher = PATTERN.matcher(condition);

			if (matcher.matches())
			{
				String s = value.toString();
				if (value instanceof Number)
				{
					judge = Double.valueOf(s).compareTo(Double.valueOf(matcher.group(1))) >= 0;
				}
				else
				{
					judge = s.compareTo(matcher.group(1)) >= 0;
				}
			}

			return judge;
		}
	}

	public static class LessConditionInterpreter implements ConditionInterpreter
	{
		public static final Pattern	PATTERN	= Pattern.compile("<([\\d\\D]+)");

		public boolean identify(String condition, Object value, String key, JSON object)
		{
			return (value instanceof Number || value instanceof String) && PATTERN.matcher(condition).matches();
		}

		public boolean judge(String condition, Object value, String key, JSON object)
		{
			boolean judge = false;

			Matcher matcher = PATTERN.matcher(condition);

			if (matcher.matches())
			{
				String s = value.toString();
				if (value instanceof Number)
				{
					judge = Double.valueOf(s).compareTo(Double.valueOf(matcher.group(1))) < 0;
				}
				else
				{
					judge = s.compareTo(matcher.group(1)) < 0;
				}
			}

			return judge;
		}
	}

	public static class LessEqualConditionInterpreter implements ConditionInterpreter
	{
		public static final Pattern	PATTERN	= Pattern.compile("<=([\\d\\D]+)");

		public boolean identify(String condition, Object value, String key, JSON object)
		{
			return (value instanceof Number || value instanceof String) && PATTERN.matcher(condition).matches();
		}

		public boolean judge(String condition, Object value, String key, JSON object)
		{
			boolean judge = false;

			Matcher matcher = PATTERN.matcher(condition);

			if (matcher.matches())
			{
				String s = value.toString();
				if (value instanceof Number)
				{
					judge = Double.valueOf(s).compareTo(Double.valueOf(matcher.group(1))) <= 0;
				}
				else
				{
					judge = s.compareTo(matcher.group(1)) <= 0;
				}
			}

			return judge;
		}
	}

	public static class NotEqualConditionInterpreter implements ConditionInterpreter
	{
		public static final Pattern	PATTERN	= Pattern.compile("!=([\\d\\D]+)");

		public boolean identify(String condition, Object value, String key, JSON object)
		{
			return (value instanceof Number || value instanceof String || value instanceof Boolean)
					&& PATTERN.matcher(condition).matches();
		}

		public boolean judge(String condition, Object value, String key, JSON object)
		{
			boolean judge = false;

			Matcher matcher = PATTERN.matcher(condition);

			if (matcher.matches())
			{
				String s = value.toString();
				if (value instanceof Number)
				{
					judge = Double.valueOf(s).compareTo(Double.valueOf(matcher.group(1))) != 0;
				}
				else
				{
					judge = s.compareTo(matcher.group(1)) != 0;
				}
			}

			return judge;
		}
	}

	public static class RegexConditionInterpreter implements ConditionInterpreter
	{
		public static final Pattern	PATTERN	= Pattern.compile("=~\\/(.*)\\/([gi]*)");

		public boolean identify(String condition, Object value, String key, JSON object)
		{
			return PATTERN.matcher(condition).matches();
		}

		public boolean judge(String condition, Object value, String key, JSON object)
		{
			boolean judge = false;

			Matcher matcher = PATTERN.matcher(condition);

			if (matcher.matches())
			{
				String expr = matcher.group(1);
				String flag = matcher.group(2);

				int flags = 0;
				if (flag.contains("i"))
				{
					flags |= Pattern.CASE_INSENSITIVE;
				}

				matcher = Pattern.compile(expr, flags).matcher(value.toString());

				if (flag.contains("g"))
				{
					judge = matcher.matches();
				}
				else
				{
					judge = matcher.find();
				}
			}

			return judge;
		}
	}

	public static class UndefinedConditionInterpreter implements ConditionInterpreter
	{
		public boolean identify(String condition, Object value, String key, JSON object)
		{
			return Tools.equals("!#", condition);
		}

		public boolean judge(String condition, Object value, String key, JSON object)
		{
			return !object.has(key);
		}
	}

	public static class ValuedConditionInterpreter implements ConditionInterpreter
	{
		public boolean identify(String condition, Object value, String key, JSON object)
		{
			return Tools.equals("=$", condition);
		}

		public boolean judge(String condition, Object value, String key, JSON object)
		{
			return value != null;
		}
	}

	protected static final int		LOGICAL_NOT			= -1;
	protected static final String	LOGICAL_NOT_MARK	= "!";

	protected static final int		LOGICAL_OR			= 0;
	protected static final String	LOGICAL_OR_MARK		= "|";

	protected static final int		LOGICAL_AND			= 1;
	protected static final String	LOGICAL_AND_MARK	= "&";

	protected static final int		DEFAULT_LOGIC		= LOGICAL_OR;

	public static boolean Arbitrate(JSON values, JSAN condition, Iterable<ConditionInterpreter> interpreters)
	{
		boolean result = true;

		if (values != null && condition != null && !condition.isEmpty())
		{
			result = false;

			JSON cnd = null;
			JSAN nst = null;
			Object v = null;
			Object c = null;

			int logic = DEFAULT_LOGIC;

			for (Object o : condition)
			{
				if (JSON.IsJSON(o))
				{
					boolean present = false;

					if ((nst = JSON.AsJSAN(o)) != null)
					{
						present = Arbitrate(values, nst, interpreters);
					}
					else if ((cnd = JSON.AsJSON(o)) != null)
					{
						present = true;

						for (String k : cnd.keySet())
						{
							v = values.attr(k);
							c = cnd.attr(k);

							String cs = c == null ? null : c.toString();

							boolean hit = false;

							if (interpreters != null)
							{
								for (ConditionInterpreter interpreter : interpreters)
								{
									if (interpreter.identify(cs, v, k, values))
									{
										hit = true;
										present = interpreter.judge(cs, v, k, values);
										break;
									}
								}
							}

							if (!hit)
							{
								if ((v instanceof CharSequence && c instanceof CharSequence)
										|| (v instanceof Number && c instanceof Number)
										|| (v instanceof Boolean && c instanceof Boolean))
								{
									present = v.equals(c);
								}
								else if (v != null)
								{
									present = Pattern.compile(cs).matcher(v.toString()).find();
								}
							}

							if (!present)
							{
								break;
							}
						}
					}

					switch (logic)
					{
						case LOGICAL_NOT:
							result = !present;
							break;

						case LOGICAL_OR:
							result = result || present;
							break;

						case LOGICAL_AND:
							result = result && present;
							break;
					}

					logic = DEFAULT_LOGIC;
				}
				else if (o instanceof String)
				{
					String op = o.toString();
					if (LOGICAL_NOT_MARK.equals(op))
					{
						logic = LOGICAL_NOT;
					}
					else if (LOGICAL_OR_MARK.equals(op))
					{
						logic = LOGICAL_OR;
						if (result)
						{
							break;
						}
					}
					else if (LOGICAL_AND_MARK.equals(op))
					{
						logic = LOGICAL_AND;
						if (!result)
						{
							break;
						}
					}
				}
			}
		}

		return result;
	}

	public static Iterable<ConditionInterpreter> LoadInterpreters()
	{
		Set<ConditionInterpreter> loads = new LinkedHashSet<ConditionInterpreter>();

		loads.add(new ValuedConditionInterpreter());
		loads.add(new DefinedConditionInterpreter());
		loads.add(new EqualConditionInterpreter());
		loads.add(new NotEqualConditionInterpreter());
		loads.add(new GreaterEqualConditionInterpreter());
		loads.add(new LessEqualConditionInterpreter());
		loads.add(new GreaterConditionInterpreter());
		loads.add(new LessConditionInterpreter());
		loads.add(new RegexConditionInterpreter());
		loads.add(new UndefinedConditionInterpreter());

		return loads;
	}

	public static Iterable<ConditionInterpreter> LoadInterpreters(File file)
	{
		return LoadInterpreters(JSAN.Parse(Tools.inputStringFromFile(file)).toJSAN());
	}

	public static Iterable<ConditionInterpreter> LoadInterpreters(InputStream is)
	{
		return LoadInterpreters(JSAN.Parse(Tools.inputStreamToString(is)).toJSAN());
	}

	@SuppressWarnings("unchecked")
	public static Iterable<ConditionInterpreter> LoadInterpreters(Iterable<Object> classes)
	{
		Set<ConditionInterpreter> loads = new LinkedHashSet<ConditionInterpreter>();

		for (Object object : classes)
		{
			try
			{
				Object o = null;

				if (object instanceof ConditionInterpreter)
				{
					o = (ConditionInterpreter) object;
				}
				else
				{
					Class<ConditionInterpreter> cls = null;

					if (object instanceof Class<?>)
					{
						cls = (Class<ConditionInterpreter>) object;
					}
					else
					{
						cls = (Class<ConditionInterpreter>) ExtensionLoader.forName(object.toString());
					}

					o = cls.newInstance();
				}

				if (o instanceof ConditionInterpreter)
				{
					loads.add((ConditionInterpreter) o);
				}
			}
			catch (ClassCastException e)
			{
				e.printStackTrace();
			}
			catch (InstantiationException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
			}
		}

		return loads;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}
}
