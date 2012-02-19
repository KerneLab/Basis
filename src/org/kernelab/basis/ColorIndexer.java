package org.kernelab.basis;

import java.awt.Color;

/**
 * This class provides Color indexed from 0 to ColorIndexer.TOTAL-1
 * 
 * @author Dilly King
 * @version 2011.10.17.2
 */
public class ColorIndexer
{
	private static final int	ARGS		= 3;

	private static final int	ARG			= ARGS - 1;

	public static final int		ARG_BITS	= 8;

	public static final int		COLOR_BITS	= ARGS * ARG_BITS;

	public static final int		COLOR_RANGE	= 1 << ARG_BITS;

	public static final int		ARG_RANGE	= COLOR_RANGE - 1;

	public static final int		TOTAL		= ARGS * COLOR_RANGE;

	public static final int		RANGE		= TOTAL - 1;

	public static Color getColorMagentaBounded(double index)
	{
		return getColorMagentaBounded((int) (RANGE * index));
	}

	public static Color getColorMagentaBounded(float index)
	{
		return getColorMagentaBounded((int) (RANGE * index));
	}

	public static Color getColorMagentaBounded(int index)
	{
		index = Math.min(Math.max(index, 0), RANGE);

		int pos = index >> ARG_BITS;

		int mod = index & ARG_RANGE;

		int[] col = { 0, 0, 0 };

		col[pos] = ARG_RANGE;

		col[(pos + 1) % ARGS] = mod;

		col[(pos + 2) % ARGS] = ARG_RANGE - mod;

		return new Color(col[0], col[1], col[2]);
	}

	public static Color getColorRedBlueBounded(double index)
	{
		return getColorRedBlueBounded((int) (RANGE * index));
	}

	public static Color getColorRedBlueBounded(float index)
	{
		return getColorRedBlueBounded((int) (RANGE * index));
	}

	public static Color getColorRedBlueBounded(int index)
	{
		index = Math.min(Math.max(index, 0), RANGE);

		int pos = index >> ARG_BITS;

		int mod = index & ARG_RANGE;

		int[] col = { 0, 0, 0 };

		col[pos] = ARG_RANGE;

		if (pos != ARG) {
			col[pos + 1] = mod;
		}

		if (pos != 0) {
			col[pos - 1] = ARG_RANGE - mod;
		}

		return new Color(col[0], col[1], col[2]);
	}

	public static String getColorString(Color color)
	{
		String[] c = new String[3];
		c[0] = Integer.toHexString(color.getRed() & 0xFF);
		c[1] = Integer.toHexString(color.getGreen() & 0xFF);
		c[2] = Integer.toHexString(color.getBlue() & 0xFF);

		String string = "#";
		for (String s : c) {
			if (s.length() == 1) {
				string += "0";
			}
			string += s;
		}

		return string;
	}

}
