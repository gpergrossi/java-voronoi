package test.gpergrossi.util;

import java.util.Iterator;

import com.gpergrossi.util.data.storage.AbstractStorageItem;
import com.gpergrossi.util.data.storage.FixedSizeStorage;
import com.gpergrossi.util.data.storage.GrowingStorage;

public class StorageTest {

	public static class Item extends AbstractStorageItem {

		int value;
		
		public Item(int value) {
			super();
			this.value = value;
		}
		
	}
	
	public static void main(String[] args) {
		
		fixedSizeTest();
		growingTest();
		
		System.out.println("All tests passed");
		
	}
	
    private static void fixedSizeTest() {
		FixedSizeStorage<Item> storage = new FixedSizeStorage<>(t -> new Item[t], 5);
		
		Item a = new Item(1);
		Item b = new Item(2);
		Item c = new Item(3);
		Item d = new Item(4);
		Item e = new Item(5);
		Item f = new Item(6);
		
		// Test storage addition and max capacity
		assertTrue(storage.add(a));
		assertTrue(storage.add(b));
		assertTrue(storage.add(c));
		assertTrue(storage.add(d));
		assertTrue(storage.add(e));
		assertFalse(storage.add(f));
		
		// Test assignment of proper indices
		assertEquals(a.getStorageIndex(storage), 0);
		assertEquals(b.getStorageIndex(storage), 1);
		assertEquals(c.getStorageIndex(storage), 2);
		assertEquals(d.getStorageIndex(storage), 3);
		assertEquals(e.getStorageIndex(storage), 4);
		assertTrue(f.getStorageIndex(storage) == null);
		
		// Test capacity via remove and add
		storage.remove(c);
		storage.add(f);
		
		// Check indices after removal and addition
		assertEquals(a.getStorageIndex(storage), 0);
		assertEquals(b.getStorageIndex(storage), 1);
		assertTrue(c.getStorageIndex(storage) == null);
		assertEquals(d.getStorageIndex(storage), 3);
		assertEquals(e.getStorageIndex(storage), 2);
		assertEquals(f.getStorageIndex(storage), 4);		
		
		// Does clear work?
		storage.clear();
		assertEquals(storage.size(), 0);
		assertFalse(storage.contains(a));
		assertFalse(storage.contains(b));
		assertFalse(storage.contains(c));
		assertFalse(storage.contains(d));
		assertFalse(storage.contains(e));
		
		// Try to add the same element twice
		assertTrue(storage.add(a));
		assertFalse(storage.add(a));
		assertFalse(storage.add(a));
		assertFalse(storage.add(a));
		assertFalse(storage.add(a));
		assertFalse(storage.add(a));
		assertEquals(storage.size(), 1);
		
		// Refill array
		storage.clear();	
		assertTrue(storage.add(a));
		assertTrue(storage.add(b));
		assertTrue(storage.add(c));
		assertTrue(storage.add(d));
		assertTrue(storage.add(e));
		
		int expectedSize = 5;
		Iterator<Item> iter = storage.iterator();
		while (iter.hasNext()) {
			assertTrue(storage.size() == expectedSize);
			Item i = iter.next();
			assertTrue(i.getStorageIndex(storage) != null);
			iter.remove();
			assertTrue(i.getStorageIndex(storage) == null);
			expectedSize--;
		}
		
	}
    
	
    private static void growingTest() {
    	GrowingStorage<Item> storage = new GrowingStorage<>(t -> new Item[t], 1);
    	
    	final int N_ITEMS = 1000;
    	
    	Item[] items = new Item[N_ITEMS];
    	for (int i = 0; i < N_ITEMS; i++) {
    		Item item = new Item(i+1);
    		items[i] = item;
    	}

    	// Adding items to blank list makes size increase and uses consecutive indices for storage
    	for (int i = 0; i < N_ITEMS; i++) {
    		Item item = items[i];
    		assertTrue(storage.add(item));
        	assertEquals(item.getStorageIndex(storage), i);
        	assertEquals(storage.size(), i+1);
    	}
    	
    	// Remove items makes size decrease
    	for (int i = 0; i < N_ITEMS; i++) {
    		Item item = items[i];
    		assertTrue(storage.remove(item));
        	assertEquals(storage.size(), N_ITEMS-i-1);
    	}
    	
    	// Still empty, don't remove items
    	for (int i = 1; i < N_ITEMS; i *= 2) {
    		Item item = items[i];
    		assertFalse(storage.remove(item));
        	assertEquals(storage.size(), 0);
    	}
    	
    	storage.clear();
    	assertEquals(storage.size(), 0);
  
    	// Adding items to blank list makes size increase and uses consecutive indices for storage
    	for (int i = 0; i < N_ITEMS; i++) {
    		Item item = items[i];
    		assertTrue(storage.add(item));
    		assertEquals(item.getStorageIndex(storage), i);
        	assertEquals(storage.size(), i+1);
    	}
    	
    	// Cannot add items that are already in the Storage
    	for (int i = 0; i < N_ITEMS; i++) {
    		Item item = items[i];
    		assertFalse(storage.add(item));
        	assertEquals(storage.size(), N_ITEMS);
    	}
    	
    	// Remove items makes size decrease
    	for (int i = 0; i < N_ITEMS; i++) {
    		Item item = items[i];
    		assertTrue(storage.remove(item));
    		assertFalse(storage.contains(item));
        	assertEquals(storage.size(), N_ITEMS-i-1);
    	}
    	
    	// Adding items to a recently emptied list makes size increase and uses consecutive indices for storage
    	for (int i = 0; i < N_ITEMS; i++) {
    		Item item = items[i];
    		assertTrue(storage.add(item));
    		assertEquals(item.getStorageIndex(storage), i);
        	assertEquals(storage.size(), i+1);
    	}
    	
		int expectedSize = N_ITEMS;
		Iterator<Item> iter = storage.iterator();
		while (iter.hasNext()) {
			assertTrue(storage.size() == expectedSize);
			Item i = iter.next();
			assertTrue(i.getStorageIndex(storage) != null);
			iter.remove();
			assertTrue(i.getStorageIndex(storage) == null);
			expectedSize--;
		}
    	
 	}
    
	private static void assertFalse(boolean cond) {
		if (cond) throw new RuntimeException("Assertion failed");
	}
    
    private static void assertTrue(boolean cond) {
		if (!cond) throw new RuntimeException("Assertion failed");
	}

	private static void assertEquals(int i, int j) {
		if (i != j) throw new RuntimeException("Assertion failed, expected "+j+" got "+i);
	}
	
}
