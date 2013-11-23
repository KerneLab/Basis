package org.kernelab.basis.sql;

import java.sql.Connection;

public interface ConnectionFactory
{
	/**
	 * To create a new Connection.
	 * 
	 * @return
	 * @throws Exception
	 */
	public Connection newConnection() throws Exception;
}
