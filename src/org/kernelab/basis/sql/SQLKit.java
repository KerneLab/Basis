package org.kernelab.basis.sql;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.Accessor;
import org.kernelab.basis.Canal;
import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.JSON.Pair;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.TextFiller;
import org.kernelab.basis.Tools;

/**
 * It is known that one Connection produces a series of Statement and one
 * Statement makes one ResultSet as recommended.<br>
 * A SQLKit object is a tool kit to operate various database via given
 * Connection. It also contain the close method which close the Connection who
 * create a series of Statement to operate database.<br>
 * <br>
 * Before 2011.12.11 you can use a SQLKit object as<br>
 * 
 * <pre>
 * SQLKit kit=database.getSQLKit();
 * ...
 * try {
 * 	...
 * 	ResultSet rs = kit.query(sql);
 * 	while (rs.next()) {
 * 		...
 * 	}
 * 	...
 * 	kit.update(sql);
 * 	...
 * } catch (Exception e) {
 * 	...
 * } finally {
 * 	kit.close();
 * }
 * </pre>
 * 
 * Since 2011.12.11, the PreparedStatement began to be used to improve the
 * security and efficiency. So the usage should be follow
 * 
 * <pre>
 * SQLKit kit=database.getSQLKit();
 * ...
 * try {
 * 	...
 * 	String sql = &quot;SELECT * FROM `table` WHERE `id`&lt;? AND `name`=?&quot;;
 * 	ResultSet rs = kit.query(sql, 8, &quot;John&quot;);
 * 	while (rs.next()) {
 * 		...
 * 	}
 * 	...
 * 	sql = &quot;UPDATE `table` SET `name`=? WHERE `id`=?&quot;;
 * 	kit.update(sql, &quot;Tom&quot;, 6);
 * 	...
 * } catch (Exception e) {
 * 	...
 * } finally {
 * 	kit.close();	// THIS CLAUSE IS IMPORTANT!
 * }
 * </pre>
 * 
 * @author Dilly King
 */
public class SQLKit
{
	public static class ProjectMapper<E> implements Mapper<ResultSet, E>, Serializable
	{
		private static final long		serialVersionUID	= -6021148228942835875L;

		private Class<E>				cls;

		private Map<String, Object>		map;

		private Map<String, Accessor>	acs;

		public ProjectMapper(Class<E> cls, Map<String, Object> map)
		{
			this.cls = cls;
			this.map = map;
		}

