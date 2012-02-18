package org.kernelab.basis.gui.table;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class RowHeaderTablePane<T> extends JScrollPane
{

	private class RowHeaderModel extends AbstractTableModel
	{

		/**
		 * 
		 */
		private static final long	serialVersionUID	= 8916958023040741703L;

		public int getColumnCount()
		{
			return 1;
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			return "";
		}

		public int getRowCount()
		{
			return essentialTable.getTable().getRowCount();
		}

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			Object value = getRowHeader(rowIndex);

			if (value == null) {
				value = String.valueOf(rowIndex + 1);
			}

			return value;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return false;
		}

	}

	private class RowHeaderRenderer extends JTableHeader implements TableCellRenderer
	{

		/**
		 * 
		 */
		private static final long	serialVersionUID	= -3207815271993226519L;

		// public RowHeaderRenderer()
		// {
		// super();
		// }

		public RowHeaderRenderer(TableColumnModel tcm)
		{
			super(tcm);
		}

		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column)
		{
			// try {

			Object header = getRowHeader(row);

			if (header == null) {
				header = String.valueOf(row + 1);
			}

			return new JLabel(header.toString() + " ", SwingConstants.TRAILING);

			// } catch (Exception e) {
			// // return this;
			// return null;
			// }
		}

	}

	public static final Cursor	DEFAULT_CURSOR		= new Cursor(Cursor.DEFAULT_CURSOR);

	public static final Cursor	MOVE_CURSOR			= new Cursor(Cursor.MOVE_CURSOR);

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 6212717417755128801L;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private EssentialTable<T>	essentialTable;

	private Vector<Object>		rowsHeader;

	private RowHeaderModel		rowsHeaderModel;

	private RowHeaderRenderer	rowsHeaderRenderer;

	private JTable				rowsHeaderTable;

	private Point				originalPoint;

	private Dimension			originalSize;

	private Dimension			currentSize;

	private int					minRowsHeaderWidth;

	private int					minRowsHeaderHeight;

	public RowHeaderTablePane(EssentialTable<T> essentialTable)
	{
		super(essentialTable.getTable());

		this.essentialTable = essentialTable;

		this.rowsHeader = new Vector<Object>();

		this.rowsHeaderModel = new RowHeaderModel();

		this.rowsHeaderTable = new JTable(this.rowsHeaderModel);

		this.rowsHeaderRenderer = new RowHeaderRenderer(this.rowsHeaderTable
				.getColumnModel());

		this.rowsHeaderTable.getColumn("").setCellRenderer(this.rowsHeaderRenderer);

		this.rowsHeaderTable.getTableHeader().setVisible(false);

		// this.rowsHeaderTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		this.setRowHeaderView(this.rowsHeaderTable);

		this.currentSize = new Dimension();

		this.minRowsHeaderWidth = 40;

		this.minRowsHeaderHeight = 16;

		this.setRowsHeaderWidth(minRowsHeaderWidth);

		this.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, new JPanel());

		this.essentialTable.getTable().addPropertyChangeListener("rowHeight",
				new PropertyChangeListener() {

					public void propertyChange(PropertyChangeEvent evt)
					{
						rowsHeaderTable.setRowHeight((Integer) evt.getNewValue());
					}

				});

		this.addMouseListener(new MouseAdapter() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void mousePressed(MouseEvent e)
			{
				originalPoint = e.getPoint();
				originalSize = new Dimension(getRowHeader().getSize().width,
						getEssentialTable().getTable().getRowHeight());

				getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER).setCursor(MOVE_CURSOR);
			}

			/**
			 * {@inheritDoc}
			 */
			public void mouseReleased(MouseEvent e)
			{
				getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER)
						.setCursor(DEFAULT_CURSOR);
			}
		});

		this.addMouseMotionListener(new MouseMotionAdapter() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void mouseDragged(MouseEvent e)
			{
				Point point = e.getPoint();

				currentSize.width = originalSize.width + point.x - originalPoint.x;
				currentSize.height = originalSize.height + point.y - originalPoint.y;

				if (currentSize.width < minRowsHeaderWidth) {
					currentSize.width = minRowsHeaderWidth;
				}

				if (currentSize.height < minRowsHeaderHeight) {
					currentSize.height = minRowsHeaderHeight;
				}

				resizeRowsHeader(currentSize);
			}

		});
	}

	public EssentialTable<T> getEssentialTable()
	{
		return essentialTable;
	}

	public int getMinRowsHeaderHeight()
	{
		return minRowsHeaderHeight;
	}

	public int getMinRowsHeaderWidth()
	{
		return minRowsHeaderWidth;
	}

	public AbstractTableModel getModel()
	{
		return this.essentialTable.getModel();
	}

	public Object getRowHeader(int index)
	{
		Object header = null;

		if (index >= 0 && index < rowsHeader.size()) {
			header = rowsHeader.get(index);
		}

		return header;
	}

	public int getRowIndex(Object header)
	{
		int index = -1;

		if (header != null) {
			index = rowsHeader.indexOf(header);
		}

		return index;
	}

	public Vector<Object> getRowsHeader()
	{
		return rowsHeader;
	}

	public JTable getTable()
	{
		return this.essentialTable.getTable();
	}

	public void refreshColumnsHeader()
	{
		this.essentialTable.refreshHeader();
	}

	public void refreshData()
	{
		this.essentialTable.refreshData();
	}

	public void refreshRowsHeader()
	{
		this.rowsHeaderModel.fireTableDataChanged();
	}

	private void resizeRowsHeader(Dimension size)
	{
		this.setRowsHeaderWidth(size.width);
		this.getEssentialTable().getTable().setRowHeight(size.height);
	}

	public void setMinRowsHeaderHeight(int minRowsHeaderHeight)
	{
		this.minRowsHeaderHeight = minRowsHeaderHeight;
	}

	public void setMinRowsHeaderWidth(int minRowsHeaderWidth)
	{
		this.minRowsHeaderWidth = minRowsHeaderWidth;
	}

	public void setRowsHeaderWidth(int width)
	{
		this.getRowHeader().setPreferredSize(new Dimension(width, 0));
	}

}
