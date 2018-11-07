package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.kernelab.basis.AbstractPool;

public class ConnectionPool extends AbstractPool<Connection> implements ConnectionManager
{
	public static boolean isValid(Connection c)
	{
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

			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private ConnectionProvider provider;

	public ConnectionPool(ConnectionProvider provider, int limit)
	{
		this(provider, limit, 0);
	}

	public ConnectionPool(ConnectionProvider provider, int limit, int init)
	{
		super(limit, init);
		this.setProvider(provider);
		this.setInit(init);
	}

	protected void discardConnection(Connection conn)
	{
		if (conn != null)
		{
			try
			{
				conn.close();
			}
			catch (Exception e)
			{
			}
			finally
			{
				this.discard(conn);
			}
		}
	}

	public ConnectionProvider getProvider()
	{
		return provider;
	}

	public SQLKit getSQLKit() throws SQLException
	{
		return getSQLKit(0);
	}

	public SQLKit getSQLKit(long timeout) throws SQLException
	{
		return new SQLKit(this, timeout);
	}

	@Override
	protected Connection newElement(long timeout) throws SQLException
	{
		return provider.provideConnection(timeout);
	}

	public Connection provideConnection() throws SQLException
	{
		return provideConnection(0L);
	}

	public Connection provideConnection(long timeout) throws SQLException
	{
		try
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
		catch (Exception e)
		{
			throw new SQLException(e.getLocalizedMessage());
		}
	}

	public void recycleConnection(Connection conn) throws SQLException
	{
		if (conn != null)
		{
			if (!isValid(conn))
			{
				this.discardConnection(conn);
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
				this.recycle(conn);
			}
			catch (SQLException e)
			{
				this.discardConnection(conn);
			}
		}
	}

	protected ConnectionPool setProvider(ConnectionProvider factory)
	{
		this.provider = factory;
		return this;
	}
}
