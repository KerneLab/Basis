package org.kernelab.basis;

import java.util.AbstractQueue;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

public class FixedDeque<E> extends AbstractQueue<E> implements Deque<E>
{
	public static void main(String[] args)
	{
		FixedDeque<Integer> q = new FixedDeque<Integer>(0);

		for (int i = 0; i < 10; i++)
		{
			q.offerLast(i);
		}

		Tools.debug(q);

		Tools.debug("----");

		q.setFixed(3);

		Tools.debug(q);

		Tools.debug("----");

		q.setFixed(0);

		for (int i = 10; i < 15; i++)
		{
			q.offerLast(i);
		}

		Tools.debug(q);
	}

	private int			fixed;

	private Deque<E>	deque;

	public FixedDeque(int fixed)
	{
		this(fixed, new LinkedList<E>());
	}

	public FixedDeque(int fixed, Deque<E> deque)
	{
		this.setDeque(deque);
		this.setFixed(fixed);
	}

	@Override
	public boolean add(E e)
	{
		return offer(e);
	}

	public void addFirst(E e)
	{
		this.offerFirst(e);
	}

	public void addLast(E e)
	{
		this.offerLast(e);
	}

	public Iterator<E> descendingIterator()
	{
		return this.getDeque().descendingIterator();
	}

	@Override
	public E element()
	{
		return this.getDeque().element();
	}

	protected Deque<E> getDeque()
	{
		return deque;
	}

	public E getFirst()
	{
		return this.getDeque().getFirst();
	}

	public int getFixed()
	{
		return fixed;
	}

	public E getLast()
	{
		return this.getDeque().getLast();
	}

	@Override
	public Iterator<E> iterator()
	{
		return this.getDeque().iterator();
	}

	public boolean offer(E e)
	{
		return this.offerLast(e);
	}

	public boolean offerFirst(E e)
	{
		int fixed = this.getFixed();

		if (fixed < 0)
		{ // fixed head
			if (this.size() >= -fixed)
			{
				this.pollLast();
			}
			return this.getDeque().offerFirst(e);
		}
		else if (fixed > 0)
		{ // fixed tail
			if (this.size() >= fixed)
			{
				return false;
			}
			else
			{
				return this.getDeque().offerFirst(e);
			}
		}
		else
		{
			return this.getDeque().offerFirst(e);
		}
	}

	public boolean offerLast(E e)
	{
		int fixed = this.getFixed();

		if (fixed < 0)
		{ // fixed head
			if (this.size() >= -fixed)
			{
				return false;
			}
			else
			{
				return this.getDeque().offerLast(e);
			}
		}
		else if (fixed > 0)
		{ // fixed tail
			if (this.size() >= fixed)
			{
				this.poll();
			}
			return this.getDeque().offerLast(e);
		}
		else
		{
			return this.getDeque().offerLast(e);
		}
	}

	public E peek()
	{
		return this.getDeque().peek();
	}

	public E peekFirst()
	{
		return this.getDeque().peekFirst();
	}

	public E peekLast()
	{
		return this.getDeque().peekLast();
	}

	public E poll()
	{
		return this.getDeque().poll();
	}

	public E pollFirst()
	{
		return this.getDeque().pollFirst();
	}

	public E pollLast()
	{
		return this.getDeque().pollLast();
	}

	public E pop()
	{
		return this.getDeque().pop();
	}

	public void push(E e)
	{
		this.addFirst(e);
	}

	@Override
	public E remove()
	{
		return this.getDeque().remove();
	}

	public E removeFirst()
	{
		return this.getDeque().removeFirst();
	}

	public boolean removeFirstOccurrence(Object o)
	{
		return this.getDeque().removeFirstOccurrence(o);
	}

	public E removeLast()
	{
		return this.getDeque().removeLast();
	}

	public boolean removeLastOccurrence(Object o)
	{
		return this.getDeque().removeLastOccurrence(o);
	}

	protected void setDeque(Deque<E> deque)
	{
		this.deque = deque;
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
			int extra = this.size() - Math.abs(fixed);
			if (extra > 0)
			{
				if (fixed < 0)
				{ // head fixed so that clean tails
					for (int i = 0; i < extra; i++)
					{
						this.removeLast();
					}
				}
				else
				{ // tail fixed so that clean heads
					for (int i = 0; i < extra; i++)
					{
						this.removeFirst();
					}
				}
			}
		}

		this.fixed = fixed;

		return Tools.cast(this);
	}

	@Override
	public int size()
	{
		return this.getDeque().size();
	}
}
