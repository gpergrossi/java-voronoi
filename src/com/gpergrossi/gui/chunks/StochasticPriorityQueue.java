package com.gpergrossi.gui.chunks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;

/**
 * A loose priority queue calculates priority when an object is removed instead of when it is offered.
 * Instead of finding the absolute best on each poll(), some elements are selected at random and the highest
 * priority of the selected few is returned. This achieves constant time offering and polling while still behaving
 * similarly to a perfect priority queue in large applications.
 *
 * @param <T> the type of object stored by this dequeue
 */
public class StochasticPriorityQueue<T extends Object> extends ArrayList<T> implements Queue<T> {

	private static final long serialVersionUID = 6955654743124598514L;
	
	T currentNextBest;
	Comparator<T> comparator;
	Random random;
	T peeked;
	int searchIters = 4;
	
	public StochasticPriorityQueue(int initialCapacity) {
		this(initialCapacity, null);
	}
	
	public StochasticPriorityQueue(int initialCapacity, Comparator<T> comparator) {
		super(initialCapacity);
		this.comparator = comparator;
		this.random = new Random();
	}
	
	private int compare(T o1, T o2) {
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return 1;
		if (o2 == null) return -1;
		if (comparator != null) return comparator.compare(o1, o2);
		return 0;
	}

	public boolean offer(T item) {
		super.add(item);
		if (compare(item, currentNextBest) < 0) {
			currentNextBest = item;
		}
		return true;
	}

	public T remove() {
		T head = poll();
        if (head != null)
            return head;
        else
            throw new NoSuchElementException();
	}
	
	public T element() {
		T head = peek();
        if (head != null)
            return head;
        else
            throw new NoSuchElementException();
	}

	public T poll() {
		T head;
		if (peeked == null) head = peek();
		else head = peeked;
		peeked = null;
		
		if (head == currentNextBest) {
			currentNextBest = null;
		}
		remove(head);
		return head;
	}

	public T peek() {
		if (this.isEmpty()) return null;
		
		T localBest = randomElement();
		
		if (!localBest.equals(currentNextBest)) {
			if (compare(currentNextBest, localBest) < 0) {
				T swap = currentNextBest;
				currentNextBest = localBest;
				localBest = swap;
			}
		}

		for (int i = 0; i < searchIters; i++) {
			T element = randomElement();
			if (element.equals(localBest))continue;
			if (compare(element, localBest) < 0) {
				currentNextBest = localBest;
				localBest = element;
			}
		}
		
		peeked = localBest; // peek guarantees next poll returns peeked value
		return localBest;
	}

	private T randomElement() {
		int index = random.nextInt(this.size());
		return this.get(index);
	}
	
}
