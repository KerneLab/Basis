package org.kernelab.basis.sql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;

public class Sequel implements Iterable<Sequel>
{
	public static class ResultSetIterator implements Iterable<ResultSet>, Iterator<ResultSet>
	{
		private ResultSet	rs;

		public ResultSetIterator(ResultSet rs)
		{
			this.rs = rs;

			try
			{
				this.rs.beforeFirst();
			}
			catch (Exception e)
			{
			}
		}

		public boolean hasNext()
		{
			boolean has = false;

			try
			{
				has = rs.next();
			}
			catch (Exception e)
			{
			}

			return has;
		}

		public Iterator<ResultSet> iterator()
		{
			return this;
		}

		public ResultSet next()
		{
			return rs;
		}

		public void remove()
		{

		}
	}

	public class SequelIterator implements Iterable<Sequel>, Iterator<Sequel>
	{
		private int		current;

		private boolean	first	= true;

		public SequelIterator()
		{
			this(Statement.CLOSE_CURRENT_RESULT);
		}

		public SequelIterator(int current)
		{
			this.current = current;
		}

		public boolean hasNext()
		{
			if (first)
			{
				first = false;
			}
			else
			{
				nextResult(current);
			}

			return hasResult();
		}

		public Iterator<Sequel> iterator()
		{
			return this;
		}

		public Sequel next()
		{
			return Sequel.this;
		}

		public void remove()
		{

		}
	}

	public static final int	N_A				= -1;

	/**
	 * No result currently.
	 */
	public static final int	RESULT_NONE		= N_A;

	/**
	 * Current result is a ResultSet.
	 */
	public static final int	RESULT_SET		= 0;

	/**
	 * Current result is a update count.
	 */
	public static final int	RESULT_COUNT	= 1;

	public static ResultSetIterator iterate(ResultSet rs)
	{
		return new ResultSetIterator(rs);
	}

	private SQLKit		kit;

	private Statement	statement;

	private boolean		resultSetObject	= false;

	private ResultSet	resultSet;

	private int			updateCount		= N_A;

	public Sequel(SQLKit kit, Statement statement, boolean resultSet)
	{
		this.setKit(kit).setStatement(statement).hasResultSetObject(resultSet).refreshUpdateCount();
	}

	public Sequel close() throws SQLException
	{
		if (statement != null)
		{
			kit.closeStatement(statement);
			kit = null;
			statement = null;
		}
		return this;
	}

	public Sequel closeResultSet() throws SQLException
	{
		if (this.isResultSet())
		{
			this.getResultSet().close();
		}
		return this;
	}

	public SQLKit getKit()
	{
		return kit;
	}

	public JSAN getNextRowAsJSAN()
	{
		return getNextRowAsJSAN(null);
	}

	public JSAN getNextRowAsJSAN(Map<String, Object> map)
	{
		JSAN jsan = null;

		if (this.nextRow())
		{
			jsan = this.getRowAsJSAN(map);
		}

		return jsan;
	}

	public JSON getNextRowAsJSON()
	{
		return getNextRowAsJSON(null);
	}

	public JSON getNextRowAsJSON(Map<String, Object> map)
	{
		JSON json = null;

		if (this.nextRow())
		{
			json = this.getRowAsJSON(map);
		}

		return json;
	}

	public ResultSet getResultSet()
	{
		return resultSet;
	}

	public int getResultType()
	{
		int type = RESULT_NONE;

		if (this.getResultSet() != null)
		{
			type = RESULT_SET;
		}
		else if (this.getUpdateCount() != N_A)
		{
			type = RESULT_COUNT;
		}

		return type;
	}

	public JSAN getRowAsJSAN()
	{
		return getRowAsJSAN(null);
	}

	public JSAN getRowAsJSAN(Map<String, Object> map)
	{
		JSAN jsan = null;

		try
		{
			if (this.preparedResultSet())
			{
				if (map == null)
				{
					map = SQLKit.mapIndexOfMetaData(this.getResultSet());
				}
				jsan = SQLKit.jsanOfResultRow(this.getResultSet(), map);
			}
		}
		catch (Exception e)
		{
		}

		return jsan;
	}

