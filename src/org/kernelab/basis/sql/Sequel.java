package org.kernelab.basis.sql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.Mapper;

public class Sequel implements Iterable<ResultSet>
{
	public static abstract class AbstractIterator<T> implements Iterable<T>, Iterator<T>
	{
		private ResultSet	rs;

		private SQLKit		kit;

		private boolean		closing;

		public AbstractIterator(ResultSet rs)
		{
			this.rs = rs;
			this.closing = true;
		}

		public boolean closing()
		{
			return closing;
		}

		public AbstractIterator<T> closing(boolean closing)
		{
			this.closing = closing;
			return this;
		}

		protected abstract void done();

		public boolean hasNext()
		{
			if (rs != null)
			{
				try
				{
					if (rs.next())
					{
						return true;
					}
					else
					{
						done();
						release();
						return false;
					}
				}
				catch (SQLException e)
				{
					release();
					return false;
				}
			}
			else
			{
				release();
				return false;
			}
		}

		public Iterator<T> iterator()
		{
			return this;
		}

		public SQLKit kit()
		{
			return kit;
		}

		public AbstractIterator<T> kit(SQLKit kit)
		{
			this.kit = kit;
			return this;
		}

		public abstract T next();

		protected void release()
		{
			if (rs != null)
			{
				if (closing())
				{
					try
					{
						Statement st = rs.getStatement();

						if (st != null)
						{
							if (kit != null)
							{
								kit.closeStatement(st);
							}
							else
							{
								st.close();
							}
						}

						rs.close();
					}
					catch (SQLException e)
					{
					}
				}
				rs = null;
			}
			kit = null;
		}

		public void remove()
		{
		}

		protected ResultSet resultSet()
		{
			return this.rs;
		}
	}

	public static class ObjectIterator<T> extends AbstractIterator<T>
	{
		private Mapper<ResultSet, T>	mapper;

		public ObjectIterator(ResultSet rs)
		{
			super(rs);
		}

		@Override
		public ObjectIterator<T> closing(boolean closing)
		{
			super.closing(closing);
			return this;
		}

		@Override
		protected void done()
		{
		}

		@Override
		public ObjectIterator<T> kit(SQLKit kit)
		{
			super.kit(kit);
			return this;
		}

		public Mapper<ResultSet, T> mapper()
		{
			return mapper;
		}

		public ObjectIterator<T> mapper(Mapper<ResultSet, T> mapper)
		{
			this.mapper = mapper;
			return this;
		}

		public T next()
		{
			if (mapper() != null)
			{
				return mapper().map(this.resultSet());
			}
			else
			{
				return null;
			}
		}
	}

	public static class ResultSetIterator extends AbstractIterator<ResultSet>
	{
		public ResultSetIterator(ResultSet rs)
		{
			super(rs);
		}

		@Override
		public ResultSetIterator closing(boolean closing)
		{
			super.closing(closing);
			return this;
		}

		@Override
		protected void done()
		{
		}

		@Override
		public ResultSetIterator kit(SQLKit kit)
		{
			super.kit(kit);
			return this;
		}

		public ResultSet next()
		{
			return resultSet();
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

			if (hasResult())
			{
				Sequel.this.setIterating(true);
				return true;
			}
			else
			{
				Sequel.this.setIterating(false);
				release();
				return false;
			}
		}

		public Iterator<Sequel> iterator()
		{
			return this;
		}

		public Sequel next()
		{
			return Sequel.this;
		}

