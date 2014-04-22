package org.kernelab.basis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractAccomplishable<E> implements Accomplishable<E>
{
	private Collection<AccomplishListener<E>>	accomplishListeners;

	private boolean								accomplished;

	public AbstractAccomplishable()
	{
		setAccomplished(false);
		clearAccomplishedListeners();
	}

	/**
	 * To make the object accomplished.<br />
	 * It is strongly recommended that calling this method as the last statement
	 * in run() function in a Runnable object.
	 * 
	 * @see AbstractAccomplishable#resetAccomplishStatus()
	 */
	public void accomplished()
	{
		if (!isAccomplished())
		{
			setAccomplished(true);

			if (accomplishListeners != null)
			{
				for (AccomplishListener<E> listener : accomplishListeners)
				{
					listener.accomplish(this.getAccomplishableSubject());
				}
			}
		}
	}

	public void addAccomplishedListener(AccomplishListener<E> listener)
	{
		accomplishListeners.add(listener);
	}

	public void clearAccomplishedListeners()
	{
		if (accomplishListeners == null)
		{
			accomplishListeners = new ArrayList<AccomplishListener<E>>();
		}
		else
		{
			accomplishListeners.clear();
		}
	}

	protected abstract E getAccomplishableSubject();

	public Collection<AccomplishListener<E>> getAccomplishListeners()
	{
		return accomplishListeners;
	}

	public boolean isAccomplished()
	{
		return accomplished;
	}

	public void removeAccomplishedListener(AccomplishListener<E> listener)
	{
		accomplishListeners.remove(listener);
	}

	/**
	 * To reset the accomplish status.<br />
	 * It is strongly recommended that calling this method as the first
	 * statement in run() function in a Runnable object.
	 * 
	 * @see AbstractAccomplishable#accomplished()
	 */
	public void resetAccomplishStatus()
	{
		setAccomplished(false);
	}

	protected void setAccomplished(boolean accomplished)
	{
		this.accomplished = accomplished;
	}

	protected void setAccomplishedListeners(List<AccomplishListener<E>> listeners)
	{
		accomplishListeners = listeners;
	}
}
