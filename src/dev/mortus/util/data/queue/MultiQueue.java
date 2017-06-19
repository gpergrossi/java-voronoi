package dev.mortus.util.data.queue;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

public class MultiQueue<T extends Comparable<T>> extends AbstractQueue<T> {
	
	int modifyCount;
	List<Queue<T>> queues;
	
	public MultiQueue() {
		this.queues = new ArrayList<>();
	}
	
	@Override
	public boolean offer(T e) {
		throw new UnsupportedOperationException("You cannot offer items to a MultiQueue, instead offer them to the underlying queues.");
	}

	public void addQueue(Queue<T> queue) {
		this.queues.add(queue);
		modifyCount++;
	}
	
	public void removeQueue(Queue<T> queue) {
		this.queues.remove(queue);
		modifyCount++;
	}
	
	private Queue<T> getHighestPriorityQueue() {
		Queue<T> winner = null;
		T winningElement = null;
		for (Queue<T> queue : queues) {
			T element = queue.peek();
			if (element == null) continue;
			if (winningElement == null || winningElement.compareTo(element) > 0) {
				winningElement = element;
				winner = queue;
			}
		}
		return winner;
	}
	
	@Override
	public T poll() {		
		Queue<T> highest = getHighestPriorityQueue();
		if (highest == null) return null;
		return highest.poll();
	}

	@Override
	public T peek() {
		Queue<T> highest = getHighestPriorityQueue();
		if (highest == null) return null;
		return highest.peek();
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			{ init(); }

			int expectedModifyCount;
			int numQueues;
			Object[] iterators;
			Object[] nextElements;
			int numItems;
			
			private void init() {
				expectedModifyCount = modifyCount;
				numQueues = queues.size();
				iterators = new Object[numQueues];
				nextElements = new Object[numQueues];
				int index = 0;
				for (Queue<T> q : queues) {
					numItems += q.size();
					Iterator<T> iter = q.iterator();
					iterators[index] = iter;
					if (iter.hasNext()) nextElements[index] = iter.next();
					index++;
				}
			}
			
			@Override
			public boolean hasNext() {
				if (modifyCount != expectedModifyCount) throw new ConcurrentModificationException();
				return numItems > 0;
			}

			@Override
			@SuppressWarnings("unchecked")
			public T next() {
				if (modifyCount != expectedModifyCount) throw new ConcurrentModificationException();
				
				int winner = 0;
				T winningElement = (T) nextElements[0];
				for (int i = 1; i < numQueues; i++) {
					if (nextElements[i] == null) continue;
					T element = (T) nextElements[i];
					if (winningElement == null || element.compareTo(winningElement) < 0) {
						winningElement = element;
						winner = i;
					}
				}
				
				T ret = (T) nextElements[winner];
				Iterator<T> iter = (Iterator<T>) iterators[winner];
				if (iter.hasNext()) nextElements[winner] = iter.next();
				else nextElements[winner] = null;
				numItems--;
				
				return ret;
			}
		};
	}

	@Override
	public int size() {
		int size = 0;
		for (Queue<T> queue : queues) {
			size += queue.size();
		}
		return size;
	}

}
