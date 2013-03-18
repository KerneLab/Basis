package org.kernelab.basis;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.RenderingHints;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * The collection of tools what are common functions for basis.
 * 
 * @author Dilly King
 */
public class Tools
{
	private static final PrintStream		STD_OUT					= System.out;

	private static final PrintStream		STD_ERR					= System.err;

	private static final Set<PrintStream>	Outs					= new LinkedHashSet<PrintStream>();

	protected static final Date				DATE					= new Date();

	protected static final Calendar			CALENDAR				= new GregorianCalendar();

	public static final String				DATETIME_FORMAT_STRING	= "yyyy-MM-dd HH:mm:ss";

	public static final DateFormat			DATETIME_FORMAT			= new SimpleDateFormat(DATETIME_FORMAT_STRING);

	static
	{
		Outs.add(STD_OUT);
	}

	/**
	 * Add all elements in an Iterable object into a given Collection.
	 * 
	 * @param <T>
	 *            The generic type of the elements.
	 * @param collection
	 *            The Collection into which the elements would be added. If
	 *            null, a new LinkedList object would be created.
	 * @param iterable
	 *            The Iterable object which contains the elements to be added.
	 * @return The Collection.
	 */
	public static <T> Collection<T> addAll(Collection<T> collection, Iterable<T> iterable)
	{
		if (iterable != null)
		{
			if (collection == null)
			{
				collection = new LinkedList<T>();
			}
			for (T o : iterable)
			{
				collection.add(o);
			}
		}
		return collection;
	}

	/**
	 * To determined whether an element be contained in an array.
	 * 
	 * @param <T>
	 *            The generic type of element in the array.
	 * @param array
	 *            The array to be check.
	 * @param element
	 *            The object element.
	 * @return true if the array has the element otherwise false.
	 */
	public static <T> boolean arrayHas(T[] array, T element)
	{
		boolean has = false;

		for (T e : array)
		{
			if (e.equals(element))
			{
				has = true;
				break;
			}
		}

		return has;
	}

	/**
	 * To cast an object to a given class.
	 * 
	 * @param <T>
	 *            The generic type of the target class.
	 * @param obj
	 *            The object to be casted.
	 * @param cls
	 *            The given class.
	 * @return A casted object type of cls, null if object does not belong to
	 *         cls or obj itself is null.
	 */
	public static <T> T as(Object obj, Class<T> cls)
	{
		T t = null;
		try
		{
			t = cls.cast(obj);
		}
		catch (ClassCastException e)
		{
		}
		return t;
	}

	/**
	 * To cast an object whose class is S to class T.
	 * 
	 * @param src
	 *            The source object.
	 * @return The target class.
	 */
	@SuppressWarnings("unchecked")
	public static <S, T extends S> T cast(S src)
	{
		return (T) src;
	}

	/**
	 * To convert a CharSequence into char[].
	 * 
	 * @param sequence
	 *            The CharSequence to be converted.
	 * @return The char array which holds all the char in the CharSequence.
	 */
	public static char[] charArrayOfCharSequence(CharSequence sequence)
	{
		char[] chars = new char[sequence.length()];

		for (int i = 0; i < sequence.length(); i++)
		{
			chars[i] = sequence.charAt(i);
		}

		return chars;
	}

	/**
	 * To transfer the char from lower case to upper case.
	 * 
	 * @param c
	 *            the char to be transferred.
	 * @return the upper case of the char.
	 */
	public static char charToUpper(char c)
	{
		if ((int) c >= (int) 'a' && (int) c <= (int) 'z')
		{
			c -= (int) 'a' - (int) 'A';
		}
		return c;
	}

	/**
	 * Delete files and folders in a given directory.
	 * 
	 * @param directory
	 *            The given directory in which files would be deleted.
	 */
	public static void clearDirectory(File directory)
	{
		if (directory.isDirectory())
		{
			for (File file : directory.listFiles())
			{
				if (file.isDirectory())
				{
					clearDirectory(file);
				}
				file.delete();
			}
		}
	}

	/**
	 * Clear the StringBuffer.
	 * 
	 * @param stringBuffer
	 *            The StringBuffer to be cleared.
	 * @return The StringBuffer.
	 */
	public static StringBuffer clearStringBuffer(StringBuffer stringBuffer)
	{
		return stringBuffer.delete(0, stringBuffer.length());
	}

	/**
	 * Clear the StringBuilder.
	 * 
	 * @param stringBuilder
	 *            The StringBuilder to be cleared.
	 * @return The StringBuilder.
	 */
	public static StringBuilder clearStringBuilder(StringBuilder stringBuilder)
	{
		return stringBuilder.delete(0, stringBuilder.length());
	}

	/**
	 * Configure the application's look and feel as the system style.<br />
	 * Attention that this method should be called in static block of the
	 * <b>Main Class</b> and before any other GUI components being instanced.
	 * 
	 * <pre>
	 * static {
	 * 	Tools.configLookAndFeel();
	 * 	...
	 * }
	 * </pre>
	 */
	public static void configLookAndFeel()
	{
		Tools.configLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	}

