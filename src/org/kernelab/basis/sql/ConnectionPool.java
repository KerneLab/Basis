package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * The ConnectionPool class is an abstract connection pool of database support
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
public class ConnectionPool
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private String		dataSourceName;

	private DataSource	dataSource;

	/**
	 * Create a connection pool with the given data source name.
	 * 
	 * @param dataSourceName
	 *            The name of data source such as "jdbc/mysql".
	 */
	public ConnectionPool(String dataSourceName)
	{
		this.setDataSourceName(dataSourceName);

		DataSource dataSource = null;

		try {
			InitialContext initialContext = new InitialContext();
			dataSource = (DataSource) initialContext.lookup("java:comp/env/"
					+ this.getDataSourceName());
		} catch (NamingException e) {
			e.printStackTrace();
		}

		this.setDataSource(dataSource);
	}

	public Connection getConnection()
	{
		Connection connection = null;

		try {
			connection = this.getDataSource().getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return connection;
	}

	public DataSource getDataSource()
	{
		return dataSource;
	}

	public String getDataSourceName()
	{
		return dataSourceName;
	}

	public SQLKit getSQLKit()
	{
		return new SQLKit(this.getConnection());
	}

	public void setDataSource(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	public void setDataSourceName(String dataSourceName)
	{
		this.dataSourceName = dataSourceName;
	}

}
