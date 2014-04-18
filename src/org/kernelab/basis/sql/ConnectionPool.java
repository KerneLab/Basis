package org.kernelab.basis.sql;

import java.sql.Connection;
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

	public ConnectionPool(ConnectionProvider factory, int limit)
	{
		this(factory, limit, true);
	}

	public ConnectionPool(ConnectionProvider factory, int limit, boolean lazy)
	{
		super(limit, lazy);
		this.setProvider(factory);
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
	protected Connection newElement()
	{
		try
		{
			return provider.provideConnection();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public Connection provideConnection() throws SQLException
	{
		return this.provide();
	}

	public void recycleConnection(Connection c) throws SQLException
	{
		if (c != null)
		{
			this.recycle(c);
		}
	}

	protected ConnectionPool setProvider(ConnectionProvider factory)
	{
		this.provider = factory;
		return this;
	}
}
