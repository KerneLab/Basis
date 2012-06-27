package org.kernelab.basis.sql;

import java.awt.event.ActionEvent;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.kernelab.basis.AbstractAccomplishable;
import org.kernelab.basis.Tools;
import org.kernelab.basis.sql.DataBase.MySQL;

/**
 * SQLProcess is a class which wrap the SQLKit to make the usage more
 * convenient. Users do not need to concern the try-catch-finally block or even
 * the operation of closing the kit.
 * 
 * @author Dilly King
 * 
 */
public abstract class SQLProcess extends AbstractAccomplishable implements Runnable
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		DataBase db = new MySQL("test", "root", "root");

		SQLProcess processor = new SQLProcess() {

			@Override
			protected void process(SQLKit kit) throws SQLException
			{
				ResultSet rs = kit.query("SELECT * FROM `user` WHERE id>?", 3);
				while (rs.next()) {
					Tools.debug(rs.getString(1) + "\t" + rs.getString(2));
				}
			}
		};

		processor.setDataBase(db);
		processor.process();
	}

	private DataBase	dataBase;

	private boolean		processing;

	@Override
	public ActionEvent getAccomplishedEvent()
	{
		return null;
	}

	public DataBase getDataBase()
	{
		return dataBase;
	}

	public boolean isProcessing()
	{
		return processing;
	}

	public void process()
	{
		SQLKit kit = dataBase.getSQLKit();
		try {
			this.processing = true;
			this.process(kit);
			this.processing = false;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			kit.close();
		}
	}

	protected abstract void process(SQLKit kit) throws SQLException;

	public void run()
	{
		this.resetAccomplishStatus();
		this.process();
		this.accomplished();
	}

	public SQLProcess setDataBase(DataBase dataBase)
	{
		this.dataBase = dataBase;
		return this;
	}
}
