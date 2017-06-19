package dev.mortus.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import dev.mortus.util.data.DoublyLinkedList;

/*
 * All tests and code very closely based on the Apache LinkedListTest
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class LinkedListTest {
	
	public static void main(String[] args) {
		basicTest();
		sortTest();
		simpleConcurrentModificationTest();
		advancedConcurrentModificationTest();
		reverseTest();
		System.out.println("All tests passed");
	}
	
    public static void basicTest() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        list.add("B");
        list.add("C");
        list.add("D");
        list.add(0, "A");

        assertEquals(list.indexOf("A"), 0);

        String removed = list.remove(0);
        assertEquals(removed, "A");
        assertEquals(list.get(0), "B");
        assertEquals(list.size(), 3);
        assertEquals(list.indexOf("C"), 1);

     	removed = list.remove(1);
        assertEquals(removed, "C");

        list.add(1, "E");
        assertEquals(list.size(), 3);
        assertEquals(list.get(1), "E");

        list.set(1, "F");
        assertEquals(list.get(1), "F");

        list.add(0, "G");
        assertTrue(list.equals(makeList("G", "B", "F", "D")));
        assertFalse(list.equals(makeList("G", "B", "F", "D", "E")));
        assertFalse(list.equals(makeList("B", "F", "E")));

        assertEquals(4, list.size());

        List<String> copy = makeList("G", "B", "F", "D");
        int i = 0;
        for (String item : list) {
            assertEquals(item, copy.get(i++));
        }

        int j = 0;
        ListIterator<String> iterator = list.listIterator();
        while (j < list.size()) {
            iterator.next();
            j++;
        }

        while (iterator.hasPrevious()) {
            String s = iterator.previous();
            assertEquals(s, copy.get(--j));
        }

        iterator = list.listIterator();
        assertEquals(iterator.next(), "G");
        assertEquals(iterator.next(), "B");
        assertEquals(iterator.previous(), "B");
        assertEquals(iterator.previous(), "G");
        assertEquals(iterator.next(), "G");

        iterator = list.listIterator();
        iterator.add("M");
        assertEquals(list, makeList("M", "G", "B", "F", "D"));

        assertEquals(iterator.next(), "G");
        iterator.add("N");
        assertEquals(list, makeList("M", "G", "N", "B", "F", "D"));

        iterator = list.listIterator();
        while (iterator.hasNext()) iterator.next();
        assertEquals(iterator.previous(), "D");

        while (iterator.hasPrevious()) iterator.previous();
        assertEquals(iterator.next(), "M");
    }

    public static void sortTest() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();

        list.add("N");
        list.add("P");
        list.add("D");
        list.add("A");
        list.add("Z");

        Collections.sort(list);
        
        assertEquals(list, makeList("A", "D", "N", "P", "Z"));
    }

    public static void simpleConcurrentModificationTest() {
        DoublyLinkedList<String> list = makeLinkedList("a", "b", "c", "d", "e");
        ListIterator<String> iterator = list.listIterator();
        list.remove(0);

        try {
        	iterator.next();
        	throw new RuntimeException("Assertion failed, expected ConcurrentModificationException");
        } catch (ConcurrentModificationException e) {}
    }

    public static void advancedConcurrentModificationTest() {
    	DoublyLinkedList<String> list = makeLinkedList("a", "b", "c", "d", "e");

        ListIterator<String> iterator1 = list.listIterator();
        ListIterator<String> iterator2 = list.listIterator();

        iterator1.next();
        iterator2.next();

        iterator1.add("a1");
        
        try {
	        iterator2.next();
	    	throw new RuntimeException("Assertion failed, expected ConcurrentModificationException");
	    } catch (ConcurrentModificationException e) {}
    }	
    
    private static DoublyLinkedList<String> makeLinkedList(String... vals) {
    	DoublyLinkedList<String> list = new DoublyLinkedList<>();
    	for (String s : vals) {
    		list.add(s);
    	}
		return list;
	}
    
	private static List<String> makeList(String... vals) {
    	List<String> list = new ArrayList<>(vals.length);
    	for (String s : vals) {
    		list.add(s);
    	}
		return list;
	}

    private static void assertFalse(boolean cond) {
		if (cond) throw new RuntimeException("Assertion failed");
	}
    
    private static void assertTrue(boolean cond) {
		if (!cond) throw new RuntimeException("Assertion failed");
	}
    
	private static void assertEquals(Object i, Object j) {
		if (!i.equals(j)) throw new RuntimeException("Assertion failed, expected "+j+" got "+i);
	}

	private static void assertEquals(int i, int j) {
		if (i != j) throw new RuntimeException("Assertion failed, expected "+j+" got "+i);
	}
	
    public static void reverseTest() {
        DoublyLinkedList<String> list = makeLinkedList("a", "b", "c", "d", "e");

        assertEquals(list.pollLast(), "e");
        assertEquals(list.pollLast(), "d");
        assertEquals(list.pollLast(), "c");
        assertEquals(list.pollLast(), "b");
        assertEquals(list.pollLast(), "a");
        
        list.push("a");
        list.push("b");
        list.push("c");
        
        assertEquals(list.pop(), "c");
        assertEquals(list.pop(), "b");
        assertEquals(list.pop(), "a");
        
        list.addAll(makeLinkedList("a", "b", "c", "b", "d", "b", "e"));
        list.removeFirstOccurrence("b");
        list.removeLastOccurrence("b");
        
        assertEquals(list, makeList("a", "c", "b", "d", "e"));
        
        Iterator<String> backwards = list.descendingIterator();
        assertEquals(backwards.next(), "e");
        assertEquals(backwards.next(), "d");
        assertEquals(backwards.next(), "b");
        assertEquals(backwards.next(), "c");
        assertEquals(backwards.next(), "a");
        
    }
	
}