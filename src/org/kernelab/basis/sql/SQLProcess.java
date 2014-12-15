package org.kernelab.basis.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

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
public abstract class SQLProcess extends AbstractAccomplishable<SQLProcess> implements Runnable, Callable<SQLProcess>
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
				while (rs.next())
				{
					Tools.debug(rs.getString(1) + "\t" + rs.getString(2));
				}
			}
		};

		processor.setDataBase(db);
		try
		{
			processor.process();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	private DataBase	dataBase;

	private SQLKit		kit;

	private boolean		processing;

	public SQLProcess call() throws Exception
	{
		this.resetAccomplishStatus();
		this.process();
		this.accomplished();
		return this;
	}

	protected SQLProcess getAccomplishableSubject()
	{
		return this;
	}

	public DataBase getDataBase()
	{
		return dataBase;
	}

	public SQLKit getKit()
	{
		return kit;
	}

	public boolean isProcessing()
	{
		return processing;
	}

	public void process() throws SQLException
	{
		kit = dataBase.getSQLKit();
		if (kit != null)
		{
			try
			{
				this.processing = true;
				this.process(kit);
				this.processing = false;
			}
			finally
			{
				kit.close();
			}
		}
	}

	protected abstract void process(SQLKit kit) throws SQLException;

	public void run()
	{
		try
		{
			this.call();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public SQLProcess setDataBase(DataBase dataBase)
	{
		this.dataBase = dataBase;
		return this;
	}
}
