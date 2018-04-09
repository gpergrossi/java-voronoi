package com.gpergrossi.util.data.btree;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static com.gpergrossi.util.data.btree.IBinaryNode.getFirstDescendant;
import static com.gpergrossi.util.data.btree.IBinaryNode.getLastDescendant;
import static com.gpergrossi.util.data.btree.IBinaryNode.getSuccessor;

public class TreeNodeIterator<T extends IBinaryNode<T>> implements Iterator<T> {

	T nextElem;
	T lastElem;

	/**
	 * Constructs an iterator that will iterate over each of the Nodes in its root node's subtree in order.
	 * This iterator is not safe with concurrent modifications, however, it will remain functional
	 * as long as the lastDescendant() of the root node remains the same (==, not .equals).
	 * @param localRoot
	 */
	public TreeNodeIterator(T localRoot) {
		this.nextElem = getFirstDescendant(localRoot);
		this.lastElem = getSuccessor(getLastDescendant(localRoot));		// This may be null when localRoot is a true root node, however the successor of the
																		// last descendant may be defined if more nodes exist outside of this localRoot's subtree
	}
	
	public boolean hasNext() {
		return nextElem != null && nextElem != lastElem;
	}

	public T next() {
		if (!hasNext()) throw new NoSuchElementException();
		T result = nextElem;
		nextElem = getSuccessor(nextElem);
		return result;
	}
	
	public static class Traversal<S extends IBinaryNode<S>, T> implements Iterator<T> {
		TreeNodeIterator<S> iterator;
		Function<S, T> valueConverter;
		 
		public Traversal(S localRoot, Function<S, T> valueConverter) {
			this.iterator = new TreeNodeIterator<S>(localRoot);
			this.valueConverter = valueConverter;
		}
		
		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}
		
		@Override
		public T next() {
			return valueConverter.apply(iterator.next());
		}
	}
	
	public static class Empty<T> implements Iterator<T> {
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public T next() {
			return null;
		}
	}
	
}
