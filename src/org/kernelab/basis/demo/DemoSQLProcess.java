package org.kernelab.basis.demo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.kernelab.basis.sql.DataBase;
import org.kernelab.basis.sql.SQLKit;
import org.kernelab.basis.sql.SQLProcess;
import org.kernelab.basis.sql.DataBase.MySQL;

public class DemoSQLProcess
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		SQLProcess processor = new SQLProcess() {

			@Override
			protected void process(SQLKit kit) throws SQLException
			{
				String sql = "SELECT * FROM `table` WHERE `id`<10";
				ResultSet rs = kit.query(sql);
				while (rs.next()) {
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
				for (int i = 0; i < 10; i++) {
					kit.addBatch(SQLKit.NULL, String.valueOf(i));
				}
				kit.executeBatch();
				kit.commit(); // 提交批量操作
			}
		};

		DataBase database = new MySQL("localhost", "test", "root", "root");
		processor.setDataBase(database);

		// 同步执行
		processor.process();

		// 异步执行
		new Thread(processor).start(); // 直到执行完成前processor.isProcessing()始终是true
	}
}
