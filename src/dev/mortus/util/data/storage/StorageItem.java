package dev.mortus.util.data.storage;

/**
 * All classes in the "storage" package are based on this interface and the containers that 
 * implement the Storage interface.
 * 
 * These storage classes place maximum priority on speed and are design to deal with very large
 * numbers of elements. As such, items are stored as contiguously as possible (to increase cache
 * performance at least as far as the array of object references) without using memory copies.
 * 
 * Elements stored in these classes must implement the StorageItem interface so that they can be
 * assigned an index. These indices are used to provide constant time lookup and removal from the
 * data structures at the cost of the necessary memory. However the design goals for these data
 * structures were to provide speed as a highest priority.
 * 
 * The StorageItem interface is implemented by the AbstractStorageItem class for convenience.
 * 
 * @author Gregary Pergrossi
 */
public interface StorageItem {
	
	/**
	 * StorageItem objects should remember the index assigned to them by this method (with relation 
	 * to the Storage object provided). Future calls to getStorageIndex should correctly return the
	 * most recently assigned index. <br /><br />
	 * 
	 * Note that this can and should be implemented by a Map field in the class. In cases where your 
	 * items will only by placed in a single Storage, it may be acceptable to ignore the storage object
	 * argument and store the index in a single Integer for speed and memory conservation.
	 */
	public void setStorageIndex(Storage<?> storageKey, int index);
	
	/**
	 * StorageItem objects should be able to provide the most recent index assigned to them with 
	 * relation to the storage object provided. Null should be returned if no index has yet been
	 * assigned or if clearStorageIndex was more recently called than the last setStorageIndex.
	 * Otherwise the index assigned to the object via setStorageIndex should be returned. <br /><br />
	 * 
	 * Note that this can and should be implemented by a Map field in the class. In cases where your 
	 * items will only by placed in a single Storage, it may be acceptable to ignore the storage object
	 * argument and store the index in a single Integer for speed and memory conservation.
	 */ 
	public Integer getStorageIndex(Storage<?> storageKey);
	
	/**
	 *  When clearStorageIndex is called, the StorageItem should forget its storage index with relation
	 *  to the provided Storage object. Future calls to getStorageIndex should return null. <br /><br />
	 * 
	 * Note that this can and should be implemented by a Map field in the class. In cases where your 
	 * items will only by placed in a single Storage, it may be acceptable to ignore the storage object
	 * argument and store the index in a single Integer for speed and memory conservation.
	 */
	public void clearStorageIndex(Storage<?> storageKey);
	
}
