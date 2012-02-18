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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
		initiate();
	}

	protected void initiate()
	{
		updates = Collections.unmodifiableMap(Updates(BelongingJarFile(this.getClass())));
	}

	protected void present(String... args)
	{
		switch (args.length)
		{
			case 1:
				if (args[0].equals("-v")) {
					Tools.debug(version());
				} else if (args[0].equals("-u")) {
					for (Entry<String, String> entry : updates().entrySet()) {
						Tools.debug(entry.getValue() + '\t' + entry.getKey());
					}
				}
				break;
			case 2:
				if (args[0].equals("-v")) {
					for (Entry<String, String> entry : updates().entrySet()) {
						if (entry.getValue().contains(args[1])) {
							Tools.debug(entry.getValue() + '\t' + entry.getKey());
						}
					}
				} else if (args[0].equals("-u")) {
					for (Entry<String, String> entry : updates().entrySet()) {
						if (entry.getKey().contains(args[1])) {
							Tools.debug(entry.getValue() + '\t' + entry.getKey());
						}
					}
				}
				break;
			default:
				Tools.debug(version());
				break;
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
