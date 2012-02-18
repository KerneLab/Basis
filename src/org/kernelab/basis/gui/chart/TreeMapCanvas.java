package org.kernelab.basis.gui.chart;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.JPanel;

import org.kernelab.basis.Function;
import org.kernelab.basis.Interval;
import org.kernelab.basis.Variable;

public class TreeMapCanvas extends JPanel
{

	/**
	 * 
	 */
	private static final long	serialVersionUID		= 1654370123436938604L;

	public static final int		AXIS					= 2;

	public static final int		AXIS_X					= 0;

	public static final int		AXIS_Y					= 1;

	public static Color			AXIS_COLOR				= Color.BLACK;

	public static Color			CANVAS_COLOR			= Color.WHITE;

	public static final Insets	DEFAULT_MARGIN			= new Insets(5, 5, 5, 5);

	public static final Insets	DEFAULT_PADDING			= new Insets(5, 5, 5, 5);

	public static final Cursor	DEFAULT_CURSOR			= new Cursor(
																Cursor.CROSSHAIR_CURSOR);

	public static final Cursor	MOVE_CURSOR				= new Cursor(Cursor.MOVE_CURSOR);

	public static int			COORDINATE_FONT_WIDTH	= 7;

	public static int			COORDINATE_FONT_HEIGHT	= 8;

	public static Font			COORDINATE_FONT			= new Font(Font.MONOSPACED,
																Font.PLAIN, 12);

