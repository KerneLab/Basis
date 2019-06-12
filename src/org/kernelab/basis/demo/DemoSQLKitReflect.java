package org.kernelab.basis.demo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.kernelab.basis.JSON;
import org.kernelab.basis.Tools;
import org.kernelab.basis.sql.DataBase;
import org.kernelab.basis.sql.DataBase.OracleClient;
import org.kernelab.basis.sql.SQLKit;
import org.kernelab.basis.sql.Sequel;

@SuppressWarnings("unused")
public class DemoSQLKitReflect
{
	public static class DemoPojo
	{
		private String	id;
		private String	grp;
		private int		rnk;
		private double	sal;

		public String getGrp()
		{
			return grp;
		}

		public String getId()
		{
			return id;
		}

		public int getRnk()
		{
			return rnk;
		}

		public double getSal()
		{
			return sal;
		}

		public void setGrp(String grp)
		{
			this.grp = grp;
		}

		public void setId(String id)
		{
			this.id = id;
		}

		public void setRnk(int rnk)
		{
			this.rnk = rnk;
		}

		public void setSal(double sal)
		{
			this.sal = sal;
		}

		public String toString()
		{
			return this.id + "\t" + this.grp + "\t" + this.rnk + "\t" + this.sal;
		}
	}

	public static void main(String[] args)
	{
		DataBase database = new OracleClient("orcl", "TEST", "TEST");

		try
		{
			SQLKit kit = database.getSQLKit();

			Sequel seq = kit.execute(
					"select id \"id\", grp \"grp\", rnk \"rnk\", sal \"sal\" from jdl_test_part where id>?id?",
					new JSON().attr("id", "3"));

			for (ResultSet rs : seq)
			{
				Tools.debug(seq.getRow(DemoPojo.class));
			}

			Tools.debug("==============================");

			seq = kit.execute("select id, grp, rnk, sal from jdl_test_part where id>?id? order by id desc",
					new JSON().attr("id", "3"));

			Map<String, Object> cols = new HashMap<String, Object>();

			cols.put("id", "ID");
			cols.put("grp", "GRP");
			cols.put("rnk", "RNK");
			// cols.put("sal", "SAL");

			for (ResultSet rs : seq)
			{
				Tools.debug(seq.getRow(DemoPojo.class, cols));
			}

			Tools.debug("==============================");

			seq = kit.execute("select id, grp, rnk, sal from jdl_test_part where id>?id? order by id desc",
					new JSON().attr("id", "3"));

			cols = SQLKit.generateCamelStyleKeyMap(seq.getMetaMapName().keySet());

			for (ResultSet rs : seq)
			{
				Tools.debug(seq.getRow(DemoPojo.class, cols));
			}

		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

}
