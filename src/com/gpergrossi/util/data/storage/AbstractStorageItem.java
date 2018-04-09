package com.gpergrossi.util.data.storage;

import java.util.HashMap;
import java.util.Map;

/**
 * An abstract implementation of the necessary methods of the StorageItem interface.
 * 
 * @author Gregary Pergrossi
 */
public class AbstractStorageItem implements StorageItem {

	Map<Storage<?>, Integer> indices;
	
	protected AbstractStorageItem() {
		indices = new HashMap<>(2);
	}
	
	@Override
	public void setStorageIndex(Storage<?> storage, int index) {
		indices.put(storage, index);
	}

	@Override
	public Integer getStorageIndex(Storage<?> storage) {
		return indices.get(storage);
	}

	@Override
	public void clearStorageIndex(Storage<?> storage) {
		indices.remove(storage);
	}

}
