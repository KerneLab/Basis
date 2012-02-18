package org.kernelab.basis.gui.table;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 * Essential Table interface for the decorate class RowHeaderTablePane.
 * 
 * @author Dilly King
 * 
 */

public interface EssentialTable<T>
{

	public T getData();

	public AbstractTableModel getModel();

	public T getSelectedData();

	public JTable getTable();

	public void refreshData();

	public void refreshHeader();

}
