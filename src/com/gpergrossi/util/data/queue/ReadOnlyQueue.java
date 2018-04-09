package com.gpergrossi.util.data.queue;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A queue wrapper for an iterator. The queue will always have one element until
 * the iterator has been emptied. Only peek() and poll() are supported.
 *
 * @param <T>
 */
public class ReadOnlyQueue<T> extends AbstractQueue<T> {

	private int nextIndex;
	private final List<T> items;
	
	public ReadOnlyQueue(Collection<T> collection) {
		this.nextIndex = 0;
		this.items = new ArrayList<>(collection);
	}

	@Override
	public boolean offer(T e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T poll() {
		if (nextIndex >= items.size()) return null;
		return items.get(nextIndex++);
	}

	@Override
	public T peek() {
		if (nextIndex >= items.size()) return null;
		return items.get(nextIndex);
	}

	@Override
	public Iterator<T> iterator() {
		return items.listIterator(nextIndex);
	}

	@Override
	public int size() {
		return items.size() - nextIndex;
	}

}
