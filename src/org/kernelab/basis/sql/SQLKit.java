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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public static abstract class ProjectMapper<S, T> implements Mapper<S, T>, Serializable
	{
		private static final long						serialVersionUID	= -6021148228942835875L;

		private Class<T>								cls;

		private Map<String, Object>						map;

		private T										obj;

		private Map<String, Accessor>					acs;

		private Map<Class<?>, Mapper<Object, Object>>	typeMap;

		public ProjectMapper(Class<T> cls, Map<String, Object> map)
		{
			this.cls = cls;
			this.map = map;
		}

		public ProjectMapper(Map<String, Object> map, T obj)
		{
			this.map = map;
			this.obj = obj;
		}

		protected abstract Map<String, Object> dict(S src) throws Exception;

		protected abstract Object get(S src, Object key) throws Exception;

		protected T getInstance() throws Exception
		{
			return this.obj != null ? this.obj : this.cls.newInstance();
		}

		public Map<Class<?>, Mapper<Object, Object>> getTypeMap()
		{
			return typeMap;
		}

		@Override
		public T map(S src)
		{
			try
			{
				if (this.map == null)
				{
					this.map = dict(src);
				}

				boolean finding = false;

				if (this.acs == null)
				{
					this.acs = new LinkedHashMap<String, Accessor>();
					finding = true;
				}

				T obj = this.getInstance();
				if (obj == null)
				{
					return null;
				}

				Accessor acs = null;
				String key = null;
				Object col = null, dat = null;

				for (Entry<String, Object> entry : this.map.entrySet())
				{
					key = entry.getKey();
					col = entry.getValue();

					if (key != null && col != null)
					{
						dat = get(src, col);

						if (obj instanceof Row || obj instanceof Map || obj instanceof Collection)
						{
							set(obj, key, dat);
						}
						else
						{
							if (finding)
							{
								acs = Accessor.Of(Tools.fieldOf(obj.getClass(), key));
							}
							else
							{
								acs = this.acs.get(key);
							}

							if (acs != null)
							{
								set(obj, acs, dat);

								if (finding)
								{
									this.acs.put(key, acs);
								}
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

		protected void set(T obj, Accessor acs, Object dat) throws Exception
		{
			Object val = null;

			if (dat != null)
			{
				Mapper<Object, Object> cas = this.getTypeMap() == null ? null
						: this.getTypeMap().get(acs.getField().getType());
				val = cas != null ? cas.map(dat) : JSON.ProjectTo(dat, acs.getField().getType(), dat, null);
			}

			acs.set(obj, val != null ? val : dat);
		}

		@SuppressWarnings("unchecked")
		protected void set(T obj, String key, Object dat)
		{
			if (obj instanceof Row)
			{
				((Row) obj).set(key, dat);
			}
			else if (obj instanceof JSON)
			{
				((JSON) obj).attr(key, dat);
			}
			else if (obj instanceof Map)
			{
				((Map<String, Object>) obj).put(key, dat);
			}
			else if (obj instanceof Collection)
			{
				((Collection<Object>) obj).add(dat);
			}
		}

		public ProjectMapper<S, T> setTypeMap(Map<Class<?>, Mapper<Object, Object>> typeMap)
		{
			this.typeMap = typeMap;
			return this;
		}
	}

	public static class ResultSetMapper<E> extends ProjectMapper<ResultSet, E>
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 5643973407094832255L;

		public ResultSetMapper(Class<E> cls, Map<String, Object> map)
		{
			super(cls, map);
		}

		public ResultSetMapper(Map<String, Object> map, E obj)
		{
			super(map, obj);
		}

		@Override
		protected Map<String, Object> dict(ResultSet rs) throws SQLException
		{
			return mapNameOfMetaData(rs.getMetaData());
		}

		@Override
		protected Object get(ResultSet rs, Object key) throws SQLException
		{
			return key instanceof Integer //
					? rs.getObject((Integer) key) //
					: rs.getObject(key.toString());
		}

		@Override
		public ResultSetMapper<E> setTypeMap(Map<Class<?>, Mapper<Object, Object>> typeMap)
		{
			super.setTypeMap(typeMap);
			return this;
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

	private static final Logger		log							= LoggerFactory.getLogger(SQLKit.class);

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
	 *            null then a new ArrayList&lt;Object&gt; would be created
	 *            instead.
	 * 
	 * @return The parameters value list.
	 */
	public static List<Object> fillParameters(Iterable<String> keys, JSON params, List<Object> list)
	{
		if (list == null)
		{
			list = new ArrayList<Object>();
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
	 *            null then a new ArrayList&lt;Object&gt; would be created
	 *            instead.
	 * 
	 * @return The parameters value list.
	 */
	public static List<Object> fillParameters(Iterable<String> keys, Map<String, ?> params, List<Object> list)
	{
		if (list == null)
		{
			list = new ArrayList<Object>();
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
	 * To determine whether the params can be reused or not.
	 * 
	 * @param params
	 *            the params.
	 * @return false if any Iterable value was in the params, otherwise true.
	 */
	public static boolean isReusable(JSON params)
	{
		for (Object v : params.values())
		{
			if (v instanceof Iterable)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * To determine whether the params can be reused or not.
	 * 
	 * @param params
	 *            the params.
	 * @return false if any Iterable or Array value was in the params, otherwise
	 *         true.
	 */
	public static boolean isReusable(Map<String, ?> params)
	{
		for (Object v : params.values())
		{
			if (v instanceof Iterable || (v != null && v.getClass().isArray()))
			{
				return false;
			}
		}
		return true;
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

	public static Canal<JSON> jsonOfResultSet(SQLKit kit, final ResultSet rs, final Map<String, Object> map,
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
				if (index != null)
				{
					list = new ArrayList<String>(index.size());

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

					list = new ArrayList<String>(columns);

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

	public static <E> E mapResultRow(ResultSet rs, Class<E> cls, Map<String, Object> map) throws SQLException
	{
		return mapResultRow(rs, cls, map, null);
	}

	public static <E> E mapResultRow(ResultSet rs, Class<E> cls, Map<String, Object> map,
			Map<Class<?>, Mapper<Object, Object>> typeMap) throws SQLException
	{
		return mapResultRow(rs, new ResultSetMapper<E>(cls, map).setTypeMap(typeMap));
	}

	public static <E> E mapResultRow(ResultSet rs, Map<String, Object> map, E object) throws SQLException
	{
		return mapResultRow(rs, map, null, object);
	}

	public static <E> E mapResultRow(ResultSet rs, Map<String, Object> map,
			Map<Class<?>, Mapper<Object, Object>> typeMap, E object) throws SQLException
	{
		return mapResultRow(rs, new ResultSetMapper<E>(map, object).setTypeMap(typeMap));
	}

	public static <E> E mapResultRow(ResultSet rs, Mapper<ResultSet, E> mapper) throws SQLException
	{
		if (rs != null && mapper != null)
		{
			try
			{
				return mapper.map(rs);
			}
			catch (Exception e)
			{
				throw new SQLException(e);
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
		return mapResultSet(rs, rows, cls, map, null, limit);
	}

	public static <E> Collection<E> mapResultSet(ResultSet rs, Collection<E> rows, Class<E> cls,
			Map<String, Object> map, Map<Class<?>, Mapper<Object, Object>> typeMap, int limit) throws SQLException
	{
		return mapResultSet(rs, rows, new ResultSetMapper<E>(cls, map).setTypeMap(typeMap), limit);
	}

	public static <E> Collection<E> mapResultSet(ResultSet rs, Collection<E> rows, Mapper<ResultSet, E> mapper,
			int limit) throws SQLException
	{
		if (rs != null)
		{
			if (rows == null)
			{
				rows = new ArrayList<E>();
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

	public static <E> Canal<E> mapResultSet(SQLKit kit, ResultSet rs, Class<E> cls, Map<String, Object> map)
			throws SQLException
	{
		return mapResultSet(kit, rs, cls, map, null);
	}

	public static <E> Canal<E> mapResultSet(SQLKit kit, ResultSet rs, Class<E> cls, Map<String, Object> map,
			Map<Class<?>, Mapper<Object, Object>> typeMap) throws SQLException
	{
		return mapResultSet(kit, rs, new ResultSetMapper<E>(cls, map).setTypeMap(typeMap));
	}

	public static <E> Canal<E> mapResultSet(SQLKit kit, ResultSet rs, Mapper<ResultSet, E> mapper) throws SQLException
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

	private ConnectionManager						manager;

	private Connection								connection;

	private Class<? extends Connection>				unwrap;

	private Connection								real;

	private Statement								statement;

	private Map<String, Statement>					sentences;

	private Map<Statement, String>					statements;

	private Map<String, List<String>>				parameters;

	private Collection<ResultSet>					resultSets;

	private String									boundary;

	private boolean									reuseStatements			= true;

	private Set<Statement>							batchStatements			= new LinkedHashSet<Statement>();

	private int										resultSetType			= OPTIMIZING_PRESET_SCHEMES[OPTIMIZING_AS_DEFAULT][0];

	private int										resultSetConcurrency	= OPTIMIZING_PRESET_SCHEMES[OPTIMIZING_AS_DEFAULT][1];

	private int										resultSetHoldability	= OPTIMIZING_PRESET_SCHEMES[OPTIMIZING_AS_DEFAULT][2];

	private Map<Class<?>, Mapper<Object, Object>>	typeMap;

	private String									cursorName				= null;

	private Boolean									escapeProcessing		= null;

	private Integer									fetchDirection			= null;

	private Integer									fetchSize				= null;

	private Integer									maxFieldSize			= null;

	private Integer									maxRows					= null;

	private Boolean									statementPoolable		= null;

	private Integer									queryTimeout			= null;

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
		this.setResultSets(new LinkedHashSet<ResultSet>());
	}

	public void addBatch(Iterable<?> params) throws SQLException
	{
		addBatch((PreparedStatement) statement, params);
	}

	public void addBatch(JSON params) throws SQLException
	{
		addBatch((PreparedStatement) statement, params);
	}

	public void addBatch(Map<String, ?> params) throws SQLException
	{
		addBatch((PreparedStatement) statement, params);
	}

	public void addBatch(Object... params) throws SQLException
	{
		addBatch((PreparedStatement) statement, params);
	}

	public void addBatch(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		addBatchRaw(statement, params);
	}

	public void addBatch(PreparedStatement statement, JSAN params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		addBatchRaw(statement, (Iterable<?>) params);
	}

	public void addBatch(PreparedStatement statement, JSON params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		bindParameters(statement, fillParameters(getParameter(statement), params));
		statement.addBatch();
		getBatchStatements().add(statement);
	}

	public void addBatch(PreparedStatement statement, Map<String, ?> params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		bindParameters(statement, fillParameters(getParameter(statement), params));
		statement.addBatch();
		getBatchStatements().add(statement);
	}

	public void addBatch(PreparedStatement statement, Object... params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		bindParameters(statement, params);
		statement.addBatch();
		getBatchStatements().add(statement);
	}

	public void addBatch(String sql) throws SQLException
	{
		log.debug("{}", sql);
		statement.addBatch(sql);
		getBatchStatements().add(statement);
	}

	public void addBatch(String sql, Iterable<?> params) throws SQLException
	{
		addBatch(prepareStatement(sql), params);
	}

	public void addBatch(String sql, JSAN params) throws SQLException
	{
		addBatch(prepareStatement(sql), params);
	}

	public void addBatch(String sql, JSON params) throws SQLException
	{
		addBatch(prepareStatement(sql, params), params);
	}

	public void addBatch(String sql, Map<String, ?> params) throws SQLException
	{
		addBatch(prepareStatement(sql, params), params);
	}

	public void addBatch(String sql, Object... params) throws SQLException
	{
		addBatch(prepareStatement(sql), params);
	}

	protected void addBatchRaw(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		bindParameters(statement, params);
		statement.addBatch();
		getBatchStatements().add(statement);
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
		this.cleanResultSets();

		this.cleanStatements();

		if (parameters != null)
		{
			parameters.clear();
		}

		return this;
	}

	public SQLKit cleanResultSets()
	{
		for (ResultSet rs : this.getResultSets())
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
		}

		this.getResultSets().clear();

		return this;
	}

	public SQLKit cleanStatements()
	{
		if (statement != null)
		{
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
		if (batchStatements != null)
		{
			batchStatements.clear();
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
			try
			{
				statement.clearBatch();
			}
			finally
			{
				this.getBatchStatements().remove(statement);
			}
		}
	}

	public void clearBatches() throws SQLException
	{
		Set<Statement> batches = new LinkedHashSet<Statement>(this.getBatchStatements());

		for (Statement s : batches)
		{
			clearBatch(s);
		}

		batches.clear();

		this.getBatchStatements().clear();
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
		if (statement != null && (!isReuseStatements() || !isReused(statement)))
		{
			this.getSentences().remove(this.getStatements().remove(statement));
			statement.close();
		}
		return this;
	}

	public void commit() throws SQLException
	{
		if (Boolean.FALSE.equals(this.isAutoCommit()))
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

	public void commitBatches() throws SQLException
	{
		this.executeBatches();
		this.commit();
		this.clearBatches();
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
		try
		{
			statement = this.getConnection().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		catch (SQLException e)
		{
			statement = this.getConnection().createStatement();
		}
		statements.put(statement, sql);

		return statement;
	}

	protected PreparedStatement decorate(PreparedStatement ps) throws SQLException
	{
		decorate((Statement) ps);
		return ps;
	}

	protected Statement decorate(Statement statement) throws SQLException
	{
		String vs = null;
		Boolean vb = null;
		Integer vi = null;

		if ((vs = this.getCursorName()) != null)
		{
			statement.setCursorName(vs);
		}

		if ((vb = this.getEscapeProcessing()) != null)
		{
			statement.setEscapeProcessing(vb);
		}

		if ((vi = this.getFetchDirection()) != null)
		{
			statement.setFetchDirection(vi);
		}

		if ((vi = this.getFetchSize()) != null)
		{
			statement.setFetchSize(vi);
		}

		if ((vi = this.getMaxFieldSize()) != null)
		{
			statement.setMaxFieldSize(vi);
		}

		if ((vi = this.getMaxRows()) != null)
		{
			statement.setMaxRows(vi);
		}

		if ((vb = this.getStatementPoolable()) != null)
		{
			statement.setPoolable(vb);
		}

		if ((vi = this.getQueryTimeout()) != null)
		{
			statement.setQueryTimeout(vi);
		}

		return statement;
	}

	protected void destroy()
	{
		clean();
		this.typeMap = null;
		this.sentences = null;
		this.statements = null;
		this.parameters = null;
		if (this.manager != null)
		{
			try
			{
				this.manager.recycleConnection(this.getConnectionOrigin());
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
		log.debug("{} {}", statements.get(statement), params);
		return executeRaw(statement, params);
	}

	public Sequel execute(CallableStatement statement, JSAN params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeRaw(statement, (Iterable<?>) params);
	}

	public Sequel execute(CallableStatement statement, JSON params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeRaw(statement, fillParameters(getParameter(statement), params));
	}

	public Sequel execute(CallableStatement statement, Map<String, ?> params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeRaw(statement, fillParameters(getParameter(statement), params));
	}

	public Sequel execute(CallableStatement statement, Object... params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return new Sequel(this, statement, executeStatement(bindParameters(statement, params)))
				.setTypeMap(this.getTypeMap());
	}

	public Sequel execute(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeRaw(statement, params);
	}

	public Sequel execute(PreparedStatement statement, JSAN params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeRaw(statement, (Iterable<?>) params);
	}

	public Sequel execute(PreparedStatement statement, JSON params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeRaw(statement, fillParameters(getParameter(statement), params));
	}

	public Sequel execute(PreparedStatement statement, Map<String, ?> params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeRaw(statement, fillParameters(getParameter(statement), params));
	}

	public Sequel execute(PreparedStatement statement, Object... params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return new Sequel(this, statement, executeStatement(bindParameters(statement, params)))
				.setTypeMap(this.getTypeMap());
	}

	protected Sequel execute(Statement statement, String sql) throws SQLException
	{
		log.debug("{}", sql);
		return new Sequel(this, statement, executeStatement(statement, sql)).setTypeMap(this.getTypeMap());
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
		return this.decorate(statement).executeBatch();
	}

	public Map<Statement, int[]> executeBatches() throws SQLException
	{
		Map<Statement, int[]> res = new LinkedHashMap<Statement, int[]>();

		for (Statement s : this.getBatchStatements())
		{
			res.put(s, this.executeBatch(s));
		}

		return res;
	}

	protected boolean executeExists(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		return exists(executeQuery(bindParameters(statement, params)));
	}

	protected ResultSet executeQuery(PreparedStatement statement) throws SQLException
	{
		return record(decorate(statement).executeQuery());
	}

	protected ResultSet executeQuery(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		return executeQuery(bindParameters(statement, params));
	}

	protected ResultSet executeQuery(Statement statement, String sql) throws SQLException
	{
		return record(decorate(statement).executeQuery(sql));
	}

	protected Sequel executeRaw(CallableStatement statement, Iterable<?> params) throws SQLException
	{
		return new Sequel(this, statement, executeStatement(bindParameters(statement, params)))
				.setTypeMap(this.getTypeMap());
	}

	protected Sequel executeRaw(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		return new Sequel(this, statement, executeStatement(bindParameters(statement, params)))
				.setTypeMap(this.getTypeMap());
	}

	protected boolean executeStatement(PreparedStatement statement) throws SQLException
	{
		return decorate(statement).execute();
	}

	protected boolean executeStatement(Statement statement, String sql) throws SQLException
	{
		return decorate(statement).execute(sql);
	}

	protected int executeUpdate(PreparedStatement statement) throws SQLException
	{
		return decorate(statement).executeUpdate();
	}

	protected int executeUpdate(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		return executeUpdate(bindParameters(statement, params));
	}

	protected int executeUpdate(Statement statement, String sql) throws SQLException
	{
		return decorate(statement).executeUpdate(sql);
	}

	public boolean exists(PreparedStatement statement, Iterable<?> params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeExists(statement, params);
	}

	public boolean exists(PreparedStatement statement, JSAN params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeExists(statement, (Iterable<?>) params);
	}

	public boolean exists(PreparedStatement statement, JSON params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeExists(statement, fillParameters(getParameter(statement), params));
	}

	public boolean exists(PreparedStatement statement, Map<String, ?> params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeExists(statement, fillParameters(getParameter(statement), params));
	}

	public boolean exists(PreparedStatement statement, Object... params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return exists(executeQuery(bindParameters(statement, params)));
	}

	protected boolean exists(ResultSet rs) throws SQLException
	{
		try
		{
			return rs.next();
		}
		finally
		{
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	protected boolean exists(Statement statement, String sql) throws SQLException
	{
		log.debug("{}", sql);
		return exists(executeQuery(statement, sql));
	}

	public boolean exists(String sql) throws SQLException
	{
		return exists(createStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql);
	}

	public boolean exists(String sql, Iterable<?> params) throws SQLException
	{
		return exists(prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public boolean exists(String sql, JSAN params) throws SQLException
	{
		return exists(sql, (Iterable<?>) params);
	}

	public boolean exists(String sql, JSON params) throws SQLException
	{
		return exists(prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public boolean exists(String sql, Map<String, ?> params) throws SQLException
	{
		return exists(prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public boolean exists(String sql, Object... params) throws SQLException
	{
		return exists(prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	@Override
	protected void finalize() throws Throwable
	{
		this.close();
		super.finalize();
	}

	protected Set<Statement> getBatchStatements()
	{
		return batchStatements;
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
		return connection;
	}

	public String getCursorName()
	{
		return cursorName;
	}

	public Boolean getEscapeProcessing()
	{
		return escapeProcessing;
	}

	public Integer getFetchDirection()
	{
		return fetchDirection;
	}

	public Integer getFetchSize()
	{
		return fetchSize;
	}

	public ResultSet getGeneratedKeys() throws SQLException
	{
		return getGeneratedKeys(statement);
	}

	public ResultSet getGeneratedKeys(Statement statement) throws SQLException
	{
		return record(statement.getGeneratedKeys());
	}

	public ConnectionManager getManager()
	{
		return manager;
	}

	public Integer getMaxFieldSize()
	{
		return maxFieldSize;
	}

	public Integer getMaxRows()
	{
		return maxRows;
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

	public Integer getQueryTimeout()
	{
		return queryTimeout;
	}

	protected Collection<ResultSet> getResultSets()
	{
		return resultSets;
	}

	public String getSentence(Statement statement)
	{
		return this.getStatements().get(statement);
	}

	protected Map<String, Statement> getSentences()
	{
		return sentences;
	}

	public Statement getStatement()
	{
		return statement;
	}

	public Statement getStatement(String sql)
	{
		return this.getSentences().get(sql);
	}

	public Boolean getStatementPoolable()
	{
		return statementPoolable;
	}

	protected Map<Statement, String> getStatements()
	{
		return statements;
	}

	public Map<Class<?>, Mapper<Object, Object>> getTypeMap()
	{
		return typeMap;
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
		try
		{
			return this.getConnection().getAutoCommit();
		}
		catch (Exception e)
		{
		}
		return null;
	}

	public boolean isClosed()
	{
		if (getConnectionOrigin() == null)
		{
			return true;
		}
		try
		{
			return getConnectionOrigin().isClosed();
		}
		catch (SQLException e)
		{
			return true;
		}
	}

	protected boolean isReused(Statement statement)
	{
		return this.getStatement(this.getSentence(statement)) != null;
	}

	public boolean isReuseStatements()
	{
		return reuseStatements;
	}

	public boolean isValid()
	{
		return this.isValid(0);
	}

	public boolean isValid(int timeout)
	{
		try
		{
			return getConnectionOrigin() != null && getConnectionOrigin().isValid(Math.max(timeout, 0));
		}
		catch (SQLException e)
		{
			return false;
		}
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
		if (isReuseStatements() && isReusable(params))
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
		if (isReuseStatements() && isReusable(params))
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
		if (isReuseStatements() && isReusable(params))
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
		if (isReuseStatements() && isReusable(params))
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
		if (isReuseStatements() && isReusable(params))
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
		if (isReuseStatements() && isReusable(params))
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
		if (isReuseStatements() && isReusable(params))
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
		if (isReuseStatements() && isReusable(params))
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
		if (isReuseStatements() && isReusable(params))
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
		if (isReuseStatements() && isReusable(params))
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
		log.debug("{} {}", statements.get(statement), params);
		return executeQuery(statement, params);
	}

	public ResultSet query(PreparedStatement statement, JSAN params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeQuery(statement, (Iterable<?>) params);
	}

	public ResultSet query(PreparedStatement statement, JSON params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeQuery(statement, fillParameters(getParameter(statement), params));
	}

	public ResultSet query(PreparedStatement statement, Map<String, ?> params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeQuery(statement, fillParameters(getParameter(statement), params));
	}

	public ResultSet query(PreparedStatement statement, Object... params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeQuery(bindParameters(statement, params));
	}

	protected ResultSet query(Statement statement, String sql) throws SQLException
	{
		log.debug("{}", sql);
		return executeQuery(statement, sql);
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

	protected ResultSet record(ResultSet rs)
	{
		if (rs != null)
		{
			this.getResultSets().add(rs);
		}
		return rs;
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
					new ArrayList<String>(indexOfParameters(sql, getBoundary(), params).values())));
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
				.setReuseStatements(true) //
				.resetStatementDecorates();
	}

	/**
	 * Reset all decorates parameters to null which would affect the next
	 * executing statement.
	 * 
	 * @return The SQLKit itself.
	 */
	public SQLKit resetStatementDecorates()
	{
		return this.resetStatementDecorates(null, null, null, null, null, null, null, null);
	}

	/**
	 * Reset decorates parameters to given value which would affect the next
	 * executing statement.
	 * 
	 * @param cursorName
	 * @param escapeProcessing
	 * @param fetchDirection
	 * @param fetchSize
	 * @param maxFieldSize
	 * @param maxRows
	 * @param poolable
	 * @param queryTimeout
	 * @return The SQLKit itself.
	 */
	public SQLKit resetStatementDecorates(String cursorName, Boolean escapeProcessing, Integer fetchDirection,
			Integer fetchSize, Integer maxFieldSize, Integer maxRows, Boolean poolable, Integer queryTimeout)
	{
		return this.setCursorName(cursorName) //
				.setEscapeProcessing(escapeProcessing) //
				.setFetchDirection(fetchDirection) //
				.setFetchSize(fetchSize) //
				.setMaxFieldSize(maxFieldSize) //
				.setMaxRows(maxRows) //
				.setStatementPoolable(poolable) //
				.setQueryTimeout(queryTimeout);
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

	public SQLKit rollback() throws SQLException
	{
		if (!this.getConnection().getAutoCommit())
		{
			this.getConnection().rollback();
		}
		return this;
	}

	public SQLKit rollback(Savepoint savepoint) throws SQLException
	{
		if (!this.getConnection().getAutoCommit())
		{
			this.getConnection().rollback(savepoint);
		}
		return this;
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

	/**
	 * Set cursor name for next executing statement.
	 * 
	 * @param cursorName
	 * @return
	 */
	public SQLKit setCursorName(String cursorName)
	{
		this.cursorName = cursorName;
		return this;
	}

	/**
	 * Set escape processing for next executing statement.
	 * 
	 * @param escapeProcessing
	 * @return
	 */
	public SQLKit setEscapeProcessing(Boolean escapeProcessing)
	{
		this.escapeProcessing = escapeProcessing;
		return this;
	}

	/**
	 * Set fetch direction for next executing statement.
	 * 
	 * @param fetchDirection
	 * @return
	 */
	public SQLKit setFetchDirection(Integer fetchDirection)
	{
		this.fetchDirection = fetchDirection;
		return this;
	}

	/**
	 * Set fetch size for next executing statement.
	 * 
	 * @param fetchSize
	 * @return
	 */
	public SQLKit setFetchSize(Integer fetchSize)
	{
		this.fetchSize = fetchSize;
		return this;
	}

	protected SQLKit setManager(ConnectionManager source)
	{
		this.manager = source;
		return this;
	}

	/**
	 * Set max field size for next executing statement.
	 * 
	 * @param maxFieldSize
	 * @return
	 */
	public SQLKit setMaxFieldSize(Integer maxFieldSize)
	{
		this.maxFieldSize = maxFieldSize;
		return this;
	}

	/**
	 * Set max rows for next executing statement.
	 * 
	 * @param maxRows
	 * @return
	 */
	public SQLKit setMaxRows(Integer maxRows)
	{
		this.maxRows = maxRows;
		return this;
	}

	protected SQLKit setParameters(Map<String, List<String>> parameters)
	{
		this.parameters = parameters;
		return this;
	}

	/**
	 * Set query timeout for next executing statement.
	 * 
	 * @param seconds
	 * @return
	 */
	public SQLKit setQueryTimeout(Integer seconds)
	{
		this.queryTimeout = seconds;
		return this;
	}

	protected SQLKit setResultSets(Collection<ResultSet> resultSets)
	{
		this.resultSets = resultSets;
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

	/**
	 * Set poolable for next executing statement.
	 * 
	 * @param statementPoolable
	 * @return
	 */
	public SQLKit setStatementPoolable(Boolean statementPoolable)
	{
		this.statementPoolable = statementPoolable;
		return this;
	}

	protected SQLKit setStatements(Map<Statement, String> statements)
	{
		this.statements = statements;
		return this;
	}

	public SQLKit setTypeMap(Map<Class<?>, Mapper<Object, Object>> typeMap)
	{
		this.typeMap = typeMap;
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
		log.debug("{} {}", statements.get(statement), params);
		return executeUpdate(statement, params);
	}

	public int update(PreparedStatement statement, JSAN params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeUpdate(statement, (Iterable<?>) params);
	}

	public int update(PreparedStatement statement, JSON params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeUpdate(statement, fillParameters(getParameter(statement), params));
	}

	public int update(PreparedStatement statement, Map<String, ?> params) throws SQLException
	{
		log.debug("{} {}", statements.get(statement), params);
		return executeUpdate(statement, fillParameters(getParameter(statement), params));
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
		log.debug("{} {}", statements.get(statement), params);
		return executeUpdate(bindParameters(statement, params));
	}

	protected int update(Statement statement, String sql) throws SQLException
	{
		log.debug("{}", sql);
		return executeUpdate(statement, sql);
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
