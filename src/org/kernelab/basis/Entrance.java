package org.kernelab.basis;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Entrance of Basis or other project.
 * 
 * @author Dilly King
 * 
 */
public class Entrance
{
	private static final Calendar		CALENDAR			= new GregorianCalendar();

	protected static final DateFormat	VERSION_FORMAT		= new SimpleDateFormat("yyyy.MM.dd");

	public static final char			PARAMETER_PREFIX	= '-';

	public static final JarFile BelongingJarFile(Class<?> cls)
	{
		JarFile jarFile = null;

		try
		{
			jarFile = new JarFile(new File(cls.getProtectionDomain().getCodeSource().getLocation().toURI()));
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return jarFile;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		new Entrance().gather(args).present();
	}

	public static final Map<String, String> Updates(JarFile jarFile)
	{
		Map<String, String> map = new TreeMap<String, String>();

		if (jarFile != null)
		{
			Enumeration<JarEntry> entries = jarFile.entries();

			while (entries.hasMoreElements())
			{
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (name.endsWith(".class"))
				{
					name = name.substring(0, name.lastIndexOf('.')).replace('/', '.');
					map.put(name, UpdateVersion(entry.getTime()));
				}
			}
		}

		return map;
	}

	public static final String UpdateVersion(long time)
	{
		String version = Tools.getDateTimeString(time, VERSION_FORMAT);

		CALENDAR.setTimeInMillis(time);

		CALENDAR.set(Calendar.HOUR_OF_DAY, 0);
		CALENDAR.set(Calendar.MINUTE, 0);
		CALENDAR.set(Calendar.SECOND, 0);
		CALENDAR.set(Calendar.MILLISECOND, 0);

		time -= CALENDAR.getTimeInMillis();
		time /= 1000;

		version += "." + Variable.numberFormatString(time, "00000");

		return version;
	}

	private String[]					arguments;

	private Map<String, List<String>>	parameters;

	private Map<String, String>			updates;

	public Entrance()
	{
		initiate(BelongingJarFile(getClass()));
	}

	public Entrance(Class<?> cls)
	{
		initiate(BelongingJarFile(cls));
	}

	public Entrance(File file)
	{
		try
		{
			initiate(new JarFile(file));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public String argument(int index)
	{
		return arguments()[index];
	}

	public String argument(int index, String defaultValue)
	{
		String value = argument(index);
		return value == null ? defaultValue : value;
	}

	public String[] arguments()
	{
		return arguments;
	}

	protected Entrance delegate()
	{
		String className = this.parameter("main");
		try
		{
			Class<?> cls = Class.forName(className);
			for (Method m : cls.getMethods())
			{
				if ("main".equals(m.getName()) && m.getParameterTypes().length == 1
						&& "java.lang.String[]".equals(m.getParameterTypes()[0].getCanonicalName()))
				{
					String[] argv = new String[arguments().length - 2];
					for (int i = 0; i < argv.length; i++)
					{
						argv[i] = argument(i + 2);
					}
					m.invoke(null, new Object[] { argv });
					break;
				}
			}
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (InvocationTargetException e)
		{
			e.printStackTrace();
		}
		return this;
	}

	public Entrance gather(String... args)
	{
		arguments = args;

		parameters = new LinkedHashMap<String, List<String>>();

		String key = null;

		List<String> values = null;

		for (String arg : arguments)
		{
			if (arg.length() > 1 && arg.charAt(0) == PARAMETER_PREFIX)
			{
				key = arg.substring(1);
				values = new ArrayList<String>();
				parameters.put(key, values);
			}
			else
			{
				values.add(arg);
			}
		}

		return this;
	}

	public boolean hasParameter(String key)
	{
		return parameters(key) != null;
	}

	protected Entrance initiate(JarFile file)
	{
		updates = Collections.unmodifiableMap(Updates(file));
		return this;
	}

	public String parameter(String key)
	{
		List<String> values = this.parameters(key);
		return values == null || values.isEmpty() ? null : values.get(0);
	}

	public String parameter(String key, String defaultValue)
	{
		String value = this.parameter(key);
		return value == null ? defaultValue : value;
	}

	public Map<String, List<String>> parameters()
	{
		return parameters;
	}

	public List<String> parameters(String key)
	{
		return this.parameters().get(key);
	}

	public List<String> parameters(String key, boolean newIfNull)
	{
		List<String> values = this.parameters(key);

		if (values == null && newIfNull)
		{
			values = new ArrayList<String>();
		}

		return values;
	}

	public List<String> parameters(String key, List<String> defaultValues)
	{
		List<String> values = this.parameters(key);

		if (values == null)
		{
			values = defaultValues;
		}

		return values;
	}

	public List<String> parameters(String key, String... defaultValues)
	{
		List<String> values = this.parameters(key);

		if (values == null)
		{
			values = new ArrayList<String>();
			for (String value : defaultValues)
			{
				values.add(value);
			}
		}

		return values;
	}

	public Entrance present()
	{
		if (this.parameter("main") != null)
		{
			return this.delegate();
		}

		String f = parameter("f");
		JarFile file = null;
		if (f == null)
		{
			file = BelongingJarFile(this.getClass());
		}
		else
		{
			try
			{
				file = new JarFile(new File(f));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		initiate(file);

		List<String> u = parameters("u");
		List<String> v = parameters("v");

		if ((u == null && v == null) || (v != null && v.isEmpty()))
		{
			Tools.debug(version());
		}
		else
		{
			Set<String> filter = new HashSet<String>();

			if (u != null && !u.isEmpty())
			{
				Matcher matcher = Pattern.compile(u.get(0)).matcher("");
				for (Entry<String, String> entry : updates().entrySet())
				{
					if (!matcher.reset(entry.getKey()).find())
					{
						filter.add(entry.getKey());
					}
				}
			}

			if (v != null && !v.isEmpty())
			{
				Matcher matcher = Pattern.compile(v.get(0)).matcher("");
				for (Entry<String, String> entry : updates().entrySet())
				{
					if (!matcher.reset(entry.getValue()).find())
					{
						filter.add(entry.getKey());
					}
				}
			}

			for (Entry<String, String> entry : updates().entrySet())
			{
				if (!filter.contains(entry.getKey()))
				{
					Tools.debug(entry.getValue() + '\t' + entry.getKey());
				}
			}
		}

		return this;
	}

	public Map<String, String> updates()
	{
		return updates;
	}

	public String version()
	{
		return Collections.max(updates.values());
	}
}
