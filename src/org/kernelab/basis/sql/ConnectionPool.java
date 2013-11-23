package org.kernelab.basis.sql;

import java.sql.Connection;

import org.kernelab.basis.AbstractPool;

public class ConnectionPool extends AbstractPool<Connection> implements ConnectionSource
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private ConnectionFactory	factory;

	public ConnectionPool(ConnectionFactory factory, int limit)
	{
		this(factory, limit, false);
	}

	public ConnectionPool(ConnectionFactory factory, int limit, boolean lazy)
	{
		super(limit);
		this.setFactory(factory);
	}

	public void close(SQLKit kit)
	{
		Connection c = kit.getConnection();

		kit.setConnection(null);

		this.recycle(c);
	}

	public Connection getConnection()
	{
		return this.provide();
	}

	public ConnectionFactory getFactory()
	{
		return factory;
	}

	public SQLKit getSQLKit()
	{
		return new SQLKit(this);
	}

	@Override
	protected Connection newElement()
	{
		try
		{
			return factory.newConnection();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	protected ConnectionPool setFactory(ConnectionFactory factory)
	{
		this.factory = factory;
		return this;
	}
}
