package org.kernelab.basis.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import org.kernelab.basis.JSON.JSAN;

public class Extensions
{
	public static class ClassLoader extends URLClassLoader
	{
		public ClassLoader(java.lang.ClassLoader parent)
		{
			super(new URL[0], parent);
		}

		@Override
		public void addURL(URL url)
		{
			super.addURL(url);
		}

		public boolean load(File location)
		{
			return load(location, null);
		}

		public boolean load(File location, String pattern)
		{
			boolean success = false;
			try
			{
				if (location.isFile())
				{
					if (pattern == null || location.getCanonicalPath().matches(pattern))
					{
						success = load(location.toURI().toURL());
					}
				}
				else if (location.isDirectory())
				{
					success = true;
					for (File l : location.listFiles())
					{
						success &= load(l, pattern);
					}
				}
			}
			catch (IOException e)
			{
			}
			return success;
		}

		public boolean load(JSAN locations)
		{
			boolean success = true;
			for (Object o : locations)
			{
				if (o != null)
				{
					success &= load(o.toString());
				}
			}
			return success;
		}

		public boolean load(String location)
		{
			boolean success = false;
			URL url = null;
			File file = null;
			try
			{
				url = new URL(location);
			}
			catch (MalformedURLException e)
			{
				file = new File(location);
			}
			if (url != null)
			{
				success = load(url);
			}
			else if (file != null)
			{
				success = load(file);
			}
			return success;
		}

		public boolean load(URL location)
		{
			this.addURL(location);
			return true;
		}
	}

	private static final Extensions	Singleton	= new Extensions();

	public static Class<?> forName(String className) throws ClassNotFoundException
	{
		return Class.forName(className, true, instance().getLoader());
	}

	public static URL getResource(String name)
	{
		return instance().getLoader().getResource(name);
	}

	public static InputStream getResourceAsStream(String name)
	{
		return instance().getLoader().getResourceAsStream(name);
	}

	public static Enumeration<URL> getResources(String name) throws IOException
	{
		return instance().getLoader().getResources(name);
	}

	public static Extensions instance()
	{
		return Singleton;
	}

	public static boolean load(File location)
	{
		return instance().getLoader().load(location);
	}

	public static boolean load(File location, String pattern)
	{
		return instance().getLoader().load(location, pattern);
	}

	public static boolean load(JSAN locations)
	{
		return instance().getLoader().load(locations);
	}

	public static boolean load(String location)
	{
		return instance().getLoader().load(location);
	}

	public static boolean load(URL location)
	{
		return instance().getLoader().load(location);
	}

	private ClassLoader	loader;

	protected Extensions()
	{
		this(Extensions.class.getClassLoader() == null //
		? java.lang.ClassLoader.getSystemClassLoader() //
				: Extensions.class.getClassLoader());
	}

	protected Extensions(java.lang.ClassLoader cl)
	{
		this.setLoader(new ClassLoader(cl));
	}

	public ClassLoader getLoader()
	{
		return loader;
	}

	public Extensions setLoader(ClassLoader loader)
	{
		if (loader != null)
		{
			this.loader = loader;
		}
		return this;
	}
}
