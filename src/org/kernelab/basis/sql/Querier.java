package org.kernelab.basis.sql;

import java.util.Map;

public interface Querier<T>
{
	public Iterable<T> query(SQLKit kit, Map<String, Object> params);
}
