package org.kernelab.basis.demo;

import java.lang.reflect.InvocationTargetException;

import org.kernelab.basis.Accessor;
import org.kernelab.basis.Tools;

public class DemoAccessor
{
	public static class DemoClass1
	{
		private int		id;

		private String	name;

		public int getId()
		{
			return id;
		}

		public String getName()
		{
			return name;
		}

		public void setId(int id)
		{
			this.id = id;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return this.getId() + "\t" + this.getName();
		}
	}

	public static class DemoClass2 extends DemoClass1
	{
		private double	score;

		private String	grp;

		public String getGroup()
		{
			return grp;
		}

		public double getScore()
		{
			return score;
		}

		public void setGroup(String group)
		{
			this.grp = group;
		}

		public void setScore(double score)
		{
			this.score = score;
		}

		@Override
		public String toString()
		{
			return super.toString() + "\t" + this.getScore() + "\t" + this.getGroup();
		}
	}

	public static void main(String[] args)
	{
		Accessor a1, a2, a3, a4;

		a1 = Accessor.Of(DemoClass2.class, "id");
		a2 = Accessor.Of(DemoClass2.class, "name");
		a3 = Accessor.Of(DemoClass2.class, "score");
		a4 = Accessor.Of(DemoClass2.class, "group");

		DemoClass1 obj1 = new DemoClass1();
		DemoClass1 obj2 = new DemoClass2();

		try
		{
			a1.set(obj1, 1);
			a2.set(obj1, "mike");

			Tools.debug(obj1);
			Tools.debug(a1.get(obj1));
			Tools.debug(a2.get(obj1));

			a1.set(obj2, 2);
			a2.set(obj2, "john");
			a3.set(obj2, 2.321);
			a4.set(obj2, "G2");

			Tools.debug(obj2);
			Tools.debug(a1.get(obj2));
			Tools.debug(a2.get(obj2));
			Tools.debug(a3.get(obj2));
			Tools.debug(a4.get(obj2));

			a4.set(obj2, null);
			Tools.debug(obj2);
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (InvocationTargetException e)
		{
			e.printStackTrace();
		}
	}
}
