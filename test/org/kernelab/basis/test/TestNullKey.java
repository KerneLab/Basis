package org.kernelab.basis.test;

import java.util.LinkedHashMap;
import java.util.Map;

import org.kernelab.basis.Tools;

public class TestNullKey
{

	public static void main(String[] args)
	{
		Map<String, Integer> map = new LinkedHashMap<String, Integer>();

		map.put(null, 1);
		map.put("two", 2);

		Tools.debug(map.get(null));
	}

}
