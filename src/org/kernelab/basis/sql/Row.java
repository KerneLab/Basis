package org.kernelab.basis.sql;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
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
import org.kernelab.basis.JSON.Pair;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;

public class Row implements Map<String, Object>, Serializable, Cloneable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -225186284768405125L;

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

	public Row(Map<String, Object> data)
	{
		this();
		this.putAll(data);
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

	public Row clean()
	{
		this.clear();
		return this;
	}

	@Override
	public void clear()
	{
		this.resetCols();
		this.getData().clear();
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

	public Row delete(String key)
	{
		this.remove(key);
		return this;
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

	public Row exclude(Set<String> keys)
	{
		if (keys != null && !keys.isEmpty())
		{
			for (String key : keys)
			{
				this.remove(key);
			}
		}
		return this;
	}

	public Row exclude(String... keys)
	{
		if (keys != null && keys.length > 0)
		{
			for (String key : keys)
			{
				this.remove(key);
			}
		}
		return this;
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
			this.setData(new LinkedHashMap<String, Object>());
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

	protected Row newInstance()
	{
		try
		{
			return this.getClass().newInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return new Row();
		}
	}

	public Row newRow(Iterable<String> keys)
	{
		Row r = newInstance();
		for (String key : keys)
		{
			r.set(key, this.get(key));
		}
		return r;
	}

	public Row newRow(JSON map)
	{
		Row r = newInstance();
		for (Pair pair : map.pairs())
		{
			r.set(pair.getKey(), this.get(pair.getValue().toString()));
		}
		return r;
	}

	public Row newRow(Map<String, Object> map)
	{
		Row r = newInstance();
		Object i = null;
		for (Entry<String, Object> entry : map.entrySet())
		{
			i = entry.getValue();
			if (i instanceof Integer)
			{
				r.set(entry.getKey(), this.get((Integer) i));
			}
			else
			{
				r.set(entry.getKey(), this.get(i.toString()));
			}
		}
		return r;
	}

	public Row newRow(String... keys)
	{
		Row r = newInstance();
		for (String key : keys)
		{
			r.set(key, this.get(key));
		}
		return r;
	}

	@Override
	public Object put(String key, Object value)
	{
		return this.resetCols().putting(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m)
	{
		this.resetCols();
		if (m != null)
		{
			for (Entry<?, ?> entry : m.entrySet())
			{
				this.putting(entry.getKey() != null ? entry.getKey().toString() : null, entry.getValue());
			}
		}
	}

	protected Object putting(String key, Object value)
	{
		return this.getData().put(key, this.cast(value));
	}

	@Override
	public Object remove(Object key)
	{
		return this.resetCols().getData().remove(key);
	}

	protected Row resetCols()
	{
		this.heads = null;
		this.cols = null;
		return this;
	}

	public Row set(Map<String, Object> data)
	{
		this.putAll(data);
		return this;
	}

	public Row set(ResultSet rs) throws SQLException
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

	public Row set(String key, Object value)
	{
		this.put(key, value);
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
