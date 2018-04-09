package com.gpergrossi.util.data.storage;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

/**
 * The most basic implementation of an array-backed Storage class,
 * used as a base for the more complicated Storage classes.
 */
public class FixedSizeStorage<T extends StorageItem> implements Storage<T> {

	/** A function that allocates arrays of the appropriate type for this storage */
	IntFunction<T[]> arrayAllocator;

	/** Actual item storage array */
	private T[] items;
	
	
	
	/** Number of items actually stored in the storage */
	private int size;
	
	/** A counter to keep track of modification, used in the iterator concurrent modification check */
	private int modifyCount;
	

	
	/** Storage in which StorageItems will believe they are stored, used for parents, default = this */
	Storage<T> storageKey;
	
	/** Index offset for external indices, (I.E. items[index-indexOffset] = item) */
	int indexOffset;
	
	
	
	/**
	 * Create a new fixed size storage
	 * @param arrayAllocator - function to allocate arrays of the appropriate type
	 * @param capacity - the capacity of this storage
	 */
	public FixedSizeStorage(IntFunction<T[]> arrayAllocator, int capacity) {
		this.arrayAllocator = arrayAllocator;
		this.items = arrayAllocator.apply(capacity);
		this.storageKey = this;
	}
	
	/**
	 * All items added to this storage will receive an index with respect the parent instead. 
	 * They will be unaware of their actual storage location in this FixedSizeStorage 
	 */
	FixedSizeStorage(GrowingStorage<T> parent, int capacity, int offset) {
		this.storageKey = parent;
		this.arrayAllocator = parent.arrayAllocator;
		this.items = arrayAllocator.apply(capacity);
		this.indexOffset = offset;
	}

	
	
	/** 
	 * PACKAGE PRIVATE : clears the list and allows reassigning the indexOffset value. This can 
	 * only be done when the list is empty, otherwise all items would need to be updated.
	 * @param newOffset - indexOffset value to use going forward
	 */
	void clear(int newOffset) {
		clear();
		indexOffset = newOffset;
	}

	/**
	 * PACKAGE PRIVATE : returns the (internal) storage index of the given item after verifying that it actually
	 * is in this storage. (internal index means items[index] will be correct, no offset is applied)
	 * @param o - object to look for
	 * @return the (internal) index or null
	 */
	Integer internalIndexOf(T o) {
		if (o == null) return null;
		Integer index = o.getStorageIndex(storageKey);
		if (index == null) return null;
		index -= indexOffset;
		if (index < 0 || index >= size || !o.equals(items[index])) return null;
		return index;
	}
	
	public Integer indexOf(T o) {
		Integer index = internalIndexOf(o);
		if (index == null) return null;
		return index + indexOffset;
	}
	
	
	
	@Override
	public int size() {
		return size;
	}

	@Override
	public int capacity() {
		return items.length;
	}

	@Override
	public boolean isEmpty() {
		return (size == 0);
	}

	@Override
	public boolean isFull() {
		return (size >= items.length);
	}

	@Override
	public boolean add(T o) {
		if (o == null || this.isFull()) return false;
		if (this.contains(o)) return false;
		
		items[size] = o;
		o.setStorageIndex(storageKey, size + indexOffset);
		
		size++;
		modifyCount++;
		
		return true;
	}
	
	@Override
	public boolean remove(T o) {
		Integer index = internalIndexOf(o);
		if (index == null) return false;
		
		T item = items[index];
		item.clearStorageIndex(storageKey);
		
		// Swap the last item in the array into the newly empty slot
		if (index < size-1) {
			T replace = items[size-1];	
			items[size-1] = null;
			items[index] = replace;
			replace.setStorageIndex(storageKey, index + indexOffset);
		}
		
		size--;
		modifyCount++;
		
		return true;
	}
	
	@Override
	public boolean replace(T remove, T add) {
		Integer indexRemove = internalIndexOf(remove);
		if (indexRemove == null) return false;
		
		Integer indexAdd = internalIndexOf(add);
		if (indexAdd == indexRemove) return false;
		if (indexAdd != null) return false;
		
		items[indexRemove].clearStorageIndex(storageKey);
		items[indexRemove] = add;
		add.setStorageIndex(storageKey, indexRemove);
		
		modifyCount++;
		
		return true;
	}
	
	@Override
	public boolean contains(T o) {
		return internalIndexOf(o) != null;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			int expectedModifyCount = modifyCount;
			int indexOn = 0;
			T lastSeen = null;
			
			@Override
			public boolean hasNext() {
				if (modifyCount != expectedModifyCount) throw new RuntimeException(""+modifyCount+" != "+expectedModifyCount);
				return indexOn < size;
			}

			@Override
			public T next() {
				if (modifyCount != expectedModifyCount) throw new ConcurrentModificationException();
				if (indexOn >= size) throw new NoSuchElementException();
				lastSeen = items[indexOn];
				indexOn++;
				return lastSeen;
			}
			
			@Override
			public void remove() {
				if (lastSeen == null) throw new IllegalStateException();
				FixedSizeStorage.this.remove(lastSeen);
				
				indexOn--;
				expectedModifyCount++;
				lastSeen = null;
			}
		};
	}
	
	@Override
	public T[] toArray(T[] array) {
		if (array == null || array.length < size()) array = arrayAllocator.apply(size());
		Iterator<T> iter = iterator();
		int i = 0;
		while (iter.hasNext()) { 
			array[i++] = iter.next();
		}
		if (i < array.length) array[i] = null; // Mark the end of the overly-large array
		return array;
	}

	@Override
	public T[] toArray() {
		return toArray(null);
	}

	@Override
	public void clear() {
		if (size == 0) return;
		size = 0;	
		modifyCount++;
	}
	
}
