package org.kernelab.basis;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class LinkedQueue<E> extends AbstractQueue<E> implements List<E>, Cloneable,
		Copieable<LinkedQueue<E>>, Serializable
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 2430128485625817828L;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private List<E>	queue;

	public LinkedQueue()
	{
		super();
		queue = new LinkedList<E>();
	}

	public LinkedQueue(Collection<E> collection)
	{
		super();
		queue = new LinkedList<E>(collection);
	}

	protected LinkedQueue(LinkedQueue<E> queue)
	{
		super();
		this.queue = new LinkedList<E>(queue.queue);
	}

	/**
	 * Appends the specified element to the end of this queue.
	 * 
	 * <p>
	 * This method is equivalent to {@link #addLast}.
	 * 
	 * @param e
	 *            element to be appended to this queue
	 * @return <tt>true</tt> (as specified by {@link Collection#add})
	 */
	public boolean add(E element)
	{
		return queue.add(element);
	}

	/**
	 * Inserts the specified element at the specified position in this queue
	 * (optional operation). Shifts the element currently at that position (if
	 * any) and any subsequent elements to the right (adds one to their
	 * indices).
	 * 
	 * @param index
	 *            index at which the specified element is to be inserted
	 * @param element
	 *            element to be inserted
	 * @throws UnsupportedOperationException
	 *             if the <tt>add</tt> operation is not supported by this queue
	 * @throws ClassCastException
	 *             if the class of the specified element prevents it from being
	 *             added to this queue
	 * @throws NullPointerException
	 *             if the specified element is null and this queue does not
	 *             permit null elements
	 * @throws IllegalArgumentException
	 *             if some property of the specified element prevents it from
	 *             being added to this list
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range (
	 *             <tt>index &lt; 0 || index &gt; size()</tt>)
	 */
	public void add(int index, E element)
	{
		queue.add(index, element);
	}

	/**
	 * @Inheritdoc
	 */
	public boolean addAll(Collection<? extends E> collection)
	{
		return queue.addAll(collection);
	}

	public boolean addAll(int index, Collection<? extends E> c)
	{
		return queue.addAll(index, c);
	}

	public void addFirst(E element)
	{
		this.add(0, element);
	}

	public void addLast(E element)
	{
		this.add(queue.size(), element);
	}

	@Override
	public void clear()
	{
		queue.clear();
	}

	public LinkedQueue<E> clone()
	{
		return new LinkedQueue<E>(this);
	}

	public E get(int index)
	{
		return queue.get(index);
	}

	public E getFirst()
	{
		return queue.get(0);
	}

	public E getLast()
	{
		return queue.get(queue.size() - 1);
	}

	public int indexOf(Object o)
	{
		return queue.indexOf(o);
	}

	public void insert(E element, int index)
	{
		queue.add(index, element);
	}

	@Override
	public boolean isEmpty()
	{
		return queue.isEmpty();
	}

	public Iterator<E> iterator()
	{
		return queue.iterator();
	}

	public int lastIndexOf(Object o)
	{
		return queue.lastIndexOf(o);
	}

	public ListIterator<E> listIterator()
	{
		return queue.listIterator();
	}

	public ListIterator<E> listIterator(int index)
	{
		return queue.listIterator(index);
	}

	/**
	 * Inserts the specified element into this queue if it is possible to do so
	 * immediately without violating capacity restrictions. When using a
	 * capacity-restricted queue, this method is generally preferable to
	 * {@link #add}, which can fail to insert an element only by throwing an
	 * exception.
	 * 
	 * @param e
	 *            the element to add
	 * @return <tt>true</tt> if the element was added to this queue, else
	 *         <tt>false</tt>
	 * @throws ClassCastException
	 *             if the class of the specified element prevents it from being
	 *             added to this queue
	 * @throws NullPointerException
	 *             if the specified element is null and this queue does not
	 *             permit null elements
	 * @throws IllegalArgumentException
	 *             if some property of this element prevents it from being added
	 *             to this queue
	 */
	public boolean offer(E element)
	{
		return this.add(element);
	}

	/**
	 * Retrieves, but does not remove, the head of this queue, or returns
	 * <tt>null</tt> if this queue is empty.
	 * 
	 * @return the head of this queue, or <tt>null</tt> if this queue is empty
	 */
	public E peek()
	{
		return this.getFirst();
	}

	/**
	 * Retrieves, but does not remove, the last of this queue, or returns
	 * <tt>null</tt> if this queue is empty.
	 * 
	 * @return the last of this queue, or <tt>null</tt> if this queue is empty
	 */
	public E poke()
	{
		return this.getLast();
	}

	/**
	 * Retrieves and removes the head of this queue, or returns <tt>null</tt> if
	 * this queue is empty.
	 * 
	 * @return the head of this queue, or <tt>null</tt> if this queue is empty.
	 */
	public E poll()
	{
		return this.isEmpty() ? null : this.removeFirst();
	}

	/**
	 * Retrieves and removes the last of this queue, or returns <tt>null</tt> if
	 * this queue is empty.
	 * 
	 * @return the last of this queue, or <tt>null</tt> if this queue is empty.
	 */
	public E pop()
	{
		return this.isEmpty() ? null : this.removeLast();
	}

	/**
	 * Pushes an element onto the stack represented by this list. In other
	 * words, inserts the element at the end of this list.
	 * 
	 * <p>
	 * This method is equivalent to {@link #addLast}.
	 * 
	 * @param e
	 *            the element to push
	 */
	public void push(E element)
	{
		this.addLast(element);
	}

	/**
	 * Retrieves and removes the head (first element) of this queue.
	 * 
	 * @return the head of this queue
	 * @throws NoSuchElementException
	 *             if this queue is empty
	 */
	public E remove()
	{
		if (this.isEmpty()) {
			throw new NoSuchElementException();
		}

		return this.removeFirst();
	}

	/**
	 * Removes the element at the specified position in this queue (optional
	 * operation). Shifts any subsequent elements to the left (subtracts one
	 * from their indices). Returns the element that was removed from the queue.
	 * 
	 * @param index
	 *            the index of the element to be removed
	 * @return the element previously at the specified position
	 * @throws UnsupportedOperationException
	 *             if the <tt>remove</tt> operation is not supported by this
	 *             queue
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range (
	 *             <tt>index &lt; 0 || index &gt;= size()</tt>)
	 */
	public E remove(int index)
	{
		return queue.remove(index);
	}

	/**
	 * Retrieves and removes the head of this queue.
	 * 
	 * @return the head of this queue.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range (
	 *             <tt>index &lt; 0 || index &gt;= size()</tt>)
	 */
	public E removeFirst()
	{
		return queue.remove(0);
	}

	/**
	 * Retrieves and removes the last of this queue.
	 * 
	 * @return the last of this queue.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range (
	 *             <tt>index &lt; 0 || index &gt;= size()</tt>)
	 */
	public E removeLast()
	{
		return queue.remove(queue.size() - 1);
	}

	public LinkedQueue<E> reverse()
	{
		LinkedQueue<E> reverse = new LinkedQueue<E>();
		for (int j = size() - 1; j >= 0; j--) {
			reverse.add(this.get(j));
		}
		return reverse;
	}

	public E set(int index, E element)
	{
		return queue.set(index, element);
	}

	public int size()
	{
		return queue.size();
	}

	public LinkedQueue<E> sub(int fromIndex, int toIndex)
	{
		return new LinkedQueue<E>(queue.subList(fromIndex, toIndex));
	}

	public List<E> subList(int fromIndex, int toIndex)
	{
		return queue.subList(fromIndex, toIndex);
	}

}
