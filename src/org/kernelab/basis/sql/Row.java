package org.kernelab.basis.sql;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;
import org.kernelab.basis.sql.SQLKit.ProjectMapper;

public class Row implements Map<String, Object>, Serializable, Cloneable
{
	public static class RowProjector<E> extends ProjectMapper<Row, E>
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -1813680091310716510L;

		public RowProjector(Class<E> cls, Map<String, Object> map)
		{
			super(cls, map);
		}

		public RowProjector(Map<String, Object> map, E obj)
		{
			super(map, obj);
		}

		@Override
		protected Map<String, Object> dict(Row src) throws Exception
		{
			Map<String, Object> dict = new LinkedHashMap<String, Object>();
			for (String c : src.keySet())
			{
				dict.put(c, c);
			}
			return dict;
		}

		@Override
		protected Object get(Row src, Object key) throws Exception
		{
			return src.get(key);
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2300871853782173357L;

	public static Row of(Map<String, ?> map)
	{
		if (map == null)
		{
			return null;
		}
		else if (map instanceof Row)
		{
			return (Row) map;
		}
		else
		{
			return new Row(map);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T project(T obj, Map<String, Object> map)
	{
		if (obj == null || map == null)
		{
			return null;
		}

		if (obj instanceof JSON)
		{
			((JSON) obj).attrAll(map);
		}
		else if (obj instanceof Map)
		{
			((Map<String, Object>) obj).putAll(map);
		}
		else
		{
			String key;
			Field field;
			Object dat, val;
			Map<String, Field> fields = JSON.FieldsOf(obj);
			for (Entry<String, Object> entry : map.entrySet())
			{
				key = entry.getKey();
				if (key == null || (field = fields.get(key)) == null)
				{
					continue;
				}
				dat = entry.getValue();
				val = dat == null ? null : Tools.castTo(dat, field.getType());
				try
				{
					Tools.access(obj, key, field, val != null ? val : dat);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
		}

		return obj;
	}

	private Map<String, Object>	data;

	protected String[]			heads;

	protected Object[]			cols;

	public Row()
	{
		this.init();
	}

	public Row(Map<String, ?> data)
	{
		this();
		this.set(data);
	}

	public Row(Object... pairs)
	{
		this();
		this.set(mapOf(pairs));
	}

	public Row(ResultSet rs) throws SQLException
	{
		this();
		this.set(rs);
	}

	protected Object cast(Object value)
	{
		if (value == null)
		{
			return null;
		}

		Mapper<Object, Object> cast = null;
		Map<Class<?>, Mapper<Object, Object>> map = this.castMap();
		if (map != null)
		{
			for (Entry<Class<?>, Mapper<Object, Object>> entry : map.entrySet())
			{
				if (entry.getKey().isInstance(value))
				{
					cast = entry.getValue();
					break;
				}
			}
		}

		if (cast != null)
		{
			try
			{
				return cast.map(value);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		// Default Cast

		if (value instanceof java.sql.Clob)
		{
			try
			{
				return Tools.readerToStringBuilder(((java.sql.Clob) value).getCharacterStream()).toString();
			}
			catch (SQLException e)
			{
				throw new RuntimeException(e);
			}
		}

		return value;
	}

	protected Map<Class<?>, Mapper<Object, Object>> castMap()
	{
		return null;
	}

	/**
	 * UnsupportedOperation
	 */
	@Override
	@Deprecated
	public void clear()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Row clone()
	{
		Row row = newInstance();
		row.putAll(this);
		row.cols = this.cols;
		row.heads = this.heads;
		return row;
	}

	public Object[] columns()
	{
		if (this.cols == null)
		{
			this.cols = this.getData().values().toArray();
		}
		return this.cols;
	}

	@Override
	public boolean containsKey(Object key)
	{
		return this.getData().containsKey(key);
	}

	@Override
	public boolean containsValue(Object value)
	{
		return this.getData().containsValue(value);
	}

	protected Map<String, Object> copy()
	{
		Map<String, Object> data = this.newData();
		data.putAll(this.getData());
		return data;
	}

	/**
	 * Delete columns from this Row object.
	 * 
	 * @param cols
	 * @return
	 */
	public Row delete(Set<String> cols)
	{
		if (cols == null || cols.isEmpty())
		{
			return this;
		}

		for (String col : cols)
		{
			this.getData().remove(col);
		}

		return this;
	}

	/**
	 * Delete columns from this Row object.
	 * 
	 * @param cols
	 * @return
	 */
	public Row delete(String... cols)
	{
		if (cols == null || cols.length == 0)
		{
			return this;
		}

		for (String col : cols)
		{
			this.getData().remove(col);
		}

		return this;
	}

	/**
	 * Return a new Row object which drops the given columns.
	 * 
	 * @param cols
	 * @return
	 */
	public Row drop(Set<String> cols)
	{
		Map<String, Object> d = this.copy();
		if (cols != null && !cols.isEmpty())
		{
			for (String col : cols)
			{
				d.remove(col);
			}
		}
		Row r = this.newInstance();
		r.setData(d);
		return r;
	}

	/**
	 * Return a new Row object which drops the given columns.
	 * 
	 * @param cols
	 * @return
	 */
	public Row drop(String... cols)
	{
		Map<String, Object> d = this.copy();
		if (cols != null && cols.length > 0)
		{
			for (String col : cols)
			{
				d.remove(col);
			}
		}
		Row r = this.newInstance();
		r.setData(d);
		return r;
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet()
	{
		return this.getData().entrySet();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (!(obj instanceof Row))
		{
			return false;
		}
		return Tools.equals(this, (Row) obj);
	}

	public Object get(int index)
	{
		return this.columns()[index];
	}

	public Object get(int index, Object deft)
	{
		Object value = get(index);
		return value != null ? value : deft;
	}

	public Object get(Integer index)
	{
		return this.get(index.intValue());
	}

	public Object get(Integer index, Object deft)
	{
		return get(index.intValue(), deft);
	}

	@Override
	public Object get(Object key)
	{
		return this.getData().get(key);
	}

	public Object get(String key)
	{
		return this.getData().get(key);
	}

	public Object get(String key, Object deft)
	{
		Object value = this.get(key);
		return value != null ? value : deft;
	}

	public BigDecimal getBigDecimal(int index)
	{
		return getBigDecimal(headers()[index]);
	}

	public BigDecimal getBigDecimal(int index, double deft)
	{
		return getBigDecimal(headers()[index], deft);
	}

	public BigDecimal getBigDecimal(int index, String deft)
	{
		return getBigDecimal(headers()[index], deft);
	}

	public BigDecimal getBigDecimal(String key)
	{
		return JSON.CastToBigDecimal(this.get(key));
	}

	public BigDecimal getBigDecimal(String key, double deft)
	{
		BigDecimal get = getBigDecimal(key);
		return get == null ? new BigDecimal(deft) : get;
	}

	public BigDecimal getBigDecimal(String key, String deft)
	{
		BigDecimal get = getBigDecimal(key);
		return get == null ? new BigDecimal(deft) : get;
	}

	public BigInteger getBigInteger(int index)
	{
		return getBigInteger(headers()[index]);
	}

	public BigInteger getBigInteger(int index, long deft)
	{
		return getBigInteger(headers()[index], deft);
	}

	public BigInteger getBigInteger(int index, String deft)
	{
		return getBigInteger(headers()[index], deft);
	}

	public BigInteger getBigInteger(String key)
	{
		return JSON.CastToBigInteger(this.get(key));
	}

	public BigInteger getBigInteger(String key, long deft)
	{
		BigInteger get = getBigInteger(key);
		return get == null ? BigInteger.valueOf(deft) : get;
	}

	public BigInteger getBigInteger(String key, String deft)
	{
		BigInteger get = getBigInteger(key);
		return get == null ? new BigInteger(deft) : get;
	}

	public Boolean getBoolean(int index)
	{
		return getBoolean(headers()[index]);
	}

	public Boolean getBoolean(int index, Boolean deft)
	{
		return getBoolean(headers()[index], deft);
	}

	public Boolean getBoolean(String key)
	{
		return JSON.CastToBoolean(this.get(key));
	}

	public Boolean getBoolean(String key, Boolean deft)
	{
		Boolean get = getBoolean(key);
		return get == null ? deft : get;
	}

	public Byte getByte(int index)
	{
		return getByte(headers()[index]);
	}

	public Byte getByte(int index, Byte deft)
	{
		return getByte(headers()[index], deft);
	}

	public Byte getByte(String key)
	{
		return JSON.CastToByte(this.get(key));
	}

	public Byte getByte(String key, Byte deft)
	{
		Byte get = getByte(key);
		return get == null ? deft : get;
	}

	public Calendar getCalendar(int index)
	{
		return getCalendar(headers()[index]);
	}

	public Calendar getCalendar(int index, Calendar deft)
	{
		return getCalendar(headers()[index], deft);
	}

	public Calendar getCalendar(int index, long deft)
	{
		return getCalendar(headers()[index], deft);
	}

	public Calendar getCalendar(String key)
	{
		return JSON.CastToCalendar(this.get(key));
	}

	public Calendar getCalendar(String key, Calendar deft)
	{
		Calendar get = getCalendar(key);
		return get == null ? deft : get;
	}

	public Calendar getCalendar(String key, long deft)
	{
		Calendar get = getCalendar(key);

		if (get == null)
		{
			get = new GregorianCalendar();
			get.setTimeInMillis(deft);
		}

		return get;
	}

	public <E> E getCast(int index, Class<E> cls)
	{
		return getCast(headers()[index], cls);
	}

	public <E> E getCast(int index, Class<E> cls, E deft)
	{
		return getCast(headers()[index], cls, deft);
	}

	public <E> E getCast(String key, Class<E> cls)
	{
		return JSON.CastAs(this.get(key), cls);
	}

	public <E> E getCast(String key, Class<E> cls, E deft)
	{
		E value = getCast(key, cls);
		return value != null ? value : deft;
	}

	public Character getCharacter(int index)
	{
		return getCharacter(headers()[index]);
	}

	public Character getCharacter(int index, Character deft)
	{
		return getCharacter(headers()[index], deft);
	}

	public Character getCharacter(String key)
	{
		return JSON.CastToCharacter(this.get(key));
	}

	public Character getCharacter(String key, Character deft)
	{
		Character get = getCharacter(key);
		return get == null ? deft : get;
	}

	protected Map<String, Object> getData()
	{
		return data;
	}

	public Date getDate(int index)
	{
		return getDate(headers()[index]);
	}

	public Date getDate(int index, Date deft)
	{
		return getDate(headers()[index], deft);
	}

	public Date getDate(int index, long deft)
	{
		return getDate(headers()[index], deft);
	}

	public Date getDate(String key)
	{
		return JSON.CastToDate(this.get(key));
	}

	public Date getDate(String key, Date deft)
	{
		Date get = getDate(key);
		return get == null ? deft : get;
	}

	public Date getDate(String key, long deft)
	{
		Date get = getDate(key);
		return get == null ? new Date(deft) : get;
	}

	public Double getDouble(int index)
	{
		return getDouble(headers()[index]);
	}

	public Double getDouble(int index, Double deft)
	{
		return getDouble(headers()[index], deft);
	}

	public Double getDouble(String key)
	{
		return JSON.CastToDouble(this.get(key));
	}

	public Double getDouble(String key, Double deft)
	{
		Double get = getDouble(key);
		return get == null ? deft : get;
	}

	public Float getFloat(int index)
	{
		return getFloat(headers()[index]);
	}

	public Float getFloat(int index, Float deft)
	{
		return getFloat(headers()[index], deft);
	}

	public Float getFloat(String key)
	{
		return JSON.CastToFloat(this.get(key));
	}

	public Float getFloat(String key, Float deft)
	{
		Float get = getFloat(key);
		return get == null ? deft : get;
	}

	public Integer getInteger(int index)
	{
		return getInteger(headers()[index]);
	}

	public Integer getInteger(int index, Integer deft)
	{
		return getInteger(headers()[index], deft);
	}

	public Integer getInteger(String key)
	{
		return JSON.CastToInteger(this.get(key));
	}

	public Integer getInteger(String key, Integer deft)
	{
		Integer get = getInteger(key);
		return get == null ? deft : get;
	}

	public JSAN getJSAN(int index)
	{
		return getJSAN(headers()[index]);
	}

	public JSAN getJSAN(int index, boolean newIfNull)
	{
		return getJSAN(headers()[index], newIfNull);
	}

	public JSAN getJSAN(int index, JSAN deft)
	{
		return getJSAN(headers()[index], deft);
	}

	public JSAN getJSAN(String key)
	{
		return JSON.CastToJSAN(this.get(key));
	}

	public JSAN getJSAN(String key, boolean newIfNull)
	{
		JSAN get = getJSAN(key);
		return get == null && newIfNull ? new JSAN() : get;
	}

	public JSAN getJSAN(String key, JSAN deft)
	{
		JSAN get = getJSAN(key);
		return get == null ? deft : get;
	}

	public JSON getJSON(int index)
	{
		return getJSON(headers()[index]);
	}

	public JSON getJSON(int index, boolean newIfNull)
	{
		return getJSON(headers()[index], newIfNull);
	}

	public JSON getJSON(int index, JSON deft)
	{
		return getJSON(headers()[index], deft);
	}

	public JSON getJSON(String key)
	{
		return JSON.CastToJSON(this.get(key));
	}

	public JSON getJSON(String key, boolean newIfNull)
	{
		JSON get = getJSON(key);
		return get == null && newIfNull ? new JSON() : get;
	}

	public JSON getJSON(String key, JSON deft)
	{
		JSON get = getJSON(key);
		return get == null ? deft : get;
	}

	public Long getLong(int index)
	{
		return getLong(headers()[index]);
	}

	public Long getLong(int index, Long deft)
	{
		return getLong(headers()[index], deft);
	}

	public Long getLong(String key)
	{
		return JSON.CastToLong(this.get(key));
	}

	public Long getLong(String key, Long deft)
	{
		Long get = getLong(key);
		return get == null ? deft : get;
	}

	public Short getShort(int index)
	{
		return getShort(headers()[index]);
	}

	public Short getShort(int index, Short deft)
	{
		return getShort(headers()[index], deft);
	}

	public Short getShort(String key)
	{
		return JSON.CastToShort(this.get(key));
	}

	public Short getShort(String key, Short deft)
	{
		Short get = getShort(key);
		return get == null ? deft : get;
	}

	public String getString(int index)
	{
		return getString(headers()[index]);
	}

	public String getString(int index, String deft)
	{
		return getString(headers()[index], deft);
	}

	public String getString(String key)
	{
		return JSON.CastToString(this.get(key));
	}

	public String getString(String key, String deft)
	{
		String get = getString(key);
		return get == null ? deft : get;
	}

	public Time getTime(int index)
	{
		return getTime(headers()[index]);
	}

	public Time getTime(int index, long deft)
	{
		return getTime(headers()[index], deft);
	}

	public Time getTime(int index, Time deft)
	{
		return getTime(headers()[index], deft);
	}

	public Time getTime(String key)
	{
		return JSON.CastToTime(this.get(key));
	}

	public Time getTime(String key, long deft)
	{
		Time get = getTime(key);
		return get == null ? new Time(deft) : get;
	}

	public Time getTime(String key, Time deft)
	{
		Time get = getTime(key);
		return get == null ? deft : get;
	}

	public Timestamp getTimestamp(int index)
	{
		return getTimestamp(headers()[index]);
	}

	public Timestamp getTimestamp(int index, long deft)
	{
		return getTimestamp(headers()[index], deft);
	}

	public Timestamp getTimestamp(int index, Timestamp deft)
	{
		return getTimestamp(headers()[index], deft);
	}

	public Timestamp getTimestamp(String key)
	{
		return JSON.CastToTimestamp(this.get(key));
	}

	public Timestamp getTimestamp(String key, long deft)
	{
		Timestamp get = getTimestamp(key);
		return get == null ? new Timestamp(deft) : get;
	}

	public Timestamp getTimestamp(String key, Timestamp deft)
	{
		Timestamp get = getTimestamp(key);
		return get == null ? deft : get;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		for (Object v : this.getData().values())
		{
			result = prime * result + (v == null ? 0 : v.hashCode());
		}
		return result;
	}

	public String[] headers()
	{
		if (this.heads == null)
		{
			this.heads = this.getData().keySet().toArray(new String[0]);
		}
		return this.heads;
	}

	protected void init()
	{
		if (this.getData() == null)
		{
			this.setData(this.newData());
		}
	}

	@Override
	public boolean isEmpty()
	{
		return this.getData().isEmpty();
	}

	@Override
	public Set<String> keySet()
	{
		return this.getData().keySet();
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> mapOf(Object... over)
	{
		if (over == null || over.length == 0)
		{
			return null;
		}

		Map<String, Object> data = newData();

		if (over[0] instanceof Map)
		{
			for (Object obj : over)
			{
				if (obj instanceof Map)
				{
					Map<String, Object> m = (Map<String, Object>) obj;
					for (Entry<String, Object> entry : m.entrySet())
					{
						data.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}
		else if (over.length > 1)
		{
			for (int i = 0; i + 1 < over.length; i += 2)
			{
				if (over[i] != null)
				{
					data.put(over[i].toString(), over[i + 1]);
				}
			}
		}

		return data;
	}

	protected Map<String, Object> newData()
	{
		return new LinkedHashMap<String, Object>();
	}

	protected Row newInstance()
	{
		try
		{
			return this.getClass().newInstance();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * UnsupportedOperation
	 */
	@Override
	@Deprecated
	public Object put(String key, Object value)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * UnsupportedOperation
	 */
	@Override
	@Deprecated
	public void putAll(Map<? extends String, ?> m)
	{
		throw new UnsupportedOperationException();
	}

	protected Object putting(String col, Object value)
	{
		return this.getData().put(col, this.cast(value));
	}

	/**
	 * UnsupportedOperation
	 */
	@Override
	@Deprecated
	public Object remove(Object key)
	{
		throw new UnsupportedOperationException();
	}

	protected Row resetCols()
	{
		this.heads = null;
		this.cols = null;
		return this;
	}

	/**
	 * Return a new Row object which contains the selected columns from this Row
	 * object and overwrite columns.
	 * 
	 * @param select
	 *            the columns selected from this Row object.
	 * @param over
	 *            overwrite column-value pairs.
	 * @return
	 */
	public Row select(String[] select, Object... over)
	{
		Map<String, Object> data = this.newData();

		if (select == null)
		{ // select *
			for (String col : this.keySet())
			{
				data.put(col, this.get(col));
			}
		}
		else if (select.length > 0)
		{
			for (String col : select)
			{
				data.put(col, this.get(col));
			}
		}

		Row r = this.newInstance();
		r.setData(data);

		Map<String, Object> overs = mapOf(over);

		if (overs != null)
		{
			for (Entry<String, Object> entry : overs.entrySet())
			{
				r.putting(entry.getKey(), entry.getValue());
			}
		}

		return r;
	}

	/**
	 * Set the given column-values map to this Row object.
	 * 
	 * @param data
	 * @return
	 */
	public Row set(Map<String, ?> data)
	{
		this.resetCols();
		for (Entry<String, ?> entry : data.entrySet())
		{
			this.putting(entry.getKey(), entry.getValue());
		}
		return this;
	}

	protected Row set(ResultSet rs) throws SQLException
	{
		this.resetCols();
		if (rs != null)
		{
			ResultSetMetaData meta = rs.getMetaData();
			int count = meta.getColumnCount();
			for (int i = 1; i <= count; i++)
			{
				this.putting(meta.getColumnLabel(i), rs.getObject(i));
			}
		}
		return this;
	}

	/**
	 * Set a given column-value pair to this Row object.
	 * 
	 * @param col
	 * @param value
	 * @return
	 */
	public Row set(String col, Object value)
	{
		this.resetCols();
		this.putting(col, value);
		return this;
	}

	protected void setData(Map<String, Object> data)
	{
		this.data = data;
		this.resetCols();
	}

	@Override
	public int size()
	{
		return this.getData().size();
	}

	public <T> T to(Class<T> cls)
	{
		try
		{
			return to(cls.newInstance());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public <T> T to(T object)
	{
		try
		{
			return project(object, this.getData());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString()
	{
		return this.to(JSON.class).toString();
	}

	@Override
	public Collection<Object> values()
	{
		return this.getData().values();
	}
}
