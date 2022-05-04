package org.kernelab.basis.demo;

import org.kernelab.basis.Agent;
import org.kernelab.basis.Agent.AgentFactory;

public class DemoImplement
{
	public static void main(String[] args)
	{
		DemoInterface i = Agent.newInstance(DemoInterface.class, new AgentFactory()
		{
			@Override
			public <T> Agent newAgent(Class<T> face, Object real)
			{
				return new DemoAgent(real);
			}
		}, new DemoImplement());

		i.say("hey");
	}

	public void say(String msg)
	{
		System.out.println("Say: " + msg);
	}
}
