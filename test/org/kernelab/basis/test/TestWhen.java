package org.kernelab.basis.test;

import org.kernelab.basis.Canal;
import org.kernelab.basis.Filter;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;
import org.kernelab.basis.Canal.Action;

public class TestWhen
{
	public static void main(String[] args)
	{
		Canal.of(new String[] { "1", "22", "", "333" }).map(Tools.when(new Filter<CharSequence>()
		{
			@Override
			public boolean filter(CharSequence el) throws Exception
			{
				return el.toString().length() > 1;
			}

		}, new Mapper<CharSequence, Integer>()
		{
			@Override
			public Integer map(CharSequence el) throws Exception
			{
				return el.toString().length();
			}
		})).foreach(new Action<Number>()
		{
			@Override
			public void action(Number el) throws Exception
			{
				Tools.debug(el);
			}
		});
	}
}
