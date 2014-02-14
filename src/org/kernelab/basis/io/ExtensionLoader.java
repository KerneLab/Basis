package org.kernelab.basis.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.kernelab.basis.JSON.JSAN;

public class ExtensionLoader
{
	private static final ExtensionLoader	Loader	= new ExtensionLoader();

	public static ExtensionLoader getInstance()
	{
		return Loader;
	}

	protected URLClassLoader	loader;

	protected Method			load;

	private ExtensionLoader()
	{
		try
		{
			loader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			load = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
			load.setAccessible(true);
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchMethodException e)
		{
			e.printStackTrace();
		}
	}

	public boolean load(File location)
	{
		boolean success = false;
		try
		{
			if (location.isFile())
			{
				success = load(location.toURL());
			}
			else if (location.isDirectory())
			{
				success = true;
				for (File l : location.listFiles())
				{
					success &= load(l);
				}
			}
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		return success;
	}

	public boolean load(JSAN locations)
	{
		boolean success = true;
		for (Object o : locations)
		{
			success &= load(o.toString());
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
		boolean success = false;
		try
		{
			load.invoke(loader, location);
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
		return success;
	}
}
