package org.kernelab.basis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.JSON.Pair;
import org.kernelab.basis.io.Extensions;

/**
 * The Entrance of Basis or other project.
 * 
 * @author Dilly King
 * 
 */
public class Entrance
{
	protected static class ClassFile
	{
		protected static final int	MASK_VERSION	= 1;

		protected static final int	MASK_LEVEL		= 1 << 1;

		private JarFile				file;

		private JarEntry			entry;

		private String				name;

		private String				version;

		private String				level;

		public ClassFile(JarFile file, JarEntry entry)
		{
			this.setFile(file).setEntry(entry).setName(entry.getName()).setVersion(UpdateVersion(entry.getTime()))
					.setLevel(Level(file, entry));
		}

		public String getCompileLevel()
		{
			float level = Float.NaN;

			String version = this.getLevel();
			try
			{
				int value = Integer.parseInt(version.substring(0, version.indexOf('.')));
				level = (value - 34) / 10.0f;
			}
			catch (Exception e)
			{
			}

			return String.valueOf(level);
		}

		public JarEntry getEntry()
		{
			return entry;
		}

		public JarFile getFile()
		{
			return file;
		}

		public String getLevel()
		{
			return level;
		}

		public String getName()
		{
			return name;
		}

		public String getVersion()
		{
			return version;
		}

		private ClassFile setEntry(JarEntry entry)
		{
			this.entry = entry;
			return this;
		}

		private ClassFile setFile(JarFile file)
		{
			this.file = file;
			return this;
		}

		private ClassFile setLevel(String level)
		{
			this.level = level;
			return this;
		}

		private ClassFile setName(String name)
		{
			this.name = name.substring(0, name.lastIndexOf('.')).replace('/', '.');
			return this;
		}

		private ClassFile setVersion(String version)
		{
			this.version = version;
			return this;
		}

		public String toString(int mask)
		{
			StringBuilder buffer = new StringBuilder();

			if ((mask & MASK_VERSION) != 0)
			{
				buffer.append(this.getVersion());
				buffer.append(DISPLAY_SEPARATOR);
			}

			if ((mask & MASK_LEVEL) != 0)
			{
				buffer.append(this.getCompileLevel());
				buffer.append(DISPLAY_SEPARATOR);
			}

			return buffer.toString();
		}
	}

	private static final Calendar		CALENDAR			= new GregorianCalendar();

	protected static final DateFormat	VERSION_FORMAT		= new SimpleDateFormat("yyyy.MM.dd");

	public static final char			PARAMETER_PREFIX	= '-';

	public static final char			DISPLAY_SEPARATOR	= ' ';

