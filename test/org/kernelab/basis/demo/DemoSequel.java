package org.kernelab.basis.demo;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.kernelab.basis.JSON;
import org.kernelab.basis.Tools;
import org.kernelab.basis.sql.DataBase;
import org.kernelab.basis.sql.DataBase.OracleClient;
import org.kernelab.basis.sql.SQLKit;
import org.kernelab.basis.sql.Sequel;

public class DemoSequel
{

	/**
	 * @param args
	 * @throws SQLException
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) throws SQLException
	{
		DataBase db = new OracleClient("orcl", "TEST", "TEST");

		SQLKit kit = db.getSQLKit();

		try
		{
			// kit.execute()返回Sequel对象
			String sql = "select * from jdl_test_record where id=1";
			Sequel seq = kit.execute(sql);
			// Sequel可以识别多个不同的执行结果
			switch (seq.getResultType())
			{
				case Sequel.RESULT_COUNT:
					// 执行结果为行数，通常为update、delete的结果
					seq.getUpdateCount();
					break;

				case Sequel.RESULT_SET:
					// 执行结果为ResultSet，通常为查询的结果
					while (seq.getResultSet().next())
					{
						Tools.debug(SQLKit.jsonOfResultRow(seq.getResultSet(), seq.getMetaMapName()));
					}
					break;

				case Sequel.RESULT_CALL:
					// 执行调用的结果，可以通过sql.getValueXXX获取输出内容
					break;

				default:
					// 无结果
					break;
			}
			// 建议在使用后关闭Sequel，虽然在kit关闭时也会清理相关资源
			seq.close();

			// 由于Sequel实现了Iterable<ResultSet>
			// 因此，这里可以使用for语法，而不用while(rs.next())
			for (ResultSet rs : seq = kit.execute("select * from jdl_test_record where id=?", 2))
			{
				Tools.debug(SQLKit.jsonOfResultRow(rs, seq.getMetaMapName()));
			}
			// Sequel返回的ResultSetIterator默认会在循环完成后自动关闭ResultSet对应的Statement

			// 如果不希望自动关闭，则可以将closing置为false
			for (ResultSet rs : (seq = kit.execute("select * from jdl_test_record where id=?", 3)) //
					.iterator(false) // 取消自动关闭功能
			)
			{
				Tools.debug(SQLKit.jsonOfResultRow(rs, seq.getMetaMapName()));
			}
			// 这么做的弊端是，使用者必须记得手动关闭Statement，才能避免在一个连接中开启过多的Statement
			kit.closeStatement();

			// 在某些查询中，会返回多个ResultSet，相应地，Sequel通过iterate()方法返回Iterable<Sequel>对象
			// 由此可以对多个ResultSet进行遍历
			for (Sequel sq : kit.setBoundary("#")
					.execute("select * from jdl_test_record where id=#id#", new JSON().attr("id", 1)).iterate())
			{ // 对Sequel迭代将隐含地取消了自动关闭功能，否则，当遍历到下一个ResultSet时，Statement已经被关闭
				for (ResultSet rs : sq)
				{
					Tools.debug(SQLKit.jsonOfResultRow(rs, sq.getMetaMapName()));
				}
				// 可以使用Sequel.closeResultSet()关闭当前的ResultSet
				sq.closeResultSet();
			}
			// 无论如何，在所有ResultSet被遍历完毕之后，Sequel对象默认会被自动关闭；
			// 除非在进入循环时Sequel对象不是自动关闭的

			JSON data = new JSON().attr("out", SQLKit.NONE).attr("in", "hello");
			CallableStatement cs = kit.prepareCall("{call P_JDL_TEST_OUT(#out#,#in#)}", data);
			cs.registerOutParameter(1, Types.VARCHAR);
			seq = kit.execute(cs, data);
			Tools.debug(seq.getValueString(1));

			kit.setBoundary("?");

			// 将结果映射为对象，使用JSON作为字段到属性的映射
			for (ResultSet rs : seq = kit.execute("select ID, NAME from jdl_test_obj where id=?i?",
					new JSON().attr("i", 1)))
			{
				Tools.debug(seq.getRow(DemoObject.class, new JSON().attr("id", "ID").attr("name", "NAME")));
			}

			// 直接使用字段别名作为对象的属性名
			for (ResultSet rs : seq = kit.execute("select ID \"id\", NAME \"name\" from jdl_test_obj where id=?i?",
					new JSON().attr("i", 1)))
			{
				Tools.debug(seq.getRow(DemoObject.class));
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			kit.close();
		}
	}

	public static class DemoObject
	{
		private int		id;

		private String	name;

		public int getId()
		{
			return id;
		}

		public String getName()
		{
			return name;
		}

		public void setId(int id)
		{
			this.id = id;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return "id=" + id + ", name=" + name;
		}
	}
}
