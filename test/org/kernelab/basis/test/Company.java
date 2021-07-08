package org.kernelab.basis.test;

public class Company
{
	private String	id;

	private String	name;

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public void setId(String id)
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
		return "compId=" + id + ", compName=" + name;
	}
}
