package org.kernelab.basis.demo;

import java.lang.reflect.Method;

import org.kernelab.basis.Agent;

public class DemoAgent extends Agent
{
	public DemoAgent(Object real)
	{
		super(real);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		return find(this.real.getClass(), method).invoke(this.real, args);
	}
}
