package org.kernelab.basis.demo;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.kernelab.basis.ThreadExecutorPool;
import org.kernelab.basis.Tools;

public class DemoThreadPool
{
	public static class DemoTask implements Callable<Object>
	{
		private final int i;

		public DemoTask(int i)
		{
			this.i = i;
		}

		@Override
		public Object call() throws Exception
		{
			Tools.debug(">>> " + i);
			Thread.sleep(3000);
			Tools.debug("<<< " + i);
			return null;
		}
	}

	public static void main(String[] args)
	{
		ThreadExecutorPool<Object> pool = new ThreadExecutorPool<Object>(8, 100);

		for (int i = 0; i < 20; i++)
		{
			pool.submit(new DemoTask(i));
			Tools.debug(i);
		}

		try
		{
			while (!pool.isEmpty())
			{
				try
				{
					pool.take().get();
				}
				catch (ExecutionException e)
				{
					e.printStackTrace();
				}
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}
