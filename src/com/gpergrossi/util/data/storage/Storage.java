package com.gpergrossi.util.data.storage;

/**
 * All classes in the "storage" package are based on this interface and the StorageItem interface.
 * 
 * These storage classes place maximum priority on speed (rather than memory) and are designed to deal
 * with large numbers of dynamically changing elements. Items are stored as contiguously as possible 
 * without using memory copies. The hope is to increase cache performance when iterating (at least as 
 * far as the array of object references). I don't know much about the JVM's organization of object
 * arrays, but I hope that this makes at least some sense. 
 * 
 * Elements stored in these classes must implement the StorageItem interface so that they can be
 * assigned an index. These indices are used to provide constant time lookup and removal from the
 * data structures at the cost of the small amount of necessary memory.
 * 
 * @author Gregary Pergrossi
 */
public interface Storage<T extends StorageItem> extends Iterable<T> {

	/**
	 * Check how many items are currently in the Storage.
	 * @return
	 */
	public int size();
	
	/**
	 * Check the capacity of the Storage. Some storages will have no maximum limit,
	 * in which case Integer.MAX_VALUE is a suitable return value. Storage capacity
	 * is not a fixed value and may increase or decrease depending on the backing
	 * data structures. For example, removing items from an ordered storage may leave
	 * an empty bubble in the backing arrays because another value cannot easily be
	 * swapped into the empty space.
	 * @return how many items can be fit into the Storage at max
	 */
	public int capacity();
	
	/**
	 * Check if the Storage is completely empty
	 * @return true if no more items can be removed, false otherwise
	 */
	public boolean isEmpty();
	
	/**
	 * Check if the Storage is full
	 * @return true if no more items can be added, false otherwise
	 */
	public boolean isFull();

	/**
	 * Add an item to the storage. Note that because of the indexing system, the
	 * item to be added must be non-null and not already in this storage.
	 * @return true if the item was added, false if it failed due to capacity, duplicate entries, or a null item
	 */
	public boolean add(T o);
	
	/**
	 * Remove an item from this storage
	 * @return true if the item was removed, false if the item was not present or was null
	 */
	public boolean remove(T o);
	
	/**
	 * Swap a new item into the same place as an old item
	 * @return true if the swap was acceptable, false if the Storage contains a copy of the added item elsewhere or if either argument is null
	 */
	public boolean replace(T remove, T add);
	
	/**
	 * Check if this storage contains an item
	 * @return true if the storage contains the item, false otherwise
	 */
	public boolean contains(T o);

	/**
	 * Automatically allocates the correct size array and packs the items into it
	 */
	public T[] toArray();
	
	/**
	 * Places the items into the given array if it is large enough, otherwise a
	 * new array is created. Items are packed into the array. When the provided
	 * array is too large, the last element added will be a null, marking the 
	 * last array position that was updated by this method. 
	 */
	public T[] toArray(T[] array);
	
	/**
	 * Remove all items from the Storage
	 */
	public void clear();
		
}
