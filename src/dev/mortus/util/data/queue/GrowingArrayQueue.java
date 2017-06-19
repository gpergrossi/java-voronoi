package dev.mortus.util.data.queue;

import java.util.AbstractQueue;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

import dev.mortus.util.data.DoublyLinkedList;

/**
 * A queue based on an array of a given size, if too many items are added to the queue, a larger array
 * (2x the size) is created to store the new items. However, to avoid copying overhead, the old array 
 * will remain in memory until its items are completely used up. This provides a constant add and remove 
 * time where the cost of growth is quite small in terms of CPU time. In exchange this no-copy scheme 
 * uses 50% more memory than is strictly necessary.
 * 
 * In some cases even more memory is required because the new array may become filled before its items
 * have started to empty out. In this case the original array (which is still not empty) as well as
 * a second array at 2x the original size, a third array of 4x the size, and on and on, could all
 * exist simultaneously. While this situation is not ideal, it can be avoided by providing an accurate
 * initialCapacity value. In the worst case, the memory usage is no more than double what it needs to be.
 * 
 * @author Gregary
 */
public class GrowingArrayQueue<T> extends AbstractQueue<T> {

	private int size;
	private int modifyCount;
	IntFunction<T[]> arrayAllocator;
	DoublyLinkedList<FixedSizeArrayQueue<T>> queues;
	
	public GrowingArrayQueue(IntFunction<T[]> arrayAllocator, int initialCapacity) {
		this.arrayAllocator = arrayAllocator;
		this.queues = new DoublyLinkedList<>();
		this.modifyCount = 0;
		queues.offer(new FixedSizeArrayQueue<>(arrayAllocator, initialCapacity));
	}

	@Override
	public boolean offer(T item) {
		boolean success = queues.getLast().offer(item);
		if (!success) {
			queues.offer(new FixedSizeArrayQueue<>(arrayAllocator, (size()+1)*2));
			success = queues.getLast().offer(item);
			if (!success) throw new RuntimeException();
		}
		
		size++;
		modifyCount++;
		return success;
	}

	@Override
	public T poll() {
		FixedSizeArrayQueue<T> queue = queues.peek();
		if (queue.isEmpty()) {
			if (queues.size() == 1) return null;
			queues.removeFirst();
			queue = queues.peek();
		}
		
		size--;
		modifyCount++;
		return queue.poll();
	}

	@Override
	public T peek() {
		FixedSizeArrayQueue<T> queue = queues.peek();
		if (queue.isEmpty()) {
			if (queues.size() == 1) return null;
			queues.removeFirst();
			queue = queues.peek();
		}

		return queue.peek();
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			int expectedModifyCount = GrowingArrayQueue.this.modifyCount;
			Iterator<FixedSizeArrayQueue<T>> queueIterator = GrowingArrayQueue.this.queues.iterator();
			Iterator<T> itemIterator = null;
			
			@Override
			public boolean hasNext() {
				if (modifyCount != expectedModifyCount) throw new ConcurrentModificationException();
				if (itemIterator != null && itemIterator.hasNext()) return true;
				if (!queueIterator.hasNext()) return false;
				itemIterator = queueIterator.next().iterator();
				return itemIterator.hasNext();
			}

			@Override
			public T next() {
				if (modifyCount != expectedModifyCount) throw new ConcurrentModificationException();
				if (itemIterator != null && itemIterator.hasNext()) return itemIterator.next();
				if (!queueIterator.hasNext()) throw new NoSuchElementException();
				itemIterator = queueIterator.next().iterator();
				return itemIterator.next();
			}
			
			
		};
	}

	@Override
	public int size() {
		return size;
	}
	

}