	public static final byte[]			CLASS_MAGIC_NUMBER	= new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA,
			(byte) 0xBE									};

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

	public static final Map<String, ClassFile> Classes(JarFile file)
	{
		Map<String, ClassFile> map = new TreeMap<String, ClassFile>();

		if (file != null)
		{
			Enumeration<JarEntry> entries = file.entries();

			while (entries.hasMoreElements())
			{
				JarEntry entry = entries.nextElement();
				if (!entry.isDirectory() && entry.getName().endsWith(".class"))
				{
					ClassFile cls = new ClassFile(file, entry);
					map.put(cls.getName(), cls);
				}
			}
		}

		return map;
	}

	public static final String Level(JarFile file, JarEntry entry)
	{
		String level = null;

		if (file != null && entry != null)
		{
			byte[] head = new byte[8];

			InputStream is = null;
			try
			{
				is = file.getInputStream(entry);

				if (is.read(head) == head.length && Tools.samePrefix(CLASS_MAGIC_NUMBER, head))
				{
					level = ((((int) head[6]) << 8) + head[7]) + "." + ((((int) head[4]) << 8) + head[5]);
				}
			}
			catch (IOException e)
			{
			}
			finally
			{
				if (is != null)
				{
					try
					{
						is.close();
					}
					catch (IOException e)
					{
					}
				}
			}
		}

		return level;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		new Entrance().handle(args).present();
	}

	/**
	 * To output the manual information.<br />
	 * The manual data should be formed as <br />
	 * 
	 * <pre>
	 * {
	 * 	"key":{
	 * 		"":"main text of key",
	 * 		"sub1":"sub1 direct text",
	 * 		"sub2":{
	 * 			...
	 * 		}
	 * 		...
	 * 	},
	 * 	"other key":{
	 * 		...
	 * 	}
	 * }
	 * </pre>
	 * 
	 * @param out
	 * @param json
	 * @param indents
	 * @param indent
	 * @param lineWrap
	 * @return
	 * @throws IOException
	 */
	public static final Writer Manual(Writer out, JSON json, int indents, String indent, String lineWrap)
			throws IOException
	{
		if (out != null && json != null)
		{
			for (Pair pair : json.pairs())
			{
				String key = pair.key();

				if (Tools.notNullOrEmpty(key))
				{
					Tools.repeat(out, indent, indents);

					out.append(key);

					Object val = pair.val();

					if (JSON.IsJSON(val))
					{
						JSON j = (JSON) val;

						String mainText = j.valString("");
						if (Tools.notNullOrWhite(mainText))
						{
							out.append(indent);
							out.append(mainText);
						}
						out.append(lineWrap);

						Manual(out, j, indents + 1, indent, lineWrap);
					}
					else
					{
						if (val != null)
						{
							out.append(indent);
							out.append(val.toString());
						}
						out.append(lineWrap);
					}
				}
			}
		}

		return out;
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

	private Map<String, ClassFile>		classes;

	public Entrance()
	{
		initiate(BelongingJarFile(getClass()));
	}

	public Entrance(Class<?> cls)
	{
		initiate(BelongingJarFile(cls));
	}

	public Entrance(File file) throws IOException
	{
		initiate(new JarFile(file));
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

	/**
	 * To handle the given parameters like below
	 * 
	 * <pre>
	 * key1=val1 key2=val2
	 * </pre>
	 * 
	 * @param args
	 * @return
	 */
	public Entrance assign(String... args)
	{
		this.reset(args);

		if (arguments != null)
		{
			for (String arg : arguments)
			{
				if (arg != null)
				{
					String[] pair = Tools.splitCharSequence(arg, '=', 2);

					String key = pair[0];

					if (key.length() > 0)
					{
						List<String> values = parameters.get(key);

						if (values == null)
						{
							values = new ArrayList<String>(1);
							parameters.put(key, values);
						}

						if (pair.length > 1)
						{
							values.add(pair[1]);
						}
					}
				}
			}
		}

		return this;
	}

	public Map<String, ClassFile> classes()
	{
		return classes;
	}

	protected Entrance delegate()
	{
		String className = this.parameter("main");
		try
		{
			Class<?> cls = Extensions.forName(className);
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

	/**
	 * To handle the given parameters like below
	 * 
	 * <pre>
	 * -key1 val1 -key2 val21 val22
	 * </pre>
	 * 
	 * @param args
	 * @return
	 */
	public Entrance gather(String... args)
	{
		this.reset(args);

		String key = null;

		List<String> values = null;

		if (arguments != null)
		{
			for (String arg : arguments)
			{
				if (arg != null)
				{
					if (arg.length() > 1 && arg.charAt(0) == PARAMETER_PREFIX)
					{
						key = arg.substring(1);
						values = new ArrayList<String>(1);
						parameters.put(key, values);
					}
					else if (values != null)
					{
						values.add(arg);
					}
				}
			}
		}

		return this;
	}

	/**
	 * To handle different form of arguments. If the first argument is start
	 * with '-' then the gather(String...) method would be used, otherwise,
	 * assign(String...) would be used.
	 * 
	 * @param args
	 * @return
	 */
	public Entrance handle(String... args)
	{
		if (args != null && args.length > 0)
		{
			String param = args[0];

			if (param != null)
			{
				if (param.length() > 1 && param.charAt(0) == PARAMETER_PREFIX)
				{
					this.gather(args);
				}
				else
				{
					this.assign(args);
				}
			}
		}
		else
		{
			this.reset(args);
		}

		return this;
	}

	public boolean hasParameter(String key)
	{
		return parameters(key) != null;
	}

	protected Entrance initiate(JarFile file)
	{
		classes = Collections.unmodifiableMap(Classes(file));
		return this;
	}

	public String parameter(String key)
	{
		List<String> values = this.parameters(key);
		return values == null || values.isEmpty() ? null : values.get(values.size() - 1);
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
		if (parameters("jars") != null)
		{
			for (String file : parameters("jars"))
			{
				Extensions.load(new File(file), "^.+\\.jar$");
			}
		}

		if (parameter("main") != null)
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

		// Display the last update version.
		List<String> v = parameters("v");

		// Filter via a given class name pattern.
		List<String> c = parameters("c");

		// Filter via a given update version pattern.
		List<String> u = parameters("u");

		// Filter via a given compile level pattern.
		List<String> l = parameters("l");

		if (v != null || (c == null && u == null && l == null))
		{
			Tools.debug(version());
		}
		else
		{
			int mask = 0;

			Matcher classMatcher = null;
			if (c != null)
			{
				if (!c.isEmpty())
				{
					classMatcher = Pattern.compile(c.get(0)).matcher("");
				}
			}

			Matcher updateMatcher = null;
			if (u != null)
			{
				mask |= ClassFile.MASK_VERSION;
				if (!u.isEmpty())
				{
					updateMatcher = Pattern.compile(u.get(0)).matcher("");
				}
			}

			Matcher levelMatcher = null;
			if (l != null)
			{
				mask |= ClassFile.MASK_LEVEL;
				if (!l.isEmpty())
				{
					levelMatcher = Pattern.compile(l.get(0)).matcher("");
				}
			}

			for (Entry<String, ClassFile> entry : classes().entrySet())
			{
				String className = entry.getKey();
				ClassFile classFile = entry.getValue();

				boolean filter = false;

				if (classMatcher != null)
				{
					if (!classMatcher.reset(className).find())
					{
						filter = true;
					}
				}

				if (!filter && updateMatcher != null)
				{
					if (!updateMatcher.reset(classFile.getVersion()).find())
					{
						filter = true;
					}
				}

				if (!filter && levelMatcher != null)
				{
					if (!levelMatcher.reset(classFile.getCompileLevel()).find())
					{
						filter = true;
					}
				}

				if (!filter)
				{
					Tools.debug(classFile.toString(mask) + className);
				}
			}
		}

		return this;
	}

	protected Entrance reset(String... args)
	{
		arguments = args;

		parameters = new LinkedHashMap<String, List<String>>();

		return this;
	}

	public String version()
	{
		String version = "";

		for (ClassFile file : this.classes().values())
		{
			if (file.getVersion().compareTo(version) > 0)
			{
				version = file.getVersion();
			}
		}

		return version;
	}
}
