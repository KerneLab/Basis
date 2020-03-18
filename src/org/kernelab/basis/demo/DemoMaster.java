package org.kernelab.basis.demo;

import java.util.Collection;
import java.util.LinkedList;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.Tools;

public class DemoMaster
{
	public static void main(String[] args)
	{
		JSON slave1 = new JSON().attr("id", 11).attr("name", "Mike");
		JSON slave2 = new JSON().attr("id", 12).attr("name", "Peter");
		JSON king = new JSON() //
				.attr("id", 1) //
				.attr("name", "King") //
				.attr("slaves", new JSAN().addAll(0, slave1, slave2));
		Tools.debug(king.toString(0));

		DemoMaster m = king.project(new DemoMaster());
		Tools.debug(m);

	}

	private int						id;

	private String					name;

	private Collection<DemoSlave>	slaves	= new LinkedList<DemoSlave>();

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public Collection<DemoSlave> getSlaves()
	{
		return slaves;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setSlaves(Collection<DemoSlave> slaves)
	{
		this.slaves = slaves;
	}
}
