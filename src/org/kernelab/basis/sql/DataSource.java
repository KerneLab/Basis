package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

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
public class DataSource implements ConnectionFactory, ConnectionSource
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private String					dataSourceName;

	private javax.sql.DataSource	dataSource;

	private Set<SQLKit>				kits	= new HashSet<SQLKit>();

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

		javax.sql.DataSource dataSource = null;

		try
		{
			InitialContext initialContext = new InitialContext();
			dataSource = (javax.sql.DataSource) initialContext.lookup("java:comp/env/" + this.getDataSourceName());
		}
		catch (NamingException e)
		{
			e.printStackTrace();
		}

		this.setDataSource(dataSource);
	}

	public void close(SQLKit kit)
	{
		synchronized (this.getKits())
		{
			this.getKits().remove(kit);
			try
			{
				kit.getConnection().close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	public Connection getConnection()
	{
		try
		{
			return this.newConnection();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public javax.sql.DataSource getDataSource()
	{
		return dataSource;
	}

	public String getDataSourceName()
	{
		return dataSourceName;
	}

	private Set<SQLKit> getKits()
	{
		return kits;
	}

	public SQLKit getSQLKit()
	{
		SQLKit kit = null;

		synchronized (this.getKits())
		{
			kit = new SQLKit(this);
			this.getKits().add(kit);
		}

		return kit;
	}

	public boolean isClosed(SQLKit kit)
	{
		boolean is = true;

		synchronized (this.getKits())
		{
			is = !this.getKits().contains(kit);
		}

		return is;
	}

	public Connection newConnection() throws SQLException
	{
		return this.getDataSource().getConnection();
	}

	public void setDataSource(javax.sql.DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	public void setDataSourceName(String dataSourceName)
	{
		this.dataSourceName = dataSourceName;
	}
}
