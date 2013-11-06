package org.kernelab.basis.sql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;

public class Sequel implements Iterable<Sequel>
{
	public static class JSONIterator<T extends JSON> implements Iterable<T>, Iterator<T>
	{
		public static JSAN jsanOfResultRow(ResultSet rs, String[] head)
		{
			JSAN jsan = new JSAN();

			for (int i = 0; i < head.length; i++)
			{
				try
				{
					jsan.add(rs.getObject(i + 1));
				}
				catch (SQLException e)
				{
				}
			}

			return jsan;
		}

		@SuppressWarnings("unchecked")
		public static <T extends JSON> T jsonOfResultRow(ResultSet rs, Class<T> type, Map<String, Object> map)
				throws SQLException
		{
			if (type == JSAN.class)
			{
				return (T) SQLKit.jsanOfResultRow(rs, map);
			}
			else
			{
				return (T) SQLKit.jsonOfResultRow(rs, map);
			}
		}

		private ResultSet			rs;

		private Class<T>			type;

		private Map<String, Object>	map;

		private boolean				next;

		private T					curr;

		public JSONIterator(ResultSet rs)
		{
			this(rs, null);
		}

		public JSONIterator(ResultSet rs, Class<T> type)
		{
			this(rs, type, null);
		}

		public JSONIterator(ResultSet rs, Class<T> type, Map<String, Object> map)
		{
			this.rs = rs;

			this.type = type;

			try
			{
				if (map == null)
				{
					map = new LinkedHashMap<String, Object>();

					ResultSetMetaData meta = rs.getMetaData();

					int length = meta.getColumnCount();

					for (int i = 0; i < length; i++)
					{
						if (type == JSAN.class)
						{
							map.put(String.valueOf(i), i + 1);
						}
						else
						{
							map.put(meta.getColumnName(i + 1), meta.getColumnName(i + 1));
						}
					}
				}

				this.map = map;

				this.next = rs.next();

				if (this.next)
				{
					this.curr = jsonOfResultRow(rs, type, this.map);
				}
			}
			catch (Exception e)
			{
				this.next = false;
			}
		}

		public boolean hasNext()
		{
			return next;
		}

		public Iterator<T> iterator()
		{
			return this;
		}

		public T next()
		{
			T last = curr;

			try
			{
				next = rs.next();
				curr = jsonOfResultRow(rs, type, map);
			}
			catch (SQLException e)
			{
				next = false;
			}

			return last;
		}

		public void remove()
		{

		}
	}

	private class SequelIterator implements Iterator<Sequel>
	{
		private boolean	first	= true;

		public boolean hasNext()
		{
			return hasResult();
		}

		public Sequel next()
		{
			if (first)
			{
				first = false;
				return Sequel.this;
			}
			else
			{
				return nextResult();
			}
		}

		public void remove()
		{

		}
	}

	public static final int	N_A				= -1;

	/**
	 * No result currently.
	 */
	public static final int	RESULT_NONE		= N_A;

	/**
	 * Current result is a ResultSet.
	 */
	public static final int	RESULT_SET		= 0;

	/**
	 * Current result is a update count.
	 */
	public static final int	RESULT_COUNT	= 1;

	public static JSONIterator<JSON> iterate(ResultSet rs)
	{
		return new JSONIterator<JSON>(rs);
	}

	public static <T extends JSON> JSONIterator<T> iterate(ResultSet rs, Class<T> type)
	{
		return new JSONIterator<T>(rs, type);
	}

	public static <T extends JSON> JSONIterator<T> iterate(ResultSet rs, Class<T> type, Map<String, Object> map)
	{
		return new JSONIterator<T>(rs, type, map);
	}

	private Statement	statement;

	private boolean		resultSetObject	= false;

	private ResultSet	resultSet;

	private int			updateCount		= N_A;

	public Sequel(Statement statement, boolean resultSet)
	{
		this.setStatement(statement).hasResultSetObject(resultSet).refreshUpdateCount();
	}

	public ResultSet getResultSet()
	{
		return resultSet;
	}

	public int getResultType()
	{
		int type = RESULT_NONE;

		if (this.getResultSet() != null)
		{
			type = RESULT_SET;
		}
		else if (this.getUpdateCount() != N_A)
		{
			type = RESULT_COUNT;
		}

		return type;
	}

	public Statement getStatement()
	{
		return statement;
	}

	public int getUpdateCount()
	{
		return updateCount;
	}

	public boolean hasResult()
	{
		return this.hasResultSetObject() || this.getUpdateCount() != N_A;
	}

	private boolean hasResultSetObject()
	{
		return resultSetObject;
	}

	private Sequel hasResultSetObject(boolean resultSetObject)
	{
		this.resultSetObject = resultSetObject;

		if (resultSetObject)
		{
			try
			{
				this.setResultSet(statement.getResultSet());
			}
			catch (SQLException e)
			{
				this.setResultSet(null);
			}
		}
		else
		{
			this.setResultSet(null);
		}

		return this;
	}

	public boolean isResultSet()
	{
		return this.getResultSet() != null;
	}

	public boolean isUpdateCount()
	{
		return this.getUpdateCount() != N_A;
	}

	public JSONIterator<JSON> iterate()
	{
		return iterate(this.getResultSet());
	}

	public <T extends JSON> JSONIterator<T> iterate(Class<T> type)
	{
		return iterate(this.getResultSet(), type);
	}

	public <T extends JSON> JSONIterator<T> iterate(Class<T> type, Map<String, Object> map)
	{
		return iterate(this.getResultSet(), type, map);
	}

	public Iterator<Sequel> iterator()
	{
		return new SequelIterator();
	}

	public Sequel nextResult()
	{
		return nextResult(Statement.CLOSE_CURRENT_RESULT);
	}

	public Sequel nextResult(int current)
	{
		try
		{
			this.hasResultSetObject(this.getStatement().getMoreResults(current));
		}
		catch (SQLException e)
		{
			this.hasResultSetObject(false);
		}

		this.refreshUpdateCount();

		return this;
	}

	private Sequel refreshUpdateCount()
	{
		try
		{
			this.updateCount = statement.getUpdateCount();
		}
		catch (SQLException e)
		{
			this.updateCount = N_A;
		}
		return this;
	}

	private Sequel setResultSet(ResultSet resultSet)
	{
		this.resultSet = resultSet;
		return this;
	}

	private Sequel setStatement(Statement statement)
	{
		this.statement = statement;
		return this;
	}
}
