package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionAgent implements ConnectionManager
{
	private Connection	connection;

	public ConnectionAgent(Connection c)
	{
		connection = c;
	}

	public SQLKit getSQLKit() throws SQLException
	{
		return new SQLKit(this);
	}

	public Connection provideConnection() throws SQLException
	{
		return connection;
	}

	public void recycleConnection(Connection c) throws SQLException
	{
		c.close();
	}
}
