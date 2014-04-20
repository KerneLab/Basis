package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * The DataSource is a class which wraps javax.sql.DataSource.
 * 
 * To support various database, but the connection resource should be config in
 * follow files: <br>
 * 
 * <pre>
 * <b>"%CATALINA_HOME%/HOSTNAME/appName.xml"</b><br>
 * {@code <Context path="/appName" docBase="%appPath%" reloadable="true">
 *   <Resource name="jdbc/mysql" username="root" password="root"
 *    type="javax.sql.DataSource" driverClassName="com.mysql.jdbc.Driver"
 *    url="jdbc:mysql://localhost:3306/test"
 *    maxIdle="0"  maxWait="5000" maxActive="0" />
 * </Context>}
 * <br>
 * <b>"/META-INF/context.xml"</b><br> 
 * {@code <Context docBase="%appPath%" path="/appName" reloadable="true">
 *   <ResourceLink name="jdbc/mysql" global="jdbc/mysql" type="javax.sql.DataSource" />
 * </Context>}
 * <br>
 * <b>"/WEB-INF/web.xml"</b><br> 
 * {@code <web-app>
 * ... 
 * <resource-ref> 
 *   <description>DB Connection</description>
 *   <res-ref-name>jdbc/mysql</res-ref-name>
 *   <res-type>javax.sql.DataSource</res-type>
 *   <res-auth>Container</res-auth>
 * </resource-ref>
 * ...}
 * </pre>
 * 
 * @author Dilly King
 * @version 1.1.0
 * @update 2010-02-18
 */
public class DataSource implements ConnectionManager
{
	private String					dataSourceName;

	private javax.sql.DataSource	dataSource;

	/**
	 * Create a Data Source with the given data source object.
	 * 
	 * @param ds
	 *            A data source object.
	 */
	public DataSource(javax.sql.DataSource ds)
	{
		this.setDataSource(ds);
	}

	/**
	 * Create a Data Source with the given data source name.
	 * 
	 * @param dataSourceName
	 *            The name of data source such as "jdbc/mysql".
	 */
	public DataSource(String dataSourceName)
	{
		this.setDataSourceName(dataSourceName);
	}

	public javax.sql.DataSource getDataSource()
	{
		return dataSource;
	}

	public String getDataSourceName()
	{
		return dataSourceName;
	}

	public SQLKit getSQLKit()
	{
		SQLKit kit = null;

		try
		{
			kit = new SQLKit(this);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return kit;
	}

	public Connection provideConnection(long timeout) throws SQLException
	{
		return this.getDataSource().getConnection();
	}

	public void recycleConnection(Connection c) throws SQLException
	{
		if (c != null)
		{
			c.close();
		}
	}

	public void setDataSource(javax.sql.DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	public void setDataSourceName(String dataSourceName)
	{
		try
		{
			this.setDataSource((javax.sql.DataSource) new InitialContext().lookup("java:comp/env/" + dataSourceName));
			this.dataSourceName = dataSourceName;
		}
		catch (NamingException e)
		{
			e.printStackTrace();
		}
	}
}
