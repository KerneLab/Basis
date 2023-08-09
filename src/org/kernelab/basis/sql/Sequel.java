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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.kernelab.basis.Canal;
import org.kernelab.basis.CloseableIterator;
import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;

public class Sequel implements Iterable<ResultSet>
{
	public static abstract class AbstractIterator<T> implements Iterable<T>, CloseableIterator<T>
	{
		private ResultSet	rs;

		private ResultSet	next;

		private SQLKit		kit;

		private boolean		closing;

		public AbstractIterator(ResultSet rs)
		{
			this.rs = rs;
			this.closing = true;
		}

		@Override
		public void close()
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
					}
					catch (SQLException e)
					{
					}
				}

				try
				{
					rs.close();
				}
				catch (SQLException e)
				{
				}

				rs = null;
			}
			kit = null;
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

		@Override
		public boolean hasNext()
		{
			if (rs != null)
			{
				if (next != null)
				{
					return true;
				}

				try
				{
					if (rs.next())
					{
						next = rs;
						return true;
					}
					else
					{
						close();
						return false;
					}
				}
				catch (SQLException e)
				{
					close();
					return false;
				}
			}
			else
			{
				close();
				return false;
			}
		}

		@Override
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

		@Override
		public T next()
		{
			try
			{
				return this.next(this.next);
			}
			finally
			{
				this.next = null;
			}
		}

		protected abstract T next(ResultSet rs);

		@Override
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
		private Mapper<ResultSet, T> mapper;

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

		@Override
		protected T next(ResultSet rs)
		{
			if (mapper() != null)
			{
				try
				{
					return mapper().map(rs);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
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
		public ResultSetIterator kit(SQLKit kit)
		{
			super.kit(kit);
			return this;
		}

		@Override
		protected ResultSet next(ResultSet rs)
		{
			return rs;
		}
	}

	public class SequelIterator implements Iterable<Sequel>, CloseableIterator<Sequel>
	{
		private int		current;

		private boolean	first	= true;

		private Sequel	next	= null;

		public SequelIterator()
		{
			this(Statement.CLOSE_CURRENT_RESULT);
		}

		public SequelIterator(int current)
		{
			this.current = current;
		}

		@Override
		public void close()
		{
			setIterating(false);
			Sequel.this.close();
		}

		@Override
		public boolean hasNext()
		{
			if (Sequel.this.isClosed())
			{
				return false;
			}

			if (next != null)
			{
				return true;
			}

			if (first)
			{
				first = false;
			}
			else
			{
				try
				{
					Sequel.this.nextResult(current);
				}
				catch (SQLException e)
				{
					throw new RuntimeException(e);
				}
			}

			if (Sequel.this.hasResult())
			{
				Sequel.this.setIterating(true);
				next = Sequel.this;
				return true;
			}
			else
			{
				this.close();
				return false;
			}
		}

		@Override
		public Iterator<Sequel> iterator()
		{
			return this;
		}

		@Override
		public Sequel next()
		{
			try
			{
				return next;
			}
			finally
			{
				next = null;
			}
		}

		@Override
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

	private SQLKit									kit;

	private Statement								statement;

	private ResultSet								resultSet;

	private int										updateCount		= N_A;

	private boolean									closed			= false;

	private boolean									closing			= true;

	private boolean									iterating		= false;

	private Map<String, Object>						metaMapIndex	= null;

	private Map<String, Object>						metaMapName		= null;

	private Map<Class<?>, Mapper<Object, Object>>	typeMap			= null;

	public Sequel(ResultSet rs)
	{
		this.setResultSet(rs);
	}

	public Sequel(SQLKit kit, Statement statement, boolean hasResultSet) throws SQLException
	{
		this.setKit(kit).setStatement(statement) //
				.hasResultSetObject(hasResultSet) //
				.refreshUpdateCount();
	}

	public Canal<?, ResultSet> canal()
	{
		return Canal.of(this);
	}

	public Canal<?, Sequel> canals()
	{
		return Canal.of(this.iterate());
	}

	public Canal<?, Sequel> canals(int current)
	{
		return Canal.of(this.iterate(current));
	}

	public Sequel close()
	{
		if (!isClosed())
		{
			if (isClosing())
			{
				try
				{
					this.closeStatement();
				}
				catch (SQLException e)
				{
				}
			}

			try
			{
				this.closeResultSet();
			}
			catch (SQLException e)
			{
			}

			this.setIterating(false);

			kit = null;
			metaMapIndex = null;
			metaMapName = null;
			typeMap = null;
			closed = true;
		}

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

	public Sequel getGeneratedKeys() throws SQLException
	{
		return new Sequel(this.getStatement().getGeneratedKeys());
	}

	protected SQLKit getKit()
	{
		return kit;
	}

	public ResultSetMetaData getMetaData() throws SQLException
	{
		if (this.isResultSet())
		{
			return this.getResultSet().getMetaData();
		}
		else
		{
			return null;
		}
	}

	public Map<String, Object> getMetaMapIndex() throws SQLException
	{
		if (metaMapIndex == null)
		{
			metaMapIndex = SQLKit.mapIndexOfMetaData(this.getResultSet().getMetaData());
		}
		return metaMapIndex;
	}

	public Map<String, Object> getMetaMapName() throws SQLException
	{
		if (metaMapName == null)
		{
			metaMapName = SQLKit.mapNameOfMetaData(this.getResultSet().getMetaData());
		}
		return metaMapName;
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

	public <E> E getRow(Class<E> cls) throws SQLException
	{
		return this.getRow(cls, null);
	}

	public <E> E getRow(Class<E> cls, Map<String, Object> map) throws SQLException
	{
		return this.getRow(cls, map, this.getTypeMap());
	}

	public <E> E getRow(Class<E> cls, Map<String, Object> map, Map<Class<?>, Mapper<Object, Object>> typeMap)
			throws SQLException
	{
		if (this.preparedResultSet())
		{
			if (map == null)
			{
				map = this.getMetaMapName();
			}
			return SQLKit.mapResultRow(this.getResultSet(), cls, map, typeMap);
		}
		else
		{
			return null;
		}
	}

	public <E> E getRow(Mapper<ResultSet, E> mapper) throws SQLException
	{
		if (this.preparedResultSet())
		{
			return SQLKit.mapResultRow(this.getResultSet(), mapper);
		}
		else
		{
			return null;
		}
	}

	public JSAN getRowAsJSAN() throws SQLException
	{
		return this.getRowAsJSAN(null);
	}

	public JSAN getRowAsJSAN(Map<String, Object> map) throws SQLException
	{
		if (this.preparedResultSet())
		{
			if (map == null)
			{
				map = this.getMetaMapIndex();
			}
			return SQLKit.jsanOfResultRow(this.getResultSet(), map);
		}
		return null;
	}

	public JSON getRowAsJSON() throws SQLException
	{
		return this.getRowAsJSON(null);
	}

	public JSON getRowAsJSON(Map<String, Object> map) throws SQLException
	{
		JSON json = null;

		if (this.preparedResultSet())
		{
			if (map == null)
			{
				map = this.getMetaMapName();
			}
			json = SQLKit.jsonOfResultRow(this.getResultSet(), map);
		}

		return json;
	}

	@SuppressWarnings("unchecked")
	public <E> Canal<?, E> getRows(Class<E> cls) throws SQLException
	{
		if (Tools.isSubClass(cls, JSON.class))
		{
			if (Tools.isSubClass(cls, JSAN.class))
			{
				return (Canal<?, E>) this.getRows(SQLKit.mapIndexOfMetaData(this.getResultSet().getMetaData()),
						JSAN.class);
			}
			else
			{
				return (Canal<?, E>) this.getRows(SQLKit.mapNameOfMetaData(this.getResultSet().getMetaData()),
						JSON.class);
			}
		}
		else
		{
			return this.getRows(cls, null);
		}
	}

	public <E> Canal<?, E> getRows(Class<E> cls, Map<String, Object> map) throws SQLException
	{
		return this.getRows(cls, map, this.getTypeMap());
	}

	public <E> Canal<?, E> getRows(Class<E> cls, Map<String, Object> map, Map<Class<?>, Mapper<Object, Object>> typeMap)
			throws SQLException
	{
		return SQLKit.mapResultSet(this.getKit(), this.getResultSet(), cls, map, typeMap);
	}

	public <E> Collection<E> getRows(Collection<E> rows, Class<E> cls) throws SQLException
	{
		return this.getRows(rows, cls, -1);
	}

	public <E> Collection<E> getRows(Collection<E> rows, Class<E> cls, int limit) throws SQLException
	{
		return this.getRows(rows, cls, null, limit);
	}

	public <E> Collection<E> getRows(Collection<E> rows, Class<E> cls, Map<String, Object> map) throws SQLException
	{
		return this.getRows(rows, cls, map, -1);
	}

	public <E> Collection<E> getRows(Collection<E> rows, Class<E> cls, Map<String, Object> map, int limit)
			throws SQLException
	{
		return this.getRows(rows, cls, map, this.getTypeMap(), limit);
	}

	public <E> Collection<E> getRows(Collection<E> rows, Class<E> cls, Map<String, Object> map,
			Map<Class<?>, Mapper<Object, Object>> typeMap, int limit) throws SQLException
	{
		try
		{
			rows = SQLKit.mapResultSet(this.getResultSet(), rows, cls, map, typeMap, limit);
		}
		finally
		{
			this.close();
		}
		return rows;
	}

	public <E> Collection<E> getRows(Collection<E> rows, Mapper<ResultSet, E> mapper) throws SQLException
	{
		return this.getRows(rows, mapper, -1);
	}

	public <E> Collection<E> getRows(Collection<E> rows, Mapper<ResultSet, E> mapper, int limit) throws SQLException
	{
		try
		{
			rows = SQLKit.mapResultSet(this.getResultSet(), rows, mapper, limit);
		}
		finally
		{
			this.close();
		}
		return rows;
	}

	public JSAN getRows(JSAN rows, Class<? extends JSON> cls) throws SQLException
	{
		return this.getRows(rows, null, cls);
	}

	public JSAN getRows(JSAN rows, Class<? extends JSON> cls, int limit) throws SQLException
	{
		return this.getRows(rows, null, cls, limit);
	}

	public JSAN getRows(JSAN rows, Map<String, Object> map, Class<? extends JSON> cls) throws SQLException
	{
		return this.getRows(rows, map, cls, -1);
	}

	public JSAN getRows(JSAN rows, Map<String, Object> map, Class<? extends JSON> cls, int limit) throws SQLException
	{
		try
		{
			rows = SQLKit.jsanOfResultSet(this.getResultSet(), rows, map, cls, limit);
		}
		finally
		{
			this.close();
		}
		return rows;
	}

	public Canal<?, JSON> getRows(Map<String, Object> map) throws SQLException
	{
		return this.getRows(map, JSON.class);
	}

	public Canal<?, JSON> getRows(Map<String, Object> map, Class<? extends JSON> cls) throws SQLException
	{
		return SQLKit.jsonOfResultSet(this.getKit(), this.getResultSet(), map, cls);
	}

	public <E> Canal<?, E> getRows(Mapper<ResultSet, E> mapper) throws SQLException
	{
		return SQLKit.mapResultSet(this.getKit(), this.getResultSet(), mapper);
	}

	public Statement getStatement()
	{
		return statement;
	}

	public Map<Class<?>, Mapper<Object, Object>> getTypeMap()
	{
		return typeMap;
	}

	public int getUpdateCount()
	{
		return updateCount;
	}

	public Array getValueArray(int column) throws SQLException
	{
		Array value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getArray(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getArray(column);
		}

		return value;
	}

	public Array getValueArray(int column, Array defaultValue) throws SQLException
	{
		Array value = getValueArray(column);
		return value == null ? defaultValue : value;
	}

	public Array getValueArray(String column) throws SQLException
	{
		Array value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getArray(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getArray(column);
		}

		return value;
	}

	public Array getValueArray(String column, Array defaultValue) throws SQLException
	{
		Array value = getValueArray(column);
		return value == null ? defaultValue : value;
	}

	public InputStream getValueAsciiStream(int column) throws SQLException
	{
		InputStream value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getAsciiStream(column);
		}

		return value;
	}

	public InputStream getValueAsciiStream(int column, InputStream defaultValue) throws SQLException
	{
		InputStream value = getValueAsciiStream(column);
		return value == null ? defaultValue : value;
	}

	public InputStream getValueAsciiStream(String column) throws SQLException
	{
		InputStream value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getAsciiStream(column);
		}

		return value;
	}

	public InputStream getValueAsciiStream(String column, InputStream defaultValue) throws SQLException
	{
		InputStream value = getValueAsciiStream(column);
		return value == null ? defaultValue : value;
	}

	public BigDecimal getValueBigDecimal(int column) throws SQLException
	{
		BigDecimal value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getBigDecimal(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getBigDecimal(column);
		}

		return value;
	}

	public BigDecimal getValueBigDecimal(int column, BigDecimal defaultValue) throws SQLException
	{
		BigDecimal value = getValueBigDecimal(column);
		return value == null ? defaultValue : value;
	}

	public BigDecimal getValueBigDecimal(String column) throws SQLException
	{
		BigDecimal value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getBigDecimal(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getBigDecimal(column);
		}

		return value;
	}

	public BigDecimal getValueBigDecimal(String column, BigDecimal defaultValue) throws SQLException
	{
		BigDecimal value = getValueBigDecimal(column);
		return value == null ? defaultValue : value;
	}

	public InputStream getValueBinaryStream(int column) throws SQLException
	{
		InputStream value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getBinaryStream(column);
		}

		return value;
	}

	public InputStream getValueBinaryStream(int column, InputStream defaultValue) throws SQLException
	{
		InputStream value = getValueBinaryStream(column);
		return value == null ? defaultValue : value;
	}

	public InputStream getValueBinaryStream(String column) throws SQLException
	{
		InputStream value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getBinaryStream(column);
		}

		return value;
	}

	public InputStream getValueBinaryStream(String column, InputStream defaultValue) throws SQLException
	{
		InputStream value = getValueBinaryStream(column);
		return value == null ? defaultValue : value;
	}

	public Blob getValueBlob(int column) throws SQLException
	{
		Blob value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getBlob(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getBlob(column);
		}

		return value;
	}

	public Blob getValueBlob(int column, Blob defaultValue) throws SQLException
	{
		Blob value = getValueBlob(column);
		return value == null ? defaultValue : value;
	}

	public Blob getValueBlob(String column) throws SQLException
	{
		Blob value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getBlob(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getBlob(column);
		}

		return value;
	}

	public Blob getValueBlob(String column, Blob defaultValue) throws SQLException
	{
		Blob value = getValueBlob(column);
		return value == null ? defaultValue : value;
	}

	public Boolean getValueBoolean(int column) throws SQLException
	{
		Boolean value = null;

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

		return value;
	}

	public Boolean getValueBoolean(int column, Boolean defaultValue) throws SQLException
	{
		Boolean value = getValueBoolean(column);
		return value == null ? defaultValue : value;
	}

	public Boolean getValueBoolean(String column) throws SQLException
	{
		Boolean value = null;

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

		return value;
	}

	public Boolean getValueBoolean(String column, Boolean defaultValue) throws SQLException
	{
		Boolean value = getValueBoolean(column);
		return value == null ? defaultValue : value;
	}

	public Byte getValueByte(int column) throws SQLException
	{
		Byte value = null;

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

		return value;
	}

	public Byte getValueByte(int column, Byte defaultValue) throws SQLException
	{
		Byte value = getValueByte(column);
		return value == null ? defaultValue : value;
	}

	public Byte getValueByte(String column) throws SQLException
	{
		Byte value = null;

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

		return value;
	}

	public Byte getValueByte(String column, Byte defaultValue) throws SQLException
	{
		Byte value = getValueByte(column);
		return value == null ? defaultValue : value;
	}

	public byte[] getValueBytes(int column) throws SQLException
	{
		byte[] value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getBytes(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getBytes(column);
		}

		return value;
	}

	public byte[] getValueBytes(int column, byte[] defaultValue) throws SQLException
	{
		byte[] value = getValueBytes(column);
		return value == null ? defaultValue : value;
	}

	public byte[] getValueBytes(String column) throws SQLException
	{
		byte[] value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getBytes(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getBytes(column);
		}

		return value;
	}

	public byte[] getValueBytes(String column, byte[] defaultValue) throws SQLException
	{
		byte[] value = getValueBytes(column);
		return value == null ? defaultValue : value;
	}

	public Reader getValueCharacterStream(int column) throws SQLException
	{
		Reader value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getCharacterStream(column);
		}

		return value;
	}

	public Reader getValueCharacterStream(int column, Reader defaultValue) throws SQLException
	{
		Reader value = getValueCharacterStream(column);
		return value == null ? defaultValue : value;
	}

	public Reader getValueCharacterStream(String column) throws SQLException
	{
		Reader value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getCharacterStream(column);
		}

		return value;
	}

	public Reader getValueCharacterStream(String column, Reader defaultValue) throws SQLException
	{
		Reader value = getValueCharacterStream(column);
		return value == null ? defaultValue : value;
	}

	public Clob getValueClob(int column) throws SQLException
	{
		Clob value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getClob(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getClob(column);
		}

		return value;
	}

	public Clob getValueClob(int column, Clob defaultValue) throws SQLException
	{
		Clob value = getValueClob(column);
		return value == null ? defaultValue : value;
	}

	public Clob getValueClob(String column) throws SQLException
	{
		Clob value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getClob(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getClob(column);
		}

		return value;
	}

	public Clob getValueClob(String column, Clob defaultValue) throws SQLException
	{
		Clob value = getValueClob(column);
		return value == null ? defaultValue : value;
	}

	public Date getValueDate(int column) throws SQLException
	{
		Date value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getDate(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getDate(column);
		}

		return value;
	}

	public Date getValueDate(int column, Calendar calendar) throws SQLException
	{
		Date value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getDate(column, calendar);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getDate(column, calendar);
		}

		return value;
	}

	public Date getValueDate(int column, Calendar calendar, Date defaultValue) throws SQLException
	{
		Date value = getValueDate(column, calendar);
		return value == null ? defaultValue : value;
	}

	public Date getValueDate(int column, Date defaultValue) throws SQLException
	{
		Date value = getValueDate(column);
		return value == null ? defaultValue : value;
	}

	public Date getValueDate(String column) throws SQLException
	{
		Date value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getDate(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getDate(column);
		}

		return value;
	}

	public Date getValueDate(String column, Calendar calendar) throws SQLException
	{
		Date value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getDate(column, calendar);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getDate(column, calendar);
		}

		return value;
	}

	public Date getValueDate(String column, Calendar calendar, Date defaultValue) throws SQLException
	{
		Date value = getValueDate(column, calendar);
		return value == null ? defaultValue : value;
	}

	public Date getValueDate(String column, Date defaultValue) throws SQLException
	{
		Date value = getValueDate(column);
		return value == null ? defaultValue : value;
	}

	public Double getValueDouble(int column) throws SQLException
	{
		Double value = null;

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

		return value;
	}

	public Double getValueDouble(int column, Double defaultValue) throws SQLException
	{
		Double value = getValueDouble(column);
		return value == null ? defaultValue : value;
	}

	public Double getValueDouble(String column) throws SQLException
	{
		Double value = null;

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

		return value;
	}

	public Double getValueDouble(String column, Double defaultValue) throws SQLException
	{
		Double value = getValueDouble(column);
		return value == null ? defaultValue : value;
	}

	public Float getValueFloat(int column) throws SQLException
	{
		Float value = null;

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

		return value;
	}

	public Float getValueFloat(int column, Float defaultValue) throws SQLException
	{
		Float value = getValueFloat(column);
		return value == null ? defaultValue : value;
	}

	public Float getValueFloat(String column) throws SQLException
	{
		Float value = null;

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

		return value;
	}

	public Float getValueFloat(String column, Float defaultValue) throws SQLException
	{
		Float value = getValueFloat(column);
		return value == null ? defaultValue : value;
	}

	public Integer getValueInteger(int column) throws SQLException
	{
		Integer value = null;

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

		return value;
	}

	public Integer getValueInteger(int column, Integer defaultValue) throws SQLException
	{
		Integer value = getValueInteger(column);
		return value == null ? defaultValue : value;
	}

	public Integer getValueInteger(String column) throws SQLException
	{
		Integer value = null;

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

		return value;
	}

	public Integer getValueInteger(String column, Integer defaultValue) throws SQLException
	{
		Integer value = getValueInteger(column);
		return value == null ? defaultValue : value;
	}

	public Long getValueLong(int column) throws SQLException
	{
		Long value = null;

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

		return value;
	}

	public Long getValueLong(int column, Long defaultValue) throws SQLException
	{
		Long value = getValueLong(column);
		return value == null ? defaultValue : value;
	}

	public Long getValueLong(String column) throws SQLException
	{
		Long value = null;

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

		return value;
	}

	public Long getValueLong(String column, Long defaultValue) throws SQLException
	{
		Long value = getValueLong(column);
		return value == null ? defaultValue : value;
	}

	public Object getValueObject(int column) throws SQLException
	{
		Object value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getObject(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getObject(column);
		}

		return value;
	}

	public Object getValueObject(int column, Map<String, Class<?>> map) throws SQLException
	{
		Object value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getObject(column, map);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getObject(column, map);
		}

		return value;
	}

	public Object getValueObject(int column, Map<String, Class<?>> map, Object defaultValue) throws SQLException
	{
		Object value = getValueObject(column, map);
		return value == null ? defaultValue : value;
	}

	public Object getValueObject(int column, Object defaultValue) throws SQLException
	{
		Object value = getValueObject(column);
		return value == null ? defaultValue : value;
	}

	public Object getValueObject(String column) throws SQLException
	{
		Object value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getObject(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getObject(column);
		}

		return value;
	}

	public Object getValueObject(String column, Map<String, Class<?>> map) throws SQLException
	{
		Object value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getObject(column, map);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getObject(column, map);
		}

		return value;
	}

	public Object getValueObject(String column, Map<String, Class<?>> map, Object defaultValue) throws SQLException
	{
		Object value = getValueObject(column, map);
		return value == null ? defaultValue : value;
	}

	public Object getValueObject(String column, Object defaultValue) throws SQLException
	{
		Object value = getValueObject(column);
		return value == null ? defaultValue : value;
	}

	public Ref getValueRef(int column) throws SQLException
	{
		Ref value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getRef(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getRef(column);
		}

		return value;
	}

	public Ref getValueRef(int column, Ref defaultValue) throws SQLException
	{
		Ref value = getValueRef(column);
		return value == null ? defaultValue : value;
	}

	public Ref getValueRef(String column) throws SQLException
	{
		Ref value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getRef(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getRef(column);
		}

		return value;
	}

	public Ref getValueRef(String column, Ref defaultValue) throws SQLException
	{
		Ref value = getValueRef(column);
		return value == null ? defaultValue : value;
	}

	public Short getValueShort(int column) throws SQLException
	{
		Short value = null;

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

		return value;
	}

	public Short getValueShort(int column, Short defaultValue) throws SQLException
	{
		Short value = getValueShort(column);
		return value == null ? defaultValue : value;
	}

	public Short getValueShort(String column) throws SQLException
	{
		Short value = null;

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

		return value;
	}

	public Short getValueShort(String column, Short defaultValue) throws SQLException
	{
		Short value = getValueShort(column);
		return value == null ? defaultValue : value;
	}

	public String getValueString(int column) throws SQLException
	{
		String value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getString(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getString(column);
		}

		return value;
	}

	public String getValueString(int column, String defaultValue) throws SQLException
	{
		String value = getValueString(column);
		return value == null ? defaultValue : value;
	}

	public String getValueString(String column) throws SQLException
	{
		String value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getString(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getString(column);
		}

		return value;
	}

	public String getValueString(String column, String defaultValue) throws SQLException
	{
		String value = getValueString(column);
		return value == null ? defaultValue : value;
	}

	public Time getValueTime(int column) throws SQLException
	{
		Time value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getTime(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getTime(column);
		}

		return value;
	}

	public Time getValueTime(int column, Calendar calendar) throws SQLException
	{
		Time value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getTime(column, calendar);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getTime(column, calendar);
		}

		return value;
	}

	public Time getValueTime(int column, Calendar calendar, Time defaultValue) throws SQLException
	{
		Time value = getValueTime(column, calendar);
		return value == null ? defaultValue : value;
	}

	public Time getValueTime(int column, Time defaultValue) throws SQLException
	{
		Time value = getValueTime(column);
		return value == null ? defaultValue : value;
	}

	public Time getValueTime(String column) throws SQLException
	{
		Time value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getTime(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getTime(column);
		}

		return value;
	}

	public Time getValueTime(String column, Calendar calendar) throws SQLException
	{
		Time value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getTime(column, calendar);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getTime(column, calendar);
		}

		return value;
	}

	public Time getValueTime(String column, Calendar calendar, Time defaultValue) throws SQLException
	{
		Time value = getValueTime(column, calendar);
		return value == null ? defaultValue : value;
	}

	public Time getValueTime(String column, Time defaultValue) throws SQLException
	{
		Time value = getValueTime(column);
		return value == null ? defaultValue : value;
	}

	public Timestamp getValueTimestamp(int column) throws SQLException
	{
		Timestamp value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getTimestamp(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getTimestamp(column);
		}

		return value;
	}

	public Timestamp getValueTimestamp(int column, Calendar calendar) throws SQLException
	{
		Timestamp value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getTimestamp(column, calendar);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getTimestamp(column, calendar);
		}

		return value;
	}

	public Timestamp getValueTimestamp(int column, Calendar calendar, Timestamp defaultValue) throws SQLException
	{
		Timestamp value = getValueTimestamp(column, calendar);
		return value == null ? defaultValue : value;
	}

	public Timestamp getValueTimestamp(int column, Timestamp defaultValue) throws SQLException
	{
		Timestamp value = getValueTimestamp(column);
		return value == null ? defaultValue : value;
	}

	public Timestamp getValueTimestamp(String column) throws SQLException
	{
		Timestamp value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getTimestamp(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getTimestamp(column);
		}

		return value;
	}

	public Timestamp getValueTimestamp(String column, Calendar calendar) throws SQLException
	{
		Timestamp value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getTimestamp(column, calendar);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getTimestamp(column, calendar);
		}

		return value;
	}

	public Timestamp getValueTimestamp(String column, Calendar calendar, Timestamp defaultValue) throws SQLException
	{
		Timestamp value = getValueTimestamp(column, calendar);
		return value == null ? defaultValue : value;
	}

	public Timestamp getValueTimestamp(String column, Timestamp defaultValue) throws SQLException
	{
		Timestamp value = getValueTimestamp(column);
		return value == null ? defaultValue : value;
	}

	public URL getValueURL(int column) throws SQLException
	{
		URL value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getURL(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getURL(column);
		}

		return value;
	}

	public URL getValueURL(int column, URL defaultValue) throws SQLException
	{
		URL value = getValueURL(column);
		return value == null ? defaultValue : value;
	}

	public URL getValueURL(String column) throws SQLException
	{
		URL value = null;

		if (this.preparedResultSet())
		{
			value = this.getResultSet().getURL(column);
		}
		else if (this.isCallResult())
		{
			value = this.getCallableStatement().getURL(column);
		}

		return value;
	}

	public URL getValueURL(String column, URL defaultValue) throws SQLException
	{
		URL value = getValueURL(column);
		return value == null ? defaultValue : value;
	}

	public boolean hasResult()
	{
		return this.isResultSet() || this.getUpdateCount() != N_A;
	}

	private Sequel hasResultSetObject(boolean resultSetObject) throws SQLException
	{
		if (resultSetObject)
		{
			return this.setResultSet(this.getKit().record(this.getStatement().getResultSet()));
		}
		else
		{
			return this.setResultSet(null);
		}
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
		return closed;
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

	@Override
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

	public <E> E mapRow(Map<String, Object> map, E object) throws SQLException
	{
		if (this.preparedResultSet())
		{
			if (map == null)
			{
				map = this.getMetaMapName();
			}
			return SQLKit.mapResultRow(this.getResultSet(), map, this.getTypeMap(), object);
		}
		else
		{
			return null;
		}
	}

	public boolean nextResult() throws SQLException
	{
		return nextResult(Statement.CLOSE_CURRENT_RESULT);
	}

	public boolean nextResult(int current) throws SQLException
	{
		this.hasResultSetObject(false);

		if (this.getStatement().getMoreResults(current))
		{
			this.hasResultSetObject(true);

			if (this.isResultSet())
			{
				return true;
			}
		}

		return this.refreshUpdateCount();
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

	private boolean preparedResultSet() throws SQLException
	{
		if (this.isResultSet())
		{
			if (this.getResultSet().isBeforeFirst())
			{
				return this.getResultSet().next();
			}
			else
			{
				return true;
			}
		}
		else
		{
			return false;
		}
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

	private boolean refreshUpdateCount() throws SQLException
	{
		this.updateCount = statement.getUpdateCount();
		return this.updateCount != N_A;
	}

	protected Sequel setClosed(boolean closed)
	{
		this.closed = closed;
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

	public Sequel setTypeMap(Map<Class<?>, Mapper<Object, Object>> typeMap)
	{
		this.typeMap = typeMap;
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
