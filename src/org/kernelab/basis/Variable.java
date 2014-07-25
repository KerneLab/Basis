package org.kernelab.basis;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Variable class which can change its value.
 * 
 * @author Dilly King
 * 
 * @param <N>
 *            The generic type of number.
 */
public class Variable<N extends java.lang.Number & Comparable<N>> extends java.lang.Number implements
		Comparable<Variable<N>>, Serializable, Copieable<Variable<N>>
{
	/**
     * 
     */
	private static final long			serialVersionUID	= -4353893839356953344L;

	public static final DecimalFormat	DECIMAL_FORMAT		= new DecimalFormat();

	/**
	 * Try to convert the string to a Byte value, if could not convert then
	 * return null.
	 * 
	 * @param string
	 *            the number string.
	 * @return The Byte value.
	 */
	public static Byte asByte(String string)
	{
		return asByte(string, null);
	}

	/**
	 * Try to convert the string to a Byte value, if could not convert then use
	 * the defaultValue instead.
	 * 
	 * @param string
	 *            the number string.
	 * @param defaultValue
	 *            the default value which would be used if the string could not
	 *            be converted to a Byte.
	 * @return The Byte value.
	 */
	public static Byte asByte(String string, Byte defaultValue)
	{
		try
		{
			return Byte.valueOf(string);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}

	/**
	 * Try to convert the string to a Double value, if could not convert then
	 * return null.
	 * 
	 * @param string
	 *            the number string.
	 * @return The Double value.
	 */
	public static Double asDouble(String string)
	{
		return asDouble(string, null);
	}

	/**
	 * Try to convert the string to a Double value, if could not convert then
	 * use the defaultValue instead.
	 * 
	 * @param string
	 *            the number string.
	 * @param defaultValue
	 *            the default value which would be used if the string could not
	 *            be converted to a Double.
	 * @return The Double value.
	 */
	public static Double asDouble(String string, Double defaultValue)
	{
		try
		{
			return Double.valueOf(string);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}

	/**
	 * Try to convert the string to a Float value, if could not convert then
	 * return null.
	 * 
	 * @param string
	 *            the number string.
	 * @return The Float value.
	 */
	public static Float asFloat(String string)
	{
		return asFloat(string, null);
	}

	/**
	 * Try to convert the string to a Float value, if could not convert then use
	 * the defaultValue instead.
	 * 
	 * @param string
	 *            the number string.
	 * @param defaultValue
	 *            the default value which would be used if the string could not
	 *            be converted to a Float.
	 * @return The Float value.
	 */
	public static Float asFloat(String string, Float defaultValue)
	{
		try
		{
			return Float.valueOf(string);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}

	/**
	 * Try to convert the string to a Integer value, if could not convert then
	 * return null.
	 * 
	 * @param string
	 *            the number string.
	 * @return The Integer value.
	 */
	public static Integer asInteger(String string)
	{
		return asInteger(string, null);
	}

	/**
	 * Try to convert the string to an Integer value, if could not convert then
	 * use the defaultValue instead.
	 * 
	 * @param string
	 *            the number string.
	 * @param defaultValue
	 *            the default value which would be used if the string could not
	 *            be converted to an Integer.
	 * @return The Integer value.
	 */
	public static Integer asInteger(String string, Integer defaultValue)
	{
		try
		{
			return Integer.valueOf(string);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}

	/**
	 * Try to convert the string to a Long value, if could not convert then
	 * return null.
	 * 
	 * @param string
	 *            the number string.
	 * @return The Long value.
	 */
	public static Long asLong(String string)
	{
		return asLong(string, null);
	}

	/**
	 * Try to convert the string to a Long value, if could not convert then use
	 * the defaultValue instead.
	 * 
	 * @param string
	 *            the number string.
	 * @param defaultValue
	 *            the default value which would be used if the string could not
	 *            be converted to a Long.
	 * @return The Long value.
	 */
	public static Long asLong(String string, Long defaultValue)
	{
		try
		{
			return Long.valueOf(string);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}

	/**
	 * Try to convert the string to a Short value, if could not convert then
	 * return null.
	 * 
	 * @param string
	 *            the number string.
	 * @return The Short value.
	 */
	public static Short asShort(String string)
	{
		return asShort(string, null);
	}

	/**
	 * Try to convert the string to a Short value, if could not convert then use
	 * the defaultValue instead.
	 * 
	 * @param string
	 *            the number string.
	 * @param defaultValue
	 *            the default value which would be used if the string could not
	 *            be converted to a Short.
	 * @return The Short value.
	 */
	public static Short asShort(String string, Short defaultValue)
	{
		try
		{
			return Short.valueOf(string);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}

	/**
	 * Get the Byte value of a java.lang.Number
	 * 
	 * @param number
	 *            The java.lang.Number
	 * @return The Byte value
	 */
	public static Byte byteValueOfNumber(java.lang.Number number)
	{
		Byte value = null;

		if (number != null)
		{
			if (number instanceof Byte)
			{
				value = (Byte) number;
			}
			else
			{
				try
				{
					value = Byte.valueOf(number.toString());
				}
				catch (Exception e)
				{
					value = Variable.doubleValueOfNumber(number).byteValue();
				}
			}
		}

		return value;
	}

	/**
	 * Get the Double value of a java.lang.Number
	 * 
	 * @param number
	 *            The java.lang.Number
	 * @return The Double value
	 */
	public static Double doubleValueOfNumber(java.lang.Number number)
	{
		Double value = null;

		if (number != null)
		{
			if (number instanceof Double)
			{
				value = (Double) number;
			}
			else
			{
				value = Double.valueOf(number.toString());
			}
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
	public static Float floatValueOfNumber(java.lang.Number number)
	{
		Float value = null;

		if (number != null)
		{
			if (number instanceof Float)
			{
				value = (Float) number;
			}
			else
			{
				try
				{
					value = Float.valueOf(number.toString());
				}
				catch (Exception e)
				{
					value = Variable.doubleValueOfNumber(number).floatValue();
				}
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
	public static Integer integerValueOfNumber(java.lang.Number number)
	{
		Integer value = null;

		if (number != null)
		{
			if (number instanceof Integer)
			{
				value = (Integer) number;
			}
			else
			{
				try
				{
					value = Integer.valueOf(number.toString());
				}
				catch (Exception e)
				{
					value = Variable.doubleValueOfNumber(number).intValue();
				}
			}
		}

		return value;
	}

	/**
	 * Determine whether the String represents a float number.
	 * 
	 * @param string
	 *            the number string.
	 * @return <code>TRUE</code> if the string is Double otherwise
	 *         <code>FALSE</code>.
	 */
	public static boolean isFloatNumber(String string)
	{
		try
		{
			Double.parseDouble(string);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Determine whether the String represents a integer number.
	 * 
	 * @param string
	 *            the number string.
	 * @return <code>TRUE</code> if the string is Integer otherwise
	 *         <code>FALSE</code>.
	 */
	public static boolean isIntegerNumber(String string)
	{
		try
		{
			Long.parseLong(string);
			return true;
		}
		catch (Exception ex)
		{
			return false;
		}
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
		return isFloatNumber(string);
	}

	/**
	 * Get the Long value of a java.lang.Number
	 * 
	 * @param number
	 *            The java.lang.Number
	 * @return The Long value
	 */
	public static Long longValueOfNumber(java.lang.Number number)
	{
		Long value = null;

		if (number != null)
		{
			if (number instanceof Long)
			{
				value = (Long) number;
			}
			else
			{
				try
				{
					value = Long.valueOf(number.toString());
				}
				catch (Exception e)
				{
					value = Variable.doubleValueOfNumber(number).longValue();
				}
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
	public static <N extends java.lang.Number & Comparable<N>> Variable<N> newInstance(N number)
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
	public static <N extends java.lang.Number> String numberFormatString(N number, String format)
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
	 * Get the Short value of a java.lang.Number
	 * 
	 * @param number
	 *            The java.lang.Number
	 * @return The Short value
	 */
	public static Short shortValueOfNumber(java.lang.Number number)
	{
		Short value = null;

		if (number != null)
		{
			if (number instanceof Short)
			{
				value = (Short) number;
			}
			else
			{
				try
				{
					value = Short.valueOf(number.toString());
				}
				catch (Exception e)
				{
					value = Variable.doubleValueOfNumber(number).shortValue();
				}
			}
		}

		return value;
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

	public double doubleValue()
	{
		return Variable.doubleValueOfNumber(value);
	}

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
	public Variable<N> get(Variable<N> variable)
	{
		variable.value = this.value;
		return variable;
	}

	public N getValue()
	{
		return value;
	}

	public int intValue()
	{
		return Variable.integerValueOfNumber(value);
	}

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
	public Variable<N> set(Variable<N> variable)
	{
		this.value = variable.value;
		return this;
	}

	public Variable<N> setValue(N value)
	{
		this.value = value;
		return this;
	}

	@Override
	public String toString()
	{
		return value.toString();
	}
}
