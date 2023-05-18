package org.kernelab.basis.test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.kernelab.basis.Canal.Action;
import org.kernelab.basis.Canal.Tuple;
import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;
import org.kernelab.basis.sql.DataBase;
import org.kernelab.basis.sql.DataBase.MariaDB;
import org.kernelab.basis.sql.SQLKit;
import org.kernelab.basis.sql.Sequel;

public class TestSQLKit
{

	public static void main(String[] args)
	{
		// DataBase db = new OracleClient("orcl", "test", "TEST");
		DataBase db = new MariaDB("localhost", 3306, "demo", "test", "test");

		try
		{
			SQLKit kit = db.getSQLKit();

			// testInScope(kit);
			// testTimestamp(kit);
			// testExists(kit);
			testBatches(kit);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	public static void testBatches(SQLKit kit) throws SQLException
	{
		kit.setAutoCommit(false);

		String s1 = "insert into jdl_test_batch1 (id,name) values (?id?, ?v1?)";
		String s2 = "insert into jdl_test_batch2 (id,name) values (?id?, ?v2?)";

		int j = 0;
		for (int i = 0; i < 10000; i++)
		{
			if (j >= 500)
			{
				kit.commitBatches();
				j = 0;
			}

			Map<String, Object> p = new HashMap<String, Object>();
			p.put("id", i);
			p.put("v1", "aa" + i);
			p.put("v2", "bb" + j);

			kit.addBatch(s1, p);
			kit.addBatch(s2, p);

			j++;
		}

		kit.commitBatches();
	}

	public static void testExists(SQLKit kit) throws SQLException
	{
		String sql = "select 1 from jdl_test_a where id=?";

		Tools.debug(kit.exists(sql, 2));
	}

	public static void testGenerateKeys(SQLKit kit) throws SQLException
	{
		String sql = "insert into jdl_test_uuid (name, id, seq) values (?, CVM_GEN_ID, SEQ_DEMO.nextval)";

		Sequel seq = kit.execute(sql, new String[] { "id", "SEQ" }, new JSAN().add("mike"));

		Sequel keys = seq.getGeneratedKeys();

		String uuid = keys.getValueString(1);
		String seqno = keys.getValueString(2);

		Tools.debug(uuid + " " + seqno);
	}

	public static void testInScope(SQLKit kit) throws SQLException
	{
		String sql = "select * from staf where comp_id = ?comp? and (dept_id, staf_id) in (?ids?)";

		JSON param = new JSON().attr("ids", new JSAN().addAll(0, //
				new JSAN().addAll(0, "13", "a22"), //
				new JSAN().addAll(0, "11", "a12"), //
				Tuple.of("12", "a22")//
		)) //
				.attr("comp", "1");

		Tools.debug(kit.execute(sql, param).getRows(new JSAN(), JSON.class));

		Map<String, Object> param1 = new HashMap<String, Object>();

		param1.put("comp", "1");
		param1.put("ids", new Object[] { //
				new String[] { "11", "a12" }, //
				Tuple.of("11", "a11") //
		});

		Tools.debug(kit.execute(sql, param1).getRows(new JSAN(), JSON.class));
	}

	public static void testSequelCanal(SQLKit kit) throws SQLException
	{
		String sql = "select * from staf";

		kit.execute(sql).canals().flatMap(new Mapper<Sequel, Iterable<JSON>>()
		{
			@Override
			public Iterable<JSON> map(Sequel el) throws Exception
			{
				final Map<String, Object> meta = el.getMetaMapName();
				return el.canal().map(new Mapper<ResultSet, JSON>()
				{
					@Override
					public JSON map(ResultSet rs) throws SQLException
					{
						return SQLKit.jsonOfResultRow(rs, meta);
					}
				});
			}
		}).foreach(new Action<JSON>()
		{
			@Override
			public void action(JSON el)
			{
				Tools.debug(el.toString());
			}
		});
	}

	public static void testSequelCanal1(SQLKit kit) throws SQLException
	{
		String sql = "select comp_id \"id\", com_name \"name\" from comp where com_name like ?";

		kit.setReuseStatements(false);

		kit.execute(sql, "B%").getRows(Company.class).foreach(new Action<Company>()
		{
			@Override
			public void action(Company el)
			{
				Tools.debug(el);
			}
		});

		kit.execute(sql, "B%").getRows(Company.class).limit(2).skip(1).foreach(new Action<Company>()
		{
			@Override
			public void action(Company el)
			{
				Tools.debug(el);
			}
		});

		kit.execute(sql, "B%").getRows(JSON.class).limit(2).skip(1).foreach(new Action<JSON>()
		{
			@Override
			public void action(JSON el)
			{
				Tools.debug(el);
			}
		});

		kit.execute(sql, "B%").getRows(JSAN.class).limit(2).skip(1).foreach(new Action<JSAN>()
		{
			@Override
			public void action(JSAN el)
			{
				Tools.debug(el);
			}
		});

	}

	public static void testTimestamp(SQLKit kit) throws SQLException
	{
		String sql = "insert into jdl_test_ts (ts) values (?ts?)";

		JSON param = new JSON().attr("ts",
				new Timestamp(Tools.getDate("2022-12-31 14:35:43", "yyyy-MM-dd HH:mm:ss").getTime()));

		kit.update(sql, param);
	}
}
