package org.kernelab.basis;

import java.awt.event.ActionListener;
import java.util.List;

/**
 * The Additional interface for Runnable objects.<br />
 * In this interface, the call-back methods are available in a List.<br />
 * When the Runnable objects accomplished its task, these call-back method would
 * be called via {@code accomplished()}.
 * 
 * @author Dilly King
 * 
 */
public interface Accomplishable
{
	public static final int		ACCOMPLISHED_CODE	= 0;

	public static final String	ACCOMPLISHED_MARK	= "ACCOMPLISHED";

	/**
	 * This method should be called at the end of "run" in Runnable interface.
	 */
	public void accomplished();

	// public void addAccomplishedListener(ActionListener listener);

	// public void clearAccomplishedListeners();

	public List<ActionListener> getAccomplishedListeners();

	public boolean isAccomplished();

	// public void removeAccomplishedListener(ActionListener listener);
}
