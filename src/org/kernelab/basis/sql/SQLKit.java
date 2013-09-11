package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.TextFiller;
import org.kernelab.basis.Tools;
import org.kernelab.basis.sql.DataBase.MySQL;

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
	public static final char		VALUE_HOLDER_CHAR			= '?';

	public static final String		VALUE_HOLDER_MARK			= "?";

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
	public static final Object		NULL						= new byte[0];

	/**
	 * To declare that there is no parameter.<br />
	 * This object would be ignored if it is not the first parameter.
	 */
	public static final Object		EMPTY						= new byte[0];

	public static final byte		OPTIMIZING_AS_DEFAULT		= 0;

	public static final byte		OPTIMIZING_AS_TRACER		= 1;

	public static final byte		OPTIMIZING_AS_HUNTER		= 2;

	protected static final int[][]	OPTIMIZING_PRESET_SCHEMES	= {
			{ ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			{ ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			{ ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT } };

	/**
	 * To fill a list of parameters according to the keys which are the names of
	 * parameters.
	 * 
	 * @param keys
	 *            the parameters' name.
	 * @param params
	 *            the parameters in form of key/value organized by JSON.
	 * @return the parameters value list.
	 * @see SQLKit#fillParametersList(Iterable, Map, List)
	 */
	public static final List<Object> fillParametersList(Iterable<String> keys, JSON params)
	{
		return fillParametersList(keys, params, null);
	}

	/**
	 * To fill a list of parameters according to the keys which are the names of
	 * parameters.
	 * 
	 * @param keys
	 *            the parameters' name.
	 * @param params
	 *            the parameters in form of key/value organized by JSON.
	 * @param list
	 *            the List&lt;Object&gt; object which holds the result list. If
	 *            null then a new LinkedList&lt;Object&gt; would be created
	 *            instead.
	 * 
	 * @return the parameters value list.
	 */
	public static final List<Object> fillParametersList(Iterable<String> keys, JSON params, List<Object> list)
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
			list.add(params.get(key));
		}

		return list;
	}

	/**
	 * To fill a list of parameters according to the keys which are the names of
	 * parameters.
	 * 
	 * @param keys
	 *            the parameters' name.
	 * @param params
	 *            the parameters in form of key/value organized by
	 *            Map&lt;String,Object&gt;.
	 * @return the parameters value list.
	 * @see SQLKit#fillParametersList(Iterable, Map, List)
	 */
	public static final List<Object> fillParametersList(Iterable<String> keys, Map<String, Object> params)
	{
		return fillParametersList(keys, params, null);
	}

	/**
	 * To fill a list of parameters according to the keys which are the names of
	 * parameters.
	 * 
	 * @param keys
	 *            the parameters' name.
	 * @param params
	 *            the parameters in form of key/value organized by
	 *            Map&lt;String,Object&gt;.
	 * @param list
	 *            the List&lt;Object&gt; object which holds the result list. If
	 *            null then a new LinkedList&lt;Object&gt; would be created
	 *            instead.
	 * 
	 * @return the parameters value list.
	 */
	public static final List<Object> fillParametersList(Iterable<String> keys, Map<String, Object> params,
			List<Object> list)
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
			list.add(params.get(key));
		}

		return list;
	}

	/**
	 * Get the result number of a ResultSet.
	 * 
	 * @param rs
	 *            the ResultSet.
	 * @return the number of result in the ResultSet
	 */
	public static final int getResultNumber(ResultSet rs)
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
	 *            the SQL String.
	 * @param params
	 *            the parameters name Collection.
	 * @return the index of each parameter.
	 * @see SQLKit#indexOfParameters(String, Iterable, TreeMap)
	 */
	public static final TreeMap<Integer, String> indexOfParameters(String sql, Iterable<String> params)
	{
		return indexOfParameters(sql, params, null);
	}

	/**
	 * To index each parameter's position in the SQL.
	 * 
	 * @param sql
	 *            the SQL String.
	 * @param params
	 *            the parameters name Collection.
	 * @param index
	 *            the TreeMap&lt;Integer,String&gt; object which holds the
	 *            index. If null, then a new TreeMap&lt;Integer,String&gt; would
	 *            be created instead.
	 * @return the index of each parameter.
	 */
	public static final TreeMap<Integer, String> indexOfParameters(String sql, Iterable<String> params,
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
			Matcher m = Pattern.compile(
					Pattern.quote(TextFiller.DEFAULT_BOUNDARY + param + TextFiller.DEFAULT_BOUNDARY)).matcher(sql);

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
	 *            the ResultSet.
	 * @param map
	 *            the Map<String,Object> which describe the relationship of each
	 *            column between the ResultSet and JSON object.
	 * @return the JSAN object.
	 * @throws SQLException
	 */
	public static final JSAN jsanOfResultRow(ResultSet rs, Map<String, Object> map) throws SQLException
	{
		return (JSAN) jsonOfResultRow(rs, new JSAN(), map);
	}

	/**
	 * To read each row in a ResultSet into a JSAN object.
	 * 
	 * @param rs
	 *            the ResultSet.
	 * @param jsan
	 *            the JSAN object to hold the ResultSet. If null then an empty
	 *            JSAN would be created instead.
	 * @param map
	 *            the Map<String,Object> which describe the relationship of each
	 *            column between the ResultSet and JSON object.
	 * @param cls
	 *            the Class which indicates what data type that each row would
	 *            be converted to.
	 * @return the JSAN object.
	 * @throws SQLException
	 */
	public static final JSAN jsanOfResultSet(ResultSet rs, JSAN jsan, Map<String, Object> map, Class<? extends JSON> cls)
			throws SQLException
	{
		if (rs != null)
		{
			if (jsan == null)
			{
				jsan = new JSAN();
			}

			if (map == null)
			{
				map = new LinkedHashMap<String, Object>();
				ResultSetMetaData meta = rs.getMetaData();
				int columns = meta.getColumnCount();
				String name;
				for (int column = 1; column <= columns; column++)
				{
					name = meta.getColumnName(column);
					map.put(name, name);
				}
			}

			try
			{
				while (rs.next())
				{
					jsan.add(jsonOfResultRow(rs, cls.newInstance(), map));
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
	 *            the ResultSet.
	 * @param json
	 *            the JSON object to hold the data. If null then an empty JSON
	 *            object would be created instead.
	 * @param map
	 *            the Map<String,Object> which describe the relationship of each
	 *            column between the ResultSet and JSON object.
	 * @return the JSON object.
	 * @throws SQLException
	 */
	public static final JSON jsonOfResultRow(ResultSet rs, JSON json, Map<String, Object> map) throws SQLException
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
	 *            the ResultSet.
	 * @param map
	 *            the Map<String,Object> which describe the relationship of each
	 *            column between the ResultSet and JSON object.
	 * @return the JSON object.
	 * @throws SQLException
	 */
	public static final JSON jsonOfResultRow(ResultSet rs, Map<String, Object> map) throws SQLException
	{
		return jsonOfResultRow(rs, new JSON(), map);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		DataBase db = new MySQL("test", "root", "root");

		SQLKit kit = null;
		try
		{
			kit = db.getSQLKit();

			String sql = "INSERT INTO `test` VALUES (?id?,?name?,?value?)";

			JSON data = new JSON();
			data.attr("name", "King");
			data.attr("value", 1.2);
			data.attr("id", SQLKit.NULL);

			kit.setAutoCommit(false);

			kit.prepareStatement(sql, data);

			for (int i = 0; i < 10; i++)
			{
				data.attr("name", "King");
				data.attr("value", i);
				kit.addBatch(data);
			}

			kit.commitBatch();

			sql = "SELECT * FROM `test` WHERE id>?id?";

			data.attr("id", 9);

			ResultSet rs = kit.query(sql, data);

			while (rs.next())
			{
				Tools.debug(rs.getString(1) + "\t" + rs.getString(2) + "\t" + rs.getString(3));
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			kit.close();
		}
	}

	/**
	 * Replace each parameters name in SQL with the VALUE_HOLDER in order to be
	 * prepared.
	 * 
	 * @param sql
	 *            the SQL String.
	 * @param params
	 *            the names of parameters.
	 * @return the SQL String which could be prepared.
	 */
	public static final String replaceParameters(String sql, Iterable<String> params)
	{
		TextFiller filler = new TextFiller(sql).reset();

		for (String key : params)
		{
			filler.fillWith(key, VALUE_HOLDER_MARK);
		}

		return filler.toString();
	}

	/**
	 * Replace each parameters name in SQL with the VALUE_HOLDER in order to be
	 * prepared.
	 * 
	 * @param sql
	 *            the SQL String.
	 * @param params
	 *            the parameters in form of key/value organized by
	 *            Map<String,Object>.
	 * @return the SQL String which could be prepared.
	 */
	public static final String replaceParameters(String sql, Map<String, Object> params)
	{
		return replaceParameters(sql, params.keySet());
	}

	private SQLSource								source;

	private Connection								connection;

	private Statement								statement;

	private Map<String, Statement>					statements;

	private Map<PreparedStatement, List<String>>	parameters				= new HashMap<PreparedStatement, List<String>>();

	private int										resultSetType			= OPTIMIZING_PRESET_SCHEMES[OPTIMIZING_AS_DEFAULT][0];

	private int										resultSetConcurrency	= OPTIMIZING_PRESET_SCHEMES[OPTIMIZING_AS_DEFAULT][1];

	private int										resultSetHoldability	= OPTIMIZING_PRESET_SCHEMES[OPTIMIZING_AS_DEFAULT][2];

	public SQLKit(SQLSource source)
	{
		this.setSource(source);
		this.setConnection(source.getConnection());
		this.setStatements(new HashMap<String, Statement>());
	}

	public void addBatch(Iterable<Object> params) throws SQLException
	{
		this.addBatch((PreparedStatement) statement, params);
	}

	public void addBatch(JSON params) throws SQLException
	{
		this.addBatch((PreparedStatement) statement, params);
	}

	public void addBatch(Map<String, Object> params) throws SQLException
	{
		this.addBatch((PreparedStatement) statement, params);
	}

	public void addBatch(Object... params) throws SQLException
	{
		this.addBatch((PreparedStatement) statement, params);
	}

	public void addBatch(PreparedStatement statement, Iterable<Object> params) throws SQLException
	{
		this.fillParameters(statement, params);
		statement.addBatch();
	}

	public void addBatch(PreparedStatement statement, JSON params) throws SQLException
	{
		List<String> keys = parameters.get(statement);
		this.fillParameters(statement, fillParametersList(keys, params));
		statement.addBatch();
	}

	public void addBatch(PreparedStatement statement, Map<String, Object> params) throws SQLException
	{
		List<String> keys = parameters.get(statement);
		this.fillParameters(statement, fillParametersList(keys, params));
		statement.addBatch();
	}

	public void addBatch(PreparedStatement statement, Object... params) throws SQLException
	{
		this.fillParameters(statement, params);
		statement.addBatch();
	}

	public void addBatch(String sql) throws SQLException
	{
		statement.addBatch(sql);
	}

	public void cancel()
	{
		cancel(statement);
	}

	public void cancel(Statement statement)
	{
		if (statement != null)
		{
			try
			{
				statement.cancel();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
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
		statement = null;
		if (statements != null)
		{
			for (Statement s : statements.values())
			{
				try
				{
					s.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
			statements.clear();
			statements = null;
		}
		this.getSource().close(this);
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
		statement = statements.get(sql);

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
			statements.put(sql, statement);
		}

		return statement;
	}

	public int[] executeBatch() throws SQLException
	{
		return executeBatch(statement);
	}

	public int[] executeBatch(Statement statement) throws SQLException
	{
		return statement.executeBatch();
	}

	public PreparedStatement fillParameters(Iterable<Object> params) throws SQLException
	{
		return fillParameters((PreparedStatement) statement, params);
	}

	public PreparedStatement fillParameters(Object[] params) throws SQLException
	{
		return fillParameters((PreparedStatement) statement, params);
	}

	public PreparedStatement fillParameters(PreparedStatement statement, Iterable<Object> params) throws SQLException
	{
		statement.clearParameters();
		int index = 1;
		for (Object param : params)
		{
			if (param == EMPTY)
			{
				if (index != 1)
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
				else
				{
					statement.setObject(index, param);
				}
				index++;
			}
		}
		return statement;
	}

	public PreparedStatement fillParameters(PreparedStatement statement, Object[] params) throws SQLException
	{
		statement.clearParameters();
		int index = 1;
		for (Object param : params)
		{
			if (param == EMPTY)
			{
				if (index != 1)
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
				else
				{
					statement.setObject(index, param);
				}
				index++;
			}
		}

		return statement;
	}

	protected void finalize() throws Throwable
	{
		this.close();
		super.finalize();
	}

	public Connection getConnection()
	{
		return connection;
	}

	public SQLSource getSource()
	{
		return source;
	}

	public Statement getStatement()
	{
		return statement;
	}

	public Map<String, Statement> getStatements()
	{
		return statements;
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
		return this.getSource().isClosed(this);
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
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null)
		{
			ps = this.getConnection().prepareStatement(sql,
					autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
			statements.put(sql, ps);
		}

		statement = ps;

		return ps;
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
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null)
		{
			try
			{
				ps = this.getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency,
						resultSetHoldability);
			}
			catch (SQLException e)
			{
				ps = this.getConnection().prepareStatement(sql);
			}
			statements.put(sql, ps);
		}

		statement = ps;

		return ps;
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
	{
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null)
		{
			ps = this.getConnection().prepareStatement(sql, columnIndexes);
			statements.put(sql, ps);
		}

		statement = ps;

		return ps;
	}

	public PreparedStatement prepareStatement(String sql, Iterable<String> params) throws SQLException
	{
		return prepareStatement(sql, params, resultSetType);
	}

	public PreparedStatement prepareStatement(String sql, Iterable<String> params, boolean autoGeneratedKeys)
			throws SQLException
	{
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null)
		{
			ps = this.getConnection().prepareStatement(replaceParameters(sql, params),
					autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
			statements.put(sql, ps);
			TreeMap<Integer, String> index = indexOfParameters(sql, params);
			parameters.put(ps, new LinkedList<String>(index.values()));
		}

		statement = ps;

		return ps;
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
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null)
		{
			try
			{
				ps = this.getConnection().prepareStatement(replaceParameters(sql, params), resultSetType,
						resultSetConcurrency, resultSetHoldability);
			}
			catch (SQLException e)
			{
				ps = this.getConnection().prepareStatement(replaceParameters(sql, params));
			}
			statements.put(sql, ps);
			TreeMap<Integer, String> index = indexOfParameters(sql, params);
			parameters.put(ps, new LinkedList<String>(index.values()));
		}

		statement = ps;

		return ps;
	}

	public PreparedStatement prepareStatement(String sql, Iterable<String> params, int[] columnIndexes)
			throws SQLException
	{
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null)
		{
			ps = this.getConnection().prepareStatement(replaceParameters(sql, params), columnIndexes);
			statements.put(sql, ps);
			TreeMap<Integer, String> index = indexOfParameters(sql, params);
			parameters.put(ps, new LinkedList<String>(index.values()));
		}

		statement = ps;

		return ps;
	}

	public PreparedStatement prepareStatement(String sql, Iterable<String> params, String[] columnNames)
			throws SQLException
	{
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null)
		{
			ps = this.getConnection().prepareStatement(replaceParameters(sql, params), columnNames);
			statements.put(sql, ps);
			TreeMap<Integer, String> index = indexOfParameters(sql, params);
			parameters.put(ps, new LinkedList<String>(index.values()));
		}

		statement = ps;

		return ps;
	}

	public PreparedStatement prepareStatement(String sql, Map<String, Object> params) throws SQLException
	{
		return prepareStatement(sql, params, resultSetType);
	}

	public PreparedStatement prepareStatement(String sql, Map<String, Object> params, boolean autoGeneratedKeys)
			throws SQLException
	{
		return prepareStatement(sql, params.keySet(), autoGeneratedKeys);
	}

	public PreparedStatement prepareStatement(String sql, Map<String, Object> params, int resultSetType)
			throws SQLException
	{
		return prepareStatement(sql, params, resultSetType, resultSetConcurrency);
	}

	public PreparedStatement prepareStatement(String sql, Map<String, Object> params, int resultSetType,
			int resultSetConcurrency) throws SQLException
	{
		return prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public PreparedStatement prepareStatement(String sql, Map<String, Object> params, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		return prepareStatement(sql, params.keySet(), resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public PreparedStatement prepareStatement(String sql, Map<String, Object> params, int[] columnIndexes)
			throws SQLException
	{
		return prepareStatement(sql, params.keySet(), columnIndexes);
	}

	public PreparedStatement prepareStatement(String sql, Map<String, Object> params, String[] columnNames)
			throws SQLException
	{
		return prepareStatement(sql, params.keySet(), columnNames);
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
	{
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null)
		{
			ps = this.getConnection().prepareStatement(sql, columnNames);
			statements.put(sql, ps);
		}

		statement = ps;

		return ps;
	}

	public ResultSet query(PreparedStatement statement, Iterable<Object> params) throws SQLException
	{
		return fillParameters(statement, params).executeQuery();
	}

	public ResultSet query(PreparedStatement statement, Object... params) throws SQLException
	{
		return fillParameters(statement, params).executeQuery();
	}

	public ResultSet query(Statement statement, String sql) throws SQLException
	{
		return statement.executeQuery(sql);
	}

	public ResultSet query(String sql) throws SQLException
	{
		return query(createStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql);
	}

	public ResultSet query(String sql, Iterable<Object> params) throws SQLException
	{
		return query(prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public ResultSet query(String sql, JSON params) throws SQLException
	{
		PreparedStatement ps = prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
		List<String> keys = parameters.get(ps);
		return query(ps, fillParametersList(keys, params));
	}

	public ResultSet query(String sql, Map<String, Object> params) throws SQLException
	{
		PreparedStatement ps = prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
		List<String> keys = parameters.get(ps);
		return query(ps, fillParametersList(keys, params));
	}

	public ResultSet query(String sql, Object... params) throws SQLException
	{
		return query(prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException
	{
		this.getConnection().releaseSavepoint(savepoint);
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

	public void setAutoCommit(Boolean autoCommit)
	{
		if (autoCommit != null)
		{
			try
			{
				this.getConnection().setAutoCommit(autoCommit);
			}
			catch (Exception e)
			{
			}
		}
	}

	public void setConnection(Connection connection)
	{
		this.connection = connection;
	}

	public void setSource(SQLSource source)
	{
		this.source = source;
	}

	public void setStatement(Statement statement)
	{
		this.statement = statement;
	}

	public void setStatements(Map<String, Statement> statements)
	{
		this.statements = statements;
	}

	public int update(PreparedStatement statement, Iterable<Object> params) throws SQLException
	{
		return fillParameters(statement, params).executeUpdate();
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
		return fillParameters(statement, params).executeUpdate();
	}

	public int update(Statement statement, String sql) throws SQLException
	{
		return statement.executeUpdate(sql);
	}

	public int update(String sql) throws SQLException
	{
		return update(createStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql);
	}

	public int update(String sql, Iterable<Object> params) throws SQLException
	{
		return update(prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}

	public int update(String sql, JSON params) throws SQLException
	{
		PreparedStatement ps = prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
		List<String> keys = parameters.get(ps);
		return update(ps, fillParametersList(keys, params));
	}

	public int update(String sql, Map<String, Object> params) throws SQLException
	{
		PreparedStatement ps = prepareStatement(sql, params, resultSetType, resultSetConcurrency, resultSetHoldability);
		List<String> keys = parameters.get(ps);
		return update(ps, fillParametersList(keys, params));
	}

	public int update(String sql, Object... params) throws SQLException
	{
		return update(prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), params);
	}
}
