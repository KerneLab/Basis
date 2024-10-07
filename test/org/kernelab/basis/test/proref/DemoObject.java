package org.kernelab.basis.test.proref;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.Projector;
import org.kernelab.basis.JSON.Reflector;
import org.kernelab.basis.Tools;

public class DemoObject extends DemoAncest implements DemoThing
{
	private static Map<Class<?>, Object>	PROJECTOR	= new LinkedHashMap<Class<?>, Object>();

	private static Map<Class<?>, Object>	REFLECTOR	= new LinkedHashMap<Class<?>, Object>();

	static
	{
		PROJECTOR.put(DemoThing.class, new Projector<DemoThing>()
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public DemoThing instance(Class<DemoThing> cls, JSON json) throws Exception
			{
				return (DemoThing) Class.forName(json.attrString("_class")).newInstance();
			}

			@Override
			public DemoThing project(DemoThing obj, JSON json) throws Exception
			{
				return (DemoThing) JSON.Project(obj, json);
			}
		});
		PROJECTOR = Collections.unmodifiableMap(PROJECTOR);

		///////////////////////////////////////////

		REFLECTOR.put(DemoThing.class, new Reflector<DemoThing>()
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public JSON reflect(JSON json, DemoThing obj)
			{
				return JSON.Reflect(json.attr("_class", obj.getClass().getName()), obj);
			}
		});
		REFLECTOR = Collections.unmodifiableMap(REFLECTOR);
	}

	public static void main(String[] args)
	{
		// DemoObject o = new DemoObject(1, "abc", new BigDecimal("1234.56"));
		// o.getThings().add(new DemoCar("4wd", new BigDecimal("444.80")));
		// o.getDict().put("x", new DemoCar("2wd", new BigDecimal("22.50")));
		// Tools.debug(o.toString());
		//
		// Tools.debug(Tools.repeat('=', 50));
		//
		// JSON j = reflect(o);

		String s = "{\"_class\":\"org.kernelab.basis.test.proref.DemoObject\"," //
				+ "\"id\":1,\"name\":\"abc\",\"income\":1234.56," //
				+ "\"nums\":[[1,2],[3]]," //
				+ "\"strs\":[[\"a\",\"b\"],[\"c\"]]," //
				+ "\"things\":[{\"_class\":\"org.kernelab.basis.test.proref.DemoCar\",\"type\":\"4wd\",\"runs\":444.80}],"
				+ "\"dict\":{\"x\":{\"_class\":\"org.kernelab.basis.test.proref.DemoCar\",\"type\":\"2wd\",\"runs\":22.50}},"
				+ "\"listOfStrs\":[[\"x\"],[\"y\",\"z\"]]" //
				+ "}";
		JSON j = JSON.Parse(s);
		Tools.debug(j.toString(0));

		Tools.debug(Tools.repeat('=', 50));

		DemoObject oo = (DemoObject) project(j);
		Tools.debug(oo.toString());
	}

	public static Object project(JSON json)
	{
		if (json == null)
		{
			return null;
		}
		String cls = json.attrString("_class");
		if (cls == null)
		{
			return null;
		}
		try
		{
			return json.projectStrict(true).projects(projector()).project(Class.forName(cls));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static Map<Class<?>, Object> projector()
	{
		return PROJECTOR;
	}

	public static JSON reflect(Object obj)
	{
		if (obj == null)
		{
			return null;
		}
		else
		{
			return new JSON().reflects(reflector()).reflect(obj);
		}
	}

	public static Map<Class<?>, Object> reflector()
	{
		return REFLECTOR;
	}

	private BigDecimal							income;

	private int[][]								nums;

	private String[][]							strs;

	private List<DemoThing>						things		= new LinkedList<DemoThing>();

	private Map<String, ? extends DemoThing>	dict		= new LinkedHashMap<String, DemoThing>();

	private List<String[]>						listOfStrs	= new LinkedList<String[]>();

	public DemoObject()
	{
		super();
	}

	public DemoObject(int id, String name, BigDecimal income)
	{
		super(id, name);
		this.setIncome(income);
	}

	public Map<String, ? extends DemoThing> getDict()
	{
		return dict;
	}

	public BigDecimal getIncome()
	{
		return income;
	}

	public List<String[]> getListOfStrs()
	{
		return listOfStrs;
	}

	public int[][] getNums()
	{
		return nums;
	}

	public String[][] getStrs()
	{
		return strs;
	}

	public List<DemoThing> getThings()
	{
		return things;
	}

	public void setDict(Map<String, ? extends DemoThing> dict)
	{
		this.dict = dict;
	}

	public void setIncome(BigDecimal income)
	{
		this.income = income;
	}

	public void setListOfStrs(List<String[]> listOfStrs)
	{
		this.listOfStrs = listOfStrs;
	}

	public void setNums(int[][] nums)
	{
		this.nums = nums;
	}

	public void setStrs(String[][] strs)
	{
		this.strs = strs;
	}

	public void setThings(List<DemoThing> things)
	{
		this.things = things;
	}

	@Override
	public String toString()
	{
		String str = super.toString() + ", income=" + this.getIncome();

		str += "\nnums:";
		if (this.getNums() != null)
		{
			for (int[] i : this.getNums())
			{
				for (int j : i)
				{
					str += "\n " + j;
				}
				str += "\n---";
			}
		}
		else
		{
			str += " null";
		}

		str += "\nstrs:";
		if (this.getStrs() != null)
		{
			for (String[] i : this.getStrs())
			{
				for (String j : i)
				{
					str += "\n " + j;
				}
				str += "\n---";
			}
		}
		else
		{
			str += " null";
		}

		str += "\nthings:";
		for (Object d : this.getThings())
		{
			str += "\n " + d.toString();
		}

		str += "\ndict:";
		for (Entry<String, ?> entry : this.getDict().entrySet())
		{
			str += "\n " + entry.getKey() + " -> " + entry.getValue();
		}

		str += "\nlistOfStrs:";
		for (String[] i : this.getListOfStrs())
		{
			for (String j : i)
			{
				str += "\n " + j;
			}
			str += "\n---";
		}

		return str;
	}
}
