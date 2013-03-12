package org.kernelab.basis.sql;

import java.sql.Connection;

public interface SQLSource
{
	public void close(SQLKit kit);

	public Connection getConnection();

	public SQLKit getSQLKit();

	public boolean isClosed();
}