		public E map(ResultSet rs)
		{
			try
			{
				if (this.map == null)
				{
					this.map = mapNameOfMetaData(rs.getMetaData());
				}

				boolean finding = false;

				if (this.acs == null)
				{
					this.acs = new LinkedHashMap<String, Accessor>();
					finding = true;
				}

				E obj = this.cls.newInstance();

				Accessor acs = null;
				String key = null;
				Object col = null, val = null;

				for (Entry<String, Object> entry : this.map.entrySet())
				{
					key = entry.getKey();
					col = entry.getValue();

					if (key != null && col != null)
					{
						if (finding)
						{
							acs = Accessor.Of(Tools.fieldOf(this.cls, key));
						}
						else
						{
							acs = this.acs.get(key);
						}

						if (acs != null)
						{
							val = col instanceof Integer //
									? rs.getObject((Integer) col) //
									: rs.getObject(col.toString());

							acs.set(obj, JSON.ProjectTo(val, acs.getField().getType(), val, null));

							if (finding)
							{
								this.acs.put(key, acs);
							}
						}
					}
				}

				return obj;
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	public static final char		VALUE_HOLDER_CHAR			= '?';

	public static final String		VALUE_HOLDER_MARK			= "?";

	/**
	 * Indicate that do not specify any value to the parameter which is useful
	 * when binding out parameter.
	 */
	public static final Object		NONE						= new JSON();

	/**
	 * In some sql, null parameter is required.<br />
	 * e.g. {@code update("INSERT INTO `table` (`id`,`name`) VALUES (?,?)",
	 * null, "John");}<br />
	 * <br />
	 * This sentence is OK. But, if there is no other parameter and null is the
	 * only one, it would be a problem.<br />
	 * e.g. {@code query("SELECT * FROM `table` WHERE `name`!=?",null);}<br />
	 * <br />
	 * In this case, we can use NULL Object.<br />
	 * That is
	 * {@code query("SELECT * FROM `table` WHERE `name`!=?",SQLKit.NULL);}
	 * 
	 */
	public static final Object		NULL						= new JSON();

	public static final String		NULL_MARK					= "NULL";

	/**
	 * To declare that there is no parameter.<br />
	 * This object would be ignored if it is not the first parameter.
	 */
	public static final Object		EMPTY						= new JSON();

	public static final byte		OPTIMIZING_AS_DEFAULT		= 0;

	public static final byte		OPTIMIZING_AS_TRACER		= 1;

	public static final byte		OPTIMIZING_AS_SENTRY		= 2;

	public static final byte		OPTIMIZING_AS_ARCHER		= 3;

	public static final byte		OPTIMIZING_AS_HUNTER		= 4;

	protected static final int[][]	OPTIMIZING_PRESET_SCHEMES	= {
			{ ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			{ ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			{ ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			{ ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			{ ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT } };

	/**
	 * When SQLKit being initialized, Connection must be provided by the
	 * ConnectionManager. If none Connection could be provided, SQLException
	 * would be thrown with this message.
	 */
	public static final String		ERR_NO_DB_CONN				= "No available database connection.";

	public static PreparedStatement bindParameters(int offset, PreparedStatement statement, Iterable<?> params)
			throws SQLException
	{
		if (statement != null && params != null && params.iterator().hasNext())
		{
			int from = 1 + offset;
			int index = from;
			for (Object param : params)
			{
				if (param == EMPTY)
				{
					if (index != from)
					{
						throw new SQLException("SQLKit.EMPTY should be the first parameter.");
					}
					break;
				}
				else
				{
					if (param == NULL)
					{
						statement.setObject(index, null);
					}
					else if (param != NONE)
					{
						statement.setObject(index, param);
					}
					index++;
				}
			}
		}
		return statement;
	}

	public static PreparedStatement bindParameters(int offset, PreparedStatement statement, JSAN params)
			throws SQLException
	{
		return bindParameters(offset, statement, (Iterable<?>) params);
	}

	public static PreparedStatement bindParameters(int offset, PreparedStatement statement, Object... params)
			throws SQLException
	{
		if (statement != null && params != null && params.length > 0)
		{
			int from = 1 + offset;
			int index = from;
			for (Object param : params)
			{
				if (param == EMPTY)
				{
					if (index != from)
					{
						throw new SQLException("SQLKit.EMPTY should be the first parameter.");
					}
					break;
				}
				else
				{
					if (param == NULL)
					{
						statement.setObject(index, null);
					}
					else if (param != NONE)
					{
						statement.setObject(index, param);
					}
					index++;
				}
			}
		}
		return statement;
	}

	public static PreparedStatement bindParameters(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		return bindParameters(0, statement, params);
	}

	public static PreparedStatement bindParameters(PreparedStatement statement, JSAN params) throws SQLException
	{
		return bindParameters(statement, (Iterable<?>) params);
	}

	public static PreparedStatement bindParameters(PreparedStatement statement, Object... params) throws SQLException
	{
		return bindParameters(0, statement, params);
	}

	public static void fillParameter(Object value, List<Object> list)
	{
		if (value == SQLKit.NULL || value == SQLKit.NONE || value == SQLKit.EMPTY)
		{
			list.add(value);
		}
		else if (value instanceof Iterable)
		{
			for (Object v : (Iterable<?>) value)
			{
				fillParameter(v, list);
			}
		}
		else if (value != null && value.getClass().isArray())
		{
			int len = Array.getLength(value);
			for (int i = 0; i < len; i++)
			{
				fillParameter(Array.get(value, i), list);
			}
		}
		else
		{
			list.add(value);
		}
	}

	/**
	 * To fill a list of parameters according to the keys which are the names of
	 * parameters.
	 * 
	 * @param keys
	 *            The parameters' name.
	 * @param params
	 *            The parameters in form of key/value organized by JSON.
	 * @return The parameters value list.
	 * @see SQLKit#fillParameters(Iterable, Map, List)
	 */
	public static List<Object> fillParameters(Iterable<String> keys, JSON params)
	{
		return fillParameters(keys, params, null);
	}

	/**
	 * To fill a list of parameters according to the keys which are the names of
	 * parameters.
	 * 
	 * @param keys
	 *            The parameters' name.
	 * @param params
	 *            The parameters in form of key/value organized by JSON.
	 * @param list
	 *            The List&lt;Object&gt; object which holds the result list. If
	 *            null then a new LinkedList&lt;Object&gt; would be created
	 *            instead.
	 * 
	 * @return The parameters value list.
	 */
	public static List<Object> fillParameters(Iterable<String> keys, JSON params, List<Object> list)
	{
		if (list == null)
		{
			list = new LinkedList<Object>();
		}
		else
		{
			list.clear();
		}

		for (String key : keys)
		{
			fillParameter(params.attr(key), list);
		}

		return list;
	}

	/**
	 * To fill a list of parameters according to the keys which are the names of
	 * parameters.
	 * 
	 * @param keys
	 *            The parameters' name.
	 * @param params
	 *            The parameters in form of key/value organized by
	 *            Map&lt;String,Object&gt;.
	 * @return The parameters value list.
	 * @see SQLKit#fillParameters(Iterable, Map, List)
	 */
	public static List<Object> fillParameters(Iterable<String> keys, Map<String, ?> params)
	{
		return fillParameters(keys, params, null);
	}

	/**
	 * To fill a list of parameters according to the keys which are the names of
	 * parameters.
	 * 
	 * @param keys
	 *            The parameters' name.
	 * @param params
	 *            The parameters in form of key/value organized by
	 *            Map&lt;String,Object&gt;.
	 * @param list
	 *            The List&lt;Object&gt; object which holds the result list. If
	 *            null then a new LinkedList&lt;Object&gt; would be created
	 *            instead.
	 * 
	 * @return The parameters value list.
	 */
	public static List<Object> fillParameters(Iterable<String> keys, Map<String, ?> params, List<Object> list)
	{
		if (list == null)
		{
			list = new LinkedList<Object>();
		}
		else
		{
			list.clear();
		}

		for (String key : keys)
		{
			fillParameter(params.get(key), list);
		}

		return list;
	}

	/**
	 * Generate a map according to the given underline style naming columns. The
	 * keys in result map will be camel style.
	 * 
	 * @param underlineStyleColumns
	 *            Underline style naming columns.
	 * @return A map which is useful to reflect query result row into java
	 *         object.
	 */
	public static Map<String, Object> generateCamelStyleKeyMap(Iterable<String> underlineStyleColumns)
	{
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		for (String column : underlineStyleColumns)
		{
			map.put(Tools.mapUnderlineNamingToCamelStyle(column), column);
		}

		return map;
	}

	/**
	 * Generate a map according to the given underline style naming columns. The
	 * keys in result map will be camel style.
	 * 
	 * @param underlineStyleColumns
	 *            Underline style naming columns.
	 * @return A map which is useful to reflect query result row into java
	 *         object.
	 */
	public static Map<String, Object> generateCamelStyleKeyMap(String... underlineStyleColumns)
	{
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		for (String column : underlineStyleColumns)
		{
			map.put(Tools.mapUnderlineNamingToCamelStyle(column), column);
		}

		return map;
	}

	/**
	 * Get the result number of a ResultSet.
	 * 
	 * @param rs
	 *            The ResultSet.
	 * @return The number of result in the ResultSet
	 */
	public static int getResultNumber(ResultSet rs)
	{
		int number = -1;

		if (rs != null)
		{
			try
			{
				int row = rs.getRow();
				rs.last();
				number = rs.getRow();

				if (row == 0)
				{
					rs.beforeFirst();
				}
				else
				{
					rs.absolute(row);
				}
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}

		return number;
	}

	/**
	 * To index each parameter's position in the SQL.
	 * 
	 * @param sql
	 *            The SQL String.
	 * @param boundary
	 *            The boundary string of each parameter.
	 * @param params
	 *            The parameters name Collection.
	 * @return The index of each parameter.
	 * @see SQLKit#indexOfParameters(String, Iterable, TreeMap)
	 */
	public static TreeMap<Integer, String> indexOfParameters(String sql, String boundary, Iterable<String> params)
	{
		return indexOfParameters(sql, boundary, params, null);
	}

	/**
	 * To index each parameter's position in the SQL.
	 * 
	 * @param sql
	 *            The SQL String.
	 * @param boundary
	 *            The boundary string of each parameter.
	 * @param params
	 *            The parameters name Collection.
	 * @param index
	 *            The TreeMap&lt;Integer,String&gt; object which holds the
	 *            index. If null, then a new TreeMap&lt;Integer,String&gt; would
	 *            be created instead.
	 * @return The index of each parameter.
	 */
	public static TreeMap<Integer, String> indexOfParameters(String sql, String boundary, Iterable<String> params,
			TreeMap<Integer, String> index)
	{
		if (index == null)
		{
			index = new TreeMap<Integer, String>();
		}
		else
		{
			index.clear();
		}
		for (String param : params)
		{
			Matcher m = Pattern.compile(Pattern.quote(boundary + param + boundary)).matcher(sql);

			while (m.find())
			{
				index.put(m.start(), param);
			}
		}
		return index;
	}

	/**
	 * To convert the current single row in ResultSet to a JSAN object according
	 * to a given map which must not be null.
	 * 
	 * @param rs
	 *            The ResultSet.
	 * @param map
	 *            The Map<String,Object> which describe the relationship of each
	 *            column between the ResultSet and JSON object.
	 * @return The JSAN object.
	 * @throws SQLException
	 */
	public static JSAN jsanOfResultRow(ResultSet rs, Map<String, Object> map) throws SQLException
	{
		return (JSAN) jsonOfResultRow(rs, new JSAN(), map);
	}

	/**
	 * To read rows in a ResultSet into a JSAN object.
	 * 
	 * @param rs
	 *            The ResultSet.
	 * @param jsan
	 *            The JSAN object to hold the ResultSet. If null then an empty
	 *            JSAN would be created instead.
	 * @param map
	 *            The Map<String,Object> which describe the relationship of each
	 *            column between the ResultSet and JSON object. If null then the
	 *            full map would be read from the meta data, the columns' index
	 *            would be mapped in case that the cls is JSAN, otherwise the
	 *            name would be mapped.
	 * @param cls
	 *            The Class which indicates what data type that each row would
	 *            be converted to. If null then JSAN would be used as default.
	 * @return The JSAN object.
	 * @throws SQLException
	 */
	public static JSAN jsanOfResultSet(ResultSet rs, JSAN jsan, Map<String, Object> map, Class<? extends JSON> cls)
			throws SQLException
	{
		return jsanOfResultSet(rs, jsan, map, cls, -1);
	}

	/**
	 * To read rows in a ResultSet into a JSAN object.
	 * 
	 * @param rs
	 *            The ResultSet.
	 * @param jsan
	 *            The JSAN object to hold the ResultSet. If null then an empty
	 *            JSAN would be created instead.
	 * @param map
	 *            The Map<String,Object> which describe the relationship of each
	 *            column between the ResultSet and JSON object. If null then the
	 *            full map would be read from the meta data, the columns' index
	 *            would be mapped in case that the cls is JSAN, otherwise the
	 *            name would be mapped.
	 * @param cls
	 *            The Class which indicates what data type that each row would
	 *            be converted to. If null then JSAN would be used as default.
	 * @param limit
	 *            The max rows would be returned. This parameter will be ignored
	 *            if {@code limit < 0} which means all rows will be returned.
	 * @return The JSAN object.
	 * @throws SQLException
	 */
	public static JSAN jsanOfResultSet(ResultSet rs, JSAN jsan, Map<String, Object> map, Class<? extends JSON> cls,
			int limit) throws SQLException
	{
		if (rs != null)
		{
			if (jsan == null)
			{
				jsan = new JSAN();
			}
			if (cls == null)
			{
				cls = JSAN.class;
			}
			if (map == null)
			{
				if (cls == JSAN.class || Tools.isSubClass(cls, JSAN.class))
				{
					map = mapIndexOfMetaData(rs.getMetaData());
				}
				else
				{
					map = mapNameOfMetaData(rs.getMetaData());
				}
			}
			try
			{
				int count = 0;
				while ((limit < 0 || count < limit) && rs.next())
				{
					jsan.add(jsonOfResultRow(rs, cls.newInstance().templates(jsan), map));
					count++;
				}
			}
			catch (InstantiationException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
		}
		return jsan;
	}

	/**
	 * To convert the current single row in ResultSet to a JSON object according
	 * to a given map which must not be null.
	 * 
	 * @param rs
	 *            The ResultSet.
	 * @param json
	 *            The JSON object to hold the data. If null then an empty JSON
	 *            object would be created instead.
	 * @param map
	 *            The Map<String,Object> which describe the relationship of each
	 *            column between the ResultSet and JSON object.
	 * @return The JSON object.
	 * @throws SQLException
	 */
	public static JSON jsonOfResultRow(ResultSet rs, JSON json, Map<String, Object> map) throws SQLException
	{
		if (rs != null && map != null)
		{
			if (json == null)
			{
				json = new JSON();
			}
			for (Entry<String, Object> entry : map.entrySet())
			{
				String key = entry.getKey();
				Object val = entry.getValue();
				if (key != null && val != null)
				{
					if (val instanceof Integer)
					{
						json.attr(key, rs.getObject((Integer) val));
					}
					else
					{
						json.attr(key, rs.getObject(val.toString()));
					}
				}
			}
		}
		return json;
	}

	/**
	 * To convert the current single row in ResultSet to a JSON object according
	 * to a given map which must not be null.
	 * 
	 * @param rs
	 *            The ResultSet.
	 * @param map
	 *            The Map<String,Object> which describe the relationship of each
	 *            column between the ResultSet and JSON object.
	 * @return The JSON object.
	 * @throws SQLException
	 */
	public static JSON jsonOfResultRow(ResultSet rs, Map<String, Object> map) throws SQLException
	{
		return jsonOfResultRow(rs, null, map);
	}

	public static Canal<?, JSON> jsonOfResultSet(SQLKit kit, final ResultSet rs, final Map<String, Object> map,
			final Class<? extends JSON> cls) throws SQLException
	{
		return Canal.of(Sequel.iterate(rs).kit(kit)).map(new Mapper<ResultSet, JSON>()
		{
			@Override
			public JSON map(ResultSet el)
			{
				try
				{
					return jsonOfResultRow(rs, (cls != null ? cls : JSON.class).newInstance(), map);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
		});
	}

	/**
	 * List the column names.
	 * 
	 * @param meta
	 *            The ResultSetMetaData.
	 * @return The List of the column names.
	 */
	public static List<String> listNameOfMetaData(ResultSetMetaData meta)
	{
		return listNameOfMetaData(meta, null);
	}

	/**
	 * List the column names.
	 * 
	 * @param meta
	 *            The ResultSetMetaData.
	 * @param index
	 *            The List<Integer> which indicates the indexes of columns' name
	 *            to be read. If null then all of the columns's name would be
	 *            listed.
	 * @return The List of the column names.
	 */
	public static List<String> listNameOfMetaData(ResultSetMetaData meta, List<Integer> index)
	{
		List<String> list = null;

		if (meta != null)
		{
			try
			{
				list = new LinkedList<String>();

				if (index != null)
				{
					for (Integer c : index)
					{
						if (c != null)
						{
							list.add(meta.getColumnLabel(c));
						}
					}
				}
				else
				{
					int columns = meta.getColumnCount();

					for (int c = 1; c <= columns; c++)
					{
						list.add(meta.getColumnLabel(c));
					}
				}
			}
			catch (SQLException e)
			{
			}
		}

		return list;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
	}

	/**
	 * Make insert SQL text according to a give table name and column names.
	 * 
	 * @param table
	 *            The table name.
	 * @param columns
	 *            The column names.
	 * @return The insert SQL text.
	 */
	public static String makeInsertSQL(String table, Iterable<String> columns)
	{
		if (table != null && columns != null)
		{
			StringBuilder keys = new StringBuilder();
			StringBuilder vals = new StringBuilder();

			boolean first = true;
			for (String column : columns)
			{
				if (first)
				{
					first = false;
				}
				else
				{
					keys.append(',');
					vals.append(',');
				}
				keys.append(column);
				vals.append(VALUE_HOLDER_CHAR);
				vals.append(column);
				vals.append(VALUE_HOLDER_CHAR);
			}

			StringBuilder buffer = new StringBuilder(keys.length() + vals.length() + 25);

			buffer.append("INSERT INTO ");
			buffer.append(table);
			buffer.append(" (");
			buffer.append(keys);
			buffer.append(") VALUES (");
			buffer.append(vals);
			buffer.append(')');

			return buffer.toString();
		}
		else
		{
			return null;
		}
	}

	/**
	 * Map data according to the given mapping. The keys in the mapping will be
	 * the keys in the result data. The values in the mapping should be the keys
	 * in the source data.
	 * 
	 * @param data
	 *            The source data.
	 * @param map
	 *            The mapping. The source data will be the result if the mapping
	 *            is null.
	 * @param result
	 *            The result data. A new JSON will be created if null.
	 * @return The result data.
	 */
	public static JSON mapData(JSON data, Map<String, String> map, JSON result)
	{
		if (data != null)
		{
			if (map != null)
			{
				if (result == null)
				{
					result = new JSON();
				}

				for (Entry<String, String> m : map.entrySet())
				{
					result.attr(m.getKey(), data.val(m.getValue()));
				}
			}
			else
			{
				if (result != null)
				{
					result.attrAll(data);
				}
				else
				{
					result = data;
				}
			}
		}
		return result;
	}

	/**
	 * Map data according to the given mapping. The keys in the mapping will be
	 * the keys in the result data. The values in the mapping should be the keys
	 * in the source data.
	 * 
	 * @param data
	 *            The source data.
	 * @param map
	 *            The mapping. The source data will be the result if the mapping
	 *            is null.
	 * @param result
	 *            The result data. A new LinkedHashMap will be created if null.
	 * @return The result data.
	 */
	public static Map<String, Object> mapData(Map<String, Object> data, Map<String, String> map,
			Map<String, Object> result)
	{
		if (data != null)
		{
			if (map != null)
			{
				if (result == null)
				{
					result = new LinkedHashMap<String, Object>();
				}

				for (Entry<String, String> m : map.entrySet())
				{
					result.put(m.getKey(), data.get(m.getValue()));
				}
			}
			else
			{
				if (result != null)
				{
					result.putAll(data);
				}
				else
				{
					result = data;
				}
			}
		}
		return result;
	}

	/**
	 * Map the columns' name using the column indexes begin with ZERO.
	 * 
	 * @param meta
	 *            The ResultSetMetaData.
	 * @return The map of column index against the column name.
	 * @throws SQLException
	 */
	public static Map<String, Object> mapIndexOfMetaData(ResultSetMetaData meta) throws SQLException
	{
		return mapIndexOfMetaData(meta, null);
	}

	/**
	 * Map the columns' name using the column indexes according to the given
	 * columns' index. The key in result map starts with ZERO.
	 * 
	 * @param meta
	 *            The ResultSetMetaData.
	 * @param index
	 *            The List<Integer> which indicates the indexes of columns to be
	 *            mapped. If null then all of the columns would be mapped.
	 * @return The map of column index against the column name.
	 * @throws SQLException
	 */
	public static Map<String, Object> mapIndexOfMetaData(ResultSetMetaData meta, List<Integer> index)
			throws SQLException
	{
		Map<String, Object> map = null;

		if (meta != null)
		{
			map = new LinkedHashMap<String, Object>();

			if (index != null)
			{
				int i = 0;

				for (Integer c : index)
				{
					if (c != null)
					{
						map.put(String.valueOf(i), c);
						i++;
					}
				}
			}
			else
			{
				int columns = meta.getColumnCount();

				for (int c = 1; c <= columns; c++)
				{
					map.put(String.valueOf(c - 1), c);
				}
			}
		}

		return map;
	}

	/**
	 * Map the columns' name using the column names.
	 * 
	 * @param meta
	 *            The ResultSetMetaData.
	 * @return The map of column name against the column index.
	 * @throws SQLException
	 */
	public static Map<String, Object> mapNameOfMetaData(ResultSetMetaData meta) throws SQLException
	{
		return mapNameOfMetaData(meta, null);
	}

	/**
	 * Map the columns' name using the column names according to the given
	 * columns' index.
	 * 
	 * @param meta
	 *            The ResultSetMetaData.
	 * @param index
	 *            The List<Integer> which indicates the indexes of columns to be
	 *            mapped. If null then all of the columns would be mapped.
	 * @return The map of column name against the column index.
	 * @throws SQLException
	 */
	public static Map<String, Object> mapNameOfMetaData(ResultSetMetaData meta, List<Integer> index) throws SQLException
	{
		Map<String, Object> map = null;

		if (meta != null)
		{
			map = new LinkedHashMap<String, Object>();

			if (index != null)
			{
				for (Integer c : index)
				{
					if (c != null)
					{
						map.put(meta.getColumnLabel(c), c);
					}
				}
			}
			else
			{
				int columns = meta.getColumnCount();

				for (int c = 1; c <= columns; c++)
				{
					map.put(meta.getColumnLabel(c), c);
				}
			}
		}

		return map;
	}

	public static <E> E mapResultRow(ResultSet rs, Class<E> cls, Map<String, Object> map)
	{
		return mapResultRow(rs, new ProjectMapper<E>(cls, map));
	}

	public static <E> E mapResultRow(ResultSet rs, Mapper<ResultSet, E> mapper)
	{
		if (rs != null && mapper != null)
		{
			try
			{
				return mapper.map(rs);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		else
		{
			return null;
		}
	}

	public static <E> Collection<E> mapResultSet(ResultSet rs, Collection<E> rows, Class<E> cls,
			Map<String, Object> map, int limit) throws SQLException
	{
		return mapResultSet(rs, rows, new ProjectMapper<E>(cls, map), limit);
	}

	public static <E> Collection<E> mapResultSet(ResultSet rs, Collection<E> rows, Mapper<ResultSet, E> mapper,
			int limit) throws SQLException
	{
		if (rs != null)
		{
			if (rows == null)
			{
				rows = new LinkedList<E>();
			}
			int count = 0;
			try
			{
				while ((limit < 0 || count < limit) && rs.next())
				{
					rows.add(mapper == null ? null : mapper.map(rs));
					count++;
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		return rows;
	}

	public static <E> Canal<?, E> mapResultSet(SQLKit kit, ResultSet rs, Class<E> cls, Map<String, Object> map)
			throws SQLException
	{
		return mapResultSet(kit, rs, new ProjectMapper<E>(cls, map));
	}

	public static <E> Canal<?, E> mapResultSet(SQLKit kit, ResultSet rs, Mapper<ResultSet, E> mapper)
			throws SQLException
	{
		return Canal.of(Sequel.iterate(rs).kit(kit)).map(mapper);
	}

	public static String replaceParameter(Object value)
	{
		if (value == SQLKit.NULL || value == SQLKit.NONE || value == SQLKit.EMPTY)
		{
			return VALUE_HOLDER_MARK;
		}
		else if (value instanceof Iterable)
		{
			StringBuilder buf = null;
			for (Object v : (Iterable<?>) value)
			{
				if (buf == null)
				{
					buf = new StringBuilder();
				}
				else
				{
					buf.append(',');
				}
				replaceParameter(buf, v);
			}
			if (buf == null)
			{
				return NULL_MARK;
			}
			else
			{
				return buf.toString();
			}
		}
		else if (value != null && value.getClass().isArray())
		{
			int len = Array.getLength(value);
			if (len == 0)
			{
				return NULL_MARK;
			}
			else
			{
				StringBuilder buf = new StringBuilder();
				for (int i = 0; i < len; i++)
				{
					if (i > 0)
					{
						buf.append(',');
					}
					replaceParameter(buf, Array.get(value, i));
				}
				return buf.toString();
			}
		}
		else
		{
			return VALUE_HOLDER_MARK;
		}
	}

	public static void replaceParameter(StringBuilder buf, Object value)
	{
		if (value == SQLKit.NULL || value == SQLKit.NONE || value == SQLKit.EMPTY)
		{
			buf.append(VALUE_HOLDER_MARK);
		}
		else if (value instanceof Iterable)
		{
			buf.append('(');
			boolean empty = true;
			for (Object v : (Iterable<?>) value)
			{
				if (empty)
				{
					empty = false;
				}
				else
				{
					buf.append(',');
				}
				replaceParameter(buf, v);
			}
			if (empty)
			{
				buf.append(NULL_MARK);
			}
			buf.append(')');
		}
		else if (value != null && value.getClass().isArray())
		{
			buf.append('(');
			int len = Array.getLength(value);
			if (len == 0)
			{
				buf.append(NULL_MARK);
			}
			else
			{
				for (int i = 0; i < len; i++)
				{
					if (i > 0)
					{
						buf.append(',');
					}
					replaceParameter(buf, Array.get(value, i));
				}
			}
			buf.append(')');
		}
		else
		{
			buf.append(VALUE_HOLDER_MARK);
		}
	}

	/**
	 * Replace each parameters name in SQL with the VALUE_HOLDER in order to be
	 * prepared.
	 * 
	 * @param sql
	 *            The SQL String.
	 * @param boundary
	 *            The boundary string of each parameter.
	 * @param params
	 *            The names of parameters.
	 * @return The SQL String which could be prepared.
	 */
	public static String replaceParameters(String sql, String boundary, Iterable<String> params)
	{
		TextFiller filler = new TextFiller(sql, boundary).reset();

		for (String key : params)
		{
			filler.fillWith(key, VALUE_HOLDER_MARK);
		}

		return filler.toString();
	}

	/**
	 * Replace each parameters name in SQL with the VALUE_HOLDER in order to be
	 * prepared. This method would expand the VALUE_HOLDER if the corresponding
	 * value is an Iterable object.
	 * 
	 * @param sql
	 *            The SQL String.
	 * @param boundary
	 *            The boundary string of each parameter.
	 * @param params
	 *            The parameters in form of key/value organized by JSON.
	 * @return The SQL String which could be prepared.
	 */
	public static String replaceParameters(String sql, String boundary, JSON params)
	{
		TextFiller filler = new TextFiller(sql, boundary).reset();

		for (Pair pair : params.pairs())
		{
			filler.fillWith(pair.getKey(), replaceParameter(pair.getValue()));
		}

		return filler.toString();
	}

	/**
	 * Replace each parameters name in SQL with the VALUE_HOLDER in order to be
	 * prepared. This method would expand the VALUE_HOLDER if the corresponding
	 * value is an Iterable or Array object.
	 * 
	 * @param sql
	 *            The SQL String.
	 * @param boundary
	 *            The boundary string of each parameter.
	 * @param params
	 *            The parameters in form of key/value organized by Map
	 *            <String,Object>.
	 * @return The SQL String which could be prepared.
	 */
	public static String replaceParameters(String sql, String boundary, Map<String, ?> params)
	{
		TextFiller filler = new TextFiller(sql, boundary).reset();

		for (Entry<String, ?> pair : params.entrySet())
		{
			filler.fillWith(pair.getKey(), replaceParameter(pair.getValue()));
		}

		return filler.toString();
	}

	public static Connection reset(Connection conn) throws SQLException
	{
		if (!conn.getAutoCommit())
		{
			conn.setAutoCommit(true);
		}
		conn.setReadOnly(false);
		conn.setTransactionIsolation(conn.getMetaData().getDefaultTransactionIsolation());
		return conn;
	}

	private ConnectionManager			manager;

	private Connection					connection;

	private Class<? extends Connection>	unwrap;

	private Connection					real;

	private Statement					statement;

	private Map<String, Statement>		sentences;

	private Map<Statement, String>		statements;

	private Map<String, List<String>>	parameters;

	private String						boundary;

	private boolean						reuseStatements			= true;

	private int							resultSetType			= OPTIMIZING_PRESET_SCHEMES[OPTIMIZING_AS_DEFAULT][0];

	private int							resultSetConcurrency	= OPTIMIZING_PRESET_SCHEMES[OPTIMIZING_AS_DEFAULT][1];

	private int							resultSetHoldability	= OPTIMIZING_PRESET_SCHEMES[OPTIMIZING_AS_DEFAULT][2];

	public SQLKit(ConnectionManager manager) throws SQLException
	{
		this(manager, 0L);
	}

	public SQLKit(ConnectionManager manager, long timeout) throws SQLException
	{
		Connection connection = manager.provideConnection(timeout);
		if (connection == null)
		{
			throw new SQLException(ERR_NO_DB_CONN);
		}
		this.setConnectionOrigin(connection);
		this.setManager(manager);
		this.setBoundary(TextFiller.DEFAULT_BOUNDARY);
		this.setSentences(new HashMap<String, Statement>());
		this.setStatements(new HashMap<Statement, String>());
		this.setParameters(new HashMap<String, List<String>>());
	}

	public void addBatch(Iterable<?> params) throws SQLException
	{
		this.addBatch((PreparedStatement) statement, params);
	}

	public void addBatch(JSON params) throws SQLException
	{
		this.addBatch((PreparedStatement) statement, params);
	}

	public void addBatch(Map<String, ?> params) throws SQLException
	{
		this.addBatch((PreparedStatement) statement, params);
	}

	public void addBatch(Object... params) throws SQLException
	{
		this.addBatch((PreparedStatement) statement, params);
	}

	public void addBatch(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		bindParameters(statement, params);
		statement.addBatch();
	}

	public void addBatch(PreparedStatement statement, JSAN params) throws SQLException
	{
		addBatch(statement, (Iterable<?>) params);
	}

	public void addBatch(PreparedStatement statement, JSON params) throws SQLException
	{
		bindParameters(statement, fillParameters(getParameter(statement), params));
		statement.addBatch();
	}

	public void addBatch(PreparedStatement statement, Map<String, ?> params) throws SQLException
	{
		bindParameters(statement, fillParameters(getParameter(statement), params));
		statement.addBatch();
	}

	public void addBatch(PreparedStatement statement, Object... params) throws SQLException
	{
		bindParameters(statement, params);
		statement.addBatch();
	}

	public void addBatch(String sql) throws SQLException
	{
		statement.addBatch(sql);
	}

	public PreparedStatement bindParameters(int offset, Iterable<?> params) throws SQLException
	{
		return bindParameters(offset, (PreparedStatement) statement, params);
	}

	public PreparedStatement bindParameters(int offset, JSAN params) throws SQLException
	{
		return bindParameters(offset, (Iterable<?>) params);
	}

	public PreparedStatement bindParameters(int offset, Object... params) throws SQLException
	{
		return bindParameters(offset, (PreparedStatement) statement, params);
	}

	public PreparedStatement bindParameters(Iterable<?> params) throws SQLException
	{
		return bindParameters(0, params);
	}

	public PreparedStatement bindParameters(JSAN params) throws SQLException
	{
		return bindParameters((Iterable<?>) params);
	}

	public PreparedStatement bindParameters(Object... params) throws SQLException
	{
		return bindParameters(0, params);
	}

	public void cancel() throws SQLException
	{
		cancel(statement);
	}

	public void cancel(Statement statement) throws SQLException
	{
		if (statement != null)
		{
			statement.cancel();
		}
	}

	public SQLKit clean()
	{
		if (statement != null)
		{
			try
			{
				clearBatch(statement);
			}
			catch (Exception e)
			{
			}
			statement = null;
		}
		if (sentences != null)
		{
			sentences.clear();
		}
		if (statements != null)
		{
			for (Statement s : statements.keySet())
			{
				try
				{
					s.close();
				}
				catch (Exception e)
				{
				}
			}
			statements.clear();
		}
		if (parameters != null)
		{
			parameters.clear();
		}
		return this;
	}

	public void clearBatch() throws SQLException
	{
		this.clearBatch(statement);
	}

	public void clearBatch(Statement statement) throws SQLException
	{
		if (statement != null)
		{
			statement.clearBatch();
		}
	}

	public void close()
	{
		ConnectionManager manager = this.getManager();

		if (manager instanceof SQLKitPool)
		{
			((SQLKitPool) manager).recycle(this);
		}
		else
		{
			this.destroy();
		}
	}

	public SQLKit closeStatement() throws SQLException
	{
		return closeStatement(statement);
	}

	public SQLKit closeStatement(Statement statement) throws SQLException
	{
		if (statement != null && !isReuseStatements())
		{
			sentences.remove(statements.remove(statement));
			statement.close();
		}
		return this;
	}

	public void commit() throws SQLException
	{
		if (!this.isAutoCommit())
		{
			this.getConnection().commit();
		}
	}

	/**
	 * Execute a batch of commands, commit and clear the commands.<br />
	 * Attention that setAutoCommit(false) should be called before preparing
	 * statement for these commands.
	 * 
	 * @throws SQLException
	 */
	public void commitBatch() throws SQLException
	{
		this.commitBatch(statement);
	}

	public void commitBatch(Statement statement) throws SQLException
	{
		this.executeBatch(statement);
		this.commit();
		this.clearBatch(statement);
	}

	public Statement createStatement(String sql) throws SQLException
	{
		return createStatement(sql, resultSetType);
	}

	public Statement createStatement(String sql, int resultSetType) throws SQLException
	{
		return createStatement(sql, resultSetType, resultSetConcurrency);
	}

	public Statement createStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
	{
		return createStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public Statement createStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				try
				{
					statement = this.getConnection().createStatement(resultSetType, resultSetConcurrency,
							resultSetHoldability);
				}
				catch (SQLException e)
				{
					statement = this.getConnection().createStatement();
				}
				statements.put(statement, sql);
				sentences.put(sql, statement);
			}
		}
		else
		{
			try
			{
				statement = this.getConnection().createStatement(resultSetType, resultSetConcurrency,
						resultSetHoldability);
			}
			catch (SQLException e)
			{
				statement = this.getConnection().createStatement();
			}
			statements.put(statement, sql);
		}

		return statement;
	}

	protected void destroy()
	{
		clean();
		this.sentences = null;
		this.statements = null;
		this.parameters = null;
		if (this.manager != null)
		{
			try
			{
				this.manager.recycleConnection(this.connection);
			}
			catch (SQLException e)
			{
			}
			this.manager = null;
		}
		this.connection = null;
		this.unwrap = null;
		this.real = null;
	}

	public Sequel execute(CallableStatement statement, Iterable<?> params) throws SQLException
	{
		return new Sequel(this, statement, bindParameters(statement, params).execute());
	}

	public Sequel execute(CallableStatement statement, JSAN params) throws SQLException
	{
		return execute(statement, (Iterable<?>) params);
	}

	public Sequel execute(CallableStatement statement, JSON params) throws SQLException
	{
		return execute(statement, fillParameters(getParameter(statement), params));
	}

	public Sequel execute(CallableStatement statement, Map<String, ?> params) throws SQLException
	{
		return execute(statement, fillParameters(getParameter(statement), params));
	}

	public Sequel execute(CallableStatement statement, Object... params) throws SQLException
	{
		return new Sequel(this, statement, bindParameters(statement, params).execute());
	}

	public Sequel execute(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		return new Sequel(this, statement, bindParameters(statement, params).execute());
	}

	public Sequel execute(PreparedStatement statement, JSAN params) throws SQLException
	{
		return execute(statement, (Iterable<?>) params);
	}

	public Sequel execute(PreparedStatement statement, JSON params) throws SQLException
	{
		return execute(statement, fillParameters(getParameter(statement), params));
	}

	public Sequel execute(PreparedStatement statement, Map<String, ?> params) throws SQLException
	{
		return execute(statement, fillParameters(getParameter(statement), params));
	}

	public Sequel execute(PreparedStatement statement, Object... params) throws SQLException
	{
		return new Sequel(this, statement, bindParameters(statement, params).execute());
	}

	protected Sequel execute(Statement statement, String sql) throws SQLException
	{
		return new Sequel(this, statement, statement.execute(sql));
	}

	public Sequel execute(String sql) throws SQLException
	{
		return execute(createStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql);
	}

	public Sequel execute(String sql, boolean autoGeneratedKeys) throws SQLException
	{
		return execute(prepareStatement(sql, autoGeneratedKeys));
	}

	public Sequel execute(String sql, boolean autoGeneratedKeys, Iterable<?> params) throws SQLException
	{
		return execute(prepareStatement(sql, autoGeneratedKeys), params);
	}

	public Sequel execute(String sql, boolean autoGeneratedKeys, JSAN params) throws SQLException
	{
		return execute(prepareStatement(sql, autoGeneratedKeys), params);
	}

	public Sequel execute(String sql, boolean autoGeneratedKeys, JSON params) throws SQLException
	{
		return execute(prepareStatement(sql, params, autoGeneratedKeys), params);
	}

	public Sequel execute(String sql, boolean autoGeneratedKeys, Map<String, ?> params) throws SQLException
	{
		return execute(prepareStatement(sql, params, autoGeneratedKeys), params);
	}

	public Sequel execute(String sql, boolean autoGeneratedKeys, Object... params) throws SQLException
	{
		return execute(prepareStatement(sql, autoGeneratedKeys), params);
	}

	public Sequel execute(String sql, int[] columnIndexes, Iterable<?> params) throws SQLException
	{
		return execute(prepareStatement(sql, columnIndexes), params);
	}

	public Sequel execute(String sql, int[] columnIndexes, JSAN params) throws SQLException
	{
		return execute(prepareStatement(sql, columnIndexes), params);
	}

	public Sequel execute(String sql, int[] columnIndexes, JSON params) throws SQLException
	{
		return execute(prepareStatement(sql, params, columnIndexes), params);
	}

	public Sequel execute(String sql, int[] columnIndexes, Map<String, ?> params) throws SQLException
	{
		return execute(prepareStatement(sql, params, columnIndexes), params);
	}

	public Sequel execute(String sql, int[] columnIndexes, Object... params) throws SQLException
	{
		return execute(prepareStatement(sql, columnIndexes), params);
	}

	public Sequel execute(String sql, Iterable<?> params) throws SQLException
	{
		return execute(prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public Sequel execute(String sql, JSAN params) throws SQLException
	{
		return execute(sql, (Iterable<?>) params);
	}

	public Sequel execute(String sql, JSON params) throws SQLException
	{
		return execute(prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability),
				params);
	}

	public Sequel execute(String sql, Map<String, ?> params) throws SQLException
	{
		return execute(prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability),
				params);
	}

	public Sequel execute(String sql, Object... params) throws SQLException
	{
		return execute(prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public Sequel execute(String sql, String[] columnNames, Iterable<?> params) throws SQLException
	{
		return execute(prepareStatement(sql, columnNames), params);
	}

	public Sequel execute(String sql, String[] columnNames, JSAN params) throws SQLException
	{
		return execute(prepareStatement(sql, columnNames), params);
	}

	public Sequel execute(String sql, String[] columnNames, JSON params) throws SQLException
	{
		return execute(prepareStatement(sql, params, columnNames), params);
	}

	public Sequel execute(String sql, String[] columnNames, Map<String, ?> params) throws SQLException
	{
		return execute(prepareStatement(sql, params, columnNames), params);
	}

	public Sequel execute(String sql, String[] columnNames, Object... params) throws SQLException
	{
		return execute(prepareStatement(sql, columnNames), params);
	}

	public int[] executeBatch() throws SQLException
	{
		return executeBatch(statement);
	}

	public int[] executeBatch(Statement statement) throws SQLException
	{
		return statement.executeBatch();
	}

	@Override
	protected void finalize() throws Throwable
	{
		this.close();
		super.finalize();
	}

	public String getBoundary()
	{
		return boundary;
	}

	public Connection getConnection()
	{
		return real;
	}

	protected final Connection getConnectionOrigin()
	{
		return this.connection;
	}

	public ResultSet getGeneratedKeys() throws SQLException
	{
		return getGeneratedKeys(statement);
	}

	public ResultSet getGeneratedKeys(Statement statement) throws SQLException
	{
		return statement.getGeneratedKeys();
	}

	public ConnectionManager getManager()
	{
		return manager;
	}

	public List<String> getParameter(Statement statement)
	{
		return getParameter(statements.get(statement));
	}

	public List<String> getParameter(String sql)
	{
		return parameters.get(sql);
	}

	protected Map<String, List<String>> getParameters()
	{
		return parameters;
	}

	public ResultSet getResultSet()
	{
		return getResultSet(statement);
	}

	public ResultSet getResultSet(Statement statement)
	{
		ResultSet rs = null;

		if (statement != null)
		{
			try
			{
				rs = statement.getResultSet();
			}
			catch (SQLException e)
			{
			}
		}

		return rs;
	}

	protected Map<String, Statement> getSentences()
	{
		return sentences;
	}

	public Statement getStatement()
	{
		return statement;
	}

	protected Map<Statement, String> getStatements()
	{
		return statements;
	}

	public int getUpdateCount()
	{
		return getUpdateCount(statement);
	}

	public int getUpdateCount(Statement statement)
	{
		int c = -1;

		try
		{
			c = statement.getUpdateCount();
		}
		catch (SQLException e)
		{
		}

		return c;
	}

	public int insert(String table, JSON data) throws SQLException
	{
		return insert(table, data, null);
	}

	public int insert(String table, JSON data, Map<String, String> map) throws SQLException
	{
		data = mapData(data, map, null);
		return update(makeInsertSQL(table, data.keySet()), data);
	}

	public int insert(String table, Map<String, Object> data) throws SQLException
	{
		return insert(table, data, null);
	}

	public int insert(String table, Map<String, Object> data, Map<String, String> map) throws SQLException
	{
		data = mapData(data, map, null);
		return update(makeInsertSQL(table, data.keySet()), data);
	}

	public Boolean isAutoCommit()
	{
		Boolean is = null;

		try
		{
			is = this.getConnection().getAutoCommit();
		}
		catch (Exception e)
		{
		}

		return is;
	}

	public boolean isClosed()
	{
		boolean is = connection == null;
		if (!is)
		{
			try
			{
				is = connection.isClosed();
			}
			catch (SQLException e)
			{
				is = false;
			}
		}
		return is;
	}

	public boolean isReuseStatements()
	{
		return reuseStatements;
	}

	public SQLKit optimizingAs(byte i)
	{
		return optimizingAs(OPTIMIZING_PRESET_SCHEMES[i]);
	}

	public SQLKit optimizingAs(int... params)
	{
		if (params.length > 0)
		{
			resultSetType = params[0];
			if (params.length > 1)
			{
				resultSetConcurrency = params[1];
				if (params.length > 2)
				{
					resultSetHoldability = params[2];
				}
			}
		}
		return this;
	}

	public CallableStatement prepareCall(String call) throws SQLException
	{
		return prepareCall(call, resultSetType);
	}

	public CallableStatement prepareCall(String call, int resultSetType) throws SQLException
	{
		return prepareCall(call, resultSetType, resultSetConcurrency);
	}

	public CallableStatement prepareCall(String call, int resultSetType, int resultSetConcurrency) throws SQLException
	{
		return prepareCall(call, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public CallableStatement prepareCall(String call, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(call);

			if (statement == null)
			{
				statement = prepareCallRaw(call, resultSetType, resultSetConcurrency, resultSetHoldability);
				sentences.put(call, statement);
			}
		}
		else
		{
			statement = prepareCallRaw(call, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		return (CallableStatement) statement;
	}

	public CallableStatement prepareCall(String call, Iterable<String> params) throws SQLException
	{
		return prepareCall(call, params, resultSetType);
	}

	public CallableStatement prepareCall(String call, Iterable<String> params, int resultSetType) throws SQLException
	{
		return prepareCall(call, params, resultSetType, resultSetConcurrency);
	}

	public CallableStatement prepareCall(String call, Iterable<String> params, int resultSetType,
			int resultSetConcurrency) throws SQLException
	{
		return prepareCall(call, params, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public CallableStatement prepareCall(String call, Iterable<String> params, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(call);

			if (statement == null)
			{
				statement = prepareCallRaw(call, params, resultSetType, resultSetConcurrency, resultSetHoldability);
				sentences.put(call, statement);
			}
		}
		else
		{
			statement = prepareCallRaw(call, params, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		return (CallableStatement) statement;
	}

	public CallableStatement prepareCall(String call, JSON params) throws SQLException
	{
		return prepareCall(call, params, resultSetType);
	}

	public CallableStatement prepareCall(String call, JSON params, int resultSetType) throws SQLException
	{
		return prepareCall(call, params, resultSetType, resultSetConcurrency);
	}

	public CallableStatement prepareCall(String call, JSON params, int resultSetType, int resultSetConcurrency)
			throws SQLException
	{
		return prepareCall(call, params, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public CallableStatement prepareCall(String call, JSON params, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(call);

			if (statement == null)
			{
				statement = prepareCallRaw(call, params, resultSetType, resultSetConcurrency, resultSetHoldability);
				sentences.put(call, statement);
			}
		}
		else
		{
			statement = prepareCallRaw(call, params, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		return (CallableStatement) statement;
	}

	public CallableStatement prepareCall(String call, Map<String, ?> params) throws SQLException
	{
		return prepareCall(call, params, resultSetType);
	}

	public CallableStatement prepareCall(String call, Map<String, ?> params, int resultSetType) throws SQLException
	{
		return prepareCall(call, params, resultSetType, resultSetConcurrency);
	}

	public CallableStatement prepareCall(String call, Map<String, ?> params, int resultSetType,
			int resultSetConcurrency) throws SQLException
	{
		return prepareCall(call, params, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public CallableStatement prepareCall(String call, Map<String, ?> params, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(call);

			if (statement == null)
			{
				statement = prepareCallRaw(call, params, resultSetType, resultSetConcurrency, resultSetHoldability);
				sentences.put(call, statement);
			}
		}
		else
		{
			statement = prepareCallRaw(call, params, resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		return (CallableStatement) statement;
	}

	protected CallableStatement prepareCallRaw(String call, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException
	{
		CallableStatement statement = null;

		try
		{
			statement = this.getConnection().prepareCall(call, resultSetType, resultSetConcurrency,
					resultSetHoldability);
		}
		catch (SQLException e)
		{
			statement = this.getConnection().prepareCall(call);
		}

		statements.put(statement, call);

		return statement;
	}

	protected CallableStatement prepareCallRaw(String call, Iterable<String> params, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		CallableStatement statement = null;

		String q = replaceParameters(call, getBoundary(), params);
		try
		{
			statement = this.getConnection().prepareCall(q, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		catch (SQLException e)
		{
			statement = this.getConnection().prepareCall(q);
		}

		this.registerParameters(call, params);
		statements.put(statement, call);

		return statement;
	}

	protected CallableStatement prepareCallRaw(String call, JSON params, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException
	{
		CallableStatement statement = null;

		String q = replaceParameters(call, getBoundary(), params);
		try
		{
			statement = this.getConnection().prepareCall(q, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		catch (SQLException e)
		{
			statement = this.getConnection().prepareCall(q);
		}

		this.registerParameters(call, params.keySet());
		statements.put(statement, call);

		return statement;
	}

	protected CallableStatement prepareCallRaw(String call, Map<String, ?> params, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		CallableStatement statement = null;

		String q = replaceParameters(call, getBoundary(), params);
		try
		{
			statement = this.getConnection().prepareCall(q, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		catch (SQLException e)
		{
			statement = this.getConnection().prepareCall(q);
		}

		this.registerParameters(call, params.keySet());
		statements.put(statement, call);

		return statement;
	}

	/**
	 * Fetch the PreparedStatement according to the SQL string.<br />
	 * This method will not make a new PreparedStatment if SQL has been already
	 * prepared and set the {@link SQLKit#getCurrentStatement()} to the already
	 * prepared one. If the database did not support PreparedStatement then a
	 * Statement object would be created.
	 * 
	 * @since 2011.12.11
	 */
	public PreparedStatement prepareStatement(String sql) throws SQLException
	{
		return prepareStatement(sql, resultSetType);
	}

	public PreparedStatement prepareStatement(String sql, boolean autoGeneratedKeys) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, autoGeneratedKeys);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, autoGeneratedKeys);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType) throws SQLException
	{
		return prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException
	{
		return prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, columnIndexes);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, columnIndexes);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, Iterable<String> params) throws SQLException
	{
		return prepareStatement(sql, params, resultSetType);
	}

	public PreparedStatement prepareStatement(String sql, Iterable<String> params, boolean autoGeneratedKeys)
			throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, params, autoGeneratedKeys);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, params, autoGeneratedKeys);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, Iterable<String> params, int resultSetType)
			throws SQLException
	{
		return prepareStatement(sql, params, resultSetType, resultSetConcurrency);
	}

	public PreparedStatement prepareStatement(String sql, Iterable<String> params, int resultSetType,
			int resultSetConcurrency) throws SQLException
	{
		return prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public PreparedStatement prepareStatement(String sql, Iterable<String> params, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, Iterable<String> params, int[] columnIndexes)
			throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, params, columnIndexes);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, params, columnIndexes);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, Iterable<String> params, String[] columnNames)
			throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, params, columnNames);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, params, columnNames);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, JSON params) throws SQLException
	{
		return prepareStatement(sql, params, resultSetType);
	}

	public PreparedStatement prepareStatement(String sql, JSON params, boolean autoGeneratedKeys) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, params, autoGeneratedKeys);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, params, autoGeneratedKeys);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, JSON params, int resultSetType) throws SQLException
	{
		return prepareStatement(sql, params, resultSetType, resultSetConcurrency);
	}

	public PreparedStatement prepareStatement(String sql, JSON params, int resultSetType, int resultSetConcurrency)
			throws SQLException
	{
		return prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public PreparedStatement prepareStatement(String sql, JSON params, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, JSON params, int[] columnIndexes) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, params, columnIndexes);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, params, columnIndexes);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, JSON params, String[] columnNames) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, params, columnNames);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, params, columnNames);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, Map<String, ?> params) throws SQLException
	{
		return prepareStatement(sql, params, resultSetType);
	}

	public PreparedStatement prepareStatement(String sql, Map<String, ?> params, boolean autoGeneratedKeys)
			throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, params, autoGeneratedKeys);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, params, autoGeneratedKeys);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, Map<String, ?> params, int resultSetType) throws SQLException
	{
		return prepareStatement(sql, params, resultSetType, resultSetConcurrency);
	}

	public PreparedStatement prepareStatement(String sql, Map<String, ?> params, int resultSetType,
			int resultSetConcurrency) throws SQLException
	{
		return prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public PreparedStatement prepareStatement(String sql, Map<String, ?> params, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, Map<String, ?> params, int[] columnIndexes)
			throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, params, columnIndexes);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, params, columnIndexes);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, Map<String, ?> params, String[] columnNames)
			throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, params, columnNames);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, params, columnNames);
		}
		return (PreparedStatement) statement;
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
	{
		if (isReuseStatements())
		{
			statement = sentences.get(sql);

			if (statement == null)
			{
				statement = prepareStatementRaw(sql, columnNames);
				sentences.put(sql, statement);
			}
		}
		else
		{
			statement = prepareStatementRaw(sql, columnNames);
		}
		return (PreparedStatement) statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, boolean autoGeneratedKeys) throws SQLException
	{
		PreparedStatement statement = this.getConnection().prepareStatement(sql,
				autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
		statements.put(statement, sql);
		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException
	{
		PreparedStatement statement = null;
		try
		{
			statement = this.getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency,
					resultSetHoldability);
		}
		catch (SQLException e)
		{
			statement = this.getConnection().prepareStatement(sql);
		}

		statements.put(statement, sql);

		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, int[] columnIndexes) throws SQLException
	{
		PreparedStatement statement = this.getConnection().prepareStatement(sql, columnIndexes);
		statements.put(statement, sql);
		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, Iterable<String> params, boolean autoGeneratedKeys)
			throws SQLException
	{
		String q = replaceParameters(sql, getBoundary(), params);
		PreparedStatement statement = this.getConnection().prepareStatement(q,
				autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
		this.registerParameters(sql, params);
		statements.put(statement, sql);
		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, Iterable<String> params, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		String q = replaceParameters(sql, getBoundary(), params);

		PreparedStatement statement = null;
		try
		{
			statement = this.getConnection().prepareStatement(q, resultSetType, resultSetConcurrency,
					resultSetHoldability);
		}
		catch (SQLException e)
		{
			statement = this.getConnection().prepareStatement(q);
		}

		this.registerParameters(sql, params);
		statements.put(statement, sql);

		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, Iterable<String> params, int[] columnIndexes)
			throws SQLException
	{
		String q = replaceParameters(sql, getBoundary(), params);
		PreparedStatement statement = this.getConnection().prepareStatement(q, columnIndexes);
		this.registerParameters(sql, params);
		statements.put(statement, sql);
		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, Iterable<String> params, String[] columnNames)
			throws SQLException
	{
		String q = replaceParameters(sql, getBoundary(), params);
		PreparedStatement statement = this.getConnection().prepareStatement(q, columnNames);
		this.registerParameters(sql, params);
		statements.put(statement, sql);
		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, JSON params, boolean autoGeneratedKeys)
			throws SQLException
	{
		String q = replaceParameters(sql, getBoundary(), params);
		PreparedStatement statement = this.getConnection().prepareStatement(q,
				autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
		this.registerParameters(sql, params.keySet());
		statements.put(statement, sql);
		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, JSON params, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		String q = replaceParameters(sql, getBoundary(), params);

		PreparedStatement statement = null;
		try
		{
			statement = this.getConnection().prepareStatement(q, resultSetType, resultSetConcurrency,
					resultSetHoldability);
		}
		catch (SQLException e)
		{
			statement = this.getConnection().prepareStatement(q);
		}

		this.registerParameters(sql, params.keySet());
		statements.put(statement, sql);

		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, JSON params, int[] columnIndexes) throws SQLException
	{
		String q = replaceParameters(sql, getBoundary(), params);
		PreparedStatement statement = this.getConnection().prepareStatement(q, columnIndexes);
		this.registerParameters(sql, params.keySet());
		statements.put(statement, sql);
		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, JSON params, String[] columnNames) throws SQLException
	{
		String q = replaceParameters(sql, getBoundary(), params);
		PreparedStatement statement = this.getConnection().prepareStatement(q, columnNames);
		this.registerParameters(sql, params.keySet());
		statements.put(statement, sql);
		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, Map<String, ?> params, boolean autoGeneratedKeys)
			throws SQLException
	{
		String q = replaceParameters(sql, getBoundary(), params);
		PreparedStatement statement = this.getConnection().prepareStatement(q,
				autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
		this.registerParameters(sql, params.keySet());
		statements.put(statement, sql);
		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, Map<String, ?> params, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		String q = replaceParameters(sql, getBoundary(), params);

		PreparedStatement statement = null;
		try
		{
			statement = this.getConnection().prepareStatement(q, resultSetType, resultSetConcurrency,
					resultSetHoldability);
		}
		catch (SQLException e)
		{
			statement = this.getConnection().prepareStatement(q);
		}

		this.registerParameters(sql, params.keySet());
		statements.put(statement, sql);

		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, Map<String, ?> params, int[] columnIndexes)
			throws SQLException
	{
		String q = replaceParameters(sql, getBoundary(), params);
		PreparedStatement statement = this.getConnection().prepareStatement(q, columnIndexes);
		this.registerParameters(sql, params.keySet());
		statements.put(statement, sql);
		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, Map<String, ?> params, String[] columnNames)
			throws SQLException
	{
		String q = replaceParameters(sql, getBoundary(), params);
		PreparedStatement statement = this.getConnection().prepareStatement(q, columnNames);
		this.registerParameters(sql, params.keySet());
		statements.put(statement, sql);
		return statement;
	}

	protected PreparedStatement prepareStatementRaw(String sql, String[] columnNames) throws SQLException
	{
		PreparedStatement statement = this.getConnection().prepareStatement(sql, columnNames);
		statements.put(statement, sql);
		return statement;
	}

	public ResultSet query(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		return bindParameters(statement, params).executeQuery();
	}

	public ResultSet query(PreparedStatement statement, JSAN params) throws SQLException
	{
		return query(statement, (Iterable<?>) params);
	}

	public ResultSet query(PreparedStatement statement, JSON params) throws SQLException
	{
		return query(statement, fillParameters(getParameter(statement), params));
	}

	public ResultSet query(PreparedStatement statement, Map<String, ?> params) throws SQLException
	{
		return query(statement, fillParameters(getParameter(statement), params));
	}

	public ResultSet query(PreparedStatement statement, Object... params) throws SQLException
	{
		return bindParameters(statement, params).executeQuery();
	}

	protected ResultSet query(Statement statement, String sql) throws SQLException
	{
		return statement.executeQuery(sql);
	}

	public ResultSet query(String sql) throws SQLException
	{
		return query(createStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql);
	}

	public ResultSet query(String sql, Iterable<?> params) throws SQLException
	{
		return query(prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public ResultSet query(String sql, JSAN params) throws SQLException
	{
		return query(sql, (Iterable<?>) params);
	}

	public ResultSet query(String sql, JSON params) throws SQLException
	{
		return query(prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public ResultSet query(String sql, Map<String, ?> params) throws SQLException
	{
		return query(prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public ResultSet query(String sql, Object... params) throws SQLException
	{
		return query(prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public SQLKit registerOutParameter(CallableStatement cs, int index, int type) throws SQLException
	{
		cs.registerOutParameter(index, type);
		return this;
	}

	public SQLKit registerOutParameter(CallableStatement cs, int index, int type, int scale) throws SQLException
	{
		cs.registerOutParameter(index, type, scale);
		return this;
	}

	public SQLKit registerOutParameter(CallableStatement cs, int index, int type, String typeName) throws SQLException
	{
		cs.registerOutParameter(index, type, typeName);
		return this;
	}

	public SQLKit registerOutParameter(CallableStatement cs, String name, int type) throws SQLException
	{
		int index = 1;

		for (String param : this.getParameter(cs))
		{
			if (Tools.equals(name, param))
			{
				cs.registerOutParameter(index, type);
			}
			index++;
		}

		return this;
	}

	public SQLKit registerOutParameter(CallableStatement cs, String name, int type, int scale) throws SQLException
	{
		int index = 1;

		for (String param : this.getParameter(cs))
		{
			if (Tools.equals(name, param))
			{
				cs.registerOutParameter(index, type, scale);
			}
			index++;
		}

		return this;
	}

	public SQLKit registerOutParameter(CallableStatement cs, String name, int type, String typeName) throws SQLException
	{
		int index = 1;

		for (String param : this.getParameter(cs))
		{
			if (Tools.equals(name, param))
			{
				cs.registerOutParameter(index, type, typeName);
			}
			index++;
		}

		return this;
	}

	public SQLKit registerOutParameter(int index, int type) throws SQLException
	{
		return registerOutParameter((CallableStatement) this.getStatement(), index, type);
	}

	public SQLKit registerOutParameter(int index, int type, int scale) throws SQLException
	{
		return registerOutParameter((CallableStatement) this.getStatement(), index, type, scale);
	}

	public SQLKit registerOutParameter(int index, int type, String typeName) throws SQLException
	{
		return registerOutParameter((CallableStatement) this.getStatement(), index, type, typeName);
	}

	public SQLKit registerOutParameter(String name, int type) throws SQLException
	{
		return registerOutParameter((CallableStatement) this.getStatement(), name, type);
	}

	public SQLKit registerOutParameter(String name, int type, int scale) throws SQLException
	{
		return registerOutParameter((CallableStatement) this.getStatement(), name, type, scale);
	}

	public SQLKit registerOutParameter(String name, int type, String typeName) throws SQLException
	{
		return registerOutParameter((CallableStatement) this.getStatement(), name, type, typeName);
	}

	protected SQLKit registerParameters(String sql, Iterable<String> params)
	{
		if (!parameters.containsKey(sql))
		{
			parameters.put(sql, Collections.unmodifiableList( //
					new LinkedList<String>(indexOfParameters(sql, getBoundary(), params).values())));
		}
		return this;
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException
	{
		this.getConnection().releaseSavepoint(savepoint);
	}

	/**
	 * Reset the parameters of this SQLKit to default.
	 * 
	 * @return The SQLKit itself.
	 * @throws SQLException
	 */
	public SQLKit reset() throws SQLException
	{
		SQLKit.reset(this.getConnection());
		return this.optimizingAs(SQLKit.OPTIMIZING_AS_DEFAULT) //
				.setBoundary(TextFiller.DEFAULT_BOUNDARY) //
				.setReuseStatements(true);
	}

	public int resultSetConcurrency()
	{
		return resultSetConcurrency;
	}

	public SQLKit resultSetConcurrency(int resultSetConcurrency)
	{
		this.resultSetConcurrency = resultSetConcurrency;
		return this;
	}

	public int resultSetHoldability()
	{
		return resultSetHoldability;
	}

	public SQLKit resultSetHoldability(int resultSetHoldability)
	{
		this.resultSetHoldability = resultSetHoldability;
		return this;
	}

	public int resultSetType()
	{
		return resultSetType;
	}

	public SQLKit resultSetType(int resultSetType)
	{
		this.resultSetType = resultSetType;
		return this;
	}

	public void rollback() throws SQLException
	{
		this.getConnection().rollback();
	}

	public void rollback(Savepoint savepoint) throws SQLException
	{
		this.getConnection().rollback(savepoint);
	}

	public Savepoint savepoint() throws SQLException
	{
		return this.getConnection().setSavepoint();
	}

	public Savepoint savepoint(String name) throws SQLException
	{
		return this.getConnection().setSavepoint(name);
	}

	public SQLKit setAutoCommit(Boolean autoCommit) throws SQLException
	{
		if (autoCommit != null)
		{
			this.getConnection().setAutoCommit(autoCommit);
		}
		return this;
	}

	public SQLKit setBoundary(String boundary)
	{
		this.boundary = boundary;
		return this;
	}

	protected SQLKit setConnectionOrigin(Connection connection)
	{
		this.connection = connection;
		this.real = connection;
		return this;
	}

	protected SQLKit setManager(ConnectionManager source)
	{
		this.manager = source;
		return this;
	}

	protected SQLKit setParameters(Map<String, List<String>> parameters)
	{
		this.parameters = parameters;
		return this;
	}

	public SQLKit setReuseStatements(boolean reuseStatements)
	{
		this.reuseStatements = reuseStatements;
		return this;
	}

	protected SQLKit setSentences(Map<String, Statement> sentences)
	{
		this.sentences = sentences;
		return this;
	}

	public SQLKit setStatement(Statement statement)
	{
		this.statement = statement;
		return this;
	}

	protected SQLKit setStatements(Map<Statement, String> statements)
	{
		this.statements = statements;
		return this;
	}

	public Class<? extends Connection> unwrap()
	{
		return this.unwrap;
	}

	public <T extends Connection> SQLKit unwrap(Class<T> cls) throws SQLException
	{
		this.unwrap = cls;
		this.real = cls == null ? this.getConnectionOrigin() : this.getConnectionOrigin().unwrap(cls);
		return this;
	}

	public int update(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		return bindParameters(statement, params).executeUpdate();
	}

	public int update(PreparedStatement statement, JSAN params) throws SQLException
	{
		return update(statement, (Iterable<?>) params);
	}

	public int update(PreparedStatement statement, JSON params) throws SQLException
	{
		return update(statement, fillParameters(getParameter(statement), params));
	}

	public int update(PreparedStatement statement, Map<String, ?> params) throws SQLException
	{
		return update(statement, fillParameters(getParameter(statement), params));
	}

	/**
	 * <pre>
	 * PreparedStatement statement = prepareStatement(sql, true);
	 * kit.update(statement, &quot;INSERT INTO `user` (`id`,`name`) VALUES (?,?)&quot;, SQLKit.NULL, &quot;Taylor&quot;);
	 * 
	 * ResultSet rs = kit.getStatement().getGeneratedKeys();
	 * while (rs.next())
	 * {
	 * 	Tools.debug(rs.getInt(1));
	 * 	// Return the last value of id if id is an auto-generated key.
	 * }
	 * </pre>
	 */
	public int update(PreparedStatement statement, Object... params) throws SQLException
	{
		return bindParameters(statement, params).executeUpdate();
	}

	protected int update(Statement statement, String sql) throws SQLException
	{
		return statement.executeUpdate(sql);
	}

	public int update(String sql) throws SQLException
	{
		return update(createStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql);
	}

	public int update(String sql, boolean autoGeneratedKeys) throws SQLException
	{
		return update(prepareStatement(sql, autoGeneratedKeys));
	}

	public int update(String sql, boolean autoGeneratedKeys, Iterable<?> params) throws SQLException
	{
		return update(prepareStatement(sql, autoGeneratedKeys), params);
	}

	public int update(String sql, boolean autoGeneratedKeys, JSAN params) throws SQLException
	{
		return update(prepareStatement(sql, autoGeneratedKeys), params);
	}

	public int update(String sql, boolean autoGeneratedKeys, JSON params) throws SQLException
	{
		return update(prepareStatement(sql, params, autoGeneratedKeys), params);
	}

	public int update(String sql, boolean autoGeneratedKeys, Map<String, ?> params) throws SQLException
	{
		return update(prepareStatement(sql, params, autoGeneratedKeys), params);
	}

	public int update(String sql, boolean autoGeneratedKeys, Object... params) throws SQLException
	{
		return update(prepareStatement(sql, autoGeneratedKeys), params);
	}

	public int update(String sql, int[] columnIndexes, Iterable<?> params) throws SQLException
	{
		return update(prepareStatement(sql, columnIndexes), params);
	}

	public int update(String sql, int[] columnIndexes, JSAN params) throws SQLException
	{
		return update(prepareStatement(sql, columnIndexes), params);
	}

	public int update(String sql, int[] columnIndexes, JSON params) throws SQLException
	{
		return update(prepareStatement(sql, params, columnIndexes), params);
	}

	public int update(String sql, int[] columnIndexes, Map<String, ?> params) throws SQLException
	{
		return update(prepareStatement(sql, params, columnIndexes), params);
	}

	public int update(String sql, int[] columnIndexes, Object... params) throws SQLException
	{
		return update(prepareStatement(sql, columnIndexes), params);
	}

	public int update(String sql, Iterable<?> params) throws SQLException
	{
		return update(prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public int update(String sql, JSAN params) throws SQLException
	{
		return update(sql, (Iterable<?>) params);
	}

	public int update(String sql, JSON params) throws SQLException
	{
		return update(prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public int update(String sql, Map<String, ?> params) throws SQLException
	{
		return update(prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public int update(String sql, Object... params) throws SQLException
	{
		return update(prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public int update(String sql, String[] columnNames, Iterable<?> params) throws SQLException
	{
		return update(prepareStatement(sql, columnNames), params);
	}

	public int update(String sql, String[] columnNames, JSAN params) throws SQLException
	{
		return update(prepareStatement(sql, columnNames), params);
	}

	public int update(String sql, String[] columnNames, JSON params) throws SQLException
	{
		return update(prepareStatement(sql, params, columnNames), params);
	}

	public int update(String sql, String[] columnNames, Map<String, ?> params) throws SQLException
	{
		return update(prepareStatement(sql, params, columnNames), params);
	}

	public int update(String sql, String[] columnNames, Object... params) throws SQLException
	{
		return update(prepareStatement(sql, columnNames), params);
	}
}
