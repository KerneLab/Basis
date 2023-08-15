package org.kernelab.basis.sql;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.Pair;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;

public class Row implements Map<String, Object>, Serializable, Cloneable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4523492269850284428L;

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
		Row row = null;
		try
		{
			row = this.getClass().newInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			row = new Row();
		}
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

	protected Map<String, Object> getData()
	{
		return data;
	}

	public Row gets(Iterable<String> keys)
	{
		Row r = new Row();
		for (String key : keys)
		{
			r.set(key, this.get(key));
		}
		return r;
	}

	public Row gets(JSON map)
	{
		Row r = new Row();
		for (Pair pair : map.pairs())
		{
			r.set(pair.getKey(), this.get(pair.getValue().toString()));
		}
		return r;
	}

	public Row gets(Map<String, Object> map)
	{
		Row r = new Row();
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

	public Row gets(String... keys)
	{
		Row r = new Row();
		for (String key : keys)
		{
			r.set(key, this.get(key));
		}
		return r;
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
