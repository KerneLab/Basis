package org.kernelab.basis.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import org.kernelab.basis.AbstractPool;

public class SQLKitPool extends AbstractPool<SQLKit> implements ConnectionManager
{
	private ConnectionManager manager;

	public SQLKitPool(ConnectionManager manager, int limit)
	{
		this(manager, limit, 0);
	}

	public SQLKitPool(ConnectionManager manager, int limit, int init)
	{
		this(manager, new LinkedList<SQLKit>(), limit, init);
	}

	protected SQLKitPool(ConnectionManager manager, Queue<SQLKit> pool, int limit, int init)
	{
		super(pool, limit, init);
		this.setManager(manager);
	}

	@Override
	public void close()
	{
		synchronized (this.getQueue())
		{
			for (SQLKit kit : this.getElements())
			{
				kit.close();
			}
			super.close();
		}
	}

	@Override
	public void discard(SQLKit kit)
	{
		super.discard(kit);
		if (kit != null)
		{
			kit.close();
		}
	}

	public ConnectionManager getManager()
	{
		return manager;
	}

	@Override
	public boolean isValid(Connection conn)
	{
		return this.getManager().isValid(conn);
	}

	@Override
	protected boolean isValid(SQLKit kit)
	{
		return this.isValid(kit.getConnection());
	}

	@Override
	protected SQLKit newElement(long timeout) throws Exception
	{
		return new SQLKit(this, timeout);
	}

	@Override
	public Connection provideConnection(long timeout) throws SQLException
	{
		return this.getManager().provideConnection(timeout);
	}

	@Override
	public void recycle(SQLKit kit)
	{
		if (kit != null)
		{
			kit.cleanResultSets();
		}
		super.recycle(kit);
	}

	@Override
	public void recycleConnection(Connection c) throws SQLException
	{
		this.getManager().recycleConnection(c);
	}

	@Override
	protected void resetElement(SQLKit kit) throws SQLException
	{
		kit.rollback();
		kit.reset();
	}

	@Override
	protected SQLKitPool setInit(int init)
	{
		if (this.getManager() != null)
		{
			super.setInit(init);
		}
		return this;
	}

	protected SQLKitPool setManager(ConnectionManager manager)
	{
		this.manager = manager;
		return this;
	}
}
