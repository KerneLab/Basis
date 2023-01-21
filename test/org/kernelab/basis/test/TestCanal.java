package org.kernelab.basis.test;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.kernelab.basis.Canal;
import org.kernelab.basis.Canal.Action;
import org.kernelab.basis.Canal.JointMapper;
import org.kernelab.basis.Canal.Option;
import org.kernelab.basis.Canal.Producer;
import org.kernelab.basis.Canal.Tuple;
import org.kernelab.basis.Canal.Tuple2;
import org.kernelab.basis.Filter;
import org.kernelab.basis.JSON;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Reducer;
import org.kernelab.basis.Tools;

public class TestCanal
{
	public static class DemoVal
	{
		public int val;

		public DemoVal(int val)
		{
			this.val = val;
		}

		@Override
		public String toString()
		{
			return "" + val;
		}
	}

	public static void main(String[] args)
	{
		Tuple2<Integer, String> t1 = Tuple.of(1, "b");
		Tuple2<Integer, String> t2 = Tuple.of(2, "a");
		Tools.debug(t1.reverse().toString());
		Tools.debug(t1.compareTo(t2));
		Tools.debug(t1);
		Tools.debug(t1.hashCode() + " " + t2.hashCode());
		Tools.debug("============");

		Tools.debug(Canal.of(new Integer[] { 1 }));
		Tools.debug("============");

		Collection<Integer> coll = new LinkedList<Integer>();
		coll.add(1);
		coll.add(2);
		coll.add(2);
		coll.add(3);
		coll.add(4);
		coll.add(5);

		Canal<Integer, Integer> c = Canal.of(coll).filter(new Filter<Integer>()
		{
			@Override
			public boolean filter(Integer element)
			{
				return element > 2;
			}
		});

		Tools.debug(c.collect());
		Tools.debug("============");
		Tools.debug(c.map(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer key)
			{
				return key * 2;
			}
		}).take(2));

		Tools.debug("============of(spring)");
		Tools.debug(Canal.of(new Producer<Double>()
		{
			@Override
			public Double produce()
			{
				return Math.random();
			}
		}).take(5));

		Tools.debug("============");

		Integer[] array = new Integer[] { 1, 2, 3, 4, 4, 5, 6 };
		c = Canal.of(array).filter(new Filter<Integer>()
		{
			@Override
			public boolean filter(Integer element)
			{
				return element > 3;
			}
		});
		c.distinct().foreach(new Action<Integer>()
		{
			@Override
			public void action(Integer el)
			{
				Tools.debug(el + "..");
			}
		});
		Tools.debug("------------");
		Tools.debug(c.distinct().count());
		Tools.debug("============");
		Tools.debug(c.collect());
		Tools.debug("------------");
		Tools.debug(c.count());
		Tools.debug("============");

		Integer[] array1 = new Integer[] { 1, 2, 3 };
		c = Canal.of(array1).flatMap(new Mapper<Integer, Iterable<Integer>>()
		{
			@Override
			public Iterable<Integer> map(Integer key)
			{
				Collection<Integer> a = new LinkedList<Integer>();
				for (int i = 1; i < key; i++)
				{
					a.add(key);
				}
				return a;
			}
		});
		Tools.debug(c.collect());
		Tools.debug("============");
		Tools.debug(c.countByValue());

		Tools.debug("============");
		Tools.debug(Canal.some((Integer) null).count());
		Tools.debug("============");
		Tools.debug(new Canal.None<Integer>().count());

		Tools.debug("============");
		Tools.debug(Canal.of(new Integer[] { 1, 2 }).union(Canal.of(new Integer[] { 4, 5 })).collect());

		Tools.debug("============cartesian");
		Tools.debug(Canal.of(new Integer[] { 1, 2 }).cartesian(Canal.of(new Integer[] { 4, 5 })).collect());

		Tools.debug("============reduce");
		Tools.debug(Canal.of(new Integer[] { 1, 2 }).reduce(new Reducer<Integer, Integer>()
		{
			@Override
			public Integer reduce(Integer result, Integer element)
			{
				return result + element;
			}
		}));

		Tools.debug("============reduce");
		Tools.debug(Canal.of(new Integer[] { 1, 2 }).map(new Mapper<Integer, String>()
		{
			@Override
			public String map(Integer key)
			{
				return String.valueOf(key);
			}
		}).reduce(new Reducer<String, String>()
		{
			@Override
			public String reduce(String result, String element)
			{
				return result + "," + element;
			}
		}));

		Tools.debug("============fold");
		Tools.debug(Canal.of(new Integer[] { 1, 2 }).fold(new HashSet<Integer>(),
				new Reducer<Integer, Collection<Integer>>()
				{
					@Override
					public Collection<Integer> reduce(Collection<Integer> result, Integer element)
					{
						result.add(element);
						return result;
					}
				}));

		Tools.debug("============");
		Tools.debug(Canal.of(new Integer[] { 1, 2 }).first());
		Tools.debug("============");
		Tools.debug(Canal.of(new Integer[] { 1, 2 }).first(new Filter<Integer>()
		{
			@Override
			public boolean filter(Integer element)
			{
				return element > 1;
			}
		}));
		Tools.debug("============");
		Tools.debug(Canal.of(new Integer[] { 1, 2 }).first(new Filter<Integer>()
		{
			@Override
			public boolean filter(Integer element)
			{
				return element > 3;
			}
		}));

		Tools.debug("============collectAsMap");
		Map<?, ?> map = Canal.of(new Integer[] { 1, 2 }).map(new Mapper<Integer, Tuple2<Integer, Integer>>()
		{
			@Override
			public Tuple2<Integer, Integer> map(Integer key)
			{
				return Tuple.of(key, key * 2);
			}
		}).<Integer, Integer> toPair().collectAsMap();
		Tools.debug(map);
		Tools.debug(map.get(1).getClass());

		Tools.debug("============countByKey");
		Map<Integer, Integer> map0 = Canal.of(new Integer[] { 1, 2, 1 })
				.map(new Mapper<Integer, Tuple2<Integer, Integer>>()
				{
					@Override
					public Tuple2<Integer, Integer> map(Integer key)
					{
						return Tuple.of(key, key * 2);
					}
				}).<Integer, Integer> toPair().countByKey();
		Tools.debug(map0);

		Tools.debug("============groupBy");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).groupBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer key)
			{
				return key % 2;
			}
		}).collect());

		Tools.debug("============keyBy");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer key)
			{
				return key % 2;
			}
		}).collect());

		Tools.debug("============collectAsArray");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer key)
			{
				return key % 2;
			}
		}).collect().toArray(new Tuple2[0]));

		Tools.debug("============limit");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).limit(3));

		Tools.debug("============limit -1");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).limit(-1));

		Tools.debug("============skip");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).skip(2));

		Tools.debug("============skip limit");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).skip(2).limit(2));

		Tools.debug("============spring skip limit");
		Tools.debug(Canal.of(new Producer<Integer>()
		{
			int i = 0;

			@Override
			public Integer produce()
			{
				return i++;
			}
		}).skip(2).limit(2));

		Tools.debug("============spring limit skip");
		Tools.debug(Canal.of(new Producer<Integer>()
		{
			int i = 0;

			@Override
			public Integer produce()
			{
				return i++;
			}
		}).limit(3).skip(1));

		Tools.debug("============sliding");
		Canal.of(new Producer<Integer>()
		{
			int i = 0;

			@Override
			public Integer produce()
			{
				return i++;
			}
		}).limit(11).sliding(3, 3).foreach(new Action<Iterable<Integer>>()
		{
			@Override
			public void action(Iterable<Integer> el) throws Exception
			{
				for (Integer l : el)
				{
					Tools.debug(l);
				}
				Tools.debug("---------");
			}
		});

		Tools.debug("============intersection");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).intersection(Canal.of(new Integer[] { 2, 4 })));

		Tools.debug("============subtract");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).subtract(Canal.of(new Integer[] { 2, 4 })));

		Tools.debug("============toString(split)");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).toString(",", "(", ")"));
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).toString("", "(", ""));
		Tools.debug(Canal.of(new Integer[] {}).toString(",", "(", ")", false));
		Tools.debug(Canal.of(new Integer[] {}).toString(",", "(", ")"));

		Tools.debug("============peek");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).peek(new Action<Integer>()
		{
			@Override
			public void action(Integer el)
			{
				Tools.debug(">>" + el);
			}
		}).reverse());

		Tools.debug("============reverse");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).reverse());

		Tools.debug("============zip");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 })
				.zip(Canal.of(new String[] { "one", "two", "three", "four" })).collect());

		Tools.debug("============zipWithIndex");
		Tools.debug(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).zipWithIndex().collect());

		Tools.debug("============sortBy");
		Canal.of(new Integer[][] { //
				new Integer[] { 1, 2 }, //
				new Integer[] { 1, 1 }, //
				new Integer[] { 2, 3 } }) //
				.sortBy(new Mapper<Integer[], Integer>()
				{
					@Override
					public Integer map(Integer[] el)
					{
						return el[0];
					}
				}, false, new Mapper<Integer[], Integer>()
				{
					@Override
					public Integer map(Integer[] el)
					{
						return el[1];
					}
				}, false).foreach(new Canal.Action<Integer[]>()
				{
					@Override
					public void action(Integer[] el)
					{
						Tools.debug(el[0] + "," + el[1]);
					}
				});

		Tools.debug("============sortWith");
		Tools.debug(Canal.of(new Integer[] { 5, 3, 2, 5, 1 }).sortWith(false).collect());

		Tools.debug("============of(Map)");
		Map<Integer, String> map1 = new LinkedHashMap<Integer, String>();
		map1.put(2, "two");
		map1.put(1, "one");
		map1.put(5, "five");
		map1.put(3, "five");
		Canal.of(map1).foreach(new Action<Tuple2<Integer, String>>()
		{
			@Override
			public void action(Tuple2<Integer, String> el)
			{
				Tools.debug(el._1 + ">>" + el._2);
			}
		});

		Tools.debug("============of(JSON)");
		Canal.of(new JSON().attr("one", 1).attr("five", 5).attr("two", 2)).foreach(new Action<Tuple2<String, Object>>()
		{
			@Override
			public void action(Tuple2<String, Object> el)
			{
				Tools.debug(el._1 + " : " + el._2);
			}
		});

		Tools.debug("============stratifyWith");
		Tools.debug(Canal.of(new Integer[] { 5, 3, 2, 5, 1 }).stratifyWith(new Comparator<Integer>()
		{
			@Override
			public int compare(Integer o1, Integer o2)
			{
				return o1 - o2;
			}
		}, false).collect());

		Tools.debug("============stratifyBy");
		Canal.of(new Integer[][] { //
				new Integer[] { 1, 2 }, //
				new Integer[] { 1, 1 }, //
				new Integer[] { 2, 3 } }) //
				.stratifyBy(new Mapper<Integer[], Integer>()
				{
					@Override
					public Integer map(Integer[] el)
					{
						return el[0];
					}
				}, false, new Mapper<Integer[], Integer>()
				{
					@Override
					public Integer map(Integer[] el)
					{
						return el[1];
					}
				}).foreach(new Canal.Action<Canal<?, Integer[]>>()
				{
					@Override
					public void action(Canal<?, Integer[]> el)
					{
						for (Integer[] e : el)
						{
							Tools.debug(e[0] + "," + e[1]);
						}
						Tools.debug("------------");
					}
				});

		Tools.debug("============join");
		Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 4;
			}
		}).join(Canal.of(new Integer[] { 1, 2, 3 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 3;
			}
		})).foreach(new Action<Tuple2<Integer, Tuple2<Integer, Integer>>>()
		{
			@Override
			public void action(Tuple2<Integer, Tuple2<Integer, Integer>> el)
			{
				Tools.debug(el);
			}
		});

		Tools.debug("============leftJoin");
		Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 4;
			}
		}).leftJoin(Canal.of(new Integer[] { 1, 2, 3 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 3;
			}
		})).foreach(new Action<Tuple2<Integer, Tuple2<Integer, Option<Integer>>>>()
		{
			@Override
			public void action(Tuple2<Integer, Tuple2<Integer, Option<Integer>>> el)
			{
				Tools.debug(el);
			}
		});

		Tools.debug("============rightJoin");
		Canal.of(new Integer[] { 1, 2, 3 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 4;
			}
		}).rightJoin(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 3;
			}
		})).foreach(new Action<Tuple2<Integer, Tuple2<Option<Integer>, Integer>>>()
		{
			@Override
			public void action(Tuple2<Integer, Tuple2<Option<Integer>, Integer>> el)
			{
				Tools.debug(el);
			}
		});

		Tools.debug("============fullJoin");
		Canal.of(new Integer[] { 1, 2, 3 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 4;
			}
		}).fullJoin(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 3;
			}
		})).foreach(new Action<Tuple2<Integer, Tuple2<Option<Integer>, Option<Integer>>>>()
		{
			@Override
			public void action(Tuple2<Integer, Tuple2<Option<Integer>, Option<Integer>>> el)
			{
				Tools.debug(el);
			}
		});

		Tools.debug("==============mapJoint");
		Canal.of(new Integer[] { 1, 2, 3 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 4;
			}
		}).fullJoin(Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 3;
			}
		})).mapJoint(
				new JointMapper<Option<Integer>, Option<Integer>, Integer, Tuple2<Option<Integer>, Option<Integer>>>()
				{
					@Override
					public Tuple2<Option<Integer>, Option<Integer>> map(Option<Integer> left, Option<Integer> right,
							Integer key)
					{
						return Tuple.of(left, right);
					}
				}).foreach(new Action<Tuple2<Option<Integer>, Option<Integer>>>()
				{
					@Override
					public void action(Tuple2<Option<Integer>, Option<Integer>> el)
					{
						Tools.debug(el);
					}
				});

		Tools.debug("==============groupByKey");
		Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 3;
			}
		}).groupByKey().foreach(new Action<Tuple2<Integer, Canal<?, Integer>>>()
		{
			@Override
			public void action(Tuple2<Integer, Canal<?, Integer>> el)
			{
				Tools.debug(el);
			}
		});

		Tools.debug("==============having");
		Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 3;
			}
		}).groupByKey().having(new Filter<Canal<?, Integer>>()
		{
			@Override
			public boolean filter(Canal<?, Integer> el)
			{
				return el.count() > 1;
			}
		}).foreach(new Action<Tuple2<Integer, Canal<?, Integer>>>()
		{
			@Override
			public void action(Tuple2<Integer, Canal<?, Integer>> el)
			{
				Tools.debug(el);
			}
		});

		Tools.debug("==============keys");
		Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 3;
			}
		}).keys().foreach(new Action<Integer>()
		{
			@Override
			public void action(Integer el)
			{
				Tools.debug(el);
			}
		});

		Tools.debug("==============values");
		Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 3;
			}
		}).values().foreach(new Action<Integer>()
		{
			@Override
			public void action(Integer el)
			{
				Tools.debug(el);
			}
		});

		Tools.debug("==============mapValues");
		Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 3;
			}
		}).mapValues(new Mapper<Integer, Double>()
		{
			@Override
			public Double map(Integer el)
			{
				return el / 2.0;
			}
		}).foreach(new Action<Tuple2<Integer, Double>>()
		{
			@Override
			public void action(Tuple2<Integer, Double> el)
			{
				Tools.debug(el);
			}
		});

		Tools.debug("==============foldByKey");
		Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 3;
			}
		}).foldByKey(new Producer<Integer>()
		{
			@Override
			public Integer produce()
			{
				return 0;
			}
		}, new Reducer<Integer, Integer>()
		{
			@Override
			public Integer reduce(Integer res, Integer el)
			{
				return res + el;
			}
		}).foreach(new Action<Tuple2<Integer, Integer>>()
		{
			@Override
			public void action(Tuple2<Integer, Integer> el)
			{
				Tools.debug(el);
			}
		});

		Tools.debug("==============reduceByKey");
		Canal.of(new Integer[] { 1, 2, 3, 4, 5 }).keyBy(new Mapper<Integer, Integer>()
		{
			@Override
			public Integer map(Integer el)
			{
				return el % 3;
			}
		}).reduceByKey(new Reducer<Integer, Integer>()
		{
			@Override
			public Integer reduce(Integer a, Integer b)
			{
				return a + b;
			}
		}).foreach(new Action<Tuple2<Integer, Integer>>()
		{
			@Override
			public void action(Tuple2<Integer, Integer> el)
			{
				Tools.debug(el);
			}
		});
	}
}
