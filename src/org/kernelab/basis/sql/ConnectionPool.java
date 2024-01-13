package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import org.kernelab.basis.AbstractPool;
import org.kernelab.basis.Tools;

public class ConnectionPool extends AbstractPool<Connection> implements ConnectionManager
{
	private ConnectionProvider provider;

	public ConnectionPool(ConnectionProvider provider, int limit)
	{
		this(provider, limit, 0);
	}

	public ConnectionPool(ConnectionProvider provider, int limit, int init)
	{
		super(limit, init);
		this.setProvider(provider).setInit(init);
	}

	@Override
	public void discard(final Connection conn)
	{
		try
		{
			Tools.waitFor(new Callable<Object>()
			{
				@Override
				public Object call() throws Exception
				{
					conn.close();
					return null;
				}
			}, null, -1);
			/*
			 * Do not wait for the close action to complete. Since closing a
			 * timeout connection may be blocked for a long time.
			 */
		}
		catch (Exception e)
		{
		}
		finally
		{
			super.discard(conn);
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
	public boolean isValid(Connection conn)
	{
		return this.getProvider().isValid(conn);
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

	@Override
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

	@Override
	public void recycleConnection(Connection conn)
	{
		this.recycle(conn);
	}

	@Override
	protected void resetElement(Connection conn) throws SQLException
	{
		SQLKit.reset(conn);
	}

	@Override
	protected ConnectionPool setInit(int init)
	{
		if (this.getProvider() != null)
		{
			super.setInit(init);
		}
		return this;
	}

	protected ConnectionPool setProvider(ConnectionProvider factory)
	{
		this.provider = factory;
		return this;
	}
}
