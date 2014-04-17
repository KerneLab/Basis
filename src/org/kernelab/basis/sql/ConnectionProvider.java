package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionProvider
{
	/**
	 * To provide a valid Connection.
	 * 
	 * @return
	 * @throws Exception
	 */
	public Connection provideConnection() throws SQLException;
}
