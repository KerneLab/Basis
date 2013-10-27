package org.kernelab.basis.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Sequel
{
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

	private Statement		statement;

	private boolean			resultSetObject	= false;

	private ResultSet		resultSet;

	private int				updateCount		= N_A;

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
