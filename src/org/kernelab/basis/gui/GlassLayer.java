package org.kernelab.basis.gui;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

/**
 * This class is usually used in JLayeredPane in order to cover a normal content
 * pane.<br />
 * <br />
 * e.g.<br /> {@code JFrame.getLayeredPane().add(GlassLayer, 10); }<br />
 * Do not forget to call setBounds() to make this class shown in the
 * JLayeredPane.<br />
 * <br />
 * You may draw some shape on this layer just add some PaintListeners.
 * 
 * @author Dilly King
 * 
 */
public class GlassLayer extends JPanel
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 6586665333521867046L;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private List<PaintListener>	paintListeners;

	public GlassLayer()
	{
		super();

		this.setOpaque(false);

		this.setPaintListeners(new ArrayList<PaintListener>());
	}

	public List<PaintListener> getPaintListeners()
	{
		return paintListeners;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		for (PaintListener l : paintListeners) {
			l.paint(g);
		}
	}

	protected void setPaintListeners(List<PaintListener> paintListeners)
	{
		this.paintListeners = paintListeners;
	}

}
