package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionManager extends ConnectionProvider
{
	/**
	 * To recycle a Connection.
	 * 
	 * @param c
	 * @return
	 * @throws Exception
	 */
	public void recycleConnection(Connection c) throws SQLException;
}
