package org.kernelab.basis.sql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.Copieable;
import org.kernelab.basis.Relation;
import org.kernelab.basis.Tools;
import org.kernelab.basis.io.DataReader;
import org.kernelab.basis.io.Extensions;

/**
 * The DataBase class is an abstract of database support.
 * 
 * To support various database, this class can be extended and just override the
 * method getURL().
 * 
 * At least JDBC3.0 and Java5.0 are required.
 * 
 * @author Dilly King
 */
public abstract class DataBase implements ConnectionManager, Copieable<DataBase>
{
	public static class DB2 extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "com.ibm.db2.jdbc.app.DB2Driver";

		public static int		DEFAULT_PORT_NUMBER	= 5000;

		public DB2()
		{
			super();
		}

		public DB2(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public DB2(String serverName, int portNumber, String catalog, String userName, String passWord)
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
		public String getURL()
		{
			return "jdbc:db2://" + serverName + ":" + DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + "/"
					+ catalog;
		}
	}

	public static class Derby extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "org.apache.derby.jdbc.ClientDriver";

		public static int		DEFAULT_PORT_NUMBER	= 1527;

		public Derby()
		{
			super();
		}

		public Derby(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public Derby(String serverName, int portNumber, String catalog, String userName, String passWord)
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
		public String getURL()
		{
			return "jdbc:derby://" + serverName + ":" + DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + "/"
					+ catalog;
		}
	}

	public static class EmbeddeDerby extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "org.apache.derby.jdbc.EmbeddedDriver";

		public EmbeddeDerby()
		{
			super();
		}

