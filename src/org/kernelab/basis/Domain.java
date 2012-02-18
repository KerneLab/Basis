package org.kernelab.basis;

import java.util.TreeSet;

public class Domain extends TreeSet<Interval<Double>> implements Copieable<Domain>
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -3096532408678865031L;

	public static final Domain Intersection(Domain domain, Interval<Double> interval)
	{
		Domain intersection = new Domain();

		for (Interval<Double> i : domain) {
			intersection.add(i.intersectionWith(interval));
		}

		return intersection;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	public Domain()
	{
		super();
	}

	protected Domain(Domain domain)
	{
		super(domain);
	}

	public Domain addInterval(Interval<Double> interval)
	{
		this.add(interval);
		return this;
	}

	public Domain clone()
	{
		return new Domain(this);
	}

	public Domain getIntersectionWith(Interval<Double> interval)
	{
		return Domain.Intersection(this, interval);
	}

}
