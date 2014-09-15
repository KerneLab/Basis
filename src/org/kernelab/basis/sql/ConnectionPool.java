package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.kernelab.basis.AbstractPool;

public class ConnectionPool extends AbstractPool<Connection> implements ConnectionManager
{
	public static boolean isValid(Connection c)
	{
		boolean is = true;

		try
		{
			boolean ac = c.getAutoCommit();

			if (ac)
			{
				c.setAutoCommit(false);
			}

			c.rollback();

			if (ac)
			{
				c.setAutoCommit(true);
			}
		}
		catch (SQLException e)
		{
			is = false;
		}

		return is;
	}

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
		return getSQLKit(0);
	}

	public SQLKit getSQLKit(long timeout)
	{
		try
		{
			return new SQLKit(this, timeout);
		}
		catch (SQLException e)
		{
			return null;
		}
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
			if (conn.isClosed() || !isValid(conn))
			{
				this.discard(conn);
				return;
			}

			try
			{
				if (!conn.getAutoCommit())
				{
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
