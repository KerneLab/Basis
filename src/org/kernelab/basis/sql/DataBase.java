package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.kernelab.basis.Copieable;

/**
 * The DataBase class is an abstract of database support.
 * 
 * To support various database, this class can be extended and just override the
 * method getURL().
 * 
 * JDBC3.0 and Java5.0 are required.
 * 
 * @author Dilly King
 */
public abstract class DataBase implements Copieable<DataBase>
{

	public static class DB2 extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "com.ibm.db2.jdbc.app.DB2Driver";

		public static int		DEFAULT_PORT_NUMBER	= 5000;

		public DB2(String serverName, int portNumber, String catalog, String userName,
				String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
		}

		public DB2(String userName, String passWord)
		{
			this("", userName, passWord);
		}

		public DB2(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public DB2(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		protected String getURL()
		{
			return "jdbc:db2://" + serverName + ":"
					+ DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + "/" + catalog;
		}
	}

	public static class Derby extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "org.apache.derby.jdbc.ClientDriver";

		public static int		DEFAULT_PORT_NUMBER	= 1527;

		public Derby(String serverName, int portNumber, String catalog, String userName,
				String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
			this.getInformation().setProperty("create", "true");
		}

		public Derby(String userName, String passWord)
		{
			this("", userName, passWord);
		}

		public Derby(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public Derby(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		protected String getURL()
		{
			return "jdbc:derby://" + serverName + ":"
					+ DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + "/" + catalog;
		}
	}

	public static class EmbeddeDerby extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "org.apache.derby.jdbc.EmbeddedDriver";

		public EmbeddeDerby(String serverName, int portNumber, String catalog,
				String userName, String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
			this.getInformation().setProperty("create", "true");
		}

		public EmbeddeDerby(String userName, String passWord)
		{
			this("", userName, passWord);
		}

		public EmbeddeDerby(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public EmbeddeDerby(String serverName, String catalog, String userName,
				String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		protected String getURL()
		{
			return "jdbc:derby:" + catalog;
		}
	}

	public static class Informix extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "com.informix.jdbc.IfxDriver	";

		public static int		DEFAULT_PORT_NUMBER	= 1533;

		public Informix(String serverName, int portNumber, String catalog,
				String userName, String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
		}

		public Informix(String userName, String passWord)
		{
			this("", userName, passWord);
		}

		public Informix(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public Informix(String serverName, String catalog, String userName,
				String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		protected String getURL()
		{
			return "jdbc:informix-sqli://" + serverName + ":"
					+ DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + "/" + catalog
					+ ":INFORMIXSERVER=myserver";
		}
	}

	public static class MySQL extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "com.mysql.jdbc.Driver";

		public static int		DEFAULT_PORT_NUMBER	= 3306;

		public MySQL(String serverName, int portNumber, String catalog, String userName,
				String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
			this.getInformation().setProperty("useUnicode", "true");
			this.getInformation().setProperty("characterEncoding", "utf-8");
			this.getInformation().setProperty("zeroDateTimeBehavior", "convertToNull");
			this.getInformation().setProperty("autoReconnect", "true");
		}

		public MySQL(String userName, String passWord)
		{
			this("", userName, passWord);
		}

		public MySQL(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public MySQL(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		protected String getURL()
		{
			return "jdbc:mysql://" + serverName + ":"
					+ DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + "/" + catalog;
		}
	}

	public static class ODBC extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "sun.jdbc.odbc.JdbcOdbcDriver";

		public ODBC(String serverName, int portNumber, String catalog, String userName,
				String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
		}

		public ODBC(String userName, String passWord)
		{
			this("", userName, passWord);
		}

		public ODBC(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public ODBC(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		protected String getURL()
		{
			return "jdbc:odbc:" + catalog;
		}
	}

	public static class Oracle extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "oracle.jdbc.driver.OracleDriver";

		public static int		DEFAULT_PORT_NUMBER	= 1521;

		public Oracle(String serverName, int portNumber, String catalog, String userName,
				String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
		}

		public Oracle(String userName, String passWord)
		{
			this("", userName, passWord);
		}

		public Oracle(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public Oracle(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		protected String getURL()
		{
			return "jdbc:oracle:thin:@" + serverName + ":"
					+ DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + ":" + catalog;
		}
	}

	public static class PostgreSQL extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "org.postgresql.Driver";

		public static int		DEFAULT_PORT_NUMBER	= 5432;

		public PostgreSQL(String serverName, int portNumber, String catalog,
				String userName, String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
		}

		public PostgreSQL(String userName, String passWord)
		{
			this("", userName, passWord);
		}

		public PostgreSQL(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public PostgreSQL(String serverName, String catalog, String userName,
				String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		protected String getURL()
		{
			return "jdbc:postgresql://" + serverName + ":"
					+ DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + "/" + catalog;
		}
	}

	public static class SqlServer2000 extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "com.microsoft.jdbc.sqlserver.SQLServerDriver";

		public static int		DEFAULT_PORT_NUMBER	= 1433;

		public SqlServer2000(String serverName, int portNumber, String catalog,
				String userName, String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
		}

		public SqlServer2000(String userName, String passWord)
		{
			this("", userName, passWord);
		}

		public SqlServer2000(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public SqlServer2000(String serverName, String catalog, String userName,
				String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		protected String getURL()
		{
			return "jdbc:microsoft:sqlserver://" + serverName + ":"
					+ DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER)
					+ ";databaseName=" + catalog;
		}
	}

	public static class SqlServer2005 extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "com.microsoft.sqlserver.jdbc.SQLServerDriver";

		public static int		DEFAULT_PORT_NUMBER	= 1433;

		public SqlServer2005(String serverName, int portNumber, String catalog,
				String userName, String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
		}

		public SqlServer2005(String userName, String passWord)
		{
			this("", userName, passWord);
		}

		public SqlServer2005(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public SqlServer2005(String serverName, String catalog, String userName,
				String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		protected String getURL()
		{
			return "jdbc:sqlserver://" + serverName + ":"
					+ DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER)
					+ ";databaseName=" + catalog;
		}
	}

	public static class Sybase extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "com.sybase.jdbc.SybDriver";

		public static int		DEFAULT_PORT_NUMBER	= 5007;

		public Sybase(String serverName, int portNumber, String catalog, String userName,
				String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
		}

		public Sybase(String userName, String passWord)
		{
			this("", userName, passWord);
		}

		public Sybase(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public Sybase(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		protected String getURL()
		{
			return "jdbc:sybase:Tds:" + serverName + ":"
					+ DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + "/" + catalog;
		}
	}

	public static final int		DEFAULT_PORT_NUMBER	= Integer.MIN_VALUE;

	public static final String	USER				= "user";

	public static final String	PASSWORD			= "password";

	public static final int DefaultPortNumber(int portNumber, int defaultNumber)
	{
		return portNumber == DEFAULT_PORT_NUMBER ? defaultNumber : portNumber;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	protected String		serverName;

	protected int			portNumber	= DEFAULT_PORT_NUMBER;

	protected String		catalog;

	protected Properties	information;

	private Connection		connection;

	protected DataBase(DataBase dataBase)
	{
		this.setServerName(dataBase.serverName);
		this.setPortNumber(dataBase.portNumber);
		this.setCatalog(dataBase.catalog);
		this.setInformation(dataBase.information);
	}

	public DataBase(String serverName, int portNumber, String catalog,
			Properties information)
	{
		this.setServerName(serverName);
		this.setPortNumber(portNumber);
		this.setCatalog(catalog);
		this.setInformation(information);
	}

	public DataBase(String serverName, int portNumber, String catalog, String userName,
			String passWord)
	{
		this(serverName, portNumber, catalog, new Properties());
		this.setUserName(userName);
		this.setPassWord(passWord);
	}

	public DataBase(String userName, String passWord)
	{
		this("", userName, passWord);
	}

	public DataBase(String catalog, String userName, String passWord)
	{
		this("localhost", catalog, userName, passWord);
	}

	public DataBase(String serverName, String catalog, String userName, String passWord)
	{
		this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
	}

	@Override
	public DataBase clone()
	{
		return new DataBase(this) {

			@Override
			public String getDriverName()
			{
				return DataBase.this.getDriverName();
			}

			@Override
			public String getURL()
			{
				return DataBase.this.getURL();
			}

		};
	}

	public void closeConnection()
	{
		try {
			if (this.getConnection() != null) {
				this.getConnection().close();
				this.setConnection(null);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finalize()
	{
		this.closeConnection();
		try {
			super.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public String getCatalog()
	{
		return catalog;
	}

	public Connection getConnection()
	{
		return connection;
	}

	public abstract String getDriverName();

	public Properties getInformation()
	{
		return information;
	}

	protected String getPassWord()
	{
		return information.getProperty(PASSWORD);
	}

	public int getPortNumber()
	{
		return portNumber;
	}

	public String getServerName()
	{
		return serverName;
	}

	public SQLKit getSQLKit()
	{
		SQLKit kit = null;
		try {
			if (this.isClosed()) {
				this.openConnection();
			}
			kit = new SQLKit(this.getConnection());
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return kit;
	}

	/**
	 * Get the connection URL. Override this method to support certain database.
	 * 
	 * @return the connection URL.
	 */
	protected abstract String getURL();

	protected String getUserName()
	{
		return information.getProperty(USER);
	}

	public boolean isClosed()
	{
		boolean is = true;

		if (this.getConnection() != null) {

			try {
				is = this.getConnection().isClosed();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}

		return is;
	}

	public void openConnection() throws InstantiationException, IllegalAccessException,
			ClassNotFoundException
	{
		try {

			if (this.isClosed()) {
				// No need for JDBC4.0 with Java6.0

				Class.forName(this.getDriverName()).newInstance();

				this.setConnection(DriverManager.getConnection(this.getURL(),
						this.getInformation()));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void setCatalog(String catalog)
	{
		this.catalog = catalog;
	}

	public void setConnection(Connection connection)
	{
		this.connection = connection;
	}

	protected void setInformation(Properties information)
	{
		this.information = information;
	}

	public void setPassWord(String passWord)
	{
		information.setProperty(PASSWORD, passWord);
	}

	public void setPortNumber(int portNumber)
	{
		this.portNumber = portNumber;
	}

	public void setServerName(String serverName)
	{
		this.serverName = serverName;
	}

	public void setUserName(String userName)
	{
		information.setProperty(USER, userName);
	}

}
