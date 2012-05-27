package org.kernelab.basis;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.JSON.JSAN;

public class Arbiter
{
	public static interface ConditionInterpreter
	{
		public boolean identify(Object value, String condition);

		public boolean judge(Object value, String condition);
	}

	protected static final int		LOGICAL_NOT			= -1;
	protected static final String	LOGICAL_NOT_MARK	= "!";

	protected static final int		LOGICAL_OR			= 0;
	protected static final String	LOGICAL_OR_MARK		= "|";

	protected static final int		LOGICAL_AND			= 1;
	protected static final String	LOGICAL_AND_MARK	= "&";

	protected static final int		DEFAULT_LOGIC		= LOGICAL_OR;

	protected static final String	EQUAL_MARK			= "==";

	protected static final String	NOT_EQUAL_MARK		= "!=";

	protected static final String	GREATER_MARK		= ">";

	protected static final String	GREATER_EQUAL_MARK	= ">=";

	protected static final String	LESS_MARK			= "<";

	protected static final String	LESS_EQUAL_MARK		= "<=";

	protected static final String	MATCH_MARK			= "=~";

	public static boolean Arbitrate(JSON values, JSAN condition, Iterable<ConditionInterpreter> interpreters)
	{
		boolean result = true;

		if (values != null && condition != null && !condition.isEmpty()) {

			result = false;

			JSON cnd = null;
			JSAN nst = null;
			Object v = null;
			Object c = null;

			int logic = DEFAULT_LOGIC;

			for (Object o : condition) {

				if (JSON.IsJSON(o)) {

					boolean present = false;

					if ((nst = JSON.AsJSAN(o)) != null) {

						present = Arbitrate(values, nst, interpreters);

					} else if ((cnd = JSON.AsJSON(o)) != null) {

						present = true;

						for (String k : cnd.keySet()) {

							v = values.attr(k);
							c = cnd.attr(k);

							if ((v == null && c != null) || (v != null && c == null)) {
								present = false;
							} else if (v != null && c != null) {

								String vs = v.toString();
								String cs = c.toString();

								boolean hit = false;

								if (interpreters != null) {
									for (ConditionInterpreter interpreter : interpreters) {
										if (interpreter.identify(v, cs)) {
											hit = true;
											present = interpreter.judge(v, cs);
											break;
										}
									}
								}

								if (!hit) {
									boolean isNumValue = v instanceof Number;
									Matcher matcher = null;
									if (cs.startsWith(EQUAL_MARK)) {
										cs = cs.substring(2);
										if (isNumValue && Variable.isNumber(cs)) {
											present = Double.valueOf(vs).compareTo(Double.valueOf(cs)) == 0;
										} else {
											present = vs.compareTo(cs) == 0;
										}
									} else if (cs.startsWith(NOT_EQUAL_MARK)) {
										cs = cs.substring(2);
										if (isNumValue && Variable.isNumber(cs)) {
											present = Double.valueOf(vs).compareTo(Double.valueOf(cs)) != 0;
										} else {
											present = vs.compareTo(cs) != 0;
										}
									} else if (cs.startsWith(GREATER_EQUAL_MARK)) {
										cs = cs.substring(2);
										if (isNumValue && Variable.isNumber(cs)) {
											present = Double.valueOf(vs).compareTo(Double.valueOf(cs)) >= 0;
										} else {
											present = vs.compareTo(cs) >= 0;
										}
									} else if (cs.startsWith(LESS_EQUAL_MARK)) {
										cs = cs.substring(2);
										if (isNumValue && Variable.isNumber(cs)) {
											present = Double.valueOf(vs).compareTo(Double.valueOf(cs)) <= 0;
										} else {
											present = vs.compareTo(cs) <= 0;
										}
									} else if (cs.startsWith(GREATER_MARK)) {
										cs = cs.substring(1);
										if (isNumValue && Variable.isNumber(cs)) {
											present = Double.valueOf(vs).compareTo(Double.valueOf(cs)) > 0;
										} else {
											present = vs.compareTo(cs) > 0;
										}
									} else if (cs.startsWith(LESS_MARK)) {
										cs = cs.substring(1);
										if (isNumValue && Variable.isNumber(cs)) {
											present = Double.valueOf(vs).compareTo(Double.valueOf(cs)) < 0;
										} else {
											present = vs.compareTo(cs) < 0;
										}
									} else if (cs.startsWith(MATCH_MARK)
											&& ((matcher = Pattern.compile("=~\\/(.*)\\/([gim]*)").matcher(cs))
													.matches()))
									{
										String expr = matcher.group(1);
										String flag = matcher.group(2);
										int flags = 0;
										if (flag.contains("i")) {
											flags |= Pattern.CASE_INSENSITIVE;
										}
										if (flag.contains("m")) {
											flags |= Pattern.MULTILINE;
										}
										present = Pattern.compile(expr, flags).matcher(vs).find();
									} else {
										if ((isNumValue && c instanceof Number)
												|| (v instanceof Boolean && c instanceof Boolean))
										{
											present = v.equals(c);
										} else {
											present = Pattern.compile(cs).matcher(vs).find();
										}
									}
								}
							}

							if (!present) {
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

				} else if (o instanceof String) {
					String op = o.toString();
					if (LOGICAL_NOT_MARK.equals(op)) {
						logic = LOGICAL_NOT;
					} else if (LOGICAL_OR_MARK.equals(op)) {
						logic = LOGICAL_OR;
						if (result) {
							break;
						}
					} else if (LOGICAL_AND_MARK.equals(op)) {
						logic = LOGICAL_AND;
						if (!result) {
							break;
						}
					}
				}
			}
		}

		return result;
	}

	public static Iterable<ConditionInterpreter> LoadInterpreters(File file)
	{
		return LoadInterpreters(JSAN.Parse(Tools.inputStringFromFile(file)).toJSAN());
	}

	public static Iterable<ConditionInterpreter> LoadInterpreters(InputStream is)
	{
		return LoadInterpreters(JSAN.Parse(Tools.inputStreamToString(is)).toJSAN());
	}

	public static Iterable<ConditionInterpreter> LoadInterpreters(Iterable<String> classNames)
	{
		List<ConditionInterpreter> interpreters = new LinkedList<ConditionInterpreter>();

		for (String className : classNames) {
			try {
				Object o = Class.forName(className).newInstance();
				if (o instanceof ConditionInterpreter) {
					interpreters.add((ConditionInterpreter) o);
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		return interpreters;
	}

	public static Iterable<ConditionInterpreter> LoadInterpreters(JSAN classNames)
	{
		List<ConditionInterpreter> interpreters = new LinkedList<ConditionInterpreter>();

		for (Object className : classNames) {
			try {
				Object o = Class.forName(className.toString()).newInstance();
				if (o instanceof ConditionInterpreter) {
					interpreters.add((ConditionInterpreter) o);
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		return interpreters;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

}
