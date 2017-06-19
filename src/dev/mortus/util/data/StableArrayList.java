package dev.mortus.util.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * A list implementation based on an array. Its primary feature is that
 * removal of elements does not cause an update in the position of any
 * other element.
 */
public class StableArrayList<T> implements List<T> {

	protected int initialCapacity;
	protected int capacity;
	protected int lastIndexUsed;
	protected T[] elements;
	private IntFunction<T[]> arrayAllocator;
	
	protected Queue<Integer> removedIndices;
	
	public StableArrayList(IntFunction<T[]> arrayAllocator, int initialCapacity) {
		this.initialCapacity = initialCapacity;
		this.arrayAllocator = arrayAllocator;
		
		this.capacity = initialCapacity;
		this.lastIndexUsed = -1;
		this.elements = null;
		
		this.removedIndices = new PriorityQueue<>(initialCapacity/3+1);
	}
	
	@Override
	public int size() {
		if (elements == null) return 0;
		return lastIndexUsed+1 - removedIndices.size();
	}
	
	public int getLastIndexUsed() {
		return lastIndexUsed;
	}
	
	public boolean isValid(int index) {
		return !removedIndices.contains(index);
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	
	@Override
	public int indexOf(Object o) {
		if (isEmpty()) return -1;
		
		if (o == null) {
			for (int i = 0; i <= lastIndexUsed; i++) {
				if (elements[i] == null && !removedIndices.contains(i)) return i;
			}
		} else {
			for (int i = 0; i <= lastIndexUsed; i++) {
				if (o.equals(elements[i]) && !removedIndices.contains(i)) return i;
			}
		}
		
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		if (isEmpty()) return -1;
		
		if (o == null) {
			for (int i = lastIndexUsed; i >= 0; i--) {
				if (elements[i] == null && !removedIndices.contains(i)) return i;
			}
		} else {
			for (int i = lastIndexUsed; i >= 0; i--) {
				if (o.equals(elements[i]) && !removedIndices.contains(i)) return i;
			}
		}
		
		return -1;
	}

	public T set(int index, T element, boolean grow) {
		if (index < 0) throw new IndexOutOfBoundsException();
		boolean usingFirstAvailable = (index == getFirstFreeIndex()); 
		
		int neededCap = capacity;
		while (index >= neededCap) neededCap = (neededCap + 1) * 2;
		
		// Grow?
		if (neededCap > capacity) {
			if (grow == false) throw new IndexOutOfBoundsException();
			if (elements != null) {
				T[] bigger = arrayAllocator.apply(neededCap);
				if (lastIndexUsed >= 0) {
					System.arraycopy(elements, 0, bigger, 0, lastIndexUsed+1);
				}
				elements = bigger;
				capacity = neededCap;
			}
		}
		
		// Create fresh?
		if (elements == null) {
			elements = arrayAllocator.apply(neededCap);
			capacity = neededCap;
		}
		
		T old = elements[index];
		elements[index] = element;

		if (!usingFirstAvailable) removedIndices.remove(index);
		if (index > lastIndexUsed) lastIndexUsed = index;
		
		return old;
	}
	
	@Override
	public T remove(int index) {
		if (!has(index)) throw new IndexOutOfBoundsException();
		
		T old = elements[index];
		elements[index] = null;
		
		removedIndices.add(index);
		if (index == lastIndexUsed) {
			int i = lastIndexUsed;
			while (i >= 0 && removedIndices.contains(i)) {
				removedIndices.remove(i);
				i--;
			}
			lastIndexUsed = i;
		}
		
		return old;
	}

	@Override
	public boolean contains(Object o) {
		return indexOf(o) != -1;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			int cursorIndex = getFirstIndex();
			
			public boolean hasNext() {
				if (cursorIndex > lastIndexUsed) return false;
				return true;
			}
			
			public T next() {
				T elem = elements[cursorIndex];
				cursorIndex = getNextIndex(cursorIndex);
				return elem;
			}
			
			public void remove() {
				StableArrayList.this.remove(cursorIndex);
			}
			
			public void forEachRemaining(Consumer<? super T> action) {
				while (hasNext()) action.accept(next());
			}
		};
	}

	@Override
	public Object[] toArray() {
		T[] out = arrayAllocator.apply(size());
		int i = 0;
		Iterator<T> it = iterator();
		while (it.hasNext()) {
			out[i++] = it.next();
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K> K[] toArray(K[] a) {
		if (a == null || a.length < size()) {
			a = Arrays.copyOf(a, size());
		}
		int i = 0;
		Iterator<T> it = iterator();
		while (it.hasNext()) {
			a[i++] = (K) it.next();
		}
		return a;
	}
	
	private int getFirstFreeIndex() {
		if (removedIndices.size() > 0) {
			return removedIndices.peek();
		}
		return lastIndexUsed+1;
	}
	
	@Override
	public boolean add(T e) {
		set(getFirstFreeIndex(), e, true);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		int i = indexOf(o);
		if (i != -1) remove(i);
		return (i != -1);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		Iterator<?> iter = c.iterator();
		while (iter.hasNext()) {
			if (!this.contains(iter.next())) return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(Collection<? extends T> c) {
		Iterator<?> iter = c.iterator();
		while (iter.hasNext()) {
			this.add((T) iter.next());
		}
		return c.size() > 0;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (elements == null) return false;
		boolean anyRemoved = false;
		for (int i = 0; i <= lastIndexUsed; i++) {
			if (c.contains(elements[i])) {
				remove(i);
				anyRemoved = true;
			}
		}
		return anyRemoved;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if (elements == null) return false;
		boolean anyRemoved = false;
		for (int i = 0; i <= lastIndexUsed; i++) {
			if (!c.contains(elements[i])) {
				remove(i);
				anyRemoved = true;
			}
		}
		return anyRemoved;
	}

	@Override
	public void clear() {
		this.removedIndices.clear();
		this.capacity = initialCapacity;
		this.lastIndexUsed = -1;
	}

	public void shrink() {
		if (lastIndexUsed < 0) {
			elements = null;
			return;
		}
		elements = Arrays.copyOf(elements, lastIndexUsed+1);
	}

	@Override
	public T get(int index) {
		if (elements == null) throw new IndexOutOfBoundsException();
		if (index < 0 || index > lastIndexUsed) throw new IndexOutOfBoundsException();
		if (removedIndices.contains(index)) throw new IndexOutOfBoundsException();
		
		return elements[index];
	}
	
	public int getFirstIndex() {
		return getNextIndex(-1);
	}

	public int getLastIndex() {
		return getPrevIndex(lastIndexUsed+1);
	}
	
	public int getNextIndex(int i) {
		do {
			i++;
		} while (i <= lastIndexUsed && removedIndices.contains(i));
		return i;
	}
	
	public int getPrevIndex(int i) {
		do {
			i--;
		} while (i >= 0 && removedIndices.contains(i));
		return i;
	}
	
	/**
	 * Like add, except the index at which the element was stored is returned
	 */
	public int put(T e) {
		int index = getFirstFreeIndex();
		set(index, e, true);
		return index;
	}
	
	public boolean has(int index) {
		if (index < 0) return false;
		if (index > lastIndexUsed) return false;
		return !removedIndices.contains(index);
	}
	
	@Override
	public T set(int index, T element) {
		return set(index, element, false);
	}

	
	

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void add(int index, T element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<T> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

}
