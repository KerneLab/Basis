package org.kernelab.basis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import org.kernelab.basis.JSON.JSAN;

public class Extensions
{
	public static class ClassLoader extends URLClassLoader
	{
		private Method addToParent = null;

		public ClassLoader(java.lang.ClassLoader parent)
		{
			super(new URL[0], parent);
			this.initAddToParentMethod();
		}

		@Override
		public void addURL(URL url)
		{
			super.addURL(url);
			if (this.getAddToParent() != null)
			{
				try
				{
					this.getAddToParent().invoke(this.getParent(), url);
				}
				catch (Exception e)
				{
				}
			}
		}

		protected Method getAddToParent()
		{
			return addToParent;
		}

		protected void initAddToParentMethod()
		{
			if (this.getParent() instanceof URLClassLoader)
			{
				try
				{
					this.setAddToParent(URLClassLoader.class.getDeclaredMethod("addURL", URL.class));
					this.getAddToParent().setAccessible(true);
				}
				catch (Exception e)
				{
				}
			}
		}

		public int load(File location)
		{
			return load(location, null);
		}

		public int load(File location, String pattern)
		{
			try
			{
				if (location.isFile())
				{
					if (pattern == null || location.getCanonicalPath().matches(pattern))
					{
						return load(location.toURI().toURL());
					}
				}
				else if (location.isDirectory())
				{
					int count = 0;
					count += load(location.toURI().toURL());
					for (File l : location.listFiles())
					{
						count += load(l, pattern);
					}
					return count;
				}

				return 0;
			}
			catch (IOException e)
			{
				return 0;
			}
		}

		public int load(JSAN locations)
		{
			int count = 0;
			for (Object o : locations)
			{
				if (o != null)
				{
					count += load(o.toString());
				}
			}
			return count;
		}

		public int load(String location)
		{
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
				return load(url);
			}
			else if (file != null)
			{
				return load(file);
			}
			else
			{
				return 0;
			}
		}

		public int load(URL location)
		{
			this.addURL(location);
			return 1;
		}

		protected void setAddToParent(Method addToParent)
		{
			this.addToParent = addToParent;
		}
	}

	private static final Extensions Singleton = new Extensions();

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

	public static int load(File location)
	{
		return instance().getLoader().load(location);
	}

	public static int load(File location, String pattern)
	{
		return instance().getLoader().load(location, pattern);
	}

	public static int load(JSAN locations)
	{
		return instance().getLoader().load(locations);
	}

	public static int load(String location)
	{
		return instance().getLoader().load(location);
	}

	public static int load(URL location)
	{
		return instance().getLoader().load(location);
	}

	private ClassLoader loader;

	protected Extensions()
	{
		this(Extensions.class.getClassLoader());
	}

	protected Extensions(java.lang.ClassLoader parent)
	{
		this.setLoader(new ClassLoader(parent == null ? java.lang.ClassLoader.getSystemClassLoader() : parent));
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
