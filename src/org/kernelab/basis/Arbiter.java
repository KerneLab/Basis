package org.kernelab.basis;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
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
									if ((v instanceof Number && c instanceof Number)
											|| (v instanceof Boolean && c instanceof Boolean))
									{
										present = v.equals(c);
									} else {
										present = Pattern.compile(cs).matcher(vs).find();
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

	@SuppressWarnings("unchecked")
	public static Iterable<ConditionInterpreter> LoadInterpreters(Iterable<Object> classes)
	{
		List<ConditionInterpreter> interpreters = new LinkedList<ConditionInterpreter>();

		for (Object object : classes) {
			try {
				Class<ConditionInterpreter> cls = null;
				if (object instanceof Class<?>) {
					cls = (Class<ConditionInterpreter>) object;
				} else {
					cls = (Class<ConditionInterpreter>) Class.forName(object.toString());
				}
				Object o = cls.newInstance();
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
