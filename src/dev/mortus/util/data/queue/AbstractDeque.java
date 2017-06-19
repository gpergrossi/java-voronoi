package dev.mortus.util.data.queue;

import java.util.AbstractSequentialList;
import java.util.Deque;
import java.util.Iterator;
import java.util.ListIterator;

public abstract class AbstractDeque<T> extends AbstractSequentialList<T> implements Deque<T> {

	@Override
	public void addFirst(T e) {
		this.add(0, e);		
	}

	@Override
	public void addLast(T e) {
		this.add(e);
	}

	@Override
	public boolean offerFirst(T e) {
		try {
			addFirst(e);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public boolean offerLast(T e) {
		try {
			addLast(e);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public T removeFirst() {
		return this.remove(0);
	}

	@Override
	public T removeLast() {
		return this.remove(size()-1);
	}

	@Override
	public T pollFirst() {
		try {
			return removeFirst();
		} catch (Exception ex) {
			return null;
		}
	}

	@Override
	public T pollLast() {
		try {
			return removeLast();
		} catch (Exception ex) {
			return null;
		}
	}

	@Override
	public T getFirst() {
		return this.get(0);
	}

	@Override
	public T getLast() {
		return this.get(size()-1);
	}

	@Override
	public T peekFirst() {
		return this.get(0);
	}

	@Override
	public T peekLast() {
		return this.get(size()-1);
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		if (o == null) {
			ListIterator<T> iter = this.listIterator();
			while(iter.hasNext()) {
				T t = iter.next();
				if (t == null) {
					iter.remove();
					return true;
				}
			}
			return false;
		} else {
			ListIterator<T> iter = this.listIterator();
			while(iter.hasNext()) {
				T t = iter.next();
				if (o.equals(t)) {
					iter.remove();
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		if (o == null) {
			ListIterator<T> iter = this.listIterator(size());
			while(iter.hasPrevious()) {
				T t = iter.previous();
				if (t == null) {
					iter.remove();
					return true;
				}
			}
			return false;
		} else {
			ListIterator<T> iter = this.listIterator(size());
			while(iter.hasPrevious()) {
				T t = iter.previous();
				if (o.equals(t)) {
					iter.remove();
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public boolean offer(T e) {
		return offerLast(e);
	}

	@Override
	public T remove() {
		return removeFirst();
	}

	@Override
	public T poll() {
		return pollFirst();
	}

	@Override
	public T element() {
		return peekFirst();
	}

	@Override
	public T peek() {
		return peekFirst();
	}

	@Override
	public void push(T e) {
		offerFirst(e);
	}

	@Override
	public T pop() {
		return pollFirst();
	}

	@Override
	public Iterator<T> descendingIterator() {
		return new Iterator<T>() {
			ListIterator<T> iter = AbstractDeque.this.listIterator(size());
			
			@Override
			public boolean hasNext() {
				return iter.hasPrevious();
			}

			@Override
			public T next() {
				return iter.previous();
			}
		};
	}

}
