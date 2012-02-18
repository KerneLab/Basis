package org.kernelab.basis.gui.table;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.kernelab.basis.gui.VectorAccessible;

public class ListTableModel<T extends VectorAccessible> extends AbstractTableModel
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -1564011709676416364L;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private Vector<Object>			header;

	private Map<Integer, Integer>	columnMap;

	private List<T>					data;

	private Set<Integer>			notEditableColumns;

	private Set<Integer>			notEditableRows;

	public ListTableModel(List<Object> header, List<T> data)
	{
		this.data = data;

		this.columnMap = new Hashtable<Integer, Integer>();

		this.setHeader(header);

		this.notEditableColumns = new HashSet<Integer>();

		this.notEditableRows = new HashSet<Integer>();

		this.fireTableStructureChanged();
	}

	public int convertColumnIndexToModel(int column)
	{
		return this.getColumnMap().get(column);
	}

	@Override
	public Class<?> getColumnClass(int column)
	{
		Class<?> cls = Object.class;

		Object object = this.getValueAt(0, column);

		if (object != null) {
			cls = object.getClass();
		}

		return cls;
	}

	public int getColumnCount()
	{
		return this.getHeader().size();
	}

	public Map<Integer, Integer> getColumnMap()
	{
		return columnMap;
	}

	@Override
	public String getColumnName(int column)
	{
		return this.getHeader().get(column).toString();
	}

	public List<T> getData()
	{
		return data;
	}

	protected Vector<Object> getHeader()
	{
		return header;
	}

	public Set<Integer> getNotEditableColumns()
	{
		return notEditableColumns;
	}

	public Set<Integer> getNotEditableRows()
	{
		return notEditableRows;
	}

	public int getRowCount()
	{
		return this.getData().size();
	}

	public Object getValueAt(int row, int column)
	{
		Object value = null;

		if (this.hasRowIndex(row) && this.hasColumnIndex(column)) {
			value = this.getData().get(row)
					.vectorAccess(this.convertColumnIndexToModel(column));
		}

		return value;
	}

	public boolean hasColumnIndex(int index)
	{
		return index > -1 && index < this.getColumnCount();
	}

	public boolean hasRowIndex(int index)
	{
		return index > -1 && index < this.getRowCount();
	}

	@Override
	public boolean isCellEditable(int row, int column)
	{
		return !(this.getNotEditableColumns().contains(column) || this
				.getNotEditableRows().contains(row));
	}

	public void setColumnMap(Map<Integer, Integer> map)
	{
		if (map != null) {
			this.columnMap = map;
		}
	}

	protected void setData(List<T> data)
	{
		this.data = data;
	}

	protected void setHeader(List<Object> header)
	{
		this.header = new Vector<Object>(header);

		columnMap.clear();
		for (int i = 0; i < header.size(); i++) {
			columnMap.put(i, i);
		}
	}

	@Override
	public void setValueAt(Object element, int row, int column)
	{
		if (this.hasRowIndex(row) && this.hasColumnIndex(column)) {
			this.getData().get(row)
					.vectorAccess(this.convertColumnIndexToModel(column), element);
			this.fireTableCellUpdated(row, column);
		}
	}

}
