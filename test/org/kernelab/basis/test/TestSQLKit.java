package org.kernelab.basis.test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.kernelab.basis.Canal.Action;
import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;
import org.kernelab.basis.sql.DataBase;
import org.kernelab.basis.sql.DataBase.OracleClient;
import org.kernelab.basis.sql.SQLKit;
import org.kernelab.basis.sql.Sequel;

public class TestSQLKit
{

	public static void main(String[] args)
	{
		DataBase db = new OracleClient("orcl", "test", "TEST");

		try
		{
			SQLKit kit = db.getSQLKit();

			testSequelCanal1(kit);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

	}

	public static void testSequelCanal1(SQLKit kit) throws SQLException
	{
		String sql = "select comp_id \"id\", com_name \"name\" from comp where com_name like ?";

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

	public static void testSequelCanal(SQLKit kit) throws SQLException
	{
		String sql = "select * from staf";

		kit.execute(sql).canals().flatMap(new Mapper<Sequel, Iterable<JSON>>()
		{
			@Override
			public Iterable<JSON> map(Sequel el)
			{
				final Map<String, Object> meta = el.getMetaMapName();
				return el.canal().map(new Mapper<ResultSet, JSON>()
				{
					@Override
					public JSON map(ResultSet rs)
					{
						try
						{
							return SQLKit.jsonOfResultRow(rs, meta);
						}
						catch (SQLException e)
						{
							e.printStackTrace();
							return null;
						}
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

	public static void testGenerateKeys(SQLKit kit) throws SQLException
	{
		String sql = "insert into jdl_test_uuid (name, id, seq) values (?, CVM_GEN_ID, SEQ_DEMO.nextval)";

		Sequel seq = kit.execute(sql, new String[] { "id", "SEQ" }, new JSAN().add("mike"));

		Sequel keys = seq.getGeneratedKeys();

		String uuid = keys.getValueString(1);
		String seqno = keys.getValueString(2);

		Tools.debug(uuid + " " + seqno);
	}

}