		protected void release()
		{
			if (Sequel.this.isClosing())
			{
				Sequel.this.close();
			}
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

	/**
	 * Current result is a call result.
	 */
	public static final int	RESULT_CALL		= 2;

	public static ResultSetIterator iterate(ResultSet rs)
	{
		return new ResultSetIterator(rs);
	}

	private SQLKit				kit;

	private boolean				closing			= true;

	private boolean				iterating		= false;

	private Statement			statement;

	private ResultSet			resultSet;

	private int					updateCount		= N_A;

	private Map<String, Object>	metaMapIndex	= null;

	private Map<String, Object>	metaMapName		= null;

	public Sequel(ResultSet rs)
	{
		this.setResultSet(rs);
	}

	public Sequel(SQLKit kit, Statement statement, boolean hasResultSet)
	{
		this.setKit(kit).setStatement(statement).hasResultSetObject(hasResultSet).refreshUpdateCount();
	}

	public Sequel close()
	{
		try
		{
			this.closeStatement();
		}
		catch (SQLException e)
		{
		}

		try
		{
			this.closeResultSet();
		}
		catch (SQLException e)
		{
		}

		kit = null;

		metaMapIndex = null;
		metaMapName = null;

		return this;
	}

	public Sequel closeResultSet() throws SQLException
	{
		if (this.isResultSet())
		{
			try
			{
				this.getResultSet().close();
			}
			finally
			{
				this.setResultSet(null);
			}
		}
		return this;
	}

	public Sequel closeStatement() throws SQLException
	{
		if (statement != null)
		{
			try
			{
				if (kit != null)
				{
					kit.closeStatement(statement);
				}
				else
				{
					statement.close();
				}
			}
			finally
			{
				statement = null;
			}
		}
		return this;
	}

	protected CallableStatement getCallableStatement()
	{
		return this.isCallResult() ? (CallableStatement) this.getStatement() : null;
	}

	protected SQLKit getKit()
	{
		return kit;
	}

	public ResultSetMetaData getMetaData()
	{
		if (this.isResultSet())
		{
			try
			{
				return this.getResultSet().getMetaData();
			}
			catch (SQLException e)
			{
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	public Map<String, Object> getMetaMapIndex()
	{
		if (metaMapIndex == null)
		{
			try
			{
				metaMapIndex = SQLKit.mapIndexOfMetaData(this.getResultSet().getMetaData());
			}
			catch (SQLException e)
			{
			}
		}
		return metaMapIndex;
	}

	public Map<String, Object> getMetaMapName()
	{
		if (metaMapName == null)
		{
			try
			{
				metaMapName = SQLKit.mapNameOfMetaData(this.getResultSet().getMetaData());
			}
			catch (SQLException e)
			{
			}
		}
		return metaMapName;
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

		if (this.isResultSet())
		{
			type = RESULT_SET;
		}
		else if (this.getUpdateCount() != N_A)
		{
			type = RESULT_COUNT;
		}
		else if (this.isCallResult())
		{
			type = RESULT_CALL;
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
					map = this.getMetaMapIndex();
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
					map = this.getMetaMapName();
				}
				json = SQLKit.jsonOfResultRow(this.getResultSet(), map);
			}
		}
		catch (Exception e)
		{
		}

		return json;
	}

	public JSAN getRows(JSAN rows, Class<? extends JSON> cls)
	{
		return getRows(rows, null, cls);
	}

	public JSAN getRows(JSAN rows, Class<? extends JSON> cls, int limit)
	{
		return getRows(rows, null, cls, limit);
	}

	public JSAN getRows(JSAN rows, Map<String, Object> map, Class<? extends JSON> cls)
	{
		return getRows(rows, map, cls, -1);
	}

	public JSAN getRows(JSAN rows, Map<String, Object> map, Class<? extends JSON> cls, int limit)
	{
		try
		{
			rows = SQLKit.jsanOfResultSet(this.getResultSet(), rows, map, cls, limit);
		}
		catch (SQLException e)
		{
		}
		return rows;
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getArray(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getArray(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getArray(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getArray(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getAsciiStream(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getBigDecimal(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getBigDecimal(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getBigDecimal(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getBigDecimal(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getBlob(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getBlob(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getBlob(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getBlob(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getBoolean(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getBoolean(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getBoolean(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getBoolean(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getByte(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getByte(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getByte(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getByte(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getBytes(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getBytes(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getBytes(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getBytes(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getClob(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getClob(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getClob(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getClob(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getDate(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getDate(column);
			}
		}
		catch (SQLException e)
		{
		}

		return value;
	}

	public Date getValueDate(int column, Calendar calendar)
	{
		Date value = null;

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getDate(column, calendar);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getDate(column, calendar);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getDate(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getDate(column);
			}
		}
		catch (SQLException e)
		{
		}

		return value;
	}

	public Date getValueDate(String column, Calendar calendar)
	{
		Date value = null;

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getDate(column, calendar);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getDate(column, calendar);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getDouble(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getDouble(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getDouble(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getDouble(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getFloat(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getFloat(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getFloat(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getFloat(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getInt(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getInt(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getInt(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getInt(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getLong(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getLong(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getLong(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getLong(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getObject(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getObject(column);
			}
		}
		catch (SQLException e)
		{
		}

		return value;
	}

	public Object getValueObject(int column, Map<String, Class<?>> map)
	{
		Object value = null;

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getObject(column, map);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getObject(column, map);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getObject(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getObject(column);
			}
		}
		catch (SQLException e)
		{
		}

		return value;
	}

	public Object getValueObject(String column, Map<String, Class<?>> map)
	{
		Object value = null;

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getObject(column, map);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getObject(column, map);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getRef(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getRef(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getRef(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getRef(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getShort(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getShort(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				if (this.getResultSet().getObject(column) != null)
				{
					value = this.getResultSet().getShort(column);
				}
			}
			else if (this.isCallResult())
			{
				if (this.getCallableStatement().getObject(column) != null)
				{
					value = this.getCallableStatement().getShort(column);
				}
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getString(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getString(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getString(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getString(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getTime(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getTime(column);
			}
		}
		catch (SQLException e)
		{
		}

		return value;
	}

	public Time getValueTime(int column, Calendar calendar)
	{
		Time value = null;

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getTime(column, calendar);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getTime(column, calendar);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getTime(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getTime(column);
			}
		}
		catch (SQLException e)
		{
		}

		return value;
	}

	public Time getValueTime(String column, Calendar calendar)
	{
		Time value = null;

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getTime(column, calendar);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getTime(column, calendar);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getTimestamp(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getTimestamp(column);
			}
		}
		catch (SQLException e)
		{
		}

		return value;
	}

	public Timestamp getValueTimestamp(int column, Calendar calendar)
	{
		Timestamp value = null;

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getTimestamp(column, calendar);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getTimestamp(column, calendar);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getTimestamp(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getTimestamp(column);
			}
		}
		catch (SQLException e)
		{
		}

		return value;
	}

	public Timestamp getValueTimestamp(String column, Calendar calendar)
	{
		Timestamp value = null;

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getTimestamp(column, calendar);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getTimestamp(column, calendar);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getURL(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getURL(column);
			}
		}
		catch (SQLException e)
		{
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

		try
		{
			if (this.preparedResultSet())
			{
				value = this.getResultSet().getURL(column);
			}
			else if (this.isCallResult())
			{
				value = this.getCallableStatement().getURL(column);
			}
		}
		catch (SQLException e)
		{
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
		return this.isResultSet() || this.getUpdateCount() != N_A;
	}

	private Sequel hasResultSetObject(boolean resultSetObject)
	{
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

	/**
	 * Locate the cursor to the head of the result set. Or does nothing if there
	 * is no result set presents.
	 * 
	 * @return Sequel itself.
	 * @throws SQLException
	 *             If the result set is {@code FORWARD_ONLY}.
	 */
	public Sequel head() throws SQLException
	{
		if (this.isResultSet())
		{
			this.getResultSet().beforeFirst();
		}
		return this;
	}

	public boolean isCallResult()
	{
		return this.getStatement() instanceof CallableStatement;
	}

	public boolean isClosed()
	{
		return statement == null;
	}

	public boolean isClosing()
	{
		return closing;
	}

	public boolean isIterating()
	{
		return iterating;
	}

	public boolean isResultSet()
	{
		return this.getResultSet() != null;
	}

	public boolean isUpdateCount()
	{
		return this.getUpdateCount() != N_A;
	}

	public SequelIterator iterate()
	{
		return new SequelIterator();
	}

	public SequelIterator iterate(int current)
	{
		return new SequelIterator(current);
	}

	public ResultSetIterator iterator()
	{
		return iterator(this.isClosing());
	}

	public ResultSetIterator iterator(boolean closing)
	{
		return new ResultSetIterator(this.getResultSet()) //
				.closing(closing && !this.isIterating()) //
				.kit(this.getKit());
	}

	public <T> ObjectIterator<T> iterator(Mapper<ResultSet, T> mapper)
	{
		return iterator(mapper, this.isClosing());
	}

	public <T> ObjectIterator<T> iterator(Mapper<ResultSet, T> mapper, boolean closing)
	{
		return new ObjectIterator<T>(this.getResultSet()) //
				.mapper(mapper) //
				.closing(closing && !this.isIterating()) //
				.kit(this.getKit());
	}

	/**
	 * Locate the cursor to a given position. Or does nothing if there is no
	 * result set presents.
	 * 
	 * @param row
	 *            The cursor position. {@code 0} is equal to calling
	 *            {@link Sequel#head()} method. This parameter could be negative
	 *            number which is counted from the last row.
	 * @return Sequel itself.
	 * @throws SQLException
	 *             If the result set is {@code FORWARD_ONLY}.
	 */
	public Sequel locate(int row) throws SQLException
	{
		if (this.isResultSet())
		{
			if (row == 0)
			{
				this.getResultSet().beforeFirst();
			}
			else
			{
				this.getResultSet().absolute(row);
			}
		}
		return this;
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
		try
		{
			return this.getResultSet().next();
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private boolean preparedResultSet()
	{
		boolean ok = false;

		try
		{
			if (this.isResultSet())
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
		}
		catch (Exception e)
		{
		}

		return ok;
	}

	public boolean prevRow()
	{
		try
		{
			return this.getResultSet().previous();
		}
		catch (Exception e)
		{
			return false;
		}
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

	public Sequel setClosing(boolean closing)
	{
		this.closing = closing;
		return this;
	}

	protected Sequel setIterating(boolean iterating)
	{
		this.iterating = iterating;
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
		this.metaMapIndex = null;
		this.metaMapName = null;
		return this;
	}

	private Sequel setStatement(Statement statement)
	{
		this.statement = statement;
		return this;
	}

	/**
	 * Move the cursor a given rows from the current position. Or does nothing
	 * if there is no result set presents.
	 * 
	 * @param rows
	 *            The step rows, could be negative number which means backward
	 *            step.
	 * @return Sequel itself.
	 * @throws SQLException
	 *             If the result set is {@code FORWARD_ONLY}.
	 */
	public Sequel step(int rows) throws SQLException
	{
		if (this.isResultSet())
		{
			ResultSet rs = this.getResultSet();

			if (rs.isBeforeFirst())
			{
				rs.first();
				rows--;
			}
			else if (rs.isAfterLast())
			{
				rs.last();
				rows++;
			}

			this.getResultSet().relative(rows);
		}
		return this;
	}

	/**
	 * Locate the cursor to the tail of the result set. Or does nothing if there
	 * is no result set presents.
	 * 
	 * @return Sequel itself.
	 * @throws SQLException
	 *             If the result set is {@code FORWARD_ONLY}.
	 */
	public Sequel tail() throws SQLException
	{
		if (this.isResultSet())
		{
			this.getResultSet().afterLast();
		}
		return this;
	}
}
