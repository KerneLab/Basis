package org.kernelab.basis.test;

import org.kernelab.basis.Canal;
import org.kernelab.basis.Graph;
import org.kernelab.basis.Tools;

public class TestGraph
{
	public static void main(String[] args)
	{
		Graph<Object, Object> net = new Graph<Object, Object>();

		Tools.debug("====1====");
		net.add(1, 2);
		assert net.hasCycle() == false;

		Tools.debug("====2====");
		net.clear() //
				.add(1, 2) //
				.add(2, 1) //
		;
		assert net.hasCycle() == true;

		Tools.debug("====3====");
		net.clear() //
				.add(1, 2) //
				.add(2, 3) //
				.add(3, 1) //
		;
		assert net.hasCycle() == true;

		Tools.debug("====4====");
		net.clear() //
				.add(1, 2) //
				.add(1, 3) //
				.add(2, 4) //
				.add(3, 4) //
		;
		assert net.hasCycle() == false;

		Tools.debug("====5====");
		net.clear() //
				.add(1, 2) //
				.add(2, 3) //
				.add(3, 1) //
				.add(3, 4) //
				.add(1, 4) //
		;
		assert net.hasCycle() == true;

		Tools.debug("====6====");
		net.clear() //
				.add(1, 2) //
				.add(2, 3) //
				.add(1, 3) //
				.add(3, 4) //
				.add(4, 5) //
				.add(5, 6) //
				.add(6, 4) //
		;
		assert net.hasCycle() == true;

		Tools.debug("====7====");
		net.clear() //
				.add(1, 3) //
				.add(1, 4) //
				.add(2, 4) //
				.add(3, 6) //
				.add(4, 8) //
				.add(4, 5) //
				.add(6, 7) //
				.add(6, 8) //
				.add(7, 9) //
				.add(8, 9) //
		;

		for (Object n : net.getNodes())
		{
			if (net.getInDegree(n) == 0)
			{
				Tools.debug(n);
			}
		}

		Tools.debug(Canal.of(net.topSort()).toString(","));

		Tools.debug(Canal.of(net.reverse().topSort()).toString(","));

		Tools.debug(net.stratify());
	}
}
