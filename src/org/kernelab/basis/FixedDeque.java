package org.kernelab.basis;

import java.util.Collection;
import java.util.LinkedList;

public class FixedDeque<E> extends LinkedList<E>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1767881916070322588L;

	public static void main(String[] args)
	{
		FixedDeque<Integer> q = new FixedDeque<Integer>(-3);

		q.add(1);
		q.add(2);

		for (int i = 0; i < 10; i++)
		{
			q.add(1, i);
		}

		Tools.debug(q);
	}

	private int fixed;

	public FixedDeque(int fixed)
	{
		this.setFixed(fixed);
	}

	@Override
	public boolean add(E e)
	{
		return offer(e);
	}

	@Override
	public void add(int index, E element)
	{
		super.add(index, element);
		this.cleanExtra(this.getFixed());
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c)
	{
		boolean res = super.addAll(index, c);
		this.cleanExtra(this.getFixed());
		return res;
	}

	@Override
	public void addFirst(E e)
	{
		int fixed = this.getFixed();

		if (fixed < 0)
		{ // fixed head
			if (this.size() >= -fixed)
			{
				this.pollLast();
			}
			super.addFirst(e);
		}
		else if (fixed > 0)
		{ // fixed tail
			if (this.size() >= fixed)
			{
				return;
			}
			else
			{
				super.addFirst(e);
			}
		}
		else
		{
			super.addFirst(e);
		}
	}

	@Override
	public void addLast(E e)
	{
		int fixed = this.getFixed();

		if (fixed < 0)
		{ // fixed head
			if (this.size() >= -fixed)
			{
				return;
			}
			else
			{
				super.addLast(e);
			}
		}
		else if (fixed > 0)
		{ // fixed tail
			if (this.size() >= fixed)
			{
				this.poll();
			}
			super.addLast(e);
		}
		else
		{
			super.addLast(e);
		}
	}

	protected void cleanExtra(int fixed)
	{
		if (fixed != 0)
		{
			int extra = this.size() - Math.abs(fixed);

			if (extra > 0)
			{
				if (fixed < 0)
				{ // head fixed so that clean tails
					for (int i = 0; i < extra; i++)
					{
						this.pollLast();
					}
				}
				else
				{ // tail fixed so that clean heads
					for (int i = 0; i < extra; i++)
					{
						this.pollFirst();
					}
				}
			}
		}
	}

	public int getFixed()
	{
		return fixed;
	}

	@Override
	public boolean offer(E e)
	{
		return this.offerLast(e);
	}

	@Override
	public boolean offerFirst(E e)
	{
		this.addFirst(e);
		return true;
	}

	@Override
	public boolean offerLast(E e)
	{
		this.addLast(e);
		return true;
	}

	@Override
	public E pollFirst()
	{
		if (this.isEmpty())
		{
			return null;
		}
		else
		{
			return this.removeFirst();
		}
	}

	@Override
	public E pollLast()
	{
		if (this.isEmpty())
		{
			return null;
		}
		else
		{
			return this.removeLast();
		}
	}

	@Override
	public void push(E e)
	{
		this.addFirst(e);
	}

	/**
	 * Set the fixed length of this queue.<br />
	 * Any positive length means tail fixed.<br />
	 * Any negative length means head fixed.<br />
	 * Zero length means DO NOT fix the length.<br />
	 * Any extra element would be cleaned if need.
	 * 
	 * @param fixed
	 *            fixed length.
	 * @return This queue itself.
	 */
	public <T extends FixedDeque<E>> T setFixed(int fixed)
	{
		if (fixed != 0 && (this.fixed == 0 || Math.abs(fixed) < Math.abs(this.fixed)))
		{ // clean extra elements
			this.cleanExtra(fixed);
		}

		this.fixed = fixed;

		return Tools.cast(this);
	}
}
