package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.SQLException;

import org.kernelab.basis.Tools;

public class ConnectionAgent implements ConnectionManager
{
	private Connection	connection;

	private boolean		recyclose;

	public ConnectionAgent(Connection c)
	{
		this(c, false);
	}

	public ConnectionAgent(Connection c, boolean recyclose)
	{
		this.connection = c;
		this.recyclose = recyclose;
	}

	public SQLKit getSQLKit() throws SQLException
	{
		return new SQLKit(this);
	}

	public boolean isRecyclose()
	{
		return recyclose;
	}

	public boolean isValid(Connection conn)
	{
		return true;
	}

	public Connection provideConnection(long timeout) throws SQLException
	{
		return connection;
	}

	public void recycleConnection(Connection c) throws SQLException
	{
		if (recyclose && c != null)
		{
			c.close();
		}
	}

	public <T extends ConnectionAgent> T setConnection(Connection connection)
	{
		this.connection = connection;
		return Tools.cast(this);
	}

	public <T extends ConnectionAgent> T setRecyclose(boolean recyclose)
	{
		this.recyclose = recyclose;
		return Tools.cast(this);
	}
}
