package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

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
	public static final Object	NULL	= new Object();

	/**
	 * To declare that there is no parameter.<br />
	 * This object would be ignored if it is not the first parameter.
	 */
	public static final Object	EMPTY	= new Object();

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

		if (rs != null) {

			try {

				int row = rs.getRow();
				rs.last();
				number = rs.getRow();

				if (row == 0) {
					rs.beforeFirst();
				} else {
					rs.absolute(row);
				}

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return number;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		DataBase db = new MySQL("test", "root", "root");

		SQLKit kit = null;
		try {
			kit = db.getSQLKit();

			ResultSet rs = kit.query("SELECT * FROM `table` WHERE id>?", 3);

			while (rs.next()) {
				Tools.debug(rs.getString(1) + "\t" + rs.getString(2));
			}

			int num = kit.update("UPDATE `table` SET `text`=? WHERE `id`<?", "HEY!", 5);

			Tools.debug(num);

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			kit.close();
		}
	}

	private Connection				connection;

	private Statement				statement;

	private Map<String, Statement>	statements;

	public SQLKit(Connection connection)
	{
		this.setConnection(connection);
		this.setStatements(new HashMap<String, Statement>());
	}

	public void addBatch(Iterable<Object> params) throws SQLException
	{
		if (statement instanceof PreparedStatement) {
			this.fillParameters(params);
			((PreparedStatement) statement).addBatch();
		} else {
			throw new SQLException(
					"Only PreparedStatement could be filled with parameters.");
		}
	}

	public void addBatch(Object... params) throws SQLException
	{
		if (statement instanceof PreparedStatement) {
			this.fillParameters(params);
			((PreparedStatement) statement).addBatch();
		} else {
			throw new SQLException(
					"Only PreparedStatement could be filled with parameters.");
		}
	}

	public void addBatch(String sql) throws SQLException
	{
		statement.addBatch(sql);
	}

	public void clearBatch() throws SQLException
	{
		if (statement != null) {
			statement.clearBatch();
		}
	}

	public void close()
	{
		statement = null;
		if (statements != null) {
			for (Statement s : statements.values()) {
				try {
					s.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			statements.clear();
			statements = null;
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			connection = null;
		}
	}

	public void commit() throws SQLException
	{
		if (!this.isAutoCommit()) {
			connection.commit();
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
		this.executeBatch();
		this.commit();
		this.clearBatch();
	}

	public Statement createStatement(String sql) throws SQLException
	{
		statement = statements.get(sql);

		if (statement == null) {
			statement = connection.createStatement();
			statements.put(sql, statement);
		}

		return statement;
	}

	public Statement createStatement(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException
	{
		statement = statements.get(sql);

		if (statement == null) {
			statement = connection.createStatement(resultSetType, resultSetConcurrency);
			statements.put(sql, statement);
		}

		return statement;
	}

	public Statement createStatement(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		statement = statements.get(sql);

		if (statement == null) {
			statement = connection.createStatement(resultSetType, resultSetConcurrency,
					resultSetHoldability);
			statements.put(sql, statement);
		}

		return statement;
	}

	public int[] executeBatch() throws SQLException
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

	public PreparedStatement fillParameters(PreparedStatement statement,
			Iterable<Object> params) throws SQLException
	{
		statement.clearParameters();
		int index = 1;
		for (Object param : params) {
			if (param == EMPTY) {
				if (index != 1) {
					throw new SQLException("SQLKit.EMPTY should be the first parameter.");
				}
				break;
			} else {
				if (param == NULL) {
					statement.setObject(index, null);
				} else {
					statement.setObject(index, param);
				}
				index++;
			}
		}
		return statement;
	}

	public PreparedStatement fillParameters(PreparedStatement statement, Object[] params)
			throws SQLException
	{
		statement.clearParameters();
		int index = 1;
		for (Object param : params) {
			if (param == EMPTY) {
				if (index != 1) {
					throw new SQLException("SQLKit.EMPTY should be the first parameter.");
				}
				break;
			} else {
				if (param == NULL) {
					statement.setObject(index, null);
				} else {
					statement.setObject(index, param);
				}
				index++;
			}
		}

		return statement;
	}

	public Connection getConnection()
	{
		return connection;
	}

	public Statement getCurrentStatement()
	{
		return statement;
	}

	public Statement getStatement()
	{
		return statement;
	}

	public Map<String, Statement> getStatements()
	{
		return statements;
	}

	public boolean isAutoCommit() throws SQLException
	{
		return connection.getAutoCommit();
	}

	public boolean isClosed()
	{
		boolean is = false;

		if (connection != null) {
			try {
				is = connection.isClosed();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return is;
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
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null) {
			statement = ps = connection.prepareStatement(sql);
			statements.put(sql, statement);
		}

		return ps;
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
			throws SQLException
	{
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null) {
			statement = ps = connection.prepareStatement(sql, autoGeneratedKeys);
			statements.put(sql, statement);
		}

		return ps;
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException
	{
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null) {
			statement = ps = connection.prepareStatement(sql, resultSetType,
					resultSetConcurrency);
			statements.put(sql, statement);
		}

		return ps;
	}

	public Statement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null) {
			statement = ps = connection.prepareStatement(sql, resultSetType,
					resultSetConcurrency, resultSetHoldability);
			statements.put(sql, statement);
		}

		return ps;
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
			throws SQLException
	{
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null) {
			statement = ps = connection.prepareStatement(sql, columnIndexes);
			statements.put(sql, statement);
		}

		return ps;
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames)
			throws SQLException
	{
		PreparedStatement ps = (PreparedStatement) statements.get(sql);

		if (ps == null) {
			statement = ps = connection.prepareStatement(sql, columnNames);
			statements.put(sql, statement);
		}

		return ps;
	}

	public ResultSet query(PreparedStatement statement, String sql,
			Iterable<Object> params) throws SQLException
	{
		return fillParameters(statement, params).executeQuery();
	}

	public ResultSet query(PreparedStatement statement, String sql, Object... params)
			throws SQLException
	{
		return fillParameters(statement, params).executeQuery();
	}

	/**
	 * @since 2011.12.11
	 */
	public ResultSet query(Statement statement, String sql) throws SQLException
	{
		return statement.executeQuery(sql);
	}

	public ResultSet query(String sql) throws SQLException
	{
		return query(createStatement(sql), sql);
	}

	/**
	 * @since 2011.12.11
	 */
	public ResultSet query(String sql, Iterable<Object> params) throws SQLException
	{
		return query(prepareStatement(sql), sql, params);
	}

	/**
	 * @since 2011.12.11
	 */
	public ResultSet query(String sql, Object... params) throws SQLException
	{
		return query(prepareStatement(sql), sql, params);
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException
	{
		connection.releaseSavepoint(savepoint);
	}

	public void rollback() throws SQLException
	{
		connection.rollback();
	}

	public void rollback(Savepoint savepoint) throws SQLException
	{
		connection.rollback(savepoint);
	}

	public Savepoint savepoint() throws SQLException
	{
		return connection.setSavepoint();
	}

	public Savepoint savepoint(String name) throws SQLException
	{
		return connection.setSavepoint(name);
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException
	{
		connection.setAutoCommit(autoCommit);
	}

	public void setConnection(Connection connection)
	{
		this.connection = connection;
	}

	public void setCurrentStatement(Statement statement)
	{
		this.statement = statement;
	}

	public void setStatements(Map<String, Statement> statements)
	{
		this.statements = statements;
	}

	public int update(PreparedStatement statement, String sql, Iterable<Object> params)
			throws SQLException
	{
		return fillParameters(statement, params).executeUpdate();
	}

	public int update(PreparedStatement statement, String sql, Object... params)
			throws SQLException
	{
		return fillParameters(statement, params).executeUpdate();
	}

	public int update(Statement statement, String sql) throws SQLException
	{
		return statement.executeUpdate(sql);
	}

	public int update(String sql) throws SQLException
	{
		return update(createStatement(sql), sql);
	}

	/**
	 * @since 2011.12.11
	 */
	public int update(String sql, Iterable<Object> params) throws SQLException
	{
		return update(prepareStatement(sql), sql, params);
	}

	/**
	 * <pre>
	 * kit.update(&quot;INSERT INTO `user` (`id`,`name`) VALUES (?,?)&quot;, SQLKit.NULL, &quot;Taylor&quot;);
	 * 
	 * ResultSet rs = kit.getStatement().getGeneratedKeys();
	 * while (rs.next()) {
	 * 	Tools.debug(rs.getInt(1));
	 * 	// Return the last value of id if id is an auto-generated key.
	 * }
	 * </pre>
	 * 
	 * @since 2011.12.11
	 */
	public int update(String sql, Object... params) throws SQLException
	{
		return update(prepareStatement(sql), sql, params);
	}
}
