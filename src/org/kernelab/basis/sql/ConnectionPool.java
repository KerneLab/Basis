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

	private ConnectionProvider	factory;

	public ConnectionPool(ConnectionProvider factory, int limit)
	{
		this(factory, limit, true);
	}

	public ConnectionPool(ConnectionProvider factory, int limit, boolean lazy)
	{
		super(limit, lazy);
		this.setFactory(factory);
	}

	public ConnectionProvider getFactory()
	{
		return factory;
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
			return factory.provideConnection();
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
		this.recycle(c);
	}

	protected ConnectionPool setFactory(ConnectionProvider factory)
	{
		this.factory = factory;
		return this;
	}
}
