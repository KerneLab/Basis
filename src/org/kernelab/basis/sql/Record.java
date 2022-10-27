package org.kernelab.basis.sql;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.kernelab.basis.JSON;
import org.kernelab.basis.Tools;

public class Record implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8936498030856863475L;

	public static void main(String[] args)
	{
		Record a = new Record().set("a", 1).set("b", true).set("c", 1.3);
		Record b = new Record().set("a", 2).set("b", true).set("c", 1.3);

		Tools.debug(a.equals(b));
	}

	private Map<String, Object> data;

	public Record()
	{
		this.clear();
	}

	public Record(Map<String, Object> data)
	{
		this.set(data);
	}

	public Record(Record record)
	{
		this.set(record.get());
	}

	public Record(ResultSet rs) throws SQLException
	{
		this.set(rs);
	}

	public Record clear()
	{
		if (this.data == null)
		{
			this.setData(new LinkedHashMap<String, Object>());
		}
		else
		{
			this.data.clear();
		}
		return this;
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
		if (!(obj instanceof Record))
		{
			return false;
		}
		return Tools.equals(this.get(), ((Record) obj).get());
	}

	public Map<String, Object> get()
	{
		return data;
	}

	public Object get(String key)
	{
		return this.get().get(key);
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

	public Record set(Map<String, Object> data)
	{
		this.clear();
		if (data != null)
		{
			this.data.putAll(data);
		}
		return this;
	}

	public Record set(ResultSet rs) throws SQLException
	{
		this.clear();
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

	public Record set(String key, Object value)
	{
		this.get().put(key, value);
		return this;
	}

	protected void setData(Map<String, Object> data)
	{
		this.data = data;
	}

	@Override
	public String toString()
	{
		return new JSON().attrAll(this.get()).toString();
	}
}
