package org.kernelab.basis.sql;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.Pair;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;

public class Row implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3208170451974323745L;

	@SuppressWarnings("unchecked")
	public static <T> T project(T obj, Map<String, Object> map)
	{
		if (obj == null || map == null)
		{
			return null;
		}

		if (obj instanceof Row)
		{
			((Row) obj).set(map);
		}
		else if (obj instanceof JSON)
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

	protected Object[]			cols;

	public Row()
	{
		this.init();
	}

	public Row(Map<String, Object> data)
	{
		this();
		this.set(data);
	}

	public Row(ResultSet rs) throws SQLException
	{
		this();
		this.set(rs);
	}

	public Row(Row record)
	{
		this();
		this.set(record.get());
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

	public void clear()
	{
		this.get().clear();
		this.resetCols();
	}

	public Object[] cols()
	{
		if (this.cols == null)
		{
			this.cols = this.get().values().toArray();
		}
		return this.cols;
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
		return Tools.equals(this.get(), ((Row) obj).get());
	}

	public Map<String, Object> get()
	{
		return data;
	}

	public Object get(int index)
	{
		return this.cols()[index];
	}

	public Row get(Iterable<String> keys)
	{
		Row r = new Row();
		for (String key : keys)
		{
			r.set(key, this.get(key));
		}
		return r;
	}

	public Row get(JSON map)
	{
		Row r = new Row();
		for (Pair pair : map.pairs())
		{
			r.set(pair.getKey(), this.get(pair.getValue().toString()));
		}
		return r;
	}

	public Row get(Map<String, Object> map)
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

	public Object get(String key)
	{
		return this.get().get(key);
	}

	public Row get(String... keys)
	{
		Row r = new Row();
		for (String key : keys)
		{
			r.set(key, this.get(key));
		}
		return r;
	}

	public Object get(String key, Object deft)
	{
		Object value = this.get(key);
		return value != null ? value : deft;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		for (Object v : this.get().values())
		{
			result = prime * result + (v == null ? 0 : v.hashCode());
		}
		return result;
	}

	protected void init()
	{
		if (this.get() == null)
		{
			this.setData(new LinkedHashMap<String, Object>());
		}
	}

	protected void resetCols()
	{
		this.cols = null;
	}

	public Row set(Map<String, Object> data)
	{
		this.resetCols();
		if (data != null)
		{
			for (Entry<String, Object> entry : data.entrySet())
			{
				this.set(entry.getKey(), entry.getValue());
			}
		}
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
				this.set(meta.getColumnLabel(i), rs.getObject(i));
			}
		}
		return this;
	}

	public Row set(String key, Object value)
	{
		this.resetCols();
		this.get().put(key, this.cast(value));
		return this;
	}

	protected void setData(Map<String, Object> data)
	{
		this.data = data;
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
			return project(object, this.get());
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
}
