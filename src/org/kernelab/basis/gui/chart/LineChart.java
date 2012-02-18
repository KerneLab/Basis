package org.kernelab.basis.gui.chart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.kernelab.basis.Relation;

public class LineChart extends JPanel
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1052661828503462078L;

	public static final int		AXIS				= 2;

	public static final int		AXIS_X				= 0;

	public static final int		AXIS_Y				= 1;

	public static Color			AXIS_COLOR			= Color.BLACK;

	public static Color			CHART_COLOR			= Color.WHITE;

	public static Color[]		COLORS				= new Color[] { Color.BLUE,
			Color.GREEN, Color.red, Color.ORANGE, Color.CYAN, Color.PINK, Color.GRAY,
			Color.MAGENTA							};

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		JFrame frame = new JFrame();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		LineChart chart = new LineChart(new Insets(10, 20, 20, 10), new Insets(10, 10,
				10, 10));

		chart.setPreferredSize(new Dimension(400, 300));

		frame.add(chart);

		frame.pack();

		frame.setVisible(true);

		TreeMap<Double, Double> data = new TreeMap<Double, Double>();

		data.put(1.0, 3.5);
		data.put(1.5, 6.1);
		data.put(2.0, 2.4);
		data.put(2.5, 3.2);
		data.put(3.0, 2.9);

		chart.getData().put(data, Color.BLUE);

	}

	private Map<TreeMap<Double, Double>, Color>	data;

	private double[]							min;

	private double[]							max;

	private Insets								margin;

	private Insets								padding;

	public LineChart(Insets margin, Insets padding)
	{
		super();

		this.data = new Hashtable<TreeMap<Double, Double>, Color>();
		this.min = new double[2];
		this.max = new double[2];

		this.margin = margin;
		this.padding = padding;

		this.setBackground(LineChart.CHART_COLOR);
	}

	/**
	 * A virtual point is under the coordinate system whose original point is
	 * left-bottom corner.
	 * 
	 * A real point is under the coordinate system whose original point is
	 * left-top corner as normal graphic component do.
	 * 
	 * A virtual point would become a real point after called this method
	 * whatever it was parameter or the return value. Vice versa.
	 * 
	 * @param point
	 *            A point.
	 * @return The point.
	 */
	public Point convertPointCoordinate(Point point)
	{
		point.y = this.getHeight() - point.y;
		return point;
	}

	public void drawData(Graphics g)
	{
		this.ensureAxisMinAndMaxValues();

		Relation<Double, Double> scale = new Relation<Double, Double>();

		for (int axis = 0; axis < LineChart.AXIS; axis++) {

			double ratio = this.getLength(axis)
					/ (this.getAxisMaxValue(axis) - this.getAxisMinValue(axis));

			switch (axis)
			{
				case LineChart.AXIS_X:
					scale.setKey(ratio);
					break;
				case LineChart.AXIS_Y:
					scale.setValue(ratio);
					break;
			}
		}

		for (Entry<TreeMap<Double, Double>, Color> data : this.data.entrySet()) {

			g.setColor(data.getValue());

			Point last = null;
			for (Entry<Double, Double> entry : data.getKey().entrySet()) {

				Point current = this.getRealPointOfEntry(entry, scale);

				if (last != null) {
					g.drawLine(last.x, last.y, current.x, current.y);
				}

				last = current;
			}
		}
	}

	private void ensureAxisMinAndMaxValues()
	{
		for (int axis = 0; axis < LineChart.AXIS; axis++) {
			this.setAxisMinValue(axis, Double.MAX_VALUE);
			this.setAxisMaxValue(axis, -Double.MAX_VALUE);
		}

		double x, y;
		for (TreeMap<Double, Double> data : this.data.keySet()) {
			for (Entry<Double, Double> entry : data.entrySet()) {
				x = entry.getKey();
				y = entry.getValue();

				if (x < this.getAxisMinValue(LineChart.AXIS_X)) {
					this.setAxisMinValue(LineChart.AXIS_X, x);
				}

				if (x > this.getAxisMaxValue(LineChart.AXIS_X)) {
					this.setAxisMaxValue(LineChart.AXIS_X, x);
				}

				if (y < this.getAxisMinValue(LineChart.AXIS_Y)) {
					this.setAxisMinValue(LineChart.AXIS_Y, y);
				}

				if (y > this.getAxisMaxValue(LineChart.AXIS_Y)) {
					this.setAxisMaxValue(LineChart.AXIS_Y, y);
				}
			}
		}
	}

	public int getAxisMarginHead(int axis)
	{
		int head = 0;

		switch (axis)
		{
			case LineChart.AXIS_X:
				head = margin.left;
				break;

			case LineChart.AXIS_Y:
				head = margin.bottom;
				break;
		}

		return head;
	}

	public int getAxisMarginTail(int axis)
	{
		int tail = 0;

		switch (axis)
		{
			case LineChart.AXIS_X:
				tail = margin.right;
				break;

			case LineChart.AXIS_Y:
				tail = margin.top;
				break;
		}

		return tail;
	}

	public double getAxisMaxValue(int axis)
	{
		return max[axis];
	}

	public double getAxisMinValue(int axis)
	{
		return min[axis];
	}

	public int getAxisPaddingHead(int axis)
	{
		int head = 0;

		switch (axis)
		{
			case LineChart.AXIS_X:
				head = padding.left;
				break;

			case LineChart.AXIS_Y:
				head = padding.bottom;
				break;
		}

		return head;
	}

	public int getAxisPaddingTail(int axis)
	{
		int tail = 0;

		switch (axis)
		{
			case LineChart.AXIS_X:
				tail = padding.right;
				break;

			case LineChart.AXIS_Y:
				tail = padding.top;
				break;
		}

		return tail;
	}

	public Map<TreeMap<Double, Double>, Color> getData()
	{
		return data;
	}

	public int getLength(int axis)
	{
		int length = 0;

		switch (axis)
		{
			case LineChart.AXIS_X:
				length = this.getWidth();
				break;

			case LineChart.AXIS_Y:
				length = this.getHeight();
				break;
		}

		length -= this.getAxisMarginHead(axis) + this.getAxisMarginTail(axis)
				+ this.getAxisPaddingHead(axis) + this.getAxisPaddingTail(axis);

		return length;
	}

	public Insets getMargin()
	{
		return margin;
	}

	public Insets getPadding()
	{
		return padding;
	}

	public Point getRealPointOfEntry(Entry<Double, Double> entry,
			Relation<Double, Double> scale)
	{
		double x = entry.getKey();
		double y = entry.getValue();

		x = scale.getKey() * (x - this.getAxisMinValue(LineChart.AXIS_X))
				+ this.getAxisPaddingHead(LineChart.AXIS_X)
				+ this.getAxisMarginHead(LineChart.AXIS_X);

		y = scale.getValue() * (y - this.getAxisMinValue(LineChart.AXIS_Y))
				+ this.getAxisPaddingHead(LineChart.AXIS_Y)
				+ this.getAxisMarginHead(LineChart.AXIS_Y);

		return this.convertPointCoordinate(new Point((int) x, (int) y));
	}

	@Override
	public void paint(Graphics g)
	{
		super.paint(g);
		this.drawData(g);
	}

	protected void paintAxis(Graphics g)
	{
		g.setColor(LineChart.AXIS_COLOR);

		Point head = new Point(margin.left, margin.bottom);

		this.convertPointCoordinate(head);

		Point tail = new Point();

		int width = this.getWidth();

		int height = this.getHeight();

		for (int d = 0; d < LineChart.AXIS; d++) {

			switch (d)
			{
				case LineChart.AXIS_X:
					tail.x = width - this.getAxisMarginTail(d);
					tail.y = this.getAxisMarginHead(d + 1);
					break;

				case LineChart.AXIS_Y:
					tail.x = this.getAxisMarginHead(d - 1);
					tail.y = height - this.getAxisMarginTail(d);
					break;
			}

			this.convertPointCoordinate(tail);

			g.drawLine(head.x, head.y, tail.x, tail.y);
		}
	}

	/**
	 * @inheritdoc
	 */
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		this.paintAxis(g);
	}

	public void setAxisMaxValue(int axis, double value)
	{
		max[axis] = value;
	}

	public void setAxisMinValue(int axis, double value)
	{
		min[axis] = value;
	}

	public void setMargin(Insets margin)
	{
		this.margin = margin;
	}

	public void setPadding(Insets padding)
	{
		this.padding = padding;
	}

}
