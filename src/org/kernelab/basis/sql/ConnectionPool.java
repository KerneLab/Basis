package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.kernelab.basis.AbstractPool;

public class ConnectionPool extends AbstractPool<Connection> implements ConnectionManager
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private ConnectionProvider	provider;

	public ConnectionPool(ConnectionProvider provider, int limit)
	{
		this(provider, limit, 0);
	}

	public ConnectionPool(ConnectionProvider provider, int limit, int init)
	{
		super(limit, init);
		this.setProvider(provider);
	}

	public ConnectionProvider getProvider()
	{
		return provider;
	}

	public SQLKit getSQLKit()
	{
		SQLKit kit = null;

		try
		{
			kit = new SQLKit(this);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return kit;
	}

	@Override
	protected Connection newElement(long timeout)
	{
		try
		{
			return provider.provideConnection(timeout);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public Connection provideConnection() throws SQLException
	{
		return provideConnection(0L);
	}

	public Connection provideConnection(long timeout) throws SQLException
	{
		Connection conn = this.provide(timeout);

		if (conn != null)
		{
			try
			{
				conn.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			}
			catch (SQLException e)
			{
			}
		}

		return conn;
	}

	public void recycleConnection(Connection conn) throws SQLException
	{
		if (conn != null)
		{
			if (conn.isClosed())
			{
				throw new SQLException("Connection has already been closed.");
			}

			try
			{
				if (!conn.getAutoCommit())
				{
					conn.rollback();
					conn.setAutoCommit(true);
				}

				conn.setReadOnly(false);
				conn.setTransactionIsolation(conn.getMetaData().getDefaultTransactionIsolation());
			}
			catch (SQLException e)
			{
			}

			this.recycle(conn);
		}
	}

	protected ConnectionPool setProvider(ConnectionProvider factory)
	{
		this.provider = factory;
		return this;
	}
}
