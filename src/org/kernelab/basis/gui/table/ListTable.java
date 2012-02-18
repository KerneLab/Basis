package org.kernelab.basis.gui.table;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JTable;

import org.kernelab.basis.Tools;
import org.kernelab.basis.gui.VectorAccessible;

public class ListTable<T extends VectorAccessible> implements EssentialTable<List<T>>
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private List<T>				data;

	private JTable				table;

	private ListTableModel<T>	model;

	public ListTable(List<Object> header)
	{
		this.data = new LinkedList<T>();

		this.model = new ListTableModel<T>(header, this.data);

		this.table = new JTable(model);
	}

	public ListTable(String... header)
	{
		this(Tools.vectorOfArray((Object[]) header));
	}

	public List<T> getData()
	{
		return data;
	}

	public ListTableModel<T> getModel()
	{
		return model;
	}

	public List<T> getSelectedData()
	{
		List<Integer> selected = this.getSelectedDataIndex();

		List<T> list = new ArrayList<T>(selected.size());

		for (Integer index : selected) {
			list.add(data.get(index));
		}

		return list;
	}

	public List<Integer> getSelectedDataIndex()
	{
		int[] selected = table.getSelectedRows();

		List<Integer> list = new ArrayList<Integer>(selected.length);

		for (Integer index : selected) {
			list.add(table.convertRowIndexToModel(index));
		}

		return list;
	}

	public JTable getTable()
	{
		return table;
	}

	public void refreshData()
	{
		this.getModel().fireTableDataChanged();
	}

	public void refreshHeader()
	{
		this.getModel().fireTableStructureChanged();
	}

	public void setData(List<T> data)
	{
		this.data = data;
		this.getModel().setData(data);
	}

	public void setHeader(List<Object> header)
	{
		this.getModel().setHeader(header);
	}

	public void setHeader(String... header)
	{
		this.setHeader(Tools.vectorOfArray((Object[]) header));
	}

	protected void setModel(ListTableModel<T> model)
	{
		this.model = model;
		this.table.setModel(this.getModel());
	}
}