	public static final int getStringWidth(String string)
	{
		int width = string.length() * COORDINATE_FONT_WIDTH;

		return width;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private Map<TreeMap<Double, Double>, GraphicsChartPainter>	data;

	private Map<Function, GraphicsChartPainter>					function;

	private Map<TreeMap<Double, Double>, GraphicsChartPainter>	drawing;

	private double[]											minBound;

	private double[]											maxBound;

	private double[]											min;

	private double[]											max;

	private double[]											scale;

	private Insets												margin;

	private Insets												padding;

	private int													zoomStep;

	private boolean												autoRescale;

	private boolean												needEnsure;

	private boolean												needPrepare;

	private Point												dragFrom;

	private double[]											coordinate;

	public TreeMapCanvas()
	{
		this(TreeMapCanvas.DEFAULT_MARGIN, TreeMapCanvas.DEFAULT_PADDING);
	}

	public TreeMapCanvas(Insets margin, Insets padding)
	{
		super();

		this.data = new Hashtable<TreeMap<Double, Double>, GraphicsChartPainter>();
		this.function = new Hashtable<Function, GraphicsChartPainter>();

		this.drawing = new Hashtable<TreeMap<Double, Double>, GraphicsChartPainter>();

		this.min = new double[TreeMapCanvas.AXIS];
		this.max = new double[TreeMapCanvas.AXIS];

		this.minBound = new double[TreeMapCanvas.AXIS];
		this.maxBound = new double[TreeMapCanvas.AXIS];

		this.scale = new double[TreeMapCanvas.AXIS];

		this.margin = margin;
		this.padding = padding;

		this.zoomStep = 10;

		this.autoRescale = true;
		this.needEnsure = true;
		this.needPrepare = true;

		this.config();
	}

	private void config()
	{
		this.setBackground(TreeMapCanvas.CANVAS_COLOR);

		this.setCursor(TreeMapCanvas.DEFAULT_CURSOR);

		this.addMouseListener(new MouseAdapter() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2) {
					ensureAxisMinAndMaxBound();
					needPrepare();
					repaint();
				}

			}

			/**
			 * {@inheritDoc}
			 */
			public void mouseExited(MouseEvent e)
			{
				coordinate = null;
				repaint();
			}

			/**
			 * {@inheritDoc}
			 */
			public void mousePressed(MouseEvent e)
			{
				dragFrom = e.getPoint();
				setCursor(TreeMapCanvas.MOVE_CURSOR);
			}

			/**
			 * {@inheritDoc}
			 */
			public void mouseReleased(MouseEvent e)
			{
				setCursor(TreeMapCanvas.DEFAULT_CURSOR);
				refreshBoundWithValue();
			}

		});

		this.addMouseMotionListener(new MouseMotionListener() {

			public void mouseDragged(MouseEvent e)
			{
				moveCanvas(dragFrom, e.getPoint());
			}

			public void mouseMoved(MouseEvent e)
			{
				showCoordinate(e.getPoint());
			}

		});

		this.addMouseWheelListener(new MouseWheelListener() {

			public void mouseWheelMoved(MouseWheelEvent e)
			{
				zoomCanvas(e.getWheelRotation());
				refreshBoundWithValue();
			}

		});
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

	private void drawCoordinate(Graphics g)
	{
		if (coordinate != null) {

			g.setColor(TreeMapCanvas.AXIS_COLOR);
			g.setFont(TreeMapCanvas.COORDINATE_FONT);

			int width = this.getWidth();
			int border = 5;
			int gap = 2;

			String string;
			for (int i = 0; i < coordinate.length; i++) {
				string = Variable.numberFormatString(coordinate[i], "0.00");
				g.drawString(string, width - string.length()
						* TreeMapCanvas.COORDINATE_FONT_WIDTH - border, border
						+ TreeMapCanvas.COORDINATE_FONT_HEIGHT + i
						* (gap + TreeMapCanvas.COORDINATE_FONT_HEIGHT));
			}

		}
	}

	private void drawData(Graphics g)
	{
		for (Entry<TreeMap<Double, Double>, GraphicsChartPainter> entry : this
				.getDrawing().entrySet())
		{
			this.drawData(g, entry.getKey(), entry.getValue());
		}
	}

	private void drawData(Graphics g, TreeMap<Double, Double> map,
			GraphicsChartPainter painter)
	{
		Point last = null;
		for (Entry<Double, Double> data : map.entrySet()) {

			double time = data.getKey();
			if (time >= this.getAxisMinValue(TreeMapCanvas.AXIS_X)
					&& time <= this.getAxisMaxValue(TreeMapCanvas.AXIS_X))
			{
				Point current = this.getRealPointOfEntry(data, scale);

				painter.paintNode(g, current.x, current.y);

				if (last != null) {
					painter.paintLine(g, last.x, last.y, current.x, current.y);
				}

				last = current;
			}
		}
	}

	private void ensureAxisMinAndMaxBound()
	{
		for (int axis = 0; axis < TreeMapCanvas.AXIS; axis++) {
			minBound[axis] = Double.MAX_VALUE;
			maxBound[axis] = -Double.MAX_VALUE;
		}

		double x, y;
		for (TreeMap<Double, Double> data : this.getDrawing().keySet()) {
			for (Entry<Double, Double> entry : data.entrySet()) {

				x = entry.getKey();
				y = entry.getValue();

				if (x < minBound[TreeMapCanvas.AXIS_X]) {
					minBound[TreeMapCanvas.AXIS_X] = x;
				}

				if (x > maxBound[TreeMapCanvas.AXIS_X]) {
					maxBound[TreeMapCanvas.AXIS_X] = x;
				}

				if (y < minBound[TreeMapCanvas.AXIS_Y]) {
					minBound[TreeMapCanvas.AXIS_Y] = y;
				}

				if (y > maxBound[TreeMapCanvas.AXIS_Y]) {
					maxBound[TreeMapCanvas.AXIS_Y] = y;
				}
			}
		}

		for (int axis = 0; axis < TreeMapCanvas.AXIS; axis++) {
			min[axis] = minBound[axis];
			max[axis] = maxBound[axis];
		}

		this.prepareScale();
	}

	public double getAxisBoundInterval(int axis)
	{
		return maxBound[axis] - minBound[axis];
	}

	public double getAxisMaxBound(int axis)
	{
		return maxBound[axis];
	}

	public double getAxisMaxValue(int axis)
	{
		return max[axis];
	}

	public double getAxisMinBound(int axis)
	{
		return minBound[axis];
	}

	public double getAxisMinValue(int axis)
	{
		return min[axis];
	}

	public double getAxisValueInterval(int axis)
	{
		return max[axis] - min[axis];
	}

	public Map<TreeMap<Double, Double>, GraphicsChartPainter> getData()
	{
		return data;
	}

	public Map<TreeMap<Double, Double>, GraphicsChartPainter> getDrawing()
	{
		return drawing;
	}

	public Map<Function, GraphicsChartPainter> getFunction()
	{
		return function;
	}

	public int getLength(int axis)
	{
		int length = 0;

		switch (axis)
		{
			case TreeMapCanvas.AXIS_X:
				length = this.getWidth();
				break;

			case TreeMapCanvas.AXIS_Y:
				length = this.getHeight();
				break;
		}

		length -= this.getMarginPaddingHead(axis) + this.getMarginPaddingTail(axis);

		return length;
	}

	public Insets getMargin()
	{
		return margin;
	}

	public int getMarginHead(int axis)
	{
		int head = 0;

		switch (axis)
		{
			case TreeMapCanvas.AXIS_X:
				head = margin.left;
				break;

			case TreeMapCanvas.AXIS_Y:
				head = margin.bottom;
				break;
		}

		return head;
	}

	public int getMarginPaddingHead(int axis)
	{
		return this.getMarginHead(axis) + this.getPaddingHead(axis);
	}

	public int getMarginPaddingTail(int axis)
	{
		return this.getMarginTail(axis) + this.getPaddingTail(axis);
	}

	public int getMarginTail(int axis)
	{
		int tail = 0;

		switch (axis)
		{
			case TreeMapCanvas.AXIS_X:
				tail = margin.right;
				break;

			case TreeMapCanvas.AXIS_Y:
				tail = margin.top;
				break;
		}

		return tail;
	}

	public Insets getPadding()
	{
		return padding;
	}

	public int getPaddingHead(int axis)
	{
		int head = 0;

		switch (axis)
		{
			case TreeMapCanvas.AXIS_X:
				head = padding.left;
				break;

			case TreeMapCanvas.AXIS_Y:
				head = padding.bottom;
				break;
		}

		return head;
	}

	public int getPaddingTail(int axis)
	{
		int tail = 0;

		switch (axis)
		{
			case TreeMapCanvas.AXIS_X:
				tail = padding.right;
				break;

			case TreeMapCanvas.AXIS_Y:
				tail = padding.top;
				break;
		}

		return tail;
	}

	public Point getRealPointOfEntry(Entry<Double, Double> entry, double[] scale)
	{
		double x = entry.getKey();
		double y = entry.getValue();

		x = scale[TreeMapCanvas.AXIS_X]
				* (x - this.getAxisMinValue(TreeMapCanvas.AXIS_X))
				+ this.getMarginPaddingHead(TreeMapCanvas.AXIS_X);

		y = scale[TreeMapCanvas.AXIS_Y]
				* (y - this.getAxisMinValue(TreeMapCanvas.AXIS_Y))
				+ this.getMarginPaddingHead(TreeMapCanvas.AXIS_Y);

		return this.convertPointCoordinate(new Point((int) x, (int) y));
	}

	/**
	 * Get the scale of x and y axis.<br>
	 * The scale is defined as pixel/axis.
	 * 
	 * @return The Relation which contains the scale of x and y axis.
	 */
	public double[] getScale()
	{
		return scale;
	}

	public double getScale(int axis)
	{
		return scale[axis];
	}

	public int getZoomStep()
	{
		return zoomStep;
	}

	public boolean isAutoRescale()
	{
		return autoRescale;
	}

	public boolean isNeedEnsure()
	{
		return needEnsure;
	}

	public boolean isNeedPrepare()
	{
		return needPrepare;
	}

	private void moveCanvas(Point from, Point to)
	{
		this.convertPointCoordinate(from);
		this.convertPointCoordinate(to);

		Point direction = new Point(to.x - from.x, to.y - from.y);
		this.convertPointCoordinate(from);

		double length = 0.0;

		for (int axis = 0; axis < TreeMapCanvas.AXIS; axis++) {

			switch (axis)
			{
				case TreeMapCanvas.AXIS_X:
					length = direction.x / this.getScale(axis);
					break;

				case TreeMapCanvas.AXIS_Y:
					length = direction.y / this.getScale(axis);
					break;
			}

			this.setAxisMaxValue(axis, this.getAxisMaxBound(axis) - length);

			this.setAxisMinValue(axis, this.getAxisMinBound(axis) - length);
		}

		this.needPrepare();
		this.repaint();
	}

	public void needEnsure()
	{
		needEnsure = true;
	}

	public void needPrepare()
	{
		needPrepare = true;
	}

	protected void paintAxis(Graphics g)
	{
		g.setColor(TreeMapCanvas.AXIS_COLOR);

		Point head = new Point(margin.left, margin.bottom);

		this.convertPointCoordinate(head);

		Point tail = new Point();

		int width = this.getWidth();

		int height = this.getHeight();

		for (int d = 0; d < TreeMapCanvas.AXIS; d++) {

			switch (d)
			{
				case TreeMapCanvas.AXIS_X:
					tail.x = width - this.getMarginTail(d);
					tail.y = this.getMarginHead(d + 1);
					break;

				case TreeMapCanvas.AXIS_Y:
					tail.x = this.getMarginHead(d - 1);
					tail.y = height - this.getMarginTail(d);
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

		if (this.isNeedPrepare()) {
			this.prepare();
		}

		if (this.isAutoRescale()) {
			this.prepareScale();
		}

		this.drawData(g);

		this.drawCoordinate(g);
	}

	private void prepare()
	{
		this.prepareDrawing();
		this.prepareData();
		this.prepareBoundValue();
		this.prepareFunction();

		needPrepare = false;
	}

	private void prepareBoundValue()
	{
		if (needEnsure) {
			this.ensureAxisMinAndMaxBound();
			needEnsure = false;
		}
	}

	private void prepareData()
	{
		this.getDrawing().putAll(this.getData());
	}

	private void prepareDrawing()
	{
		this.getDrawing().clear();
	}

	private void prepareFunction()
	{
		Interval<Double> interval = Interval.newInstance(this
				.getAxisMinValue(TreeMapCanvas.AXIS_X), this
				.getAxisMaxValue(TreeMapCanvas.AXIS_X), true, true);

		double step = Math.min(1.0 / this.getScale(TreeMapCanvas.AXIS_X), 1.0 / this
				.getScale(TreeMapCanvas.AXIS_Y));

		if (Double.isInfinite(step)) {
			step = Function.DEFAULT_STEP;
		}

		for (Entry<Function, GraphicsChartPainter> entry : this.function.entrySet()) {

			Function function = entry.getKey();

			try {
				this.getDrawing().put(
						function.calculateOn(function.getDomain().getIntersectionWith(
								interval), step), entry.getValue());
			} catch (NullPointerException e) {

			}
		}
	}

	private void prepareScale()
	{
		for (int axis = 0; axis < TreeMapCanvas.AXIS; axis++) {
			scale[axis] = this.getLength(axis) / this.getAxisValueInterval(axis);
		}
	}

	private void refreshBoundWithValue()
	{
		for (int axis = 0; axis < TreeMapCanvas.AXIS; axis++) {
			minBound[axis] = min[axis];
			maxBound[axis] = max[axis];
		}
	}

	public void setAutoRescale(boolean autoResize)
	{
		this.autoRescale = autoResize;
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

	public void setNeedEnsure(boolean needEnsure)
	{
		this.needEnsure = needEnsure;
	}

	public void setNeedPrepare(boolean needPrepare)
	{
		this.needPrepare = needPrepare;
	}

	public void setPadding(Insets padding)
	{
		this.padding = padding;
	}

	public void setZoomStep(int zoomStep)
	{
		this.zoomStep = zoomStep;
	}

	private void showCoordinate(Point point)
	{
		this.convertPointCoordinate(point);

		double[] values = new double[TreeMapCanvas.AXIS];

		for (int axis = 0; axis < TreeMapCanvas.AXIS; axis++) {

			switch (axis)
			{
				case TreeMapCanvas.AXIS_X:
					point.x -= this.getMarginPaddingHead(axis);
					values[axis] = point.x / this.getScale(axis)
							+ this.getAxisMinValue(axis);
					break;

				case TreeMapCanvas.AXIS_Y:
					point.y -= this.getMarginPaddingHead(axis);
					values[axis] = point.y / this.getScale(axis)
							+ this.getAxisMinValue(axis);
					break;
			}

		}

		coordinate = new double[] { values[0], values[1] };

		this.repaint();
	}

	private void zoomCanvas(int direct)
	{

		double[] max = new double[TreeMapCanvas.AXIS];
		double[] min = new double[TreeMapCanvas.AXIS];

		for (int axis = 0; axis < TreeMapCanvas.AXIS; axis++) {
			max[axis] = this.getAxisMaxValue(axis);
			min[axis] = this.getAxisMinValue(axis);
		}

		boolean proper = true;

		double ratio = this.getAxisValueInterval(TreeMapCanvas.AXIS_Y)
				/ this.getAxisValueInterval(TreeMapCanvas.AXIS_X);

		double delta = 0.0;

		for (int axis = 0; axis < TreeMapCanvas.AXIS && proper; axis++) {

			switch (axis)
			{
				case TreeMapCanvas.AXIS_X:
					delta = direct * zoomStep / this.getScale(0);

					max[axis] -= delta;
					min[axis] += delta;

					break;

				case TreeMapCanvas.AXIS_Y:

					delta *= ratio;

					max[axis] -= delta;
					min[axis] += delta;
					break;
			}

			if (min[axis] >= max[axis]) {
				proper = false;
			}
		}

		if (proper) {
			for (int axis = 0; axis < TreeMapCanvas.AXIS; axis++) {
				this.setAxisMaxValue(axis, max[axis]);
				this.setAxisMinValue(axis, min[axis]);
			}
		}

		this.needPrepare();
		this.prepareScale();

		this.repaint();
	}
}
