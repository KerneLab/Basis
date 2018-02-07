package org.kernelab.basis.demo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.kernelab.basis.Filter;
import org.kernelab.basis.IndexedReducer;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Reducer;
import org.kernelab.basis.Terminator;
import org.kernelab.basis.Tools;

public class DemoFilterMapperReducer
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// 定义一组集合
		Tools.debug("Origin:");
		Double[] array = new Double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
		List<Double> list = new ArrayList<Double>();
		Tools.listOfArray(list, array);
		Tools.debug(list);
		Tools.debug("");

		// 过滤算子
		Tools.debug("Filter:");
		Collection<Double> filtered = Tools.filter(list, new Filter<Double>()
		{

			public boolean filter(Double e)
			{
				// 将小于等于3的元素过滤保留至结果集合中
				return e <= 3 ? false : true;
			}

		}, null);
		Tools.debug(filtered);
		Tools.debug("");

		// 映射算子
		Tools.debug("Map:");
		Collection<Double> mapped = Tools.map(list, new Mapper<Double, Double>()
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 8612178379704653348L;

			public Double map(Double value)
			{
				// 新集合中的元素来源于原始集合中每个元素乘2加1
				return value * 2 + 1;
			}
		}, null);
		Tools.debug(mapped);
		Tools.debug("");

		// 化简算子
		Tools.debug("Reduce:");
		Double sum = Tools.reduce(list, new Reducer<Double, Double>()
		{

			public Double reduce(Double r, Double e)
			{
				// 对原始集合中所有元素简单求和
				return r + e;
			}
		}, 0.0);
		Tools.debug(sum);
		Tools.debug("");

		// 带循环索引的化简算子
		Tools.debug("Indexed Reduce:");
		Double part = Tools.reduce(list, new IndexedReducer<Double, Double>()
		{

			public Double reduce(Double result, Double element, int index)
			{
				if (index < 3)
				{
					// 对原始集合中前3个元素部分求和
					return result + element;
				}
				else
				{
					// 化简到其他元素时，抛出Terminator以终止化简过程
					throw Terminator.SIGNAL;
				}
			}
		}, 0.0);
		Tools.debug(part);
	}
}
