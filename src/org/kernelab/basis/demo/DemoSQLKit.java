package org.kernelab.basis.demo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kernelab.basis.sql.DataBase;
import org.kernelab.basis.sql.DataBase.Oracle;
import org.kernelab.basis.sql.SQLKit;

/**
 * SQLKit类是对Connection、Statement等的封装。<br />
 * 
 * 测试表格信息如下<br />
 * CREATE TABLE `table` ( <br />
 * `id` int(10) unsigned NOT NULL auto_increment, <br />
 * `text` text NOT NULL, <br/>
 * PRIMARY KEY (`id`) <br />
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
 * 
 * @author Dilly King
 * 
 */
public class DemoSQLKit
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String serverName = "localhost";
		String userName = "root";
		String passWord = "root";
		String catalog = "test";
		// 数据库名称为test
		DataBase database = new Oracle(serverName, catalog, userName, passWord);

		// 准备工作
		SQLKit kit = null;
		ResultSet rs = null;
		String sql = null;

		try
		{
			kit = database.getSQLKit(); // 获取kit

			// 执行查询
			sql = "SELECT * FROM `table` WHERE `id`<10";
			rs = kit.query(sql);
			while (rs.next())
			{
				// 在这里遍历每一条记录
			}

			sql = "SELECT * FROM `table` WHERE `id`<?";
			rs = kit.query(sql, 10); // 这里的10就是把10这个值填充到上面语句中的?位置

			// 执行修改
			sql = "UPDATE `table` SET `text`=? WHERE `id`=?";
			kit.update(sql, "hey!", 10);
			// 相当于把"hey!"填充到第一个?的位置，10填充到第二个问号的位置

			// 用List<Object>作为参数列表
			sql = "INSERT INTO `table` (`id`,`text`) VALUES (?,?)";
			List<Object> param = new ArrayList<Object>();
			param.add(SQLKit.NULL); // 用SQLKit.NULL来表示SQL语句中的null
			param.add("你好");
			kit.update(sql, param);

			// 批量执行操作
			kit.setAutoCommit(false);
			kit.clearBatch();
			kit.prepareStatement(sql);
			for (int i = 0; i < 10; i++)
			{
				kit.addBatch(SQLKit.NULL, String.valueOf(i));
			}
			kit.executeBatch();
			kit.commit(); // 提交批量操作

			// 在SQL语句中嵌入参数名
			sql = "SELECT * FROM `table` WHERE `id`>?i? AND `text`<?tx?";

			// 在Map<String,Object>对象中指定参数
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("i", 5);
			data.put("tx", "8");

			// 以data作为参数查询，将其中的i与tx值填充至SQL中的相应位置
			rs = kit.query(sql, data);
			while (rs.next())
			{
				// 在这里遍历每一条记录
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			kit.close(); // !!必需在finally这里关闭kit
			// 在关闭后，需要重新通过database.getSQLKit()来获取新的kit
		}
	}
}
