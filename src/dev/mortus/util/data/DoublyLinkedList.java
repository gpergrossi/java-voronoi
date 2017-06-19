package dev.mortus.util.data;

import java.util.ConcurrentModificationException;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import dev.mortus.util.data.queue.AbstractDeque;

public class DoublyLinkedList<T> extends AbstractDeque<T> {

	private int modifyCount;
	private int size;
	private Node<T> sentinel;
	
	public DoublyLinkedList() {
		sentinel = new Node<T>();
	}
	
	private static class Node<K> {		
		private K value;
		private Node<K> prev;
		private Node<K> next;

		private Node() {
			this(null);
		}
		
		private Node(K value) {
			this.value = value;
			this.prev = this;
			this.next = this;
		}
		
		private Node<K> insertAfter(K value) {
			Node<K> n = new Node<>(value);
			n.next = this.next;
			n.prev = this;
			this.next.prev = n;
			this.next = n;
			return n;
		}

		private Node<K> insertBefore(K value) {
			return prev.insertAfter(value);
		}
		
		public void remove() {
			this.prev.next = this.next;
			this.next.prev = this.prev;
		}
	}
	
	private Node<T> getNodeInternal(int index) {
		if (index < 0 || index > size) throw new IndexOutOfBoundsException();
		
		if (index < size/2) {
			Node<T> n = sentinel.next;
			for (int i = 0; i < index; i++) {
				n = n.next;
			}
			return n;
		} else {
			Node<T> n = sentinel;
			for (int i = index; i < size; i++) {
				n = n.prev;
			}
			return n;
		}
	}
	
	@Override
	public ListIterator<T> listIterator(int index) {
		return new ListIterator<T>() {
			// The view should be thought of as a cursor between elements.
			// Initially the cursor is at the start of the list before any
			// elements at all.
			
			int nextIndex = index;
			Node<T> nextNode = getNodeInternal(index);
			Node<T> recentNode = nextNode.prev;
			int expectedModCount = DoublyLinkedList.this.modifyCount;
			
			private void checkConcurrentModification() {
				if (modifyCount != expectedModCount) throw new ConcurrentModificationException();
			}
			
			@Override
			public boolean hasNext() {
				checkConcurrentModification();
				return nextNode != sentinel;
			}

			@Override
			public T next() {
				checkConcurrentModification();
				if (nextNode == sentinel) throw new NoSuchElementException();
				recentNode = nextNode;
				nextNode = nextNode.next;
				nextIndex++;
				return recentNode.value;
			}

			@Override
			public boolean hasPrevious() {
				checkConcurrentModification();
				return nextNode.prev != sentinel;
			}

			@Override
			public T previous() {
				checkConcurrentModification();
				nextNode = nextNode.prev;
				recentNode = nextNode;
				nextIndex--;
				return recentNode.value;
			}

			@Override
			public int nextIndex() {
				checkConcurrentModification();
				return nextIndex;
			}

			@Override
			public int previousIndex() {
				checkConcurrentModification();
				return nextIndex-1;
			}

			@Override
			public void remove() {
				checkConcurrentModification();
				if (recentNode == null || recentNode == sentinel) throw new IllegalStateException();
				recentNode.remove();
				
				// If the most recent node came from next(), then we are erasing behind the current
				// cursor. Therefore indices are decremented to account for the removed element.
				if (recentNode == nextNode.prev) nextIndex--;
				
				DoublyLinkedList.this.size--;
				
				// Keep track of number of modifications made to detect concurrent modification
				expectedModCount++;
				DoublyLinkedList.this.modifyCount++;
				
				// No longer allowed to remove/set elements until next call to next() or prev()
				recentNode = null;
			}

			@Override
			public void set(T value) {
				checkConcurrentModification();
				if (recentNode == null || recentNode == sentinel) throw new IllegalStateException();
				recentNode.value = value;
			}

			@Override
			public void add(T value) {
				checkConcurrentModification();
				nextNode.insertBefore(value);
				
				// An element was added in the place of the current cursor, since this new element
				// is returned by prev(), the nextIndex is incremented to account for the added element
				nextIndex++;
				
				DoublyLinkedList.this.size++;
				
				// Keep track of number of modifications made to detect concurrent modification
				expectedModCount++;
				DoublyLinkedList.this.modifyCount++;
				
				// No longer allowed to remove/set elements until next call to next() or prev()
				recentNode = null;
			}
		};
	}

	@Override
	public int size() {
		return size;
	}

}