		public EmbeddeDerby(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public EmbeddeDerby(String serverName, int portNumber, String catalog, String userName, String passWord)
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

		public EmbeddeDerby(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		public String getURL()
		{
			return "jdbc:derby:" + catalog;
		}
	}

	public static class Hive extends DataBase
	{
		public static String	DRIVER_CLASS_NAME		= "org.apache.hadoop.hive.jdbc.HiveDriver";

		public static int		DEFAULT_PORT_NUMBER		= 10000;

		public static String	DEFAULT_DATABASE_NAME	= "default";

		public Hive()
		{
			super();
		}

		public Hive(String serverName)
		{
			this(serverName, DEFAULT_PORT_NUMBER, DEFAULT_DATABASE_NAME);
		}

		public Hive(String serverName, int portNumber, String catalog)
		{
			this(serverName, portNumber, catalog, "", "");
		}

		public Hive(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public Hive(String serverName, int portNumber, String catalog, String userName, String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
		}

		public Hive(String userName, String passWord)
		{
			this(DEFAULT_DATABASE_NAME, userName, passWord);
		}

		public Hive(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public Hive(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		public String getURL()
		{
			return "jdbc:hive://" + this.getServerName() + ":" + this.getPortNumber() + "/" + this.getCatalog();
		}
	}

	public static class Hive2 extends DataBase
	{
		public static String	DRIVER_CLASS_NAME		= "org.apache.hive.jdbc.HiveDriver";

		public static int		DEFAULT_PORT_NUMBER		= 10000;

		public static String	DEFAULT_DATABASE_NAME	= "default";

		public Hive2()
		{
			super();
		}

		public Hive2(String serverName)
		{
			this(serverName, DEFAULT_PORT_NUMBER, DEFAULT_DATABASE_NAME);
		}

		public Hive2(String serverName, int portNumber, String catalog)
		{
			this(serverName, portNumber, catalog, "", "");
		}

		public Hive2(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public Hive2(String serverName, int portNumber, String catalog, String userName, String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
		}

		public Hive2(String userName, String passWord)
		{
			this(DEFAULT_DATABASE_NAME, userName, passWord);
		}

		public Hive2(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public Hive2(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		public String getURL()
		{
			return "jdbc:hive2://" + this.getServerName() + ":" + this.getPortNumber() + "/" + this.getCatalog();
		}
	}

	public static class Informix extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "com.informix.jdbc.IfxDriver	";

		public static int		DEFAULT_PORT_NUMBER	= 1533;

		public Informix()
		{
			super();
		}

		public Informix(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public Informix(String serverName, int portNumber, String catalog, String userName, String passWord)
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

		public Informix(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		public String getURL()
		{
			return "jdbc:informix-sqli://" + serverName + ":" + DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER)
					+ "/" + catalog + ":INFORMIXSERVER=myserver";
		}
	}

	public static class InvalidDataBaseConnectionException extends RuntimeException
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= 186848770749944602L;

		public InvalidDataBaseConnectionException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}

	public static class MariaDB extends MySQL
	{
		public static String	DRIVER_CLASS_NAME	= "org.mariadb.jdbc.Driver";

		public static int		DEFAULT_PORT_NUMBER	= 3306;

		public MariaDB()
		{
			super();
		}

		public MariaDB(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public MariaDB(String serverName, int portNumber, String catalog, String userName, String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
		}

		public MariaDB(String userName, String passWord)
		{
			super("", userName, passWord);
		}

		public MariaDB(String catalog, String userName, String passWord)
		{
			super("localhost", catalog, userName, passWord);
		}

		public MariaDB(String serverName, String catalog, String userName, String passWord)
		{
			super(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		public String getURL()
		{
			return "jdbc:mariadb://" + serverName + ":" + DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + "/"
					+ catalog;
		}
	}

	public static class MySQL extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "com.mysql.jdbc.Driver";

		public static int		DEFAULT_PORT_NUMBER	= 3306;

		public MySQL()
		{
			super();
		}

		public MySQL(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public MySQL(String serverName, int portNumber, String catalog, String userName, String passWord)
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
		public String getURL()
		{
			return "jdbc:mysql://" + serverName + ":" + DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + "/"
					+ catalog;
		}
	}

	public static class ODBC extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "sun.jdbc.odbc.JdbcOdbcDriver";

		public ODBC()
		{
			super();
		}

		public ODBC(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public ODBC(String serverName, int portNumber, String catalog, String userName, String passWord)
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
		public String getURL()
		{
			return "jdbc:odbc:" + catalog;
		}
	}

	public static class Oracle extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "oracle.jdbc.driver.OracleDriver";

		public static int		DEFAULT_PORT_NUMBER	= 1521;

		private boolean			connectBySID		= false;

		public Oracle()
		{
			super();
		}

		public Oracle(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public Oracle(String serverName, int portNumber, String catalog, String userName, String passWord)
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

		public Oracle connectByServiceName()
		{
			this.connectBySID = false;
			return this;
		}

		public Oracle connectBySID()
		{
			this.connectBySID = true;
			return this;
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		public String getURL()
		{
			if (this.isConnectBySID())
			{
				return "jdbc:oracle:thin:@" + this.getServerName() + ":" + this.getPortNumber() + ":"
						+ this.getCatalog();
			}
			else
			{
				return "jdbc:oracle:thin:@//" + this.getServerName() + ":" + this.getPortNumber() + "/"
						+ this.getCatalog();
			}
		}

		public boolean isConnectByServiceName()
		{
			return !connectBySID;
		}

		public boolean isConnectBySID()
		{
			return connectBySID;
		}

		public Oracle setConnectBySID(boolean connectBySID)
		{
			this.connectBySID = connectBySID;
			return this;
		}
	}

	public static class OracleClassic extends DataBase
	{
		public static String					DRIVER_CLASS_NAME	= "oracle.jdbc.driver.OracleDriver";

		public static int						DEFAULT_PORT_NUMBER	= 1521;

		private String							serverMode			= null;

		private Set<Relation<String, Integer>>	address				= new LinkedHashSet<Relation<String, Integer>>();

		private boolean							connectBySID		= false;

		public OracleClassic()
		{
			super();
		}

		public OracleClassic(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
			this.addAddress(serverName, portNumber);
		}

		public OracleClassic(String serverName, int portNumber, String catalog, String userName, String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
			this.addAddress(serverName, portNumber);
		}

		public OracleClassic(String userName, String passWord)
		{
			this("", userName, passWord);
		}

		public OracleClassic(String catalog, String userName, String passWord)
		{
			this("localhost", catalog, userName, passWord);
		}

		public OracleClassic(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		public OracleClassic addAddress(String serverName, int portNumber)
		{
			address.add(new Relation<String, Integer>(serverName, portNumber));
			return this;
		}

		public OracleClassic connectByServiceName()
		{
			this.connectBySID = false;
			return this;
		}

		public OracleClassic connectBySID()
		{
			this.connectBySID = true;
			return this;
		}

		public Set<Relation<String, Integer>> getAddress()
		{
			return address;
		}

		protected String getAddressList()
		{
			StringBuilder list = new StringBuilder();

			for (Relation<String, Integer> pair : address)
			{
				list.append("(ADDRESS=(PROTOCOL=TCP)(HOST=");
				list.append(pair.getKey());
				list.append(")(PORT=");
				list.append(pair.getValue().toString());
				list.append("))");
			}

			return list.toString();
		}

		protected String getBalanceOption()
		{
			return address.size() > 1 ? "(LOAD_BALANCE=YES)(FAILOVER=ON)" : "";
		}

		protected String getConnectData()
		{
			if (this.isConnectByServiceName())
			{
				return "SERVICE_NAME=" + catalog;
			}
			else
			{
				return "SID=" + catalog;
			}
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		protected String getServerMode()
		{
			String mode = "";

			if (serverMode != null)
			{
				mode = "(SERVER=" + serverMode.toUpperCase() + ")";
			}

			return mode;
		}

		@Override
		public String getURL()
		{
			return "jdbc:oracle:thin:@(DESCRIPTION=" + this.getBalanceOption() + "(ADDRESS_LIST="
					+ this.getAddressList() + ")(CONNECT_DATA=(" + getConnectData() + ")" + this.getServerMode() + "))";
		}

		public boolean isConnectByServiceName()
		{
			return !connectBySID;
		}

		public boolean isConnectBySID()
		{
			return connectBySID;
		}

		public OracleClassic removeAddress(String serverName, int portNumber)
		{
			address.remove(new Relation<String, Integer>(serverName, portNumber));
			return this;
		}

		public OracleClassic setConnectBySID(boolean connectBySID)
		{
			this.connectBySID = connectBySID;
			return this;
		}

		public OracleClassic setServerMode(String serverMode)
		{
			this.serverMode = serverMode;
			return this;
		}
	}

	public static class OracleClient extends DataBase
	{
		public static class TNSNamesReader extends DataReader
		{
			private static final char	COMMENT	= '#';

			private static final char	BEGIN	= '(';

			private static final char	END		= ')';

			private static final String	PATTERN	= "^(\\S+?)\\s*?=\\s*?(.+)$";

			private Map<String, String>	map		= new LinkedHashMap<String, String>();

			private String				name	= null;

			private String				value	= null;

			private StringBuilder		buffer	= new StringBuilder();

			public Map<String, String> getMap()
			{
				return map;
			}

			public String getValue()
			{
				return value;
			}

			@Override
			protected void readFinished()
			{

			}

			@Override
			protected void readLine(CharSequence line)
			{
				String text = line.toString().trim();

				if (text.length() > 0 && text.charAt(0) != COMMENT)
				{
					buffer.append(text);

					if (Tools.seekIndex(buffer, BEGIN) != -1 && Tools.dualMatchCount(buffer, BEGIN, END, 0) == 0)
					{
						Matcher matcher = Pattern.compile(PATTERN, Pattern.DOTALL).matcher(buffer);

						if (matcher.matches())
						{
							String key = matcher.group(1).trim().toUpperCase();
							String value = matcher.group(2).trim();
							map.put(key, value);
							if (Tools.equals(name, key))
							{
								this.value = value;
								this.setReading(false);
							}
						}

						Tools.clearStringBuilder(buffer);
					}
				}
			}

			@Override
			protected void readPrepare()
			{
				value = null;
				map.clear();
				Tools.clearStringBuilder(buffer);
			}

			public <T extends TNSNamesReader> T setMap(Map<String, String> map)
			{
				this.map = map;
				return Tools.cast(this);
			}

			public <T extends TNSNamesReader> T setName(String name)
			{
				this.name = name;
				if (this.name != null)
				{
					this.name = this.name.toUpperCase();
				}
				return Tools.cast(this);
			}
		}

		public static String	DRIVER_CLASS_NAME	= "oracle.jdbc.driver.OracleDriver";

		public static int		DEFAULT_PORT_NUMBER	= 1521;

		public static String getDefaultOracleTNS()
		{
			String tns = null;

			try
			{
				tns = System.getenv("TWO_TASK");
			}
			catch (Exception e)
			{
			}

			if (tns != null)
			{
				return tns;
			}

			try
			{
				tns = System.getenv("LOCAL");
			}
			catch (Exception e)
			{
			}

			if (tns != null)
			{
				return tns;
			}

			try
			{
				tns = System.getenv("ORACLE_SID");
			}
			catch (Exception e)
			{
			}

			return tns;
		}

		public static String getDefaultTNSFilePath()
		{
			String path = null;

			try
			{
				String home = System.getenv("ORACLE_HOME");

				if (home != null)
				{
					File network = new File(home, "network");

					if (!network.isDirectory())
					{
						network = new File(home, "NETWORK");
					}

					if (network.isDirectory())
					{
						File admin = new File(network, "admin");

						if (!admin.isDirectory())
						{
							admin = new File(network, "ADMIN");
						}

						if (admin.isDirectory())
						{
							File tns = new File(admin, "tnsnames.ora");

							if (!tns.isFile())
							{
								tns = new File(admin, "TNSNAMES.ORA");
							}

							if (tns.isFile())
							{
								path = tns.getCanonicalPath();
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			return path;
		}

		public static Map<String, String> listTNS()
		{
			return listTNS(new File(getDefaultTNSFilePath()));
		}

		public static Map<String, String> listTNS(File file)
		{
			return listTNS(file, null);
		}

		public static Map<String, String> listTNS(File file, String name)
		{
			try
			{
				return ((TNSNamesReader) new TNSNamesReader().setName(name).setDataFile(file).read()).getMap();
			}
			catch (Exception e)
			{
				return new LinkedHashMap<String, String>();
			}
		}

		public static Map<String, String> listTNS(String name)
		{
			return listTNS(new File(getDefaultTNSFilePath()), name);
		}

		private File	file;

		public OracleClient()
		{
			super();
		}

		public OracleClient(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public OracleClient(String serverName, int portNumber, String catalog, String userName, String passWord)
		{
			super(serverName, portNumber, catalog, userName, passWord);
		}

		public OracleClient(String userName, String passWord)
		{
			this(null, userName, passWord);
		}

		public OracleClient(String catalog, String userName, String passWord)
		{
			this(null, catalog, userName, passWord);
		}

		public OracleClient(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		public String getURL()
		{
			Map<String, String> map = listTNS(file, this.getCatalog());

			String tns = map.get(this.getCatalog());

			if (tns == null)
			{
				tns = this.getCatalog();
			}

			return "jdbc:oracle:thin:@" + tns;
		}

		@Override
		public OracleClient setCatalog(String tns)
		{
			if (tns == null)
			{
				tns = getDefaultOracleTNS();
			}

			if (tns != null)
			{
				super.setCatalog(tns.trim().toUpperCase());
			}
			else
			{
				throw new RuntimeException("TNS identifier must not be null");
			}

			return this;
		}

		@Override
		public OracleClient setServerName(String filePath)
		{
			if (filePath == null)
			{
				filePath = getDefaultTNSFilePath();
			}

			if (filePath != null)
			{
				super.setServerName(filePath);
				this.file = new File(filePath);
			}

			return this;
		}
	}

	public static class PostgreSQL extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "org.postgresql.Driver";

		public static int		DEFAULT_PORT_NUMBER	= 5432;

		public PostgreSQL()
		{
			super();
		}

		public PostgreSQL(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public PostgreSQL(String serverName, int portNumber, String catalog, String userName, String passWord)
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

		public PostgreSQL(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		public String getURL()
		{
			return "jdbc:postgresql://" + serverName + ":" + DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + "/"
					+ catalog;
		}
	}

	public static class SqlServer2000 extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "com.microsoft.jdbc.sqlserver.SQLServerDriver";

		public static int		DEFAULT_PORT_NUMBER	= 1433;

		public SqlServer2000()
		{
			super();
		}

		public SqlServer2000(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public SqlServer2000(String serverName, int portNumber, String catalog, String userName, String passWord)
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

		public SqlServer2000(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		public String getURL()
		{
			return "jdbc:microsoft:sqlserver://" + serverName + ":"
					+ DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + ";databaseName=" + catalog;
		}
	}

	public static class SqlServer2005 extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "com.microsoft.sqlserver.jdbc.SQLServerDriver";

		public static int		DEFAULT_PORT_NUMBER	= 1433;

		public SqlServer2005()
		{
			super();
		}

		public SqlServer2005(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public SqlServer2005(String serverName, int portNumber, String catalog, String userName, String passWord)
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

		public SqlServer2005(String serverName, String catalog, String userName, String passWord)
		{
			this(serverName, DEFAULT_PORT_NUMBER, catalog, userName, passWord);
		}

		@Override
		public String getDriverName()
		{
			return DRIVER_CLASS_NAME;
		}

		@Override
		public String getURL()
		{
			return "jdbc:sqlserver://" + serverName + ":" + DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER)
					+ ";databaseName=" + catalog;
		}
	}

	public static class Sybase extends DataBase
	{
		public static String	DRIVER_CLASS_NAME	= "com.sybase.jdbc.SybDriver";

		public static int		DEFAULT_PORT_NUMBER	= 5007;

		public Sybase()
		{
			super();
		}

		public Sybase(String serverName, int portNumber, String catalog, Map<String, Object> information)
		{
			super(serverName, portNumber, catalog, information);
		}

		public Sybase(String serverName, int portNumber, String catalog, String userName, String passWord)
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
		public String getURL()
		{
			return "jdbc:sybase:Tds:" + serverName + ":" + DefaultPortNumber(portNumber, DEFAULT_PORT_NUMBER) + "/"
					+ catalog;
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

	public static final Properties PropertiesOfMap(Map<?, Object> map)
	{
		Properties p = new Properties();
		p.putAll(map);
		return p;
	}

	protected String			serverName;

	protected int				portNumber	= DEFAULT_PORT_NUMBER;

	protected String			catalog;

	private String				userName;

	private String				passWord;

	private Map<String, Object>	information;

	public DataBase()
	{
		super();
		this.setInformation(new LinkedHashMap<String, Object>());
	}

	protected DataBase(DataBase dataBase)
	{
		this.setServerName(dataBase.serverName);
		this.setPortNumber(dataBase.portNumber);
		this.setCatalog(dataBase.catalog);
		this.setInformation(dataBase.information);
	}

	public DataBase(String serverName, int portNumber, String catalog, Map<String, Object> information)
	{
		this.setServerName(serverName);
		this.setPortNumber(portNumber);
		this.setCatalog(catalog);
		this.setInformation(information);
	}

	public DataBase(String serverName, int portNumber, String catalog, String userName, String passWord)
	{
		this(serverName, portNumber, catalog, new LinkedHashMap<String, Object>());
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

	public String getCatalog()
	{
		return catalog;
	}

	public abstract String getDriverName();

	public Properties getInformation()
	{
		return PropertiesOfMap(this.information);
	}

	public String getPassWord()
	{
		return passWord;
	}

	public int getPortNumber()
	{
		return portNumber;
	}

	public String getServerName()
	{
		return serverName;
	}

	public SQLKit getSQLKit() throws SQLException
	{
		return new SQLKit(this);
	}

	/**
	 * Get the connection URL. Override this method to support certain database.
	 * 
	 * @return the connection URL.
	 */
	public abstract String getURL();

	public String getUserName()
	{
		return userName;
	}

	public Connection newConnection() throws ClassNotFoundException, SQLException
	{
		// No need for JDBC4.0 with Java6.0
		Extensions.forName(this.getDriverName());

		return DriverManager.getConnection(this.getURL(), this.getInformation());
	}

	public Connection provideConnection(long timeout) throws SQLException
	{
		try
		{
			return this.newConnection();
		}
		catch (ClassNotFoundException e)
		{
			throw new SQLException(e.getLocalizedMessage());
		}
	}

	public void recycleConnection(Connection c) throws SQLException
	{
		if (c != null)
		{
			c.close();
		}
	}

	public DataBase setCatalog(String catalog)
	{
		this.catalog = catalog;
		return this;
	}

	public DataBase setInformation(Map<String, Object> information)
	{
		this.information = information;
		return this;
	}

	public DataBase setPassWord(String passWord)
	{
		this.passWord = passWord;
		information.put(PASSWORD, passWord);
		return this;
	}

	public DataBase setPortNumber(int portNumber)
	{
		this.portNumber = portNumber;
		return this;
	}

	public DataBase setServerName(String serverName)
	{
		this.serverName = serverName;
		return this;
	}

	public DataBase setUserName(String userName)
	{
		this.userName = userName;
		information.put(USER, userName);
		return this;
	}
}
