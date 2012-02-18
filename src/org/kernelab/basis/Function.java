package org.kernelab.basis;

import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;

public abstract class Function extends TreeMap<Double, Double> implements
		Copieable<Function>
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -5963799109903888876L;

	public static double		DEFAULT_STEP		= 0.01;

	public static final TreeSet<Double> discretizeDomain(Domain domain, double step)
	{
		TreeSet<Double> points = new TreeSet<Double>();

		for (Interval<Double> interval : domain) {

			double from = interval.getLower() + (interval.isLowered() ? 0 : step / 2);
			double to = interval.getUpper() - (interval.isUppered() ? 0 : step / 2);

			for (double point = from; point <= to; point += step) {
				points.add(point);
			}
		}

		return points;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private Domain	domain;

	public Function()
	{
		this.domain = new Domain();
	}

	public Function calculateOn(Collection<Double> points)
	{
		return calculateOn(new TreeSet<Double>(points));
	}

	public Function calculateOn(Domain domain, double step)
	{
		return calculateOn(Function.discretizeDomain(domain, step));
	}

	public Function calculateOn(double step)
	{
		return calculateOn(domain, step);
	}

	public Function calculateOn(TreeSet<Double> points)
	{
		this.clear();

		for (Double point : points) {
			this.put(point, valueAt(point));
		}

		return this;
	}

	public Function clone()
	{
		Function function = new Function() {

			/**
			 * 
			 */
			private static final long	serialVersionUID	= 8981302993209188725L;

			@Override
			public double valueAt(double x)
			{
				return Function.this.valueAt(x);
			}

		};

		function.domain = Function.this.domain;

		return function;
	}

	public Domain getDomain()
	{
		return domain;
	}

	public void setDomain(Domain domain)
	{
		this.domain = domain;
	}

	public abstract double valueAt(double x);

}
