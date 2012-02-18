package org.kernelab.basis;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Variable class which can change its value.
 * 
 * @author Dilly King
 * @version 2011.10.04.2
 * 
 * @param <N>
 *            The generic type of number.
 */
public class Variable<N extends java.lang.Number & Comparable<N>> extends
		java.lang.Number implements Comparable<Variable<N>>, Serializable,
		Copieable<Variable<N>>
{

	/**
         * 
         */
	private static final long			serialVersionUID	= -4353893839356953344L;

	public static final DecimalFormat	DECIMAL_FORMAT		= new DecimalFormat();

	/**
	 * Get the Double value of a java.lang.Number
	 * 
	 * @param number
	 *            The java.lang.Number
	 * @return The Double value
	 */
	public static final Double doubleValueOfNumber(java.lang.Number number)
	{
		Double value = null;

		if (number instanceof Double) {
			value = (Double) number;
		} else {
			value = Double.valueOf(number.toString());
		}

		return value;
	}

	/**
	 * Get the Float value of a java.lang.Number
	 * 
	 * @param number
	 *            The java.lang.Number
	 * @return The Float value
	 */
	public static final Float floatValueOfNumber(java.lang.Number number)
	{
		Float value = null;

		if (number instanceof Float) {
			value = (Float) number;
		} else {
			try {
				value = Float.valueOf(number.toString());
			} catch (NumberFormatException e) {
				value = Variable.doubleValueOfNumber(number).floatValue();
			}
		}

		return value;
	}

	/**
	 * Get the Integer value of a java.lang.Number
	 * 
	 * @param number
	 *            The java.lang.Number
	 * @return The Integer value
	 */
	public static final Integer integerValueOfNumber(java.lang.Number number)
	{
		Integer value = null;

		if (number instanceof Integer) {
			value = (Integer) number;
		} else {
			try {
				value = Integer.valueOf(number.toString());
			} catch (NumberFormatException e) {
				value = Variable.doubleValueOfNumber(number).intValue();
			}
		}

		return value;
	}

	/**
	 * Determine whether a String is a Double number which contains '.'.
	 * 
	 * @param string
	 *            the number string.
	 * @return <code>TRUE</code> if the string is Double otherwise
	 *         <code>FALSE</code>.
	 */
	public static final boolean isDouble(String string)
	{
		return !string.equals(".") && string.matches("^-?\\d*\\.\\d*$");
	}

	/**
	 * Determine whether a String is a Integer number which doesn't contain '.'.
	 * 
	 * @param string
	 *            the number string.
	 * @return <code>TRUE</code> if the string is Integer otherwise
	 *         <code>FALSE</code>.
	 */
	public static final boolean isInteger(String string)
	{
		return string.matches("^-?\\d+$");
	}

	/**
	 * Determine whether a Char is a number letter.
	 * 
	 * @param c
	 *            the number char.
	 * @return <code>TRUE</code> if the char is number otherwise
	 *         <code>FALSE</code>.
	 */
	public static boolean isNumber(char c)
	{
		return '0' <= c && c <= '9';
	}

	/**
	 * Determine whether a String is a number.
	 * 
	 * <pre>
	 * Example:
	 * 
	 * the String &quot;12.97&quot; is a number.
	 * the String &quot;8.4.5&quot; is not a number.
	 * the String &quot;3p2.5&quot; is not a number.
	 * </pre>
	 * 
	 * @param string
	 *            the number string.
	 * @return <code>TRUE</code> if the string is number otherwise
	 *         <code>FALSE</code>.
	 */
	public static boolean isNumber(String string)
	{
		return !string.isEmpty() && string.matches("^-?\\d*\\.?\\d*$");
	}

	/**
	 * Get the Long value of a java.lang.Number
	 * 
	 * @param number
	 *            The java.lang.Number
	 * @return The Long value
	 */
	public static final Long longValueOfNumber(java.lang.Number number)
	{
		Long value = null;

		if (number instanceof Long) {
			value = (Long) number;
		} else {
			try {
				value = Long.valueOf(number.toString());
			} catch (NumberFormatException e) {
				value = Variable.doubleValueOfNumber(number).longValue();
			}
		}

		return value;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Tools.debug(Variable.numberFormatString("1234.5", "#,##0.00"));
	}

	/**
	 * Get the instance of Variable. This method is no need to assign the
	 * generic type of the number.
	 * 
	 * <pre>
	 * Example:
	 * 
	 * Variable.instance(2);
	 * 
	 * equals to 
	 * 
	 * new Variable&lt;Integer&gt;(2);
	 * </pre>
	 * 
	 * @param <N>
	 *            The number type.
	 * @param number
	 *            The value of the number.
	 * @return The instance of Variable.
	 */
	public static <N extends java.lang.Number & Comparable<N>> Variable<N> newInstance(
			N number)
	{
		return new Variable<N>(number);
	}

	/**
	 * Get the format string of a BigDecimal number.
	 * 
	 * <pre>
	 * Example:
	 * 
	 * number: new BigDecimal("1234.5")
	 * format: &quot;#,##0.00&quot;
	 * string: &quot;1,234.50&quot;
	 * </pre>
	 * 
	 * @param number
	 *            the BigDecimal number to be formatted.
	 * @param format
	 *            the number format.
	 * @return the formatted String.
	 */
	public static String numberFormatString(BigDecimal number, String format)
	{
		Variable.DECIMAL_FORMAT.applyPattern(format);
		String string = Variable.DECIMAL_FORMAT.format(number);
		return string;
	}

	/**
	 * Get the format string of a number.
	 * 
	 * <pre>
	 * Example:
	 * 
	 * number: 1234.5
	 * format: &quot;#,##0.00&quot;
	 * string: &quot;1,234.50&quot;
	 * </pre>
	 * 
	 * @param <N>
	 *            the generic number type.
	 * @param number
	 *            the number to be formatted.
	 * @param format
	 *            the number format.
	 * @return the formatted String.
	 * @see java.text.DecimalFormat
	 */
	public static <N extends java.lang.Number> String numberFormatString(N number,
			String format)
	{
		Variable.DECIMAL_FORMAT.applyPattern(format);
		String string = Variable.DECIMAL_FORMAT.format(number);
		return string;
	}

	/**
	 * Get the format string of a number represented by a String.
	 * 
	 * <pre>
	 * Example:
	 * 
	 * number: "1234.5"
	 * format: &quot;#,##0.00&quot;
	 * string: &quot;1,234.50&quot;
	 * </pre>
	 * 
	 * @param number
	 *            the String number to be formatted.
	 * @param format
	 *            the number format.
	 * @return the formatted String.
	 */
	public static String numberFormatString(String number, String format)
	{
		Variable.DECIMAL_FORMAT.applyPattern(format);
		String string = Variable.DECIMAL_FORMAT.format(new BigDecimal(number));
		return string;
	}

	/**
	 * Parse the Variable value of a String.<br>
	 * Here, first try Integer value, if not Integer format, then try Double
	 * format, if still illegal format, then throws a NumberFormatException.
	 * 
	 * @param <N>
	 *            The generic type of number value.
	 * @param string
	 *            The String to be parsed.
	 * @return The Variable value.
	 */
	@SuppressWarnings("unchecked")
	public static <N extends java.lang.Number> N valueOf(String string)
	{
		N number = null;

		try {
			number = (N) Integer.valueOf(string);
		} catch (NumberFormatException ei) {
			number = (N) Double.valueOf(string);
		}

		return number;
	}

	public N	value;

	public Variable(N value)
	{
		this.value = value;
	}

	protected Variable(Variable<N> number)
	{
		this.value = number.value;
	}

	@Override
	public Variable<N> clone()
	{
		return new Variable<N>(this);
	}

	public int compareTo(Variable<N> number)
	{
		return this.getValue().compareTo(number.getValue());
	}

	@Override
	public double doubleValue()
	{
		return Variable.doubleValueOfNumber(value);
	}

	@Override
	public float floatValue()
	{
		return (float) doubleValue();
	}

	public String format(String format)
	{
		return Variable.numberFormatString(this.getValue(), format);
	}

	/**
	 * Get the value of this Variable into the parameter.
	 * 
	 * @param variable
	 *            The Variable which would holds the value of this Variable.
	 */
	public void get(Variable<N> variable)
	{
		variable.value = this.value;
	}

	public N getValue()
	{
		return value;
	}

	@Override
	public int intValue()
	{
		return Variable.integerValueOfNumber(value);
	}

	@Override
	public long longValue()
	{
		return Variable.longValueOfNumber(value);
	}

	/**
	 * To set the value of this Variable according to the given parameter.
	 * 
	 * @param variable
	 *            The Variable according to which the value of this Variable
	 *            would be set to.
	 */
	public void set(Variable<N> variable)
	{
		this.value = variable.value;
	}

	public void setValue(N value)
	{
		this.value = value;
	}

	@Override
	public String toString()
	{
		return value.toString();
	}
}
