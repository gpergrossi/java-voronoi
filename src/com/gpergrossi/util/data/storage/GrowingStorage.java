package com.gpergrossi.util.data.storage;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

public class GrowingStorage<T extends StorageItem> implements Storage<T> {

	IntFunction<T[]> arrayAllocator;

	private int size;
	private int modifyCount;
	private int lastStorageIndex;
	private int lastStorageCapacity;
	
	private LinkedList<FixedSizeStorage<T>> storages;
	
	public GrowingStorage(IntFunction<T[]> arrayAllocator, int initialCapacity) {
		this.storages = new LinkedList<>();
		this.arrayAllocator = arrayAllocator;
		grow(initialCapacity);
	}

	/**
	 * Allocate more space in a new FixedSizeStorage and add that new storage to the list
	 * of backing storages used by this GrowingStorage
	 */
	private FixedSizeStorage<T> grow(int capacity) {
		lastStorageIndex += lastStorageCapacity;
		FixedSizeStorage<T> store = new FixedSizeStorage<>(this, capacity, lastStorageIndex);
		storages.addLast(store);
		lastStorageCapacity = capacity;
		return store;
	}
	
	/**
	 * Returns the FixedSizeStorage for object o only if it actually belongs to one of the
	 * FixedSizeStorages belong to this GrowingStorage. If a FixedSizeStorage containing o
	 * does not exist, null is returned.
	 */
	private FixedSizeStorage<T> lookupStorage(T o) {
		if (o == null) return null;
		Integer index = o.getStorageIndex(this);
		if (index == null) return null;
		if (index < 0 || index >= lastStorageIndex+lastStorageCapacity) return null;
		Iterator<FixedSizeStorage<T>> storageIterator = storages.descendingIterator();

		// Iteration over the available storages is log(n) because each storage is twice as big as the previous
		// Additionally, we search from the largest (and most recent) storage first to increase hit chance.
		while (storageIterator.hasNext()) {
			FixedSizeStorage<T> storage = storageIterator.next();
			if (storage.indexOffset <= index) {
				if (storage.contains(o)) return storage;
				return null;
			}
		}
		return null;
	}
	
	public Integer indexOf(T o) {
		FixedSizeStorage<T> storage = lookupStorage(o);
		if (storage == null) return null;
		return storage.indexOf(o);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public int capacity() {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isEmpty() {
		return (size == 0);
	}
	
	@Override
	public boolean isFull() {
		return false;
	}

	@Override
	public boolean add(T o) {
		if (o == null) return false;
		if (this.contains(o)) return false;
		
		FixedSizeStorage<T> store = storages.getLast();
		if (store.isFull()) store = grow(lastStorageCapacity * 2);
		
		boolean success = store.add(o);
		if (!success) throw new RuntimeException("Error: internal storage failed to add object");
		
		size++;
		modifyCount++;
		
		return true;
	}

	@Override
	public boolean remove(T o) {
		FixedSizeStorage<T> store = lookupStorage(o);
		if (store == null) return false;

		boolean success = store.remove(o);
		if (!success) return false;
		o.clearStorageIndex(this);
		
		// Free the storage if possible
		if (store.isEmpty() && storages.size() > 1) storages.remove(store);
		
		size--;
		if (size == 0) this.clear();
		else modifyCount++;
		
		return true;
	}

	@Override
	public boolean replace(T remove, T add) {
		FixedSizeStorage<T> removeStore = lookupStorage(remove);
		if (removeStore == null) return false; // Storage does not contain item to be removed

		FixedSizeStorage<T> addStore = lookupStorage(remove);
		if (addStore != null && addStore != removeStore) return false; // Storage contains added item elsewhere
		
		boolean success = removeStore.replace(remove, add);
		if (!success) return false;
		
		modifyCount++;
		
		return true;
	}
	
	@Override
	public boolean contains(T o) {
		FixedSizeStorage<T> store = lookupStorage(o);
		if (store == null) return false;
		return store.contains(o);
	}
	
	public void clear() {		
		// Clear storages but keep largest FixedSizeStorage object
		FixedSizeStorage<T> largest = storages.getLast();
		storages.clear();
		
		// Re-add largest store object to storages, set index offset to 0
		largest.clear(0);
		storages.addLast(largest);
		
		// Update capacity, size, and modifyCount
		lastStorageCapacity = largest.capacity();
		lastStorageIndex = 0;
		size = 0;
		modifyCount++;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			int expectedModifyCount = modifyCount;
			Iterator<FixedSizeStorage<T>> storageIterator = storages.iterator();
			Iterator<T> itemIterator = null;
			T lastSeen;
			boolean wasListCleared = false;
			
			@Override
			public boolean hasNext() {
				if (modifyCount != expectedModifyCount) throw new ConcurrentModificationException();
				if (itemIterator != null && itemIterator.hasNext()) return true;
				
				// The list was cleared because it was empty, return false now and avoid 
				// a concurrent modification exception on storageIterator
				if (wasListCleared) return false; 
				
				while (storageIterator.hasNext()) { 
					itemIterator = storageIterator.next().iterator();
					if (itemIterator.hasNext()) return true;
				}
				return false;
			}

			@Override
			public T next() {
				if (hasNext()) {
					lastSeen = itemIterator.next();
					return lastSeen;
				}
				throw new NoSuchElementException();
			}
			
			@Override
			public void remove() {
				if (lastSeen == null) throw new IllegalStateException();
				itemIterator.remove();
				
				size--;
				if (size == 0) {
					clear();
					wasListCleared = true;
				} else {
					modifyCount++;
				}
				
				expectedModifyCount++;
				lastSeen = null;
			}
		};
	}
	
	@Override
	public T[] toArray(T[] array) {
		if (array == null || array.length != size()) array = arrayAllocator.apply(size());
		Iterator<T> iter = iterator();
		int i = 0;
		while (iter.hasNext()) { 
			array[i++] = iter.next();
		}
		return array;
	}

	@Override
	public T[] toArray() {
		return toArray(null);
	}
	
}
