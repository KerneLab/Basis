package org.kernelab.basis.demo;

import java.util.HashMap;
import java.util.Map;

import org.kernelab.basis.Canal;
import org.kernelab.basis.Canal.Tuple;
import org.kernelab.basis.Canal.Tuple2;
import org.kernelab.basis.JSON;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;

public class DemoCanal
{

	public static void main(String[] args)
	{
		Map<Tuple2<String, String>, Canal<?, JSON>> m = Canal.of(new Integer[] { 0, 1, 2, 3, 4 })
				.keyBy(new Mapper<Integer, Tuple2<String, String>>()
				{
					@Override
					public Tuple2<String, String> map(Integer el)
					{
						return Tuple.of(String.valueOf(el % 2), String.valueOf(el % 2));
					}
				}).groupByKey().peek(new Canal.Action<Canal.Tuple2<Tuple2<String, String>, Canal<?, Integer>>>()
				{
					@Override
					public void action(Tuple2<Tuple2<String, String>, Canal<?, Integer>> el)
					{
						Tools.debug(el);
					}
				}).collectAsMap(new HashMap<Tuple2<String, String>, Canal<?, JSON>>());

		Tools.debug(m);
	}

}
