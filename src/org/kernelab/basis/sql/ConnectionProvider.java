package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionProvider
{
	/**
	 * To provide a valid Connection.
	 * 
	 * @param timeout
	 *            The maximum milliseconds to be waited, 0 means always wait
	 *            until an available Connection is returned.
	 * @return
	 * @throws Exception
	 */
	public Connection provideConnection(long timeout) throws SQLException;
}
