package org.kernelab.basis.demo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DemoInvocation implements InvocationHandler
{
	public static void makeProxy()
	{
		
	}

	public static void main(String[] args)
	{
		DemoInterface i = (DemoInterface) Proxy.newProxyInstance(DemoInterface.class.getClassLoader(),
				new Class<?>[] { DemoInterface.class }, new DemoInvocation());

		i.say("hey");
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		Object target = new DemoImplement();
		return target.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(target, args);
	}
}