	/**
	 * Configure the application's look and feel as the system style.<br />
	 * Attention that this method should be called in static block of the
	 * <b>Main Class</b> and before any other GUI components being instanced.
	 * 
	 * @see Tools#configLookAndFeel()
	 */
	public static void configLookAndFeel(String lookAndFeel)
	{
		try
		{
			UIManager.setLookAndFeel(lookAndFeel);
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (UnsupportedLookAndFeelException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * To decide whether a String contains a character.
	 * 
	 * @param string
	 *            The String to be decided whether contains a character.
	 * @param contain
	 *            The character to be decided whether it is contained in the
	 *            former String.
	 * @return <code>TRUE</code> if contains, otherwise <code>FALSE</code>.
	 */
	public static boolean containCharacter(String string, char contain)
	{
		return string.indexOf(contain) > -1;
	}

	/**
	 * To check whether the Collection contains a given object.
	 * 
	 * @param <T>
	 *            The generic type of the elements.
	 * @param iterable
	 *            The Iterable object to be checked.
	 * @param object
	 *            The object to be checked.
	 * @return true if the object is contained by the Iterable object.
	 */
	public static <T> boolean contains(Iterable<T> iterable, T object)
	{
		boolean contains = false;

		if (iterable != null)
		{
			for (T o : iterable)
			{
				if (contains = object == null ? o == null : object.equals(o))
				{
					break;
				}
			}
		}

		return contains;
	}

	/**
	 * To check whether the given collection contains all of the elements in the
	 * Iterable object.
	 * 
	 * @param <T>
	 *            The generic type of the elements.
	 * @param collection
	 *            The Collection which would be checked.
	 * @param iterable
	 *            The target Iterable object which would be checked.
	 * @return true if the Collection contains all the elements in the Iterable
	 *         object.
	 */
	public static <T> boolean containsAll(Collection<T> collection, Iterable<T> iterable)
	{
		boolean contains = false;
		if (collection != null && iterable != null)
		{
			contains = true;
			for (T o : iterable)
			{
				if (!collection.contains(o))
				{
					contains = false;
					break;
				}
			}
		}
		return contains;
	}

	/**
	 * To decide whether a String contains another String. Attention that, the
	 * contained String should be <b>shorter</b> than the containing String.
	 * 
	 * @param string
	 *            The String to be decided whether contains another shorter
	 *            String.
	 * @param contain
	 *            The shorter String to be decided whether it is contained in
	 *            the former String.
	 * @return <code>TRUE</code> if contains, otherwise <code>FALSE</code>. <br />
	 * <br />
	 *         <b>Attention To The Followings</b>:<br />
	 *         When the contain equals "", the function will return
	 *         <code>TRUE</code> if and only if string equals "".<br />
	 *         If the length of contain is longer than string then the function
	 *         will always return <code>FALSE</code>.
	 */
	public static boolean containString(String string, String contain)
	{
		boolean contains = false;

		int stringLength = string.length();

		int containLength = contain.length();

		if (stringLength >= containLength)
		{

			if (containLength == 0)
			{

				if (stringLength == 0)
				{
					contains = true;
				}
				else
				{
					contains = false;
				}

			}
			else
			{

				for (int i = 0; i < stringLength - containLength + 1 && !contains; i++)
				{

					if (string.charAt(i) == contain.charAt(0))
					{

						contains = true;

						for (int j = 1; j < contain.length() && contains; j++)
						{

							if (string.charAt(i + j) != contain.charAt(j))
							{
								contains = false;
								break;
							}
						}
					}
				}
			}
		}

		return contains;
	}

	/**
	 * Convert byte[] to Object with Serialization.
	 * 
	 * @param <T>
	 *            the generic type of the object.
	 * @param array
	 *            the byte[] of the object to be converted.
	 * @return the object.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T convertByteArrayToObject(byte[] array)
	{
		T object = null;

		ByteArrayInputStream bais = new ByteArrayInputStream(array);

		ObjectInputStream ois = null;

		try
		{

			ois = new ObjectInputStream(bais);

			object = (T) ois.readObject();

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (ois != null)
				{
					ois.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		return object;
	}

	/**
	 * Convert Object to byte[] with Serialization.
	 * 
	 * @param <T>
	 *            the generic type of object
	 * @param object
	 *            the object to be converted.
	 * @return the byte[] of the object.
	 */
	public static <T> byte[] convertObjectToByteArray(T object)
	{
		byte[] array = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ObjectOutputStream oos = null;

		try
		{

			oos = new ObjectOutputStream(baos);

			oos.writeObject(object);

			oos.flush();

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (oos != null)
				{
					oos.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}

		array = baos.toByteArray();

		return array;
	}

	/**
	 * Copy all the elements from Collection<T> source to object. <br />
	 * To use this method, the type of elements in collection should implements
	 * {@code Copieable<T>}.
	 * 
	 * @param T
	 *            extends Copieable
	 * @param source
	 *            The source Collection<T> where these elements come from.
	 * @param object
	 * @see Copieable
	 */
	public static <T extends Copieable<T>> void copyCollection(Collection<T> source, Collection<T> object)
	{
		for (T t : source)
		{
			object.add(t.clone());
		}
	}

	/**
	 * Count the occurrence of a char in a CharSequence from the beginning to
	 * the end.
	 * 
	 * @param cs
	 *            The CharSequence.
	 * @param c
	 *            The char to be found.
	 * @return The occurrence number.
	 */
	public static int countCharInCharSequence(CharSequence cs, char c)
	{
		return countCharInCharSequence(cs, c, 0);
	}

	/**
	 * Count the occurrence of a char in a CharSequence from a given index to
	 * the end.
	 * 
	 * @param cs
	 *            The CharSequence.
	 * @param c
	 *            The char to be found.
	 * @param from
	 *            The given index from where to count the occurrence.
	 * @return The occurrence number.
	 */
	public static int countCharInCharSequence(CharSequence cs, char c, int from)
	{
		return countCharInCharSequence(cs, c, from, cs.length());
	}

	/**
	 * Count the occurrence of a char in a CharSequence from a given index to a
	 * given end index(exclusive).
	 * 
	 * @param cs
	 *            The CharSequence.
	 * @param c
	 *            The char to be found.
	 * @param from
	 *            The given index from where to count the occurrence.
	 * @param end
	 *            The given index to where to count the occurrence. The max end
	 *            index is cs.length() which means the end of the CharSequence.
	 * @return The occurrence number.
	 */
	public static int countCharInCharSequence(CharSequence cs, char c, int from, int end)
	{
		int count = 0;

		from = Math.max(from, 0);
		end = Math.min(end, cs.length());

		for (int index = from; index < end; index++)
		{
			if (cs.charAt(index) == c)
			{
				count++;
			}
		}

		return count;
	}

	/**
	 * To output default debug information.
	 * 
	 */
	public static void debug()
	{
		debug("DEBUG");
	}

	/**
	 * To output debug information.
	 * 
	 * @param d
	 *            the boolean information.
	 */
	public static void debug(boolean b)
	{
		for (PrintStream o : Outs)
		{
			o.println(b);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param bs
	 *            the boolean array.
	 */
	public static void debug(boolean[] bs)
	{
		for (int i = 0; i < bs.length; i++)
		{
			debug(bs[i]);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param c
	 *            the Calendar information.
	 */
	public static void debug(Calendar c)
	{
		debug(getDateTimeString(c));
	}

	/**
	 * To output debug information.
	 * 
	 * @param c
	 *            the char information.
	 */
	public static void debug(char c)
	{
		for (PrintStream o : Outs)
		{
			o.println(c);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param cs
	 *            the char array.
	 */
	public static void debug(char[] cs)
	{
		for (int i = 0; i < cs.length; i++)
		{
			debug(cs[i]);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param s
	 *            the CharSequence information.
	 */
	public static void debug(CharSequence s)
	{
		for (int i = 0; i < s.length(); i++)
		{
			mark(s.charAt(i));
		}
		debug("");
	}

	/**
	 * To output debug information.
	 * 
	 * @param ss
	 *            the CharSequence array.
	 */
	public static void debug(CharSequence[] ss)
	{
		for (CharSequence s : ss)
		{
			debug(s);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param d
	 *            the double information.
	 */
	public static void debug(double d)
	{
		for (PrintStream o : Outs)
		{
			o.println(d);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param ds
	 *            the double array.
	 */
	public static void debug(double[] ds)
	{
		for (int i = 0; i < ds.length; i++)
		{
			debug(ds[i]);
		}
	}

	/**
	 * To output debug information of an array.
	 * 
	 * @param <E>
	 *            The generic type of elements in the array.
	 * @param array
	 *            The array to be output.
	 */
	public static <E> void debug(E[] array)
	{
		for (E e : array)
		{
			debug(e);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param f
	 *            the float information.
	 */
	public static void debug(float f)
	{
		for (PrintStream o : Outs)
		{
			o.println(f);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param fs
	 *            the float array.
	 */
	public static void debug(float[] fs)
	{
		for (int i = 0; i < fs.length; i++)
		{
			debug(fs[i]);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param i
	 *            the int information.
	 */
	public static void debug(int i)
	{
		for (PrintStream o : Outs)
		{
			o.println(i);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param is
	 *            the int array.
	 */
	public static void debug(int[] is)
	{
		for (int i = 0; i < is.length; i++)
		{
			debug(is[i]);
		}
	}

	/**
	 * To output debug information of an Iterable object.
	 * 
	 * @param <E>
	 *            The generic type of elements in the Iterable object.
	 * @param iterable
	 *            The Iterable object to be output.
	 */
	public static <E> void debug(Iterable<E> iterable)
	{
		for (E e : iterable)
		{
			debug(e);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param l
	 *            the long information.
	 */
	public static void debug(long l)
	{
		for (PrintStream o : Outs)
		{
			o.println(l);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param ls
	 *            the long array.
	 */
	public static void debug(long[] ls)
	{
		for (int i = 0; i < ls.length; i++)
		{
			debug(ls[i]);
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param <K>
	 *            the generic type of key in the map.
	 * @param <V>
	 *            the generic type of value in the map.
	 * @param map
	 *            the Map information.
	 */
	public static <K, V> void debug(Map<K, V> map)
	{
		if (map == null)
		{
			debug((Object) map);
		}
		else
		{
			for (Entry<K, V> entry : map.entrySet())
			{
				debug(entry.getKey() + ":" + entry.getValue());
			}
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param o
	 *            the Object information.
	 */
	public static void debug(Object o)
	{
		if (o != null)
		{
			debug(o.toString());
		}
		else
		{
			debug("null");
		}
	}

	/**
	 * To output debug information.
	 * 
	 * @param s
	 *            the String information.
	 */
	public static void debug(String s)
	{
		for (PrintStream o : Outs)
		{
			o.println(s);
		}
	}

	/**
	 * Delete files, folders in a given directory and delete the directory
	 * itself at last.
	 * 
	 * @param directory
	 *            The given directory to be deleted.
	 */
	public static void deleteDirectory(File directory)
	{
		if (directory.isDirectory())
		{
			clearDirectory(directory);
			directory.delete();
		}
	}

	/**
	 * Find the dual match character in a CharSequence from the beginning to the
	 * end.
	 * 
	 * <pre>
	 * Example:
	 * &quot;(morning(afternoon)evening)&quot;
	 * We find a='(' and b=')' for the dual match.
	 * If from 0, this method would returns 26.
	 * If from 8 or less but greater than 0, this method returns 18.
	 * </pre>
	 * 
	 * @param sequence
	 *            The CharSequence in which we find the dual match.
	 * @param a
	 *            The left character.
	 * @param b
	 *            The right character.
	 * @param from
	 *            From which position we start to find the dual match.
	 * @return The position that we find the right character for the dual match.
	 */
	public static int dualMatchIndex(CharSequence sequence, char a, char b, int from)
	{
		int match = 0;
		int index = -1;

		for (int i = Math.max(0, from); i < sequence.length(); i++)
		{

			char c = sequence.charAt(i);

			if (c == a)
			{
				match++;
			}
			else if (c == b)
			{
				match--;
				if (match == 0)
				{
					index = i;
					break;
				}
			}
		}

		return index;
	}

	/**
	 * Encode text to code by a certain encode algorithm like MD5, SHA-1
	 * 
	 * @param text
	 *            The text to be encoded.
	 * @param algorithm
	 *            The name of encode algorithm.
	 * @return The result of encoding.
	 */
	public static String encode(String text, String algorithm)
	{
		StringBuilder code = new StringBuilder();

		try
		{
			MessageDigest md = MessageDigest.getInstance(algorithm);
			byte[] digest = md.digest(text.getBytes());

			String tmp = null;
			for (int i = 0; i < digest.length; i++)
			{
				tmp = Integer.toHexString(digest[i] & 0xFF);
				if (tmp.length() == 1)
				{
					code.append("0");
				}
				code.append(tmp);
			}

		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}

		return code.toString();
	}

	/**
	 * To decide whether two object is equal or not. It would also be recognized
	 * as equal if two parameter are both null.
	 * 
	 * @param a
	 *            one object.
	 * @param b
	 *            another object.
	 * @return true if two object is equal.
	 */
	public static boolean equals(Object a, Object b)
	{
		boolean is = false;

		if (a == b)
		{
			is = true;
		}
		else if (a != null)
		{
			is = a.equals(b);
		}
		else
		{
			is = false;
		}

		return is;
	}

	/**
	 * Get the excerpt of a string with certain length.
	 * 
	 * <pre>
	 * Example:
	 * string: &quot;Please tell me.&quot;
	 * length: 11
	 * excerpt: &quot;Please tell...&quot;
	 * </pre>
	 * 
	 * @param string
	 *            the object string.
	 * @param length
	 *            the certain length.
	 * @return
	 */
	public static String excerpt(String string, int length)
	{
		String excerpt = string;

		if (excerpt.length() > length)
		{
			excerpt = string.substring(0, length) + "...";
		}

		return excerpt;
	}

	/**
	 * To filter some element from a certain Iterable object into another
	 * Collection.
	 * 
	 * @param <E>
	 *            The generic type of the elements in the Iterable object.
	 * @param iterable
	 *            The Iterable object which is to be filtered.
	 * @param filter
	 *            The Filter object which indicates the elements should be
	 *            preserved if filter returns true.
	 * @param result
	 *            The Collection holding the result of filter. If null, an empty
	 *            LinkedList would be created.
	 * @return The result Collection.
	 */
	public static <E> Collection<E> filter(Iterable<E> iterable, Filter<E> filter, Collection<E> result)
	{
		if (result == null)
		{
			result = new LinkedList<E>();
		}
		try
		{
			for (E element : iterable)
			{
				if (filter.filter(element))
				{
					result.add(element);
				}
			}
		}
		catch (Terminator t)
		{
		}
		return result;
	}

	/**
	 * To filter some element from a certain Iterable object into another
	 * Collection.
	 * 
	 * @param <E>
	 *            The generic type of the elements in the Iterable object.
	 * @param iterable
	 *            The Iterable object which is to be filtered.
	 * @param filter
	 *            The IndexedFilter object which indicates the elements should
	 *            be preserved if filter returns true.
	 * @param result
	 *            The Collection holding the result of filter. If null, an empty
	 *            LinkedList would be created.
	 * @return The result Collection.
	 */
	public static <E> Collection<E> filter(Iterable<E> iterable, IndexedFilter<E> filter, Collection<E> result)
	{
		if (result == null)
		{
			result = new LinkedList<E>();
		}
		int index = 0;
		try
		{
			for (E element : iterable)
			{
				if (filter.filter(element, index++))
				{
					result.add(element);
				}
			}
		}
		catch (Terminator t)
		{
		}
		return result;
	}

	/**
	 * Get the Calendar object according to the datetime String which is
	 * formatted by {@link Tools#DATETIME_FORMAT_STRING}.
	 * 
	 * @param datetime
	 *            The datetime String, such as "2012-04-13 12:41:39".
	 * @return The Calendar object.
	 */
	public static Calendar getCalendar(String datetime)
	{
		return getCalendar(datetime, DATETIME_FORMAT_STRING);
	}

	/**
	 * Get the Calendar object according to the datetime String which is
	 * formatted by format String.
	 * 
	 * @param datetime
	 *            The datetime String, such as "2012-04-13 12:41:39".
	 * @param format
	 *            The datetime format String, such as "yyyy-MM-dd HH:mm:ss"
	 * @return The Calendar object.
	 */
	public static Calendar getCalendar(String datetime, String format)
	{
		return getCalendar(datetime, format, null);
	}

	/**
	 * Get the Calendar object according to the datetime String which is
	 * formatted by format String.
	 * 
	 * @param datetime
	 *            The datetime String, such as "2012-04-13 12:41:39".
	 * @param format
	 *            The datetime format String, such as "yyyy-MM-dd HH:mm:ss"
	 * @param calendar
	 *            The Calendar object which holds the result. If null, a new
	 *            GregorianCalendar object would be created.
	 * @return The Calendar object.
	 */
	public static Calendar getCalendar(String datetime, String format, Calendar calendar)
	{
		if (calendar == null)
		{
			calendar = new GregorianCalendar();
		}

		calendar.set(0, 0, 0, 0, 0, 0);

		Map<Integer, String> map = new LinkedHashMap<Integer, String>();
		map.put(Calendar.YEAR, "y");
		map.put(Calendar.MONTH, "M");
		map.put(Calendar.DAY_OF_MONTH, "d");
		map.put(Calendar.HOUR_OF_DAY, "H");
		map.put(Calendar.MINUTE, "m");
		map.put(Calendar.SECOND, "s");

		for (Entry<Integer, String> entry : map.entrySet())
		{

			int field = entry.getKey();
			int value = 0;

			Matcher matcher = Pattern.compile(entry.getValue() + "+").matcher(format);

			if (matcher.find())
			{

				value = Integer.parseInt(datetime.substring(matcher.start(), matcher.end()));

				if (field == Calendar.MONTH)
				{
					value--;
				}

				calendar.set(field, value);
			}
		}

		return calendar;
	}

	/**
	 * Get the value of a number char.
	 * 
	 * @param c
	 *            the number char.
	 * @return the value of the number char.
	 */
	public static int getCharValue(char c)
	{
		int value = -1;
		if (Variable.isNumber(c))
		{
			char[] number = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
			for (int i = 0; i < number.length; i++)
			{
				if (number[i] == c)
				{
					value = i;
					break;
				}
			}
		}
		return value;
	}

	/**
	 * Get the Chinese number char of a number
	 * 
	 * Example,
	 * 
	 * number: 5 then the Chinese number char: '五'
	 * 
	 * @param number
	 *            the number.
	 * @return the Chinese number char.
	 */
	public static char getChineseNumber(int number)
	{
		final char[] chineseNumber = { '零', '一', '二', '三', '四', '五', '六', '七', '八', '九' };

		char chinese = chineseNumber[number % 10];

		return chinese;
	}

	/**
	 * Get the ClassLoader of the context from current thread.
	 * 
	 * @return ClassLoader.
	 */
	public static ClassLoader getClassLoader()
	{
		return Thread.currentThread().getContextClassLoader();
	}

	/**
	 * Get the String of a Calendar form as "yyyy-MM-dd HH:mm:ss".
	 * 
	 * @param calendar
	 *            The Calendar.
	 * @return The String of the Calendar.
	 */
	public static String getDateTimeString(Calendar calendar)
	{
		DATE.setTime(calendar.getTimeInMillis());
		return getDateTimeString(DATE);
	}

	/**
	 * Get the String of a Calendar form as the given format.
	 * 
	 * @param calendar
	 *            The Calendar.
	 * @param dateFormat
	 *            The DateFormat object.
	 * @return The String of the Calendar.
	 */
	public static String getDateTimeString(Calendar calendar, DateFormat dateFormat)
	{
		DATE.setTime(calendar.getTimeInMillis());
		return getDateTimeString(DATE, dateFormat);
	}

	/**
	 * Get the String of a Calendar form as the given format.
	 * 
	 * @param calendar
	 *            The Calendar.
	 * @param format
	 *            The Date format string.
	 * @return The String of the Calendar.
	 * @see java.text.SimpleDateFormat
	 */
	public static String getDateTimeString(Calendar calendar, String format)
	{
		DATE.setTime(calendar.getTimeInMillis());
		return getDateTimeString(DATE, format);
	}

	/**
	 * Get the String of a Date form as "yyyy-MM-dd HH:mm:ss".
	 * 
	 * @param date
	 *            The {@link java.util.Date} or {@link java.sql.Date} object.
	 * @return The String of the Date.
	 */
	public static String getDateTimeString(Date date)
	{
		return getDateTimeString(date, DATETIME_FORMAT);
	}

	/**
	 * Get the String of a Date form as a given Date format.
	 * 
	 * @param date
	 *            The Date.
	 * @param dateFormat
	 *            The DateFormat object.
	 * @return The String of the Date Time.
	 */
	public static String getDateTimeString(Date date, DateFormat dateFormat)
	{
		return dateFormat.format(date);
	}

	/**
	 * Get the String of a Date form as a given Date format.
	 * 
	 * @param date
	 *            The Date.
	 * @param format
	 *            The Date format string.
	 * @return The String of the Date Time.
	 * @see java.text.SimpleDateFormat
	 */
	public static String getDateTimeString(Date date, String format)
	{
		return getDateTimeString(date, new SimpleDateFormat(format));
	}

	/**
	 * Get the String of a Calendar form as "yyyy-MM-dd HH:mm:ss".
	 * 
	 * @param timeStamp
	 *            The long type variable, may be given by
	 *            {@link System#currentTimeMillis()}.
	 * @return The String of the long type variable.
	 */
	public static String getDateTimeString(long timeStamp)
	{
		DATE.setTime(timeStamp);
		return getDateTimeString(DATE);
	}

	/**
	 * Get the String of a long time variable form as a given Date format.
	 * 
	 * @param timeStamp
	 *            The long type variable, may be given by
	 *            {@link System#currentTimeMillis()}.
	 * @param dateFormat
	 *            The Date format object.
	 * @return The String of the Date Time.
	 */
	public static String getDateTimeString(long timeStamp, DateFormat dateFormat)
	{
		DATE.setTime(timeStamp);
		return getDateTimeString(DATE, dateFormat);
	}

	/**
	 * Get the String of a long time variable form as a given Date format
	 * string.
	 * 
	 * @param timeStamp
	 *            The long type variable, may be given by
	 *            {@link System#currentTimeMillis()}.
	 * @param format
	 *            The Date format string.
	 * @return The String of the Date Time.
	 * @see java.text.SimpleDateFormat
	 */
	public static String getDateTimeString(long timeStamp, String format)
	{
		DATE.setTime(timeStamp);
		return getDateTimeString(DATE, format);
	}

	/**
	 * To get a certain element in an Iterable object at the given index.
	 * 
	 * @param <E>
	 *            The generic type of the element in the Iterable object.
	 * @param itr
	 *            The Iterable object.
	 * @param index
	 *            The index which should be non-negative and less than the
	 *            elements number of the Iterable object.
	 * @return The found element, null if could not be found.
	 */
	public static <E> E getElementAt(Iterable<E> itr, int index)
	{
		E element = null;

		int i = 0;
		for (E e : itr)
		{
			if (i == index)
			{
				element = e;
				break;
			}
			i++;
		}

		return element;
	}

	/**
	 * Get the name of a file which represented as URL.
	 * 
	 * @param url
	 *            The URL that represent a file.
	 * @return The name of the file such as "Readme.txt"
	 */
	public static String getFileName(URL url)
	{
		String fileName = new String();
		String[] path = url.getFile().split("/");
		fileName = path[path.length - 1];
		return fileName;
	}

	/**
	 * To get the first element of a T type array.
	 * 
	 * @param <T>
	 *            The generic type of the array.
	 * @param t
	 *            The certain array of which to fetch the first element.
	 * @return The first element of the array.
	 */
	public static <T> T getFirstElement(T[] t)
	{
		return t[0];
	}

	/**
	 * To get the last element of a T type array.
	 * 
	 * @param <T>
	 *            The generic type of the array.
	 * @param t
	 *            The certain array of which to fetch the last element.
	 * @return The last element of the array.
	 */
	public static <T> T getLastElement(T[] t)
	{
		return t[t.length - 1];
	}

	/**
	 * Get the Calendar information according to the system current date time.
	 * 
	 * @return The CALENDAR Singleton defined in Tools.
	 * @see Tools#getNowCalendar(Calendar)
	 */
	public static Calendar getNowCalendar()
	{
		return getNowCalendar(CALENDAR);
	}

	/**
	 * Get the Calendar information according to the system current date time.
	 * 
	 * @param calendar
	 *            The Calendar to be put the date time information. If null,
	 *            {@link Tools#CALENDAR} would be used instead.
	 * @return The Calendar.
	 */
	public static Calendar getNowCalendar(Calendar calendar)
	{
		if (calendar == null)
		{
			calendar = CALENDAR;
		}
		calendar.setTimeInMillis(System.currentTimeMillis());
		return calendar;
	}

	/**
	 * Get the Out print Set.
	 * 
	 * @return The Set, which holds a serials of PrintStream that print some
	 *         debug and mark information.
	 */
	public static Set<PrintStream> getOuts()
	{
		return Outs;
	}

	/**
	 * Get the path of a Class. For example, giving java.lang.String, this
	 * method will return java/lang/.
	 * 
	 * @param cls
	 *            The Class object of a class.
	 * @return The path of the class.
	 */
	public static String getPathOfClass(Class<?> cls)
	{
		String path = cls.getCanonicalName().replace('.', '/');

		path = path.substring(0, path.lastIndexOf('/') + 1);

		return path;
	}

	/**
	 * Get the <b>MILLISECONDS</b> as the time stamp. Usually be used in
	 * absolutely time.
	 * 
	 * @return The milliseconds of current time.
	 */
	public static long getTimeStamp()
	{
		return System.currentTimeMillis();
	}

	/**
	 * Get the String of <b>MILLISECONDS</b> as the time stamp.
	 * 
	 * @return The String of milliseconds of current time.
	 */
	public static String getTimeStampString()
	{
		return String.valueOf(getTimeStamp());
	}

	/**
	 * Get the <b>NANOSECONDS</b> as the time stamp. Usually be used in relative
	 * time.
	 * 
	 * @return The nanoseconds of current time.
	 */
	public static long getTimeTrace()
	{
		return System.nanoTime();
	}

	/**
	 * Convert a Graphics object to Graphics2D.
	 * 
	 * @param g
	 *            the Graphics object
	 * @return Graphics2D object.
	 */
	public static Graphics2D graphics2D(Graphics g)
	{
		return (Graphics2D) g;
	}

	/**
	 * Make the graphics possess the anti-aliasing feature.
	 * 
	 * @param g
	 *            The Graphics.
	 * @return The Graphics2D object which possess the anti-aliasing feature.
	 */
	public static Graphics2D graphicsAntiAliasing(Graphics g)
	{
		Graphics2D g2 = graphics2D(g);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		return g2;
	}

	/**
	 * Creates an AlphaComposite object which makes translucent color with alpha
	 * value.<br />
	 * <br />
	 * The AlphaComposite object can be used as follow:<br />
	 * 
	 * <pre>
	 * AlphaComposite composite = Tools.graphicsTranslucentComposite(0.6f);
	 * ...
	 * protected void paintComponent(Graphics g)
	 * {
	 * 	super.paintComponent(g);
	 * 
	 * 	Graphics2D g2 = (Graphics2D) g;
	 * 	g2.setComposite(composite);
	 * 
	 * 	g.fillOval(0, 0, 10, 10);
	 * }
	 * </pre>
	 * 
	 * @param alpha
	 *            The alpha value.
	 * @return AlphaComposite object.
	 */
	public static AlphaComposite graphicsTranslucentComposite(float alpha)
	{
		return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
	}

	/**
	 * Input object from InputStream to the object reference.
	 * 
	 * @param input
	 *            the InputStream from which to input.
	 * 
	 * @return the object input from the InputStream.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T inputObject(InputStream input) throws IOException
	{
		T object = null;

		ObjectInputStream in = null;

		try
		{

			in = new ObjectInputStream(input);

			object = (T) in.readObject();

		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (in != null)
				{
					in.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		return object;
	}

	/**
	 * Input object from File to the object reference.
	 * 
	 * @param file
	 *            the File from which to input.
	 * 
	 * @return the object input from the File.
	 */
	public static <T> T inputObjectFromFile(File file) throws IOException
	{
		T object = null;

		try
		{
			object = inputObject(new FileInputStream(file));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		return object;
	}

	/**
	 * Input the content in InputStream as a single String and using the default
	 * charSet.
	 * 
	 * @param inputStream
	 *            the InputStream.
	 * @return the String.
	 */
	public static String inputStreamToString(InputStream inputStream)
	{
		return inputStreamToString(inputStream, null);
	}

	/**
	 * Input the content in InputStream as a single String.
	 * 
	 * @param inputStream
	 *            the InputStream.
	 * @param charSetName
	 *            the name of CharSet.
	 * @return the String.
	 */
	public static String inputStreamToString(InputStream inputStream, String charSetName)
	{
		StringBuilder builder = new StringBuilder();

		Charset charSet = null;
		if (charSetName == null)
		{
			charSet = Charset.defaultCharset();
		}
		else
		{
			charSet = Charset.forName(charSetName);
		}

		InputStreamReader reader = new InputStreamReader(inputStream, charSet);

		try
		{
			char[] buffer = new char[3072];
			int length = -1;
			while ((length = reader.read(buffer)) != -1)
			{
				builder.append(buffer, 0, length);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return builder.toString();
	}

	/**
	 * Input the content in InputStream into a List of Strings.
	 * 
	 * @param inputStream
	 *            the InputStream.
	 * @return the List of Strings.
	 */
	public static List<String> inputStreamToStrings(InputStream inputStream)
	{
		return inputStreamToStrings(inputStream, null, null);
	}

	/**
	 * Input the content in InputStream into a List of Strings.
	 * 
	 * @param inputStream
	 *            the InputStream.
	 * @param list
	 *            the List which holds the Strings. A new LinkedList object
	 *            would be created if list is null.
	 * @return the List of Strings.
	 */
	public static List<String> inputStreamToStrings(InputStream inputStream, List<String> list)
	{
		return inputStreamToStrings(inputStream, list, null);
	}

	/**
	 * Input the content in InputStream into a List of Strings within a special
	 * CharSet.
	 * 
	 * @param inputStream
	 *            the InputStream.
	 * @param list
	 *            the List which holds the Strings. A new LinkedList object
	 *            would be created if list is null.
	 * @param charSetName
	 *            the name of CharSet.
	 * @return the List of Strings.
	 */
	public static List<String> inputStreamToStrings(InputStream inputStream, List<String> list, String charSetName)
	{
		if (list == null)
		{
			list = new LinkedList<String>();
		}

		Charset charSet = null;
		if (charSetName == null)
		{
			charSet = Charset.defaultCharset();
		}
		else
		{
			charSet = Charset.forName(charSetName);
		}

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charSet));

		try
		{
			String line = null;
			while ((line = bufferedReader.readLine()) != null)
			{
				list.add(line);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (bufferedReader != null)
			{
				try
				{
					bufferedReader.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return list;
	}

	/**
	 * Input the content in InputStream into a List of Strings within a special
	 * CharSet.
	 * 
	 * @param inputStream
	 *            the InputStream.
	 * @param charSetName
	 *            the name of CharSet.
	 * @return the List of Strings.
	 */
	public static List<String> inputStreamToStrings(InputStream inputStream, String charSetName)
	{
		return inputStreamToStrings(inputStream, null, charSetName);
	}

	/**
	 * To input a whole File as a String and using the default CharSet.<br />
	 * If the file is very huge and contains great amount of lines, this method
	 * would perform in low efficiency. {@link DataReader} is recommended for
	 * this situation.
	 * 
	 * @param file
	 *            the file to be input.
	 * @return the string.
	 */
	public static String inputStringFromFile(File file)
	{
		return inputStringFromFile(file, null);
	}

	/**
	 * To input a whole File as a String.<br />
	 * If the file is very huge and contains great amount of lines, this method
	 * would perform in low efficiency. {@link DataReader} is recommended for
	 * this situation.
	 * 
	 * @param file
	 *            the file to be input.
	 * @param charSetName
	 *            the CharSet name of the file.
	 * @return the string.
	 */
	public static String inputStringFromFile(File file, String charSetName)
	{
		StringBuilder builder = new StringBuilder();

		Charset charSet = null;
		if (charSetName == null)
		{
			charSet = Charset.defaultCharset();
		}
		else
		{
			charSet = Charset.forName(charSetName);
		}

		InputStreamReader reader = null;

		try
		{
			reader = new InputStreamReader(new FileInputStream(file), charSet);
			char[] buffer = new char[3072];
			int length = -1;
			while ((length = reader.read(buffer)) != -1)
			{
				builder.append(buffer, 0, length);
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return builder.toString();
	}

	/**
	 * To input strings from a File. Each line in the file will be input as a
	 * String in a LinkedList&lt;String&gt;.<br />
	 * If the file is very huge and contains great amount of lines, this method
	 * would perform in low efficiency. {@link DataReader} is recommended for
	 * this situation.
	 * 
	 * @param file
	 *            the file to be input.
	 * @return a List of String or {@code null} if file doesn't exist.
	 */
	public static List<String> inputStringsFromFile(File file)
	{
		return inputStringsFromFile(file, null);
	}

	/**
	 * To input strings from a File. Each line in the file will be input as a
	 * String in a List.<br />
	 * If the file is very huge and contains great amount of lines, this method
	 * would perform in low efficiency. {@link DataReader} is recommended for
	 * this situation.
	 * 
	 * @param file
	 *            the file to be input.
	 * @param list
	 *            A list which would hold the result Strings. A LinkedList
	 *            Object would be create if list is null.
	 * @return a List of String or {@code null} if file doesn't exist.
	 */
	public static List<String> inputStringsFromFile(File file, List<String> list)
	{
		if (list == null)
		{
			list = new LinkedList<String>();
		}

		FileReader fileReader = null;

		try
		{

			fileReader = new FileReader(file);

			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line = null;
			while ((line = bufferedReader.readLine()) != null)
			{
				list.add(line);
			}

		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (fileReader != null)
			{
				try
				{
					fileReader.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return list;
	}

	/**
	 * To get the Intersection of two Iterable object.<br />
	 * That is both of the two Iterable object have the element in the
	 * Intersection.
	 * 
	 * @param <T>
	 *            the element type in both Iterable object.
	 * @param a
	 *            the Iterable object a.
	 * @param b
	 *            the Iterable object b.
	 * @param result
	 *            the Collection which holds the result. If null, a new
	 *            LinkedList would be used.
	 * @return the Intersection of the Iterable object a and b.<br />
	 *         Attention that the Intersection result May contains DUPLICATE
	 *         elements. The result Collection should be a Set object to avoid
	 *         this case.
	 */
	public static <T> Collection<T> intersection(Iterable<T> a, Iterable<T> b, Collection<T> result)
	{
		if (result == null)
		{
			result = new LinkedList<T>();
		}

		Iterable<T> source = null;
		Iterable<T> target = null;

		if (a instanceof Set)
		{
			source = b;
			target = a;
		}
		else
		{
			source = a;
			target = b;
		}

		if (source != null)
		{
			for (T o : source)
			{
				if (contains(target, o))
				{
					result.add(o);
				}
			}
		}

		return result;
	}

	/**
	 * Joint each CharSequence in an Iterable object to a String.
	 * 
	 * @param iter
	 *            the Iterable object which contains CharSequences.
	 * @return the whole String.
	 */
	public static String jointCharSequences(Iterable<CharSequence> iter)
	{
		StringBuilder builder = new StringBuilder();

		for (CharSequence s : iter)
		{
			builder.append(s);
		}

		return builder.toString();
	}

	/**
	 * Joint each CharSequence in an Iterable object to a String with a special
	 * delimiter.
	 * 
	 * @param iter
	 *            the Iterable object which contains CharSequences.
	 * @param delimiter
	 *            a series of delimiter which would split these CharSequences.
	 * @return the whole String.
	 */
	public static String jointCharSequences(Iterable<CharSequence> iter, CharSequence... delimiter)
	{
		StringBuilder builder = new StringBuilder();

		boolean first = true;
		int index = 0;
		int length = delimiter.length;

		for (CharSequence s : iter)
		{
			if (first)
			{
				first = false;
			}
			else
			{
				builder.append(delimiter[index]);
			}
			builder.append(s);
			if (length == ++index)
			{
				index = 0;
			}
		}

		return builder.toString();
	}

	/**
	 * Joint each Strings in an Iterable object to a String.
	 * 
	 * @param iter
	 *            the Iterable object which contains Strings.
	 * @return the whole String.
	 */
	public static String jointStrings(Iterable<String> iter)
	{
		StringBuilder builder = new StringBuilder();

		for (String s : iter)
		{
			builder.append(s);
		}

		return builder.toString();
	}

	/**
	 * Joint each String in an Iterable object to a String with a special
	 * delimiter.
	 * 
	 * @param iter
	 *            the Iterable object which contains Strings.
	 * @param delimiter
	 *            a series of delimiter which would split these Strings.
	 * @return the whole String.
	 */
	public static String jointStrings(Iterable<String> iter, CharSequence... delimiter)
	{
		StringBuilder builder = new StringBuilder();

		boolean first = true;
		int index = 0;
		int length = delimiter.length;

		for (CharSequence s : iter)
		{
			if (first)
			{
				first = false;
			}
			else
			{
				builder.append(delimiter[index]);
			}
			builder.append(s);
			if (length == ++index)
			{
				index = 0;
			}
		}

		return builder.toString();
	}

	/**
	 * To limit a double number within a given range. If the number exceeds the
	 * range, then the boundary value would be taken.
	 * 
	 * @param number
	 *            The number to be limited.
	 * @param lower
	 *            The lower boundary.
	 * @param higher
	 *            The higher boundary.
	 * @return The limited number.
	 */
	public static double limitNumber(double number, double lower, double higher)
	{
		if (lower > higher)
		{
			double temp = lower;
			lower = higher;
			higher = temp;
		}

		return Math.max(lower, Math.min(higher, number));
	}

	/**
	 * To limit a float number within a given range. If the number exceeds the
	 * range, then the boundary value would be taken.
	 * 
	 * @param number
	 *            The number to be limited.
	 * @param lower
	 *            The lower boundary.
	 * @param higher
	 *            The higher boundary.
	 * @return The limited number.
	 */
	public static float limitNumber(float number, float lower, float higher)
	{
		if (lower > higher)
		{
			float temp = lower;
			lower = higher;
			higher = temp;
		}

		return Math.max(lower, Math.min(higher, number));
	}

	/**
	 * To limit a int number within a given range. If the number exceeds the
	 * range, then the boundary value would be taken.
	 * 
	 * @param number
	 *            The number to be limited.
	 * @param lower
	 *            The lower boundary.
	 * @param higher
	 *            The higher boundary.
	 * @return The limited number.
	 */
	public static int limitNumber(int number, int lower, int higher)
	{
		if (lower > higher)
		{
			int temp = lower;
			lower = higher;
			higher = temp;
		}

		return Math.max(lower, Math.min(higher, number));
	}

	/**
	 * Add elements from Array to List.<br />
	 * Attention that this method would not clear the elements that contained in
	 * the List before called.
	 * 
	 * @param <T>
	 *            The type of elements contained in the Array.
	 * @param list
	 *            The object List which add element to.
	 * @param array
	 *            The source Array which contains elements.
	 * @return The List which contains all elements of collection and of the
	 *         same sequence.
	 */
	public static <T> List<T> listOfArray(List<T> list, T[] array)
	{
		for (T t : array)
		{
			list.add(t);
		}
		return list;
	}

	/**
	 * Add elements from Collection to List.<br />
	 * Attention that this method would not clear the elements that contained in
	 * the List before called.
	 * 
	 * @param <T>
	 *            The type of elements contained in the Collection.
	 * @param list
	 *            The object List which add element to.
	 * @param collection
	 *            The source Collection which contains elements.
	 * @return The List which contains all elements of collection and of the
	 *         same sequence.
	 */
	public static <T> List<T> listOfCollection(List<T> list, Collection<T> collection)
	{
		for (T t : collection)
		{
			list.add(t);
		}
		return list;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String s = "2012/12/07 13:23:58";
		String f = "yyyy/dd/MM HH";
		Calendar c = getCalendar(s, f, null);
		Tools.debug(getDateTimeString(c, "yyyy-MM-dd HH:mm:ss"));
	}

	/**
	 * Make a default GridBagConstraints with BOTH filled, 0 grid in x and y,
	 * 1.0 weight of x and y.
	 * 
	 * @return The default GridBagConstraints.
	 */
	public static GridBagConstraints makePreferredGridBagConstraints()
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		return gbc;
	}

	/**
	 * To map each element in a certain Iterable object into another Collection
	 * by the definition of Mapper.
	 * 
	 * @param <K>
	 *            The generic type of the elements in the source Iterable
	 *            object.
	 * @param <V>
	 *            The generic type of the elements in the target Collection.
	 * @param iterable
	 *            The source Iterable object to be mapped.
	 * @param mapper
	 *            The IndexedMapper object which defines the mapping operation.
	 * @param result
	 *            The Collection which holds the result of mapping operation. If
	 *            null, an empty LinkedList would be created.
	 * @return The mapping result.
	 */
	public static <K, V> Collection<V> map(Iterable<K> iterable, IndexedMapper<K, V> mapper, Collection<V> result)
	{
		if (result == null)
		{
			result = new LinkedList<V>();
		}
		int index = 0;
		try
		{
			for (K value : iterable)
			{
				result.add(mapper.map(value, index++));
			}
		}
		catch (Terminator t)
		{
		}
		return result;
	}

	/**
	 * To map each element in a certain Iterable object into another Collection
	 * by the definition of Mapper.
	 * 
	 * @param <K>
	 *            The generic type of the elements in the source Iterable
	 *            object.
	 * @param <V>
	 *            The generic type of the elements in the target Collection.
	 * @param iterable
	 *            The source Iterable object to be mapped.
	 * @param mapper
	 *            The Mapper object which defines the mapping operation.
	 * @param result
	 *            The Collection which holds the result of mapping operation. If
	 *            null, an empty LinkedList would be created.
	 * @return The mapping result.
	 */
	public static <K, V> Collection<V> map(Iterable<K> iterable, Mapper<K, V> mapper, Collection<V> result)
	{
		if (result == null)
		{
			result = new LinkedList<V>();
		}
		try
		{
			for (K value : iterable)
			{
				result.add(mapper.map(value));
			}
		}
		catch (Terminator t)
		{
		}
		return result;
	}

	/**
	 * To output default debug information without line wrapper.
	 * 
	 */
	public static void mark()
	{
		mark("MARK");
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param d
	 *            the boolean information.
	 */
	public static void mark(boolean b)
	{
		for (PrintStream o : Outs)
		{
			o.print(b);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param bs
	 *            the boolean array.
	 */
	public static void mark(boolean[] bs)
	{
		mark(bs, "");
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param bs
	 *            the boolean array.
	 * @param split
	 *            the split between each element.
	 */
	public static void mark(boolean[] bs, CharSequence split)
	{
		for (int i = 0; i < bs.length; i++)
		{
			if (i != 0)
			{
				mark(split);
			}
			mark(bs[i]);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param c
	 *            the Calendar information.
	 */
	public static void mark(Calendar c)
	{
		mark(getDateTimeString(c));
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param c
	 *            the char information.
	 */
	public static void mark(char c)
	{
		for (PrintStream o : Outs)
		{
			o.print(c);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param cs
	 *            the char array.
	 */
	public static void mark(char[] cs)
	{
		mark(cs, "");
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param cs
	 *            the char array.
	 * @param split
	 *            the split between each element.
	 */
	public static void mark(char[] cs, CharSequence split)
	{
		for (int i = 0; i < cs.length; i++)
		{
			if (i != 0)
			{
				mark(split);
			}
			mark(cs[i]);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param s
	 *            the CharSequence information.
	 */
	public static void mark(CharSequence s)
	{
		for (int i = 0; i < s.length(); i++)
		{
			mark(s.charAt(i));
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param ss
	 *            the CharSequence array.
	 */
	public static void mark(CharSequence[] ss)
	{
		mark(ss, "");
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param ss
	 *            the CharSequence array.
	 * @param split
	 *            the split between each element.
	 */
	public static void mark(CharSequence[] ss, CharSequence split)
	{
		for (int i = 0; i < ss.length; i++)
		{
			if (i != ss.length)
			{
				mark(split);
			}
			mark(ss[i]);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param d
	 *            the double information.
	 */
	public static void mark(double d)
	{
		for (PrintStream o : Outs)
		{
			o.print(d);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param ds
	 *            the double array.
	 */
	public static void mark(double[] ds)
	{
		mark(ds, "");
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param ds
	 *            the double array.
	 * @param split
	 *            the split between each element.
	 */
	public static void mark(double[] ds, CharSequence split)
	{
		for (int i = 0; i < ds.length; i++)
		{
			if (i != 0)
			{
				mark(split);
			}
			mark(ds[i]);
		}
	}

	/**
	 * To output debug information of an array without line wrapper.
	 * 
	 * @param <E>
	 *            The generic type of elements in the array.
	 * @param array
	 *            The array to be output.
	 */
	public static <E> void mark(E[] array)
	{
		mark(array, "");
	}

	/**
	 * To output debug information of an array without line wrapper.
	 * 
	 * @param <E>
	 *            The generic type of elements in the array.
	 * @param array
	 *            The array to be output.
	 * @param split
	 *            the split between each element.
	 */
	public static <E> void mark(E[] array, CharSequence split)
	{
		boolean first = true;
		for (E e : array)
		{
			if (first)
			{
				first = false;
			}
			else
			{
				mark(split);
			}
			mark(e);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param f
	 *            the float information.
	 */
	public static void mark(float f)
	{
		for (PrintStream o : Outs)
		{
			o.print(f);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param fs
	 *            the float array.
	 */
	public static void mark(float[] fs)
	{
		mark(fs, "");
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param fs
	 *            the float array.
	 * @param split
	 *            the split between each element.
	 */
	public static void mark(float[] fs, CharSequence split)
	{
		for (int i = 0; i < fs.length; i++)
		{
			if (i != 0)
			{
				mark(split);
			}
			mark(fs[i]);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param i
	 *            the int information.
	 */
	public static void mark(int i)
	{
		for (PrintStream o : Outs)
		{
			o.print(i);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param is
	 *            the int array.
	 */
	public static void mark(int[] is)
	{
		mark(is, "");
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param is
	 *            the int array.
	 * @param split
	 *            the split between each element.
	 */
	public static void mark(int[] is, CharSequence split)
	{
		for (int i = 0; i < is.length; i++)
		{
			if (i != 0)
			{
				mark(split);
			}
			mark(is[i]);
		}
	}

	/**
	 * To output debug information of an Iterable object without line wrapper.
	 * 
	 * @param <E>
	 *            The generic type of elements in the Iterable object.
	 * @param iterable
	 *            The Iterable object to be output.
	 */
	public static <E> void mark(Iterable<E> iterable)
	{
		mark(iterable, "");
	}

	/**
	 * To output debug information of an Iterable object without line wrapper.
	 * 
	 * @param <E>
	 *            The generic type of elements in the Iterable object.
	 * @param iterable
	 *            The Iterable object to be output.
	 * @param split
	 *            the split between each element.
	 */
	public static <E> void mark(Iterable<E> iterable, CharSequence split)
	{
		boolean first = true;
		for (E e : iterable)
		{
			if (first)
			{
				first = false;
			}
			else
			{
				mark(split);
			}
			mark(e);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param l
	 *            the long information.
	 */
	public static void mark(long l)
	{
		for (PrintStream o : Outs)
		{
			o.print(l);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param ls
	 *            the long array.
	 */
	public static void mark(long[] ls)
	{
		mark(ls, "");
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param ls
	 *            the long array.
	 * @param split
	 *            the split between each element.
	 */
	public static void mark(long[] ls, CharSequence split)
	{
		for (int i = 0; i < ls.length; i++)
		{
			if (i != 0)
			{
				mark(split);
			}
			mark(ls[i]);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param o
	 *            the Object information.
	 */
	public static void mark(Object o)
	{
		if (o != null)
		{
			mark(o.toString());
		}
		else
		{
			mark("null");
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param s
	 *            the String information.
	 */
	public static void mark(String s)
	{
		for (PrintStream o : Outs)
		{
			o.print(s);
		}
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param ss
	 *            the String array.
	 */
	public static void mark(String[] ss)
	{
		mark(ss, "");
	}

	/**
	 * To output debug information without line wrapper.
	 * 
	 * @param ss
	 *            the String array.
	 * @param split
	 *            the split between each element.
	 */
	public static void mark(String[] ss, CharSequence split)
	{
		for (int i = 0; i < ss.length; i++)
		{
			if (i != 0)
			{
				mark(split);
			}
			mark(ss[i]);
		}
	}

	/**
	 * Find the maximum element in an Iterable object.
	 * 
	 * @param <T>
	 *            The generic type of the elements in the Iterable object.
	 * @param iterable
	 *            The Iterable object.
	 * @return The maximum element in the Iterable object.
	 */
	public static <T extends Comparable<T>> T max(Iterable<T> iterable)
	{
		T max = null;

		for (T t : iterable)
		{
			if (max == null)
			{
				max = t;
			}
			else if (t.compareTo(max) > 0)
			{
				max = t;
			}
		}

		return max;
	}

	/**
	 * Find the maximum element in an Iterable object.
	 * 
	 * @param <T>
	 *            The generic type of the elements in the Iterable object.
	 * @param iterable
	 *            The Iterable object.
	 * @param comparator
	 *            The Comparator which indicates the comparing relation.
	 * @return The maximum element in the Iterable object.
	 */
	public static <T> T max(Iterable<T> iterable, Comparator<T> comparator)
	{
		T max = null;

		for (T t : iterable)
		{
			if (max == null)
			{
				max = t;
			}
			else if (comparator.compare(t, max) > 0)
			{
				max = t;
			}
		}

		return max;
	}

	/**
	 * Find the minimum element in an Iterable object.
	 * 
	 * @param <T>
	 *            The generic type of the elements in the Iterable object.
	 * @param iterable
	 *            The Iterable object.
	 * @return The minimum element in the Iterable object.
	 */
	public static <T extends Comparable<T>> T min(Iterable<T> iterable)
	{
		T min = null;

		for (T t : iterable)
		{
			if (min == null)
			{
				min = t;
			}
			else if (t.compareTo(min) < 0)
			{
				min = t;
			}
		}

		return min;
	}

	/**
	 * Find the minimum element in an Iterable object.
	 * 
	 * @param <T>
	 *            The generic type of the elements in the Iterable object.
	 * @param iterable
	 *            The Iterable object.
	 * @param comparator
	 *            The Comparator which indicates the comparing relation.
	 * @return The minimum element in the Iterable object.
	 */
	public static <T> T min(Iterable<T> iterable, Comparator<T> comparator)
	{
		T min = null;

		for (T t : iterable)
		{
			if (min == null)
			{
				min = t;
			}
			else if (comparator.compare(t, min) < 0)
			{
				min = t;
			}
		}

		return min;
	}

	/**
	 * Output object to the OuputStream from the object reference.
	 * 
	 * @param object
	 *            the object reference to be output.
	 * @param output
	 *            the OutputStream with which the output.
	 * @return true if input does succeed otherwise false.
	 */
	public static boolean outputObject(Object object, OutputStream output)
	{
		boolean success = false;

		ObjectOutputStream out = null;
		try
		{

			out = new ObjectOutputStream(output);

			out.writeObject(object);

			success = true;

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (out != null)
			{
				try
				{
					out.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return success;
	}

	/**
	 * Output object to File from the object reference.
	 * 
	 * @param object
	 *            the object reference to be output.
	 * @param file
	 *            the File with which the output.
	 * @return true if input does succeed otherwise false.
	 */
	public static boolean outputObjectToFile(Object object, File file)
	{
		boolean success = false;
		try
		{
			success = outputObject(object, new FileOutputStream(file));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		return success;
	}

	/**
	 * To output a Collection of String to a File. By default, this method isn't
	 * append mode.<br />
	 * If the collection of string contains great amount of lines, this method
	 * would perform in low efficiency. {@link DataWriter} is recommended for
	 * this situation.
	 * 
	 * @param file
	 *            the file to be output.
	 * @param strings
	 *            the Collection of Strings to be output.
	 * @return true if output does succeed otherwise false.
	 */
	public static boolean outputStringsToFile(File file, Collection<String> strings)
	{
		boolean success = false;

		FileWriter fileWriter = null;

		try
		{

			fileWriter = new FileWriter(file);

			success = Tools.outputStringsToFile(fileWriter, strings);

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (fileWriter != null)
			{
				try
				{
					fileWriter.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return success;
	}

	/**
	 * To output a Collection of String to a FileWriter. The FileWriter could be
	 * defined as {@code new FileWriter(File,true)} to let this method be append
	 * mode.<br />
	 * If the collection of string contains great amount of lines, this method
	 * would perform in low efficiency. {@link DataWriter} is recommended for
	 * this situation.
	 * 
	 * @param fileWriter
	 *            the FileWriter to output the strings.
	 * @param strings
	 *            the Collection of Strings to be output.
	 * @return true if output does succeed otherwise false.
	 */
	public static boolean outputStringsToFile(FileWriter fileWriter, Collection<String> strings)
	{
		boolean success = false;

		PrintWriter printWriter = new PrintWriter(fileWriter, true);

		for (String string : strings)
		{
			printWriter.println(string);
		}

		printWriter.close();

		success = true;

		return success;
	}

	/**
	 * To output a String to a File. By default, this method isn't append mode.<br />
	 * If the string contains great amount of chars, this method would perform
	 * in low efficiency. {@link DataWriter} is recommended for this situation.
	 * 
	 * @param file
	 *            the file to be output.
	 * @param strings
	 *            the String to be output.
	 * @return true if output does succeed otherwise false.
	 */
	public static boolean outputStringToFile(File file, String string)
	{
		boolean success = false;

		FileWriter fileWriter = null;

		try
		{

			fileWriter = new FileWriter(file);

			success = Tools.outputStringToFile(fileWriter, string);

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (fileWriter != null)
			{
				try
				{
					fileWriter.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return success;
	}

	/**
	 * To output a String to a FileWriter. The FileWriter could be defined as
	 * {@code new FileWriter(File,true)} to let this method be append mode.<br />
	 * If the string contains great amount of chars, this method would perform
	 * in low efficiency. {@link DataWriter} is recommended for this situation.
	 * 
	 * @param fileWriter
	 *            the FileWriter to output the string.
	 * @param string
	 *            the String to be output.
	 * @return true if output does succeed otherwise false.
	 */
	public static boolean outputStringToFile(FileWriter fileWriter, String string)
	{
		boolean success = false;

		PrintWriter printWriter = new PrintWriter(fileWriter, true);

		printWriter.println(string);

		printWriter.close();

		success = true;

		return success;
	}

	/**
	 * Read the content in Reader into a StringBuilder.
	 * 
	 * @param reader
	 *            the Reader.
	 * @return the StringBuilder which holds the content in the reader.
	 */
	public static StringBuilder readerToStringBuilder(Reader reader)
	{
		return readerToStringBuilder(reader, null);
	}

	/**
	 * Read the content in Reader into a StringBuilder with a given buffer.
	 * 
	 * @param reader
	 *            the Reader.
	 * @param result
	 *            a StringBuilder which would hold the read characters. If null,
	 *            a new StringBuilder would be created instead.
	 * @return the StringBuilder which holds the content in the reader.
	 */
	public static StringBuilder readerToStringBuilder(Reader reader, StringBuilder result)
	{
		if (result == null)
		{
			result = new StringBuilder();
		}

		int read = -1;

		char[] buffer = new char[1024];

		try
		{
			while ((read = reader.read(buffer)) >= 0)
			{
				result.append(buffer, 0, read);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				reader.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * To reduce a certain Iterable object into one value.
	 * 
	 * @param <E>
	 *            The generic type of the elements in the Iterable object.
	 * @param <R>
	 *            The generic type of the reduction result.
	 * @param iterable
	 *            The Iterable object to be reduced.
	 * @param reducer
	 *            The IndexedReducer object which defines the reducing
	 *            operation.
	 * @param result
	 *            The initial value of the reduction result.
	 * @return The result of the reduction.
	 */
	public static <E, R> R reduce(Iterable<E> iterable, IndexedReducer<E, R> reducer, R result)
	{
		int index = 0;
		try
		{
			for (E element : iterable)
			{
				result = reducer.reduce(result, element, index++);
			}
		}
		catch (Terminator t)
		{
		}
		return result;
	}

	/**
	 * To reduce a certain Iterable object into one value.
	 * 
	 * @param <E>
	 *            The generic type of the elements in the Iterable object.
	 * @param <R>
	 *            The generic type of the reduction result.
	 * @param iterable
	 *            The Iterable object to be reduced.
	 * @param reducer
	 *            The Reducer object which defines the reducing operation.
	 * @param result
	 *            The initial value of the reduction result.
	 * @return The result of the reduction.
	 */
	public static <E, R> R reduce(Iterable<E> iterable, Reducer<E, R> reducer, R result)
	{
		try
		{
			for (E element : iterable)
			{
				result = reducer.reduce(result, element);
			}
		}
		catch (Terminator t)
		{
		}
		return result;
	}

	/**
	 * Remove all elements in an Iterable object into a given Collection.
	 * 
	 * @param <T>
	 *            The generic type of the elements.
	 * @param collection
	 *            The Collection from which the elements would be removed.
	 * @param iterable
	 *            The Iterable object which contains the elements to be removed.
	 * @return The Collection.
	 */
	public static <T> Collection<T> removeAll(Collection<T> collection, Iterable<T> iterable)
	{
		if (collection != null && iterable != null)
		{
			for (T o : iterable)
			{
				collection.remove(o);
			}
		}
		return collection;
	}

	/**
	 * To remove the certain substring from a string.
	 * 
	 * <pre>
	 * Example:
	 * 	string: &quot;String&quot;, remove: &quot;tr&quot;, result: &quot;Sing&quot;.
	 * 	string: &quot;building&quot;, remove: &quot;cut&quot;, result: &quot;building&quot;.
	 * </pre>
	 * 
	 * @param string
	 *            the string which should contain the substring to be removed.
	 * @param remove
	 *            the substring which would be removed from the string.
	 * @return the result of the removing operation. If the string didn't
	 *         contain the string, returns the string.
	 */
	public static String removeString(String string, String remove)
	{
		String result = "";
		if (containString(string, remove))
		{
			boolean contains = false;
			int begin = 0;
			for (int i = 0; i < string.length() && contains == false; i++)
			{
				contains = true;
				begin = i;
				for (int j = 0; j < remove.length() && contains == true; j++)
				{
					if (i + j >= string.length())
					{
						contains = false;
						break;
					}
					if (string.charAt(i + j) != remove.charAt(j))
						contains = false;
				}
			}
			result = string.substring(0, begin)
					+ (begin + remove.length() < string.length() ? string.substring(begin + remove.length()) : "");
		}
		else
		{
			result = string;
		}
		return result;
	}

	/**
	 * To get a String of n times repeat of a certain character.
	 * 
	 * @param c
	 *            The character to be repeated.
	 * @param n
	 *            The times to repeat the character.
	 * @return The repeated String.
	 * @see Tools#repeat(CharSequence, int)
	 */
	public static String repeat(char c, int n)
	{
		return repeat(String.valueOf(c), n);
	}

	/**
	 * To get a String of n times repeat of a certain character.
	 * 
	 * @param c
	 *            The character to be repeated.
	 * @param n
	 *            The times to repeat the character.
	 * @param delimiter
	 *            The delimiter which joints the whole String.
	 * @return The repeated String.
	 * @see Tools#repeat(char, int, CharSequence)
	 */
	public static String repeat(char c, int n, char delimiter)
	{
		return repeat(c, n, String.valueOf(delimiter));
	}

	/**
	 * To get a String of n times repeat of a certain character.
	 * 
	 * @param c
	 *            The character to be repeated.
	 * @param n
	 *            The times to repeat the character.
	 * @param delimiter
	 *            The delimiter which joints the whole String.
	 * @return The repeated String.
	 * @see Tools#repeat(CharSequence, int, CharSequence)
	 */
	public static String repeat(char c, int n, CharSequence delimiter)
	{
		return repeat(String.valueOf(c), n, delimiter);
	}

	/**
	 * To get a String of n times repeat of a certain CharSequence.
	 * 
	 * @param s
	 *            The CharSequence to be repeated.
	 * @param n
	 *            The times to repeat the CharSequence.
	 * @return The repeated String.
	 * @see Tools#repeat(CharSequence, int, CharSequence)
	 */
	public static String repeat(CharSequence s, int n)
	{
		return repeat(s, n, "");
	}

	/**
	 * To get a String of n times repeat of a certain CharSequence which split
	 * by a given character.
	 * 
	 * @param s
	 *            The CharSequence to be repeated.
	 * @param n
	 *            The times to repeat the CharSequence.
	 * @param delimiter
	 *            The delimiter which joints the whole String.
	 * @return The repeated String.
	 * @see Tools#repeat(CharSequence, int, CharSequence)
	 */
	public static String repeat(CharSequence s, int n, char delimiter)
	{
		return repeat(s, n, String.valueOf(delimiter));
	}

	/**
	 * To get a String of n times repeat of a certain CharSequence which split
	 * by a given delimiter.
	 * 
	 * @param s
	 *            The CharSequence to be repeated.
	 * @param n
	 *            The times to repeat the CharSequence.
	 * @param delimiter
	 *            The delimiter which joints the whole String.
	 * @return The repeated String.
	 */
	public static String repeat(CharSequence s, int n, CharSequence delimiter)
	{
		StringBuilder buffer = new StringBuilder();

		for (int i = 0; i < n; i++)
		{
			if (i != 0)
			{
				buffer.append(delimiter);
			}
			buffer.append(s);
		}

		return buffer.toString();
	}

	/**
	 * To get a String of n times repeat of a certain character.
	 * 
	 * @param b
	 *            The buffer holds the repeat result.
	 * @param c
	 *            The character to be repeated.
	 * @param n
	 *            The times to repeat the character.
	 * @return The repeated String.
	 * @see Tools#repeat(CharSequence, int)
	 */
	public static StringBuffer repeat(StringBuffer b, char c, int n)
	{
		return repeat(b, String.valueOf(c), n);
	}

	/**
	 * To get a String of n times repeat of a certain character.
	 * 
	 * @param b
	 *            The buffer holds the repeat result.
	 * @param c
	 *            The character to be repeated.
	 * @param n
	 *            The times to repeat the character.
	 * @param delimiter
	 *            The delimiter which joints the whole String.
	 * @return The repeated String.
	 * @see Tools#repeat(char, int, CharSequence)
	 */
	public static StringBuffer repeat(StringBuffer b, char c, int n, char delimiter)
	{
		return repeat(b, c, n, String.valueOf(delimiter));
	}

	/**
	 * To get a String of n times repeat of a certain character.
	 * 
	 * @param b
	 *            The buffer holds the repeat result.
	 * @param c
	 *            The character to be repeated.
	 * @param n
	 *            The times to repeat the character.
	 * @param delimiter
	 *            The delimiter which joints the whole String.
	 * @return The repeated String.
	 * @see Tools#repeat(CharSequence, int, CharSequence)
	 */
	public static StringBuffer repeat(StringBuffer b, char c, int n, CharSequence delimiter)
	{
		return repeat(b, String.valueOf(c), n, delimiter);
	}

	/**
	 * To get a String of n times repeat of a certain CharSequence.
	 * 
	 * @param b
	 *            The buffer holds the repeat result.
	 * @param s
	 *            The CharSequence to be repeated.
	 * @param n
	 *            The times to repeat the CharSequence.
	 * @return The repeated String.
	 * @see Tools#repeat(CharSequence, int, CharSequence)
	 */
	public static StringBuffer repeat(StringBuffer b, CharSequence s, int n)
	{
		return repeat(b, s, n, "");
	}

	/**
	 * To get a String of n times repeat of a certain CharSequence which split
	 * by a given character.
	 * 
	 * @param b
	 *            The buffer holds the repeat result.
	 * @param s
	 *            The CharSequence to be repeated.
	 * @param n
	 *            The times to repeat the CharSequence.
	 * @param delimiter
	 *            The delimiter which joints the whole String.
	 * @return The repeated String.
	 * @see Tools#repeat(CharSequence, int, CharSequence)
	 */
	public static StringBuffer repeat(StringBuffer b, CharSequence s, int n, char delimiter)
	{
		return repeat(b, s, n, String.valueOf(delimiter));
	}

	/**
	 * To get a String of n times repeat of a certain CharSequence which split
	 * by a given delimiter.
	 * 
	 * @param b
	 *            The buffer holds the repeat result.
	 * @param s
	 *            The CharSequence to be repeated.
	 * @param n
	 *            The times to repeat the CharSequence.
	 * @param delimiter
	 *            The delimiter which joints the whole String.
	 * @return The repeated String.
	 */
	public static StringBuffer repeat(StringBuffer b, CharSequence s, int n, CharSequence delimiter)
	{
		for (int i = 0; i < n; i++)
		{
			if (i != 0)
			{
				b.append(delimiter);
			}
			b.append(s);
		}

		return b;
	}

	/**
	 * To get a String of n times repeat of a certain character.
	 * 
	 * @param b
	 *            The buffer holds the repeat result.
	 * @param c
	 *            The character to be repeated.
	 * @param n
	 *            The times to repeat the character.
	 * @return The repeated String.
	 * @see Tools#repeat(CharSequence, int)
	 */
	public static StringBuilder repeat(StringBuilder b, char c, int n)
	{
		return repeat(b, String.valueOf(c), n);
	}

	/**
	 * To get a String of n times repeat of a certain character.
	 * 
	 * @param b
	 *            The buffer holds the repeat result.
	 * @param c
	 *            The character to be repeated.
	 * @param n
	 *            The times to repeat the character.
	 * @param delimiter
	 *            The delimiter which joints the whole String.
	 * @return The repeated String.
	 * @see Tools#repeat(char, int, CharSequence)
	 */
	public static StringBuilder repeat(StringBuilder b, char c, int n, char delimiter)
	{
		return repeat(b, c, n, String.valueOf(delimiter));
	}

	/**
	 * To get a String of n times repeat of a certain character.
	 * 
	 * @param b
	 *            The buffer holds the repeat result.
	 * @param c
	 *            The character to be repeated.
	 * @param n
	 *            The times to repeat the character.
	 * @param delimiter
	 *            The delimiter which joints the whole String.
	 * @return The repeated String.
	 * @see Tools#repeat(CharSequence, int, CharSequence)
	 */
	public static StringBuilder repeat(StringBuilder b, char c, int n, CharSequence delimiter)
	{
		return repeat(b, String.valueOf(c), n, delimiter);
	}

	/**
	 * To get a String of n times repeat of a certain CharSequence.
	 * 
	 * @param b
	 *            The buffer holds the repeat result.
	 * @param s
	 *            The CharSequence to be repeated.
	 * @param n
	 *            The times to repeat the CharSequence.
	 * @return The repeated String.
	 * @see Tools#repeat(CharSequence, int, CharSequence)
	 */
	public static StringBuilder repeat(StringBuilder b, CharSequence s, int n)
	{
		return repeat(b, s, n, "");
	}

	/**
	 * To get a String of n times repeat of a certain CharSequence which split
	 * by a given character.
	 * 
	 * @param b
	 *            The buffer holds the repeat result.
	 * @param s
	 *            The CharSequence to be repeated.
	 * @param n
	 *            The times to repeat the CharSequence.
	 * @param delimiter
	 *            The delimiter which joints the whole String.
	 * @return The repeated String.
	 * @see Tools#repeat(CharSequence, int, CharSequence)
	 */
	public static StringBuilder repeat(StringBuilder b, CharSequence s, int n, char delimiter)
	{
		return repeat(b, s, n, String.valueOf(delimiter));
	}

	/**
	 * To get a String of n times repeat of a certain CharSequence which split
	 * by a given delimiter.
	 * 
	 * @param b
	 *            The buffer holds the repeat result.
	 * @param s
	 *            The CharSequence to be repeated.
	 * @param n
	 *            The times to repeat the CharSequence.
	 * @param delimiter
	 *            The delimiter which joints the whole String.
	 * @return The repeated String.
	 */
	public static StringBuilder repeat(StringBuilder b, CharSequence s, int n, CharSequence delimiter)
	{
		for (int i = 0; i < n; i++)
		{
			if (i != 0)
			{
				b.append(delimiter);
			}
			b.append(s);
		}

		return b;
	}

	/**
	 * Reset the System.err to the default.
	 */
	public static void resetErr()
	{
		System.setErr(Tools.STD_ERR);
	}

	/**
	 * Clear the Out print Set and add System.out as default.
	 */
	public static void resetOuts()
	{
		Outs.clear();
		Outs.add(STD_OUT);
	}

	/**
	 * Retains the elements in the given collection that are contained in the
	 * Iterable object.
	 * 
	 * @param <T>
	 *            The generic type of the elements.
	 * @param collection
	 *            The Collection which holds the elements would be retained.
	 * @param iterable
	 *            The Iterable object which contains the elements to be
	 *            retained.
	 * @return The Collection.
	 */
	public static <T> Collection<T> retainAll(Collection<T> collection, Iterable<T> iterable)
	{
		if (collection != null && iterable != null)
		{
			LinkedList<T> temp = new LinkedList<T>();
			for (T o : iterable)
			{
				if (collection.contains(o))
				{
					temp.add(o);
				}
			}
			collection.clear();
			collection.addAll(temp);
		}
		return collection;
	}

	/**
	 * Find the dual match character in a CharSequence from the end to the
	 * beginning.
	 * 
	 * <pre>
	 * Example:
	 * &quot;(morning(afternoon)evening)&quot;
	 * We find a='(' and b=')' for the dual match.
	 * If from 26, this method would returns 0.
	 * If from 18 or greater but less than 26, this method returns 8.
	 * </pre>
	 * 
	 * @param sequence
	 *            The CharSequence in which we find the dual match.
	 * @param a
	 *            The left character.
	 * @param b
	 *            The right character.
	 * @param from
	 *            From which position we start to find the dual match.
	 * @return The position that we find the left character for the dual match.
	 */
	public static int reverseDualMatchIndex(CharSequence sequence, char a, char b, int from)
	{
		int index = -1;
		int match = 0;

		for (int i = Math.min(sequence.length() - 1, from); i >= 0; i--)
		{

			char c = sequence.charAt(i);

			if (c == b)
			{
				match++;
			}
			else if (c == a)
			{
				match--;
				if (match == 0)
				{
					index = i;
					break;
				}
			}
		}

		return index;
	}

	/**
	 * To seek the first position of a certain char in a CharSequence from the
	 * beginning of the CharSequence.
	 * 
	 * @param seq
	 *            The CharSequence to be seek.
	 * @param c
	 *            The target char.
	 * @return The first position of the char. Return -1 if could not be found.
	 * @see Tools#seekLastIndex(CharSequence, char, int)
	 */
	public static int seekIndex(CharSequence seq, char c)
	{
		return seekIndex(seq, c, 0);
	}

	/**
	 * To seek the first position of a certain char in a CharSequence from a
	 * given start offset.
	 * 
	 * @param seq
	 *            The CharSequence to be seek.
	 * @param c
	 *            The target char.
	 * @param from
	 *            The given offset at where begin to seek.
	 * @return The first position of the char from the offset. Return -1 if
	 *         could not be found.
	 */
	public static int seekIndex(CharSequence seq, char c, int from)
	{
		int index = -1;

		int length = seq.length();

		from = Math.max(0, from);

		for (int i = from; i < length; i++)
		{
			if (seq.charAt(i) == c)
			{
				index = i;
				break;
			}
		}

		return index;
	}

	/**
	 * To seek the first position of a certain CharSequence in another
	 * CharSequence from the beginning of the later one.
	 * 
	 * @param seq
	 *            The CharSequence to be seek.
	 * @param sub
	 *            The sub CharSequence.
	 * @return The first position of the CharSequence. Return -1 if could not be
	 *         found.
	 * @see Tools#seekIndex(CharSequence, CharSequence, int)
	 */
	public static int seekIndex(CharSequence seq, CharSequence sub)
	{
		return seekIndex(seq, sub, 0);
	}

	/**
	 * To seek the first position of a certain CharSequence in another
	 * CharSequence from a given start offset.
	 * 
	 * @param seq
	 *            The CharSequence to be seek.
	 * @param sub
	 *            The sub CharSequence.
	 * @param from
	 *            The given offset at where begin to seek.
	 * @return The first position of the CharSequence from the offset. Return -1
	 *         if could not be found.
	 */
	public static int seekIndex(CharSequence seq, CharSequence sub, int from)
	{
		int index = -1;

		if (sub.length() < seq.length())
		{

			int small = sub.length();
			int length = seq.length() - small + 1;

			from = Math.max(0, from);

			boolean found = false;

			for (int i = from; i < length; i++)
			{

				found = true;

				for (int j = 0; j < small && found; j++)
				{
					found = seq.charAt(i + j) == sub.charAt(j);
				}

				if (found)
				{
					index = i;
					break;
				}
			}
		}

		return index;
	}

	/**
	 * To seek the last position of a certain char in a CharSequence from the
	 * end of the CharSequence.
	 * 
	 * @param seq
	 *            The CharSequence to be seek.
	 * @param c
	 *            The target char.
	 * @return The last position of the char. Return -1 if could not be found.
	 * @see Tools#seekLastIndex(CharSequence, char, int)
	 */
	public static int seekLastIndex(CharSequence seq, char c)
	{
		return seekLastIndex(seq, c, seq.length());
	}

	/**
	 * To seek the last position of a certain char in a CharSequence from a
	 * given start offset.
	 * 
	 * @param seq
	 *            The CharSequence to be seek.
	 * @param c
	 *            The target char.
	 * @param from
	 *            The given offset at where begin to seek.
	 * @return The last position of the char from the offset. Return -1 if could
	 *         not be found.
	 */
	public static int seekLastIndex(CharSequence seq, char c, int from)
	{
		int index = -1;

		from = Math.min(seq.length() - 1, from);

		for (int i = from; i > -1; i--)
		{
			if (seq.charAt(i) == c)
			{
				index = i;
				break;
			}
		}

		return index;
	}

	/**
	 * To seek the last position of a certain CharSequence in another
	 * CharSequence from the end of the later one.
	 * 
	 * @param seq
	 *            The CharSequence to be seek.
	 * @param sub
	 *            The sub CharSequence.
	 * @return The last position of the CharSequence. Return -1 if could not be
	 *         found.
	 */
	public static int seekLastIndex(CharSequence seq, CharSequence sub)
	{
		return seekLastIndex(seq, sub, seq.length());
	}

	/**
	 * To seek the last position of a certain CharSequence in another
	 * CharSequence from a given start offset.
	 * 
	 * @param seq
	 *            The CharSequence to be seek.
	 * @param sub
	 *            The sub CharSequence.
	 * @param from
	 *            The given offset at where begin to seek.
	 * @return The last position of the CharSequence from the offset. Return -1
	 *         if could not be found.
	 */
	public static int seekLastIndex(CharSequence seq, CharSequence sub, int from)
	{
		int index = -1;

		if (sub.length() < seq.length())
		{

			int small = sub.length();
			int length = seq.length() - small;

			from = Math.min(length, from);

			boolean found = false;

			for (int i = from; i > -1; i--)
			{

				found = true;

				for (int j = 0; j < small && found; j++)
				{
					found = seq.charAt(i + j) == sub.charAt(j);
				}

				if (found)
				{
					index = i;
					break;
				}
			}
		}

		return index;
	}

	/**
	 * Redirect the System.err to the certain PrintStream.
	 * 
	 * @param ps
	 *            the PrintStream to be redirected.
	 */
	public static void setErr(PrintStream ps)
	{
		System.setErr(ps);
	}

	/**
	 * Convert Collection<T> to Set<T> (HashSet<T>).
	 * 
	 * @param <T>
	 *            The type of elements contained in the Collection.
	 * @param collection
	 *            The Collection to be converted.
	 * @return The Set which contains all elements of collection and of the same
	 *         sequence. But in the result Set, there is NO DUPLICATE element.
	 */
	public static <T> Set<T> setOfCollection(Collection<T> collection)
	{
		Set<T> set = new LinkedHashSet<T>();

		for (T t : collection)
		{
			set.add(t);
		}

		return set;
	}

	/**
	 * Split a CharSequence which is divided by split.<br />
	 * If the CharSequence does not contain the split then return a String of
	 * the CharSequence.
	 * 
	 * <pre>
	 * Example:
	 * &quot;abc/2s/4t&quot; with split '/' returns {&quot;abc&quot;, &quot;2s&quot;, &quot;4t&quot;}
	 * &quot;tp.4e&quot; with split '/' returns {&quot;tp.4e&quot;}
	 * So &quot;&quot; with any split always returns {&quot;&quot;}
	 * &quot;:&quot; with split ':' returns {&quot;&quot;, &quot;&quot;}
	 * </pre>
	 * 
	 * @param sequence
	 *            The CharSequence that will be split.
	 * @param split
	 *            The Split signal such as '/' that is contained in
	 *            CharSequence.
	 * @return A String array contains these split string.
	 * @see Tools#splitCharSequence(CharSequence, char, int)
	 */
	public static String[] splitCharSequence(CharSequence sequence, char split)
	{
		return splitCharSequence(sequence, split, -0);
	}

	/**
	 * Split a CharSequence which is divided by split.<br />
	 * The limit parameter indicates that the parts of split result would not be
	 * greater than limit if limit was a positive number. If limit is a
	 * non-positive number, then the CharSequence will be split as many times as
	 * possible.<br />
	 * If the CharSequence does not contain the split then return a String of
	 * the CharSequence.
	 * 
	 * <pre>
	 * Example:
	 * &quot;abc/2s/4t&quot; with split '/' (at limit=-0) returns {&quot;abc&quot;, &quot;2s&quot;, &quot;4t&quot;}
	 * &quot;abc/2s/4t&quot; with split '/' at limit=2 returns {&quot;abc&quot;, &quot;2s/4t&quot;}
	 * &quot;tp.4e&quot; with split '/' returns {&quot;tp.4e&quot;}
	 * So &quot;&quot; with any split always returns {&quot;&quot;}
	 * &quot;:&quot; with split ':' returns {&quot;&quot;, &quot;&quot;}
	 * </pre>
	 * 
	 * @param sequence
	 *            The CharSequence that will be split.
	 * @param split
	 *            The Split signal such as '/' that is contained in
	 *            CharSequence.
	 * @param limit
	 *            The upper bound of split result number if limit is a positive
	 *            number.
	 * @return A String array contains these split string.
	 */
	public static String[] splitCharSequence(CharSequence sequence, char split, int limit)
	{
		List<String> splitString = new LinkedList<String>();

		int length = sequence.length();

		int last = 0;
		int index = seekIndex(sequence, split, last);

		while (index > -1)
		{

			if (limit > 0 && splitString.size() == limit - 1)
			{
				break;
			}

			splitString.add(sequence.subSequence(last, index).toString());

			last = index + 1;
			index = seekIndex(sequence, split, last);
		}

		splitString.add(sequence.subSequence(last, length).toString());

		String[] result = new String[splitString.size()];

		splitString.toArray(result);

		return result;
	}

	/**
	 * Split a CharSequence which is divided by split as many times as possible.<br />
	 * If the CharSequence does not contain the split then return a String of
	 * the CharSequence.
	 * 
	 * <pre>
	 * &quot;abc@=def&quot; with split &quot;@=&quot; returns {&quot;abc&quot;, &quot;def&quot;}
	 * &quot;abc@=def&quot; with split &quot;ab&quot; returns {&quot;&quot;, &quot;c@=def&quot;}
	 * &quot;abc@=def&quot; with split &quot;/=&quot; returns {&quot;abc@=def&quot;}
	 * So &quot;&quot; with any split always returns {&quot;&quot;}
	 * &quot;:&quot; with split &quot;:&quot; returns {&quot;&quot;, &quot;&quot;}
	 * Any String with split &quot;&quot; returns the String array of every char in the String
	 * &quot;String&quot; with split &quot;&quot; returns {&quot;S&quot;, &quot;t&quot;, &quot;r&quot;, &quot;i&quot;, &quot;n&quot;, &quot;g&quot;}
	 * </pre>
	 * 
	 * @param sequence
	 *            The CharSequence that will be split.
	 * @param split
	 *            The Split signal such as "@=" that is contained in
	 *            CharSequence.
	 * @return A String array contains these split string.
	 * @see Tools#splitCharSequence(CharSequence, String, int)
	 */
	public static String[] splitCharSequence(CharSequence sequence, CharSequence split)
	{
		return splitCharSequence(sequence, split, -0);
	}

	/**
	 * Split a CharSequence which is divided by split.<br />
	 * The limit parameter indicates that the parts of split result would not be
	 * greater than limit if limit was a positive number. If limit is a
	 * non-positive number, then the CharSequence will be split as many times as
	 * possible.<br />
	 * If the CharSequence does not contain the split then return a String of
	 * the CharSequence.
	 * 
	 * <pre>
	 * &quot;abc@=def&quot; with split &quot;@=&quot; (at limit=-0) returns {&quot;abc&quot;, &quot;def&quot;}
	 * &quot;abc@=def&quot; with split &quot;@=&quot; at limit=1 returns {&quot;abc@=def&quot;}
	 * &quot;abc@=def&quot; with split &quot;ab&quot; returns {&quot;&quot;, &quot;c@=def&quot;}
	 * &quot;abc@=def&quot; with split &quot;/=&quot; returns {&quot;abc@=def&quot;}
	 * So &quot;&quot; with any split always returns {&quot;&quot;}
	 * &quot;:&quot; with split &quot;:&quot; returns {&quot;&quot;, &quot;&quot;}
	 * Any String with split &quot;&quot; returns the String array of every char in the String
	 * &quot;String&quot; with split &quot;&quot; returns {&quot;S&quot;, &quot;t&quot;, &quot;r&quot;, &quot;i&quot;, &quot;n&quot;, &quot;g&quot;}
	 * </pre>
	 * 
	 * @param sequence
	 *            The CharSequence that will be split.
	 * @param splitThe
	 *            Split signal such as "@=" that is contained in CharSequence.
	 * @param limit
	 *            The upper bound of split result number if limit is a positive
	 *            number.
	 * @return A String array contains these split string.
	 */
	public static String[] splitCharSequence(CharSequence sequence, CharSequence split, int limit)
	{
		String[] result = null;

		LinkedList<String> splitString = new LinkedList<String>();

		int length = sequence.length();
		int space = split.length();

		if (split.equals(""))
		{

			result = new String[length];

			for (int i = 0; i < length; i++)
			{
				result[i] = String.valueOf(sequence.charAt(i));
			}

		}
		else
		{

			int last = 0;
			int index = seekIndex(sequence, split);

			while (index > -1)
			{

				if (limit > 0 && splitString.size() == limit - 1)
				{
					break;
				}

				splitString.add(sequence.subSequence(last, index).toString());

				last = index + space;
				index = seekIndex(sequence, split, last);
			}

			splitString.add(sequence.subSequence(last, length).toString());

			result = new String[splitString.size()];

			splitString.toArray(result);
		}

		return result;
	}

	/**
	 * Split a CharSequence into several small Strings whose length is according
	 * to a given length.
	 * 
	 * <pre>
	 * &quot;Let you know&quot; with each split length of 4 returns {&quot;Let &quot;,&quot;you &quot;,&quot;know&quot;}
	 * &quot;&quot; with any split length, always returns { } NOT { &quot;&quot; }
	 * </pre>
	 * 
	 * @param sequence
	 *            The CharSequence to be split.
	 * @param eachLength
	 *            A positive integer for each length of split String.
	 * @return A String array contains these split string.
	 */
	public static String[] splitCharSequence(CharSequence sequence, int eachLength)
	{
		int stringLength = sequence.length();

		String[] split = null;

		if (stringLength == 0 || eachLength < 0)
		{

			split = new String[] {};

		}
		else if (eachLength == stringLength || eachLength == 0)
		{

			split = new String[] { sequence.toString() };

		}
		else
		{

			split = new String[(stringLength - 1) / eachLength + 1];

			int begin = 0;

			int end = eachLength;

			for (int i = 0; i < split.length; i++)
			{

				if (end > stringLength)
				{
					end = stringLength;
				}

				split[i] = sequence.subSequence(begin, end).toString();

				begin = end;

				end += eachLength;
			}
		}

		return split;
	}

	/**
	 * To get the Subtraction of a-b.<br />
	 * That is all element in Iterable a but not in b.
	 * 
	 * @param <T>
	 *            the element type in both Collection.
	 * @param a
	 *            the Iterable object a.
	 * @param b
	 *            the Iterable object b.
	 * @param result
	 *            the Collection which holds the result. If null, a new
	 *            LinkedList would be used.
	 * @return the Subtraction of the a-b.<br />
	 *         Attention that the Subtraction result May contains DUPLICATE
	 *         elements. The result Collection should be a Set object to avoid
	 *         this case.
	 */
	public static <T> Collection<T> subtraction(Iterable<T> a, Iterable<T> b, Collection<T> result)
	{
		if (result == null)
		{
			result = new LinkedList<T>();
		}

		if (a != null)
		{
			for (T o : a)
			{
				if (!contains(b, o))
				{
					result.add(o);
				}
			}
		}

		return result;
	}

	/**
	 * To get the Union of Iterable object a and b.<br />
	 * That is all element in a or in b.
	 * 
	 * @param <T>
	 *            The element type in both Collection.
	 * @param a
	 *            The Iterable object a.
	 * @param b
	 *            The Iterable object b.
	 * @param result
	 *            the Collection which holds the result. If null, a new
	 *            LinkedList would be used.
	 * @return The Union of the iterable object a and b.<br />
	 *         Attention that the Union result May contains DUPLICATE elements.
	 *         The result Collection should be a Set object to avoid this case.
	 */
	public static <T> Collection<T> union(Iterable<T> a, Iterable<T> b, Collection<T> result)
	{
		if (result == null)
		{
			result = new LinkedList<T>();
		}

		if (result != a)
		{
			addAll(result, a);
		}
		if (result != b)
		{
			addAll(result, b);
		}

		return result;
	}

	/**
	 * Make a Vector of a given array.
	 * 
	 * @param <E>
	 *            The generic type of the elements in the array.
	 * @param array
	 *            The array to be convert to Vector
	 * @return A Vector that contains all the elements in the array.
	 */
	public static <E> Vector<E> vectorOfArray(E... array)
	{
		Vector<E> vector = new Vector<E>();

		for (E e : array)
		{
			vector.add(e);
		}

		return vector;
	}
}
