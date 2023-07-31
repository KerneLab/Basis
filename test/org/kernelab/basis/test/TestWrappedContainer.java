package org.kernelab.basis.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.kernelab.basis.HashedEquality;
import org.kernelab.basis.Tools;
import org.kernelab.basis.WrappedHashMap;
import org.kernelab.basis.WrappedHashSet;

public class TestWrappedContainer
{
	public static class DemoObject
	{
		public final int	id;

		public final String	name;

		public DemoObject(int id, String name)
		{
			this.id = id;
			this.name = name;
		}

		@Override
		public String toString()
		{
			return id + " " + name;
		}
	}

	public static void main(String[] args)
	{
		DemoObject a = new DemoObject(1, "a");
		DemoObject b = new DemoObject(1, "a");
		DemoObject c = new DemoObject(2, "c");

		HashedEquality<DemoObject> eql = new HashedEquality<TestWrappedContainer.DemoObject>()
		{
			@Override
			public boolean equals(DemoObject a, DemoObject b)
			{
				return a.id == b.id && Tools.equals(a.name, b.name);
			}

			@Override
			public int hashCode(DemoObject obj)
			{
				return obj.id * 31 + obj.name.hashCode();
			}
		};

		Set<DemoObject> set1 = new HashSet<DemoObject>();
		set1.add(a);
		set1.add(b);
		set1.add(c);
		for (DemoObject o : set1)
		{
			Tools.debug(o);
		}

		Tools.debug("==================");

		Set<DemoObject> set2 = new WrappedHashSet<DemoObject>(eql);
		set2.add(a);
		set2.add(b);
		set2.add(c);
		for (DemoObject o : set2)
		{
			Tools.debug(o);
		}

		Tools.debug("==================");

		Map<DemoObject, Object> map1 = new HashMap<DemoObject, Object>();
		map1.put(a, 1);
		map1.put(b, 2);
		map1.put(c, 3);
		for (Entry<DemoObject, Object> e : map1.entrySet())
		{
			Tools.debug(e.getKey() + " -> " + e.getValue());
		}

		Tools.debug("==================");

		Map<DemoObject, Object> map2 = new WrappedHashMap<DemoObject, Object>(eql);
		map2.put(a, 1);
		map2.put(b, 2);
		map2.put(c, 3);
		for (Entry<DemoObject, Object> e : map2.entrySet())
		{
			Tools.debug(e.getKey() + " -> " + e.getValue());
		}
	}
}
