package org.kernelab.basis.test.proref;

public abstract class DemoAncest
{
	private int		id;

	private String	name;

	public DemoAncest()
	{
		super();
	}

	public DemoAncest(int id, String name)
	{
		this.setId(id);
		this.setName(name);
	}

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
		return "id=" + this.getId() + ", name=" + this.getName();
	}
}
