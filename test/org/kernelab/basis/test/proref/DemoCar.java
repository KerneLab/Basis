package org.kernelab.basis.test.proref;

import java.math.BigDecimal;

public class DemoCar implements DemoThing
{
	private String		type;

	private BigDecimal	runs;

	public DemoCar()
	{
		super();
	}

	public DemoCar(String type, BigDecimal runs)
	{
		this.setType(type);
		this.setRuns(runs);
	}

	public BigDecimal getRuns()
	{
		return runs;
	}

	public String getType()
	{
		return type;
	}

	public void setRuns(BigDecimal runs)
	{
		this.runs = runs;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	@Override
	public String toString()
	{
		return this.getType() + ": " + this.getRuns();
	}
}