	public JSON getRowAsJSON()
	{
		return getRowAsJSON(null);
	}

	public JSON getRowAsJSON(Map<String, Object> map)
	{
		JSON json = null;

		try
		{
			if (this.preparedResultSet())
			{
				if (map == null)
				{
					map = SQLKit.mapNameOfMetaData(this.getResultSet());
				}
				json = SQLKit.jsonOfResultRow(this.getResultSet(), map);
			}
		}
		catch (Exception e)
		{
		}

		return json;
	}

	public Statement getStatement()
	{
		return statement;
	}

	public int getUpdateCount()
	{
		return updateCount;
	}

	public Array getValueArray(int column)
	{
		Array value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getArray(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Array getValueArray(int column, Array defaultValue)
	{
		Array value = getValueArray(column);
		return value == null ? defaultValue : value;
	}

	public Array getValueArray(String column)
	{
		Array value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getArray(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Array getValueArray(String column, Array defaultValue)
	{
		Array value = getValueArray(column);
		return value == null ? defaultValue : value;
	}

	public InputStream getValueAsciiStream(int column)
	{
		InputStream value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getAsciiStream(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public InputStream getValueAsciiStream(int column, InputStream defaultValue)
	{
		InputStream value = getValueAsciiStream(column);
		return value == null ? defaultValue : value;
	}

	public InputStream getValueAsciiStream(String column)
	{
		InputStream value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getAsciiStream(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public InputStream getValueAsciiStream(String column, InputStream defaultValue)
	{
		InputStream value = getValueAsciiStream(column);
		return value == null ? defaultValue : value;
	}

	public BigDecimal getValueBigDecimal(int column)
	{
		BigDecimal value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getBigDecimal(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public BigDecimal getValueBigDecimal(int column, BigDecimal defaultValue)
	{
		BigDecimal value = getValueBigDecimal(column);
		return value == null ? defaultValue : value;
	}

	public BigDecimal getValueBigDecimal(String column)
	{
		BigDecimal value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getBigDecimal(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public BigDecimal getValueBigDecimal(String column, BigDecimal defaultValue)
	{
		BigDecimal value = getValueBigDecimal(column);
		return value == null ? defaultValue : value;
	}

	public InputStream getValueBinaryStream(int column)
	{
		InputStream value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getBinaryStream(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public InputStream getValueBinaryStream(int column, InputStream defaultValue)
	{
		InputStream value = getValueBinaryStream(column);
		return value == null ? defaultValue : value;
	}

	public InputStream getValueBinaryStream(String column)
	{
		InputStream value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getBinaryStream(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public InputStream getValueBinaryStream(String column, InputStream defaultValue)
	{
		InputStream value = getValueBinaryStream(column);
		return value == null ? defaultValue : value;
	}

	public Blob getValueBlob(int column)
	{
		Blob value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getBlob(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Blob getValueBlob(int column, Blob defaultValue)
	{
		Blob value = getValueBlob(column);
		return value == null ? defaultValue : value;
	}

	public Blob getValueBlob(String column)
	{
		Blob value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getBlob(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Blob getValueBlob(String column, Blob defaultValue)
	{
		Blob value = getValueBlob(column);
		return value == null ? defaultValue : value;
	}

	public Boolean getValueBoolean(int column)
	{
		Boolean value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getBoolean(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Boolean getValueBoolean(int column, Boolean defaultValue)
	{
		Boolean value = getValueBoolean(column);
		return value == null ? defaultValue : value;
	}

	public Boolean getValueBoolean(String column)
	{
		Boolean value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getBoolean(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Boolean getValueBoolean(String column, Boolean defaultValue)
	{
		Boolean value = getValueBoolean(column);
		return value == null ? defaultValue : value;
	}

	public Byte getValueByte(int column)
	{
		Byte value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getByte(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Byte getValueByte(int column, Byte defaultValue)
	{
		Byte value = getValueByte(column);
		return value == null ? defaultValue : value;
	}

	public Byte getValueByte(String column)
	{
		Byte value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getByte(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Byte getValueByte(String column, Byte defaultValue)
	{
		Byte value = getValueByte(column);
		return value == null ? defaultValue : value;
	}

	public byte[] getValueBytes(int column)
	{
		byte[] value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getBytes(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public byte[] getValueBytes(int column, byte[] defaultValue)
	{
		byte[] value = getValueBytes(column);
		return value == null ? defaultValue : value;
	}

	public byte[] getValueBytes(String column)
	{
		byte[] value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getBytes(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public byte[] getValueBytes(String column, byte[] defaultValue)
	{
		byte[] value = getValueBytes(column);
		return value == null ? defaultValue : value;
	}

	public Reader getValueCharacterStream(int column)
	{
		Reader value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getCharacterStream(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Reader getValueCharacterStream(int column, Reader defaultValue)
	{
		Reader value = getValueCharacterStream(column);
		return value == null ? defaultValue : value;
	}

	public Reader getValueCharacterStream(String column)
	{
		Reader value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getCharacterStream(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Reader getValueCharacterStream(String column, Reader defaultValue)
	{
		Reader value = getValueCharacterStream(column);
		return value == null ? defaultValue : value;
	}

	public Clob getValueClob(int column)
	{
		Clob value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getClob(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Clob getValueClob(int column, Clob defaultValue)
	{
		Clob value = getValueClob(column);
		return value == null ? defaultValue : value;
	}

	public Clob getValueClob(String column)
	{
		Clob value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getClob(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Clob getValueClob(String column, Clob defaultValue)
	{
		Clob value = getValueClob(column);
		return value == null ? defaultValue : value;
	}

	public Date getValueDate(int column)
	{
		Date value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getDate(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Date getValueDate(int column, Calendar calendar)
	{
		Date value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getDate(column, calendar);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Date getValueDate(int column, Calendar calendar, Date defaultValue)
	{
		Date value = getValueDate(column, calendar);
		return value == null ? defaultValue : value;
	}

	public Date getValueDate(int column, Date defaultValue)
	{
		Date value = getValueDate(column);
		return value == null ? defaultValue : value;
	}

	public Date getValueDate(String column)
	{
		Date value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getDate(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Date getValueDate(String column, Calendar calendar)
	{
		Date value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getDate(column, calendar);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Date getValueDate(String column, Calendar calendar, Date defaultValue)
	{
		Date value = getValueDate(column, calendar);
		return value == null ? defaultValue : value;
	}

	public Date getValueDate(String column, Date defaultValue)
	{
		Date value = getValueDate(column);
		return value == null ? defaultValue : value;
	}

	public Double getValueDouble(int column)
	{
		Double value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getDouble(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Double getValueDouble(int column, Double defaultValue)
	{
		Double value = getValueDouble(column);
		return value == null ? defaultValue : value;
	}

	public Double getValueDouble(String column)
	{
		Double value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getDouble(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Double getValueDouble(String column, Double defaultValue)
	{
		Double value = getValueDouble(column);
		return value == null ? defaultValue : value;
	}

	public Float getValueFloat(int column)
	{
		Float value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getFloat(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Float getValueFloat(int column, Float defaultValue)
	{
		Float value = getValueFloat(column);
		return value == null ? defaultValue : value;
	}

	public Float getValueFloat(String column)
	{
		Float value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getFloat(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Float getValueFloat(String column, Float defaultValue)
	{
		Float value = getValueFloat(column);
		return value == null ? defaultValue : value;
	}

	public Integer getValueInteger(int column)
	{
		Integer value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getInt(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Integer getValueInteger(int column, Integer defaultValue)
	{
		Integer value = getValueInteger(column);
		return value == null ? defaultValue : value;
	}

	public Integer getValueInteger(String column)
	{
		Integer value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getInt(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Integer getValueInteger(String column, Integer defaultValue)
	{
		Integer value = getValueInteger(column);
		return value == null ? defaultValue : value;
	}

	public Long getValueLong(int column)
	{
		Long value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getLong(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Long getValueLong(int column, Long defaultValue)
	{
		Long value = getValueLong(column);
		return value == null ? defaultValue : value;
	}

	public Long getValueLong(String column)
	{
		Long value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getLong(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Long getValueLong(String column, Long defaultValue)
	{
		Long value = getValueLong(column);
		return value == null ? defaultValue : value;
	}

	public Object getValueObject(int column)
	{
		Object value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getObject(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Object getValueObject(int column, Map<String, Class<?>> map)
	{
		Object value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getObject(column, map);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Object getValueObject(int column, Map<String, Class<?>> map, Object defaultValue)
	{
		Object value = getValueObject(column, map);
		return value == null ? defaultValue : value;
	}

	public Object getValueObject(int column, Object defaultValue)
	{
		Object value = getValueObject(column);
		return value == null ? defaultValue : value;
	}

	public Object getValueObject(String column)
	{
		Object value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getObject(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Object getValueObject(String column, Map<String, Class<?>> map)
	{
		Object value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getObject(column, map);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Object getValueObject(String column, Map<String, Class<?>> map, Object defaultValue)
	{
		Object value = getValueObject(column, map);
		return value == null ? defaultValue : value;
	}

	public Object getValueObject(String column, Object defaultValue)
	{
		Object value = getValueObject(column);
		return value == null ? defaultValue : value;
	}

	public Ref getValueRef(int column)
	{
		Ref value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getRef(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Ref getValueRef(int column, Ref defaultValue)
	{
		Ref value = getValueRef(column);
		return value == null ? defaultValue : value;
	}

	public Ref getValueRef(String column)
	{
		Ref value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getRef(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Ref getValueRef(String column, Ref defaultValue)
	{
		Ref value = getValueRef(column);
		return value == null ? defaultValue : value;
	}

	public Short getValueShort(int column)
	{
		Short value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getShort(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Short getValueShort(int column, Short defaultValue)
	{
		Short value = getValueShort(column);
		return value == null ? defaultValue : value;
	}

	public Short getValueShort(String column)
	{
		Short value = null;

		if (this.preparedResultSet())
		{
			try
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getShort(column);
				}
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Short getValueShort(String column, Short defaultValue)
	{
		Short value = getValueShort(column);
		return value == null ? defaultValue : value;
	}

	public String getValueString(int column)
	{
		String value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getString(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public String getValueString(int column, String defaultValue)
	{
		String value = getValueString(column);
		return value == null ? defaultValue : value;
	}

	public String getValueString(String column)
	{
		String value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getString(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public String getValueString(String column, String defaultValue)
	{
		String value = getValueString(column);
		return value == null ? defaultValue : value;
	}

	public Time getValueTime(int column)
	{
		Time value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getTime(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Time getValueTime(int column, Calendar calendar)
	{
		Time value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getTime(column, calendar);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Time getValueTime(int column, Calendar calendar, Time defaultValue)
	{
		Time value = getValueTime(column, calendar);
		return value == null ? defaultValue : value;
	}

	public Time getValueTime(int column, Time defaultValue)
	{
		Time value = getValueTime(column);
		return value == null ? defaultValue : value;
	}

	public Time getValueTime(String column)
	{
		Time value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getTime(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Time getValueTime(String column, Calendar calendar)
	{
		Time value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getTime(column, calendar);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Time getValueTime(String column, Calendar calendar, Time defaultValue)
	{
		Time value = getValueTime(column, calendar);
		return value == null ? defaultValue : value;
	}

	public Time getValueTime(String column, Time defaultValue)
	{
		Time value = getValueTime(column);
		return value == null ? defaultValue : value;
	}

	public Timestamp getValueTimestamp(int column)
	{
		Timestamp value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getTimestamp(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Timestamp getValueTimestamp(int column, Calendar calendar)
	{
		Timestamp value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getTimestamp(column, calendar);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Timestamp getValueTimestamp(int column, Calendar calendar, Timestamp defaultValue)
	{
		Timestamp value = getValueTimestamp(column, calendar);
		return value == null ? defaultValue : value;
	}

	public Timestamp getValueTimestamp(int column, Timestamp defaultValue)
	{
		Timestamp value = getValueTimestamp(column);
		return value == null ? defaultValue : value;
	}

	public Timestamp getValueTimestamp(String column)
	{
		Timestamp value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getTimestamp(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Timestamp getValueTimestamp(String column, Calendar calendar)
	{
		Timestamp value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getTimestamp(column, calendar);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public Timestamp getValueTimestamp(String column, Calendar calendar, Timestamp defaultValue)
	{
		Timestamp value = getValueTimestamp(column, calendar);
		return value == null ? defaultValue : value;
	}

	public Timestamp getValueTimestamp(String column, Timestamp defaultValue)
	{
		Timestamp value = getValueTimestamp(column);
		return value == null ? defaultValue : value;
	}

	public URL getValueURL(int column)
	{
		URL value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getURL(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public URL getValueURL(int column, URL defaultValue)
	{
		URL value = getValueURL(column);
		return value == null ? defaultValue : value;
	}

	public URL getValueURL(String column)
	{
		URL value = null;

		if (this.preparedResultSet())
		{
			try
			{
				value = this.getResultSet().getURL(column);
			}
			catch (SQLException e)
			{
			}
		}

		return value;
	}

	public URL getValueURL(String column, URL defaultValue)
	{
		URL value = getValueURL(column);
		return value == null ? defaultValue : value;
	}

	public boolean hasResult()
	{
		return this.hasResultSetObject() || this.getUpdateCount() != N_A;
	}

	private boolean hasResultSetObject()
	{
		return resultSetObject;
	}

	private Sequel hasResultSetObject(boolean resultSetObject)
	{
		this.resultSetObject = resultSetObject;

		if (resultSetObject)
		{
			try
			{
				this.setResultSet(statement.getResultSet());
			}
			catch (SQLException e)
			{
				this.setResultSet(null);
			}
		}
		else
		{
			this.setResultSet(null);
		}

		return this;
	}

	public boolean isClosed()
	{
		return statement == null;
	}

	public boolean isResultSet()
	{
		return this.getResultSet() != null;
	}

	public boolean isUpdateCount()
	{
		return this.getUpdateCount() != N_A;
	}

	public ResultSetIterator iterate()
	{
		return new ResultSetIterator(this.getResultSet());
	}

	public SequelIterator iterator()
	{
		return new SequelIterator();
	}

	public SequelIterator iterator(int current)
	{
		return new SequelIterator(current);
	}

	public Sequel nextResult()
	{
		return nextResult(Statement.CLOSE_CURRENT_RESULT);
	}

	public Sequel nextResult(int current)
	{
		try
		{
			this.hasResultSetObject(this.getStatement().getMoreResults(current));
		}
		catch (SQLException e)
		{
			this.hasResultSetObject(false);
		}

		this.refreshUpdateCount();

		return this;
	}

	public boolean nextRow()
	{
		boolean next = false;

		try
		{
			next = this.getResultSet().next();
		}
		catch (Exception e)
		{
		}

		return next;
	}

	private boolean preparedResultSet()
	{
		boolean ok = false;

		try
		{
			if (this.getResultSet().isBeforeFirst())
			{
				ok = this.getResultSet().next();
			}

			if (!ok)
			{
				ok = this.getResultSet().getRow() != 0;
			}
		}
		catch (Exception e)
		{
		}

		return ok;
	}

	private Sequel refreshUpdateCount()
	{
		try
		{
			this.updateCount = statement.getUpdateCount();
		}
		catch (SQLException e)
		{
			this.updateCount = N_A;
		}
		return this;
	}

	private Sequel setKit(SQLKit kit)
	{
		this.kit = kit;
		return this;
	}

	private Sequel setResultSet(ResultSet resultSet)
	{
		this.resultSet = resultSet;
		return this;
	}

	private Sequel setStatement(Statement statement)
	{
		this.statement = statement;
		return this;
	}
}
