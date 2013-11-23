package org.kernelab.basis.sql;

import java.sql.Connection;

public interface ConnectionSource
{
	/**
	 * Close the SQLKit. Usually recycle the connection within it.
	 * 
	 * @param kit
	 */
	public void close(SQLKit kit);

	/**
	 * Get an available Connection.
	 * 
	 * @return
	 */
	public Connection getConnection();

	public SQLKit getSQLKit();
}
