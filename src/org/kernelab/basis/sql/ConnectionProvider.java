package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionProvider
{
	/**
	 * Check the given Connection is valid or not.
	 * 
	 * @param element
	 *            the given Connection to be checked.
	 * @return true if and only if the given Connection is valid.
	 */
	public boolean isValid(Connection conn);

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
