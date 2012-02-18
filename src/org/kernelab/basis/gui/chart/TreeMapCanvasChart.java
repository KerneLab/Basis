package org.kernelab.basis.gui.chart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.kernelab.basis.Function;
import org.kernelab.basis.Interval;
import org.kernelab.basis.Tools;
import org.kernelab.basis.Variable;

public class TreeMapCanvasChart extends JPanel
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -2473596963456852205L;

	public static final String formatNumberString(Double number)
	{
		return Variable.numberFormatString(number, "0.0#");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		JFrame frame = new JFrame();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		TreeMapCanvasChart chart = new TreeMapCanvasChart();

		chart.setPreferredSize(new Dimension(400, 300));

		frame.add(chart);

		frame.pack();

		frame.setVisible(true);

		TreeMap<Double, Double> data = new TreeMap<Double, Double>();

		// data.put(0.1, 1.377);
		// data.put(0.2, 1.480);
		// data.put(0.3, 1.693);
		// data.put(0.4, 1.909);
		// data.put(0.5, 2.255);
		// data.put(0.6, 2.393);
		// data.put(0.7, 2.771);
		// data.put(0.8, 3.415);
		data.put(-3.0, 1.0);
		data.put(3.0, -2.0);

		Function f = new Function() {

			/**
			 * 
			 */
			private static final long	serialVersionUID	= -3517314488389301717L;

			@Override
			public double valueAt(double x)
			{
				return Math.sin(x);
			}

		};

		f.getDomain().add(Interval.newInstance(-3.0, 3.0));

		chart.getData().put(data, new GraphicsPainterAdapter(Color.RED) {
			@Override
			public void paintLine(Graphics g, int x1, int y1, int x2, int y2)
			{

			}

			@Override
			public void paintNode(Graphics g, int x, int y)
			{
				if (color != null) {
					g.setColor(color);
				}
				g.drawLine(x - 2, y, x + 2, y);
				g.drawLine(x, y - 2, x, y + 2);
			}
		});
		chart.getFunction().put(f, new GraphicsPainterAdapter(Color.BLUE) {
			@Override
			public void paintLine(Graphics g, int x1, int y1, int x2, int y2)
			{
				 if (color != null) {
				 g.setColor(color);
				 }
				 g.drawLine(x1, y1, x2, y2);
			}

			@Override
			public void paintNode(Graphics g, int x, int y)
			{
				if (color != null) {
					g.setColor(color);
				}
				g.drawLine(x, y, x, y);
			}
		});

		chart.viewData();
	}

	public static final TreeMap<Double, Double> makeTreeMap(List<Double> time,
			List<Double> value)
	{
		TreeMap<Double, Double> map = new TreeMap<Double, Double>();

		int size = Math.min(time.size(), value.size());

		for (int i = 0; i < size; i++) {
			map.put(time.get(i), value.get(i));
		}

		return map;
	}

	private TreeMapCanvas										canvas;

	private Map<TreeMap<Double, Double>, GraphicsChartPainter>	data;

	private Map<Function, GraphicsChartPainter>					function;

	private List<Double>										timeAxis;

	private JSlider												timeFromChooser;

	private JSlider												timeToChooser;

	private Hashtable<Integer, JLabel>							timeFromTable;

	private Hashtable<Integer, JLabel>							timeToTable;

	private JLabel												timeFromLabel;

	private JLabel												timeFromBeginLabel;

	private JLabel												timeFromEndLabel;

	private JLabel												timeToBeginLabel;

	private JLabel												timeToEndLabel;

	private JLabel												timeToLabel;

	private Interval<Double>									selectedTimeAxis;

	private JSlider												timeZoneChooser;

	private JPanel												timeAxisPanel;

	public TreeMapCanvasChart()
	{
		super();

		this.canvas = new TreeMapCanvas();

		this.data = new Hashtable<TreeMap<Double, Double>, GraphicsChartPainter>();
		this.function = new Hashtable<Function, GraphicsChartPainter>();

		this.timeAxis = new ArrayList<Double>();

		this.timeFromChooser = new JSlider();
		this.timeToChooser = new JSlider();

		this.timeFromTable = new Hashtable<Integer, JLabel>();
		this.timeToTable = new Hashtable<Integer, JLabel>();

		this.timeFromLabel = new JLabel("", JLabel.CENTER);
		this.timeToLabel = new JLabel("", JLabel.CENTER);

		this.timeFromBeginLabel = new JLabel("", JLabel.CENTER);
		this.timeFromEndLabel = new JLabel("", JLabel.CENTER);

		this.timeToBeginLabel = new JLabel("", JLabel.CENTER);
		this.timeToEndLabel = new JLabel("", JLabel.CENTER);

		this.selectedTimeAxis = new Interval<Double>(0.0, 0.0, true, true);

		this.timeZoneChooser = new JSlider();

		this.timeAxisPanel = new JPanel();

		this.config();

		this.arrange();
	}

	private void arrange()
	{
		this.arrangeTimeAxisPanel();

		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = Tools.makePreferredGridBagConstraints();

		this.add(this.getCanvas(), gbc);

		gbc.weighty = 0.0;
		gbc.gridy++;
		gbc.insets = new Insets(0, 4, 0, 0);
		this.add(this.getTimeAxisPanel(), gbc);
	}

	private void arrangeTimeAxisPanel()
	{
		this.getTimeAxisPanel().setLayout(new GridBagLayout());

		GridBagConstraints gbc = Tools.makePreferredGridBagConstraints();

		gbc.insets.right = 3;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		this.getTimeAxisPanel().add(new JLabel("起始"), gbc);

		gbc.gridx++;
		gbc.insets.right = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		this.getTimeAxisPanel().add(this.getTimeFromChooser(), gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.insets.right = 3;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		this.getTimeAxisPanel().add(new JLabel("终止"), gbc);

		gbc.gridx++;
		gbc.insets.right = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		this.getTimeAxisPanel().add(this.getTimeToChooser(), gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.insets.right = 3;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		this.getTimeAxisPanel().add(new JLabel("区段"), gbc);

		gbc.gridx++;
		gbc.insets.right = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		this.getTimeAxisPanel().add(this.getTimeZoneChooser(), gbc);
	}

	private void config()
	{
		this.getTimeFromChooser().setMinimum(0);
		this.getTimeToChooser().setMinimum(0);
		this.getTimeZoneChooser().setMinimum(0);

		this.getTimeFromChooser().setMinorTickSpacing(1);
		this.getTimeToChooser().setMinorTickSpacing(1);
		this.getTimeZoneChooser().setMinorTickSpacing(1);

		this.getTimeFromChooser().setSnapToTicks(true);
		this.getTimeToChooser().setSnapToTicks(true);
		this.getTimeZoneChooser().setSnapToTicks(true);

		this.getTimeFromChooser().addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e)
			{
				paintTimeAxisChooserTick(timeFromChooser, timeFromTable, timeFromLabel,
						timeFromBeginLabel, timeFromEndLabel);
				setSelectedTimeAxis(false);
			}

		});

		this.getTimeToChooser().addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e)
			{
				paintTimeAxisChooserTick(timeToChooser, timeToTable, timeToLabel,
						timeToBeginLabel, timeToEndLabel);
				setSelectedTimeAxis(false);
			}

		});

		this.getTimeZoneChooser().addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e)
			{
				setSelectedTimeAxis(true);
			}

		});
	}

	public TreeMapCanvas getCanvas()
	{
		return canvas;
	}

	public Map<TreeMap<Double, Double>, GraphicsChartPainter> getData()
	{
		return data;
	}

	public Map<Function, GraphicsChartPainter> getFunction()
	{
		return function;
	}

	public Interval<Double> getSelectedTimeAxis()
	{
		return selectedTimeAxis;
	}

	public List<Double> getTimeAxis()
	{
		return timeAxis;
	}

	public JPanel getTimeAxisPanel()
	{
		return timeAxisPanel;
	}

	public JLabel getTimeFromBeginLabel()
	{
		return timeFromBeginLabel;
	}

	public JSlider getTimeFromChooser()
	{
		return timeFromChooser;
	}

	public JLabel getTimeFromEndLabel()
	{
		return timeFromEndLabel;
	}

	public JLabel getTimeFromLabel()
	{
		return timeFromLabel;
	}

	public Hashtable<Integer, JLabel> getTimeFromTable()
	{
		return timeFromTable;
	}

	public JLabel getTimeToBeginLabel()
	{
		return timeToBeginLabel;
	}

	public JSlider getTimeToChooser()
	{
		return timeToChooser;
	}

	public JLabel getTimeToEndLabel()
	{
		return timeToEndLabel;
	}

	public JLabel getTimeToLabel()
	{
		return timeToLabel;
	}

	public Hashtable<Integer, JLabel> getTimeToTable()
	{
		return timeToTable;
	}

	public JSlider getTimeZoneChooser()
	{
		return timeZoneChooser;
	}

	private void paintTimeAxisChooserTick(JSlider slider,
			Hashtable<Integer, JLabel> table, JLabel label, JLabel begin, JLabel end)
	{
		if (timeAxis != null) {

			table.clear();

			Integer index = slider.getValue();

			begin.setText(TreeMapCanvasChart.formatNumberString(timeAxis.get(0)));

			label.setText(TreeMapCanvasChart.formatNumberString(timeAxis.get(index)));

			end.setText(TreeMapCanvasChart.formatNumberString(timeAxis.get(timeAxis
					.size() - 1)));

			table.put(index, label);

			table.put(0, begin);

			table.put(timeAxis.size() - 1, end);

			slider.setLabelTable(table);

			slider.repaint();
		}
	}

	private void setSelectedTimeAxis(boolean isZone)
	{
		int from = this.getTimeFromChooser().getValue();
		int to = this.getTimeToChooser().getValue();

		if (to < from) {
			to = from;
			this.getTimeToChooser().setValue(to);
		}

		int len = to - from;

		try {

			if (isZone) {
				int zone = this.getTimeZoneChooser().getValue();
				this.getTimeFromChooser().setValue(zone);
				this.getTimeToChooser().setValue(zone + len);
			} else {
				this.getSelectedTimeAxis().setLower(this.getTimeAxis().get(from));
				this.getSelectedTimeAxis().setUpper(this.getTimeAxis().get(to));
				this.getTimeZoneChooser().setValue(from);
				this.getTimeZoneChooser().setMaximum(this.getTimeAxis().size() - len - 1);
			}
		} catch (IndexOutOfBoundsException e) {

		}

		this.viewSelectedTimeAxis();
	}

	public void viewData()
	{
		TreeSet<Double> timeSet = new TreeSet<Double>();

		for (Entry<TreeMap<Double, Double>, GraphicsChartPainter> entry : this.getData()
				.entrySet())
		{
			for (Entry<Double, Double> data : entry.getKey().entrySet()) {
				timeSet.add(data.getKey());
			}
		}

		this.getTimeAxis().clear();
		this.getTimeAxis().addAll(timeSet);

		String begin = this.getTimeAxis().get(0).toString();
		String end = this.getTimeAxis().get(this.getTimeAxis().size() - 1).toString();

		this.getTimeFromBeginLabel().setText(begin);
		this.getTimeFromEndLabel().setText(end);
		this.getTimeToBeginLabel().setText(begin);
		this.getTimeToEndLabel().setText(end);

		this.getTimeFromChooser().setMaximum(this.getTimeAxis().size() - 1);
		this.getTimeFromChooser().setValue(0);

		this.paintTimeAxisChooserTick(timeFromChooser, timeFromTable, timeFromLabel,
				timeFromBeginLabel, timeFromEndLabel);
		this.getTimeFromChooser().setPaintLabels(true);

		this.getTimeToChooser().setMaximum(this.getTimeAxis().size() - 1);
		this.getTimeToChooser().setValue(this.getTimeAxis().size() - 1);

		this.paintTimeAxisChooserTick(timeToChooser, timeToTable, timeToLabel,
				timeToBeginLabel, timeToEndLabel);
		this.getTimeToChooser().setPaintLabels(true);

		this.getCanvas().needEnsure();

		this.viewSelectedTimeAxis();
	}

	private void viewSelectedTimeAxis()
	{
		Map<TreeMap<Double, Double>, GraphicsChartPainter> selected = new Hashtable<TreeMap<Double, Double>, GraphicsChartPainter>();

		Interval<Double> interval = this.getSelectedTimeAxis();

		for (Entry<TreeMap<Double, Double>, GraphicsChartPainter> entry : this.getData()
				.entrySet())
		{
			TreeMap<Double, Double> drawing = new TreeMap<Double, Double>();

			for (Entry<Double, Double> data : entry.getKey().entrySet()) {

				if (interval.getUpper().compareTo(data.getKey()) < 0) {
					break;
				}

				if (interval.has(data.getKey())) {
					drawing.put(data.getKey(), data.getValue());
				}
			}

			if (drawing.size() > 0) {
				selected.put(drawing, entry.getValue());
			}
		}

		this.getCanvas().getData().clear();

		this.getCanvas().getData().putAll(selected);

		this.getCanvas().getFunction().clear();

		for (Entry<Function, GraphicsChartPainter> entry : this.getFunction().entrySet())
		{
			Function function = entry.getKey().clone();

			function.setDomain(function.getDomain().getIntersectionWith(interval));

			this.getCanvas().getFunction().put(function, entry.getValue());
		}

		this.getCanvas().needPrepare();

		this.getCanvas().repaint();
	}
}
