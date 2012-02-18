package org.kernelab.basis;

public class Interval<E extends Comparable<E>> implements Comparable<Interval<E>>,
		Copieable<Interval<E>>
{

	public static final <E extends Comparable<E>> Interval<E> Intersection(Interval<E> a,
			Interval<E> b)
	{
		Interval<E> intersection = null;

		Interval<E> lower = null;

		Interval<E> upper = null;

		int compare = a.lower.compareTo(b.lower);

		if (compare < 0) {
			lower = a;
			upper = b;
		} else if (compare > 0) {
			lower = b;
			upper = a;
		} else {
			if (a.lowered) {
				lower = b;
				upper = a;
			} else {
				lower = a;
				upper = b;
			}
		}

		compare = upper.lower.compareTo(lower.upper);

		if (compare == 0) {
			if (upper.lowered && lower.uppered) {
				intersection = new Interval<E>(upper.lower, lower.upper, upper.lowered,
						lower.uppered);
			}
		} else if (compare < 0) {

			compare = lower.upper.compareTo(upper.upper);

			if (compare > 0) {
				lower = upper;
			} else if (compare == 0) {

				if (lower.lowered) {
					lower = upper;
				}
			}

			intersection = new Interval<E>(upper.lower, lower.upper, upper.lowered,
					lower.uppered);

		}

		return intersection;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Interval<Double> i = Interval.newInstance(1.2, 4.3);

		Interval<Double> j = Interval.newInstance(1.3, 4.2);

		Tools.debug(i.intersectionWith(j));
	}

	public static final <E extends Comparable<E>> Interval<E> newInstance(E lower, E upper)
	{
		return new Interval<E>(lower, upper);
	}

	public static final <E extends Comparable<E>> Interval<E> newInstance(E lower,
			E upper, boolean lowered, boolean uppered)
	{
		return new Interval<E>(lower, upper, lowered, uppered);
	}

	private E		lower;

	private E		upper;

	private boolean	lowered;

	private boolean	uppered;

	/**
	 * Create an Interval which, as default, <b>includes</b> lower bound and
	 * <b>excludes</b> upper bound.
	 * 
	 * @param lower
	 *            The lower bound of the Interval.
	 * @param upper
	 *            The upper bound of the Interval.
	 */
	public Interval(E lower, E upper)
	{
		this(lower, upper, true, false);
	}

	public Interval(E lower, E upper, boolean lowered, boolean uppered)
	{
		this.lower = lower;
		this.upper = upper;
		this.lowered = lowered;
		this.uppered = uppered;
	}

	protected Interval(Interval<E> interval)
	{
		this.lower = interval.lower;
		this.lowered = interval.lowered;
		this.upper = interval.upper;
		this.uppered = interval.uppered;
	}

	@Override
	public Interval<E> clone()
	{
		return new Interval<E>(this);
	}

	public int compareTo(Interval<E> o)
	{
		int compare = this.lower.compareTo(o.lower);

		if (compare == 0) {
			compare = this.upper.compareTo(o.upper);
		}

		return compare;
	}

	@SuppressWarnings("unchecked")
	public boolean equals(Object o)
	{
		boolean is = false;

		if (o != null) {
			if (o instanceof Interval<?>) {
				Interval<E> i = (Interval<E>) o;
				if (this.lowered == i.lowered && this.uppered == i.uppered
						&& this.lower.compareTo(i.lower) == 0
						&& this.upper.compareTo(i.upper) == 0)
				{
					is = true;
				}
			}
		}

		return is;
	}

	public E getLower()
	{
		return lower;
	}

	public E getUpper()
	{
		return upper;
	}

	public boolean has(E element)
	{
		boolean has = false;

		int low = this.getLower().compareTo(element);

		int up = this.getUpper().compareTo(element);

		if (this.isLowered()) {
			if (low <= 0)
				has = true;
		} else {
			if (low < 0)
				has = true;
		}

		if (has) {
			has = false;

			if (!this.isUppered()) {
				if (up > 0)
					has = true;
			} else {
				if (up >= 0)
					has = true;
			}
		}

		return has;
	}

	public Interval<E> intersectionWith(Interval<E> interval)
	{
		return Interval.Intersection(this, interval);
	}

	public boolean isLowered()
	{
		return lowered;
	}

	public boolean isUppered()
	{
		return uppered;
	}

	public void setInterval(boolean lowered, boolean uppered)
	{
		this.lowered = lowered;
		this.uppered = uppered;
	}

	public void setInterval(E lower, E upper)
	{
		this.lower = lower;
		this.upper = upper;
	}

	public void setInterval(E lower, E upper, boolean lowered, boolean uppered)
	{
		this.lower = lower;
		this.upper = upper;
		this.lowered = lowered;
		this.uppered = uppered;
	}

	public void setLower(E lower)
	{
		this.lower = lower;
	}

	public void setLowered(boolean lowered)
	{
		this.lowered = lowered;
	}

	public void setUpper(E upper)
	{
		this.upper = upper;
	}

	public void setUppered(boolean uppered)
	{
		this.uppered = uppered;
	}

	@Override
	public String toString()
	{
		String string = "";

		string += this.isLowered() ? "[" : "(";

		string += this.getLower();

		string += ",";

		string += this.getUpper();

		string += this.isUppered() ? "]" : ")";

		return string;
	}

}
