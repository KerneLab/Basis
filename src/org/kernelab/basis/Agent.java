package org.kernelab.basis;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class Agent implements InvocationHandler
{
	public static interface AgentFactory
	{
		public <T> Agent newAgent(Class<T> face, Object real);
	}

	@SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<T> face, AgentFactory factory, Object real)
	{
		return (T) Proxy.newProxyInstance(real.getClass().getClassLoader(), new Class<?>[] { face },
				factory.newAgent(face, real));
	}

	@SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<T> face, Class<? extends Agent> agent, Object real)
	{
		try
		{
			return (T) Proxy.newProxyInstance(real.getClass().getClassLoader(), new Class<?>[] { face },
					agent.getConstructor(Object.class).newInstance(real));
		}
		catch (InstantiationException e)
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
		catch (NoSuchMethodException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<T> face, Object real)
	{
		return (T) Proxy.newProxyInstance(real.getClass().getClassLoader(), new Class<?>[] { face }, new Agent(real));
	}

	protected final Object real;

	public Agent(Object real)
	{
		this.real = real;
	}

	protected Method find(Class<?> real, Method method) throws Throwable
	{
		return real.getMethod(method.getName(), method.getParameterTypes());
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		return find(this.real.getClass(), method).invoke(this.real, args);
	}
}
