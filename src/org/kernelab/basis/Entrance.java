package org.kernelab.basis;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Entrance of Basis.
 * 
 * @author Dilly King
 * 
 */
public class Entrance
{
	private static final Calendar		CALENDAR		= new GregorianCalendar();

	protected static final DateFormat	VERSION_FORMAT	= new SimpleDateFormat(
																"yyyy.MM.dd");

	public static final JarFile BelongingJarFile(Class<?> cls)
	{
		JarFile jarFile = null;

		try {
			jarFile = new JarFile(new File(cls.getProtectionDomain().getCodeSource()
					.getLocation().toURI()));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return jarFile;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		new Entrance().present(args);
	}

	public static final Map<String, String> Updates(JarFile jarFile)
	{
		Map<String, String> map = new TreeMap<String, String>();

		if (jarFile != null) {

			Enumeration<JarEntry> entries = jarFile.entries();

			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (name.endsWith(".class")) {
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

	private Map<String, String>	updates;

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
		try {
			initiate(new JarFile(file));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected Map<String, String> initiate(JarFile file)
	{
		return updates = Collections.unmodifiableMap(Updates(file));
	}

	protected void present(String... args)
	{
		Map<String, String> map = new LinkedHashMap<String, String>();

		String key = null;
		String value = null;
		for (String arg : args) {
			if (arg.startsWith("-")) {
				key = arg.substring(1);
			} else {
				value = arg;
			}
			map.put(key, value);
			if (value != null) {
				key = null;
				value = null;
			}
		}

		JarFile file = null;
		if (map.get("f") == null) {
			file = BelongingJarFile(this.getClass());
		} else {
			try {
				file = new JarFile(new File(map.get("f")));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		initiate(file);

		if ((!map.containsKey("u") && !map.containsKey("v"))
				|| (map.containsKey("v") && map.get("v") == null))
		{
			Tools.debug(version());
		} else {
			Set<String> filter = new HashSet<String>();

			if (map.containsKey("u")) {
				if (map.get("u") != null) {
					Matcher matcher = Pattern.compile(map.get("u")).matcher("");
					for (Entry<String, String> entry : updates().entrySet()) {
						if (!matcher.reset(entry.getKey()).find()) {
							filter.add(entry.getKey());
						}
					}
				}
			}

			if (map.containsKey("v")) {
				if (map.get("v") != null) {
					Matcher matcher = Pattern.compile(map.get("v")).matcher("");
					for (Entry<String, String> entry : updates().entrySet()) {
						if (!matcher.reset(entry.getValue()).find()) {
							filter.add(entry.getKey());
						}
					}
				}
			}

			for (Entry<String, String> entry : updates().entrySet()) {
				if (!filter.contains(entry.getKey())) {
					Tools.debug(entry.getValue() + '\t' + entry.getKey());
				}
			}
		}
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
