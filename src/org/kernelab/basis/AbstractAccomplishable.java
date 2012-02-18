package org.kernelab.basis;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAccomplishable implements Accomplishable
{

	private List<ActionListener>	accomplishedListeners;

	private boolean					accomplished;

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
		if (!isAccomplished()) {

			setAccomplished(true);

			ActionEvent accomplishedEvent = this.getAccomplishedEvent();

			if (accomplishedEvent == null) {
				accomplishedEvent = new ActionEvent(this,
						Accomplishable.ACCOMPLISHED_CODE,
						Accomplishable.ACCOMPLISHED_MARK, Tools.getTimeStamp(), 0);
			}

			for (ActionListener listener : accomplishedListeners) {
				listener.actionPerformed(accomplishedEvent);
			}

		}
	}

	// @Override
	public void addAccomplishedListener(ActionListener listener)
	{
		accomplishedListeners.add(listener);
	}

	// @Override
	public void clearAccomplishedListeners()
	{
		accomplishedListeners = new ArrayList<ActionListener>();
	}

	public abstract ActionEvent getAccomplishedEvent();

	public List<ActionListener> getAccomplishedListeners()
	{
		return accomplishedListeners;
	}

	public boolean isAccomplished()
	{
		return accomplished;
	}

	public void removeAccomplishedListener(ActionListener listener)
	{
		accomplishedListeners.remove(listener);
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

	protected void setAccomplishedListeners(List<ActionListener> listeners)
	{
		accomplishedListeners = listeners;
	}

}
