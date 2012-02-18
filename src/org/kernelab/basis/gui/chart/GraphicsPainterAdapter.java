package org.kernelab.basis.gui.chart;

import java.awt.Color;
import java.awt.Graphics;

public class GraphicsPainterAdapter implements GraphicsChartPainter
{

	public static int		PAINTER_COUNTER	= 0;

	public static Color[]	COLORS			= new Color[] { Color.BLUE, Color.GREEN,
			Color.red, Color.ORANGE, Color.CYAN, Color.PINK, Color.GRAY, Color.MAGENTA };

	public static Color makeColor(int index)
	{
		int length = COLORS.length;

		Color color = COLORS[index % length];

		for (int i = 0; i < index / length; i++) {
			color = color.darker();
		}

		return color;
	}

	protected int	index;

	protected Color	color;

	public GraphicsPainterAdapter()
	{
		index = PAINTER_COUNTER++;
	}

	public GraphicsPainterAdapter(int index)
	{
		this.index = index;
	}

	public GraphicsPainterAdapter(Color color)
	{
		this.color = color;
	}

	public Color getColor()
	{
		return color;
	}

	public void paintLine(Graphics g, int x1, int y1, int x2, int y2)
	{
		if (color != null) {
			g.setColor(color);
		}
		g.drawLine(x1, y1, x2, y2);
	}

	public void paintNode(Graphics g, int x, int y)
	{
		if (color != null) {
			g.setColor(color);
		}
		g.drawLine(x, y, x, y);
	}

	public void setColor(Color color)
	{
		this.color = color;
	}

}
