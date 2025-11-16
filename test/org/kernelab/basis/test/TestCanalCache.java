package org.kernelab.basis.test;

import java.util.ArrayList;
import java.util.Collection;

import org.kernelab.basis.Canal;
import org.kernelab.basis.Canal.Action;
import org.kernelab.basis.Filter;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;
import org.kernelab.basis.Variable;

public class TestCanalCache
{
	public static void main(String[] args)
	{
		Collection<String> data = new ArrayList<String>();
		data.add("0");
		data.add("1");
		data.add("2");
		data.add("3");
		data.add("4");
		data.add("5");
		data.add("6");
		data.add("7");
		data.add("8");

		Canal<Double> c = Canal.of(data).filter(new Filter<String>()
		{
			@Override
			public boolean filter(String el) throws Exception
			{
				return Variable.isIntegerNumber(el);
			}
		}).map(new Mapper<String, Double>()
		{
			@Override
			public Double map(String el) throws Exception
			{
				double d = Integer.valueOf(el) + 1.1;
				Tools.debug("mapping ... " + d);
				return d;
			}
		}).filter(new Filter<Double>()
		{
			@Override
			public boolean filter(Double el) throws Exception
			{
				return Math.floor(el) % 2 == 1;
			}
		}).cache();

		c.limit(3).foreach(new Action<Double>()
		{
			@Override
			public void action(Double el) throws Exception
			{
				Tools.debug(el);
			}
		});

		Tools.debug("=============");

		c.limit(4).foreach(new Action<Double>()
		{
			@Override
			public void action(Double el) throws Exception
			{
				Tools.debug(el);
			}
		});

		Tools.debug("=============");

		c.foreach(new Action<Double>()
		{
			@Override
			public void action(Double el) throws Exception
			{
				Tools.debug(el);
			}
		});
		
		Tools.debug("=============");

		c.uncache().foreach(new Action<Double>()
		{
			@Override
			public void action(Double el) throws Exception
			{
				Tools.debug(el);
			}
		});
	}
}
