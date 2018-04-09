package com.gpergrossi.util.data;

import java.util.Iterator;
import java.util.function.Predicate;

public class OrderedPair<T> extends Tuple2<T,T> implements Iterable<T> {

	public OrderedPair(T first, T second) {
		super(first, second);
	}

	public OrderedPair<T> reverse() {
		return new OrderedPair<>(second, first);
	}
	
	public T get(int index) {
		if (index < 0) return null;
		if (index == 0) {
			if (first != null) return first;
			if (second != null) return second;
			return null;
		}
		if (index == 1) {
			if (first == null) return null;
			if (second != null) return second;
			return null;
		}
		return null;
	}
	
	/**
	 * Iterates over the non-null elements of this pair.
	 */
	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			boolean started = false;
			boolean finished = false;
			
			public boolean hasNext() {
				if (!started) {
					if (first != null) return true;
					started = true;
				}
				if (!finished) {
					if (second != null) return true;
					finished = true;
				}
				return false;
			}
			
			public T next() {
				if (!started) {
					started = true;
					if (first != null) return first;
				}
				finished = true;
				return second;
			}
		};
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Pair[first="+first+", second="+second+"]");
		return sb.toString();
	}

	public OrderedPair<T> filter(Predicate<T> predicate) {
		T first = null, second = null;
		if (predicate.test(this.first)) {
			first = this.first;
			if (predicate.test(this.second)) second = this.second;
		} else {
			if (predicate.test(this.second)) first = this.second;
		}
		return new OrderedPair<T>(first, second);
	}
	
	public boolean contains(T elem) {
		if (elem == null) {
			if (first == null) return true;
			if (second == null) return true;
			return false;
		}
		if (elem.equals(first)) return true;
		if (elem.equals(second)) return true;
		return false;
	}
	
	public OrderedPair<T> intersect(OrderedPair<T> other) {
		return this.filter(elem -> other.contains(elem));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof OrderedPair)) return false;
		OrderedPair<?> other = (OrderedPair<?>) obj;
		return first.equals(other.first) && second.equals(other.second);
	}
	
	@Override
	public int hashCode() {
		if (first == null || second == null) {
			if (first == null && second == null) throw new NullPointerException();
			if (first == null) return second.hashCode();
			if (second == null) return first.hashCode();
		}
			
		return Integer.rotateLeft(first.hashCode(), 8) ^ Integer.rotateRight(second.hashCode(), 8);
	}
	
}
