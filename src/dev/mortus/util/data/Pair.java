package dev.mortus.util.data;

import java.util.Iterator;
import java.util.function.Predicate;

public class Pair<T> extends Tuple2<T,T> implements Iterable<T> {

	public Pair(T first, T second) {
		super(first, second);
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

	public Pair<T> filter(Predicate<T> predicate) {
		T first = null, second = null;
		if (predicate.test(this.first)) {
			first = this.first;
			if (predicate.test(this.second)) second = this.second;
		} else {
			if (predicate.test(this.second)) first = this.second;
		}
		return new Pair<T>(first, second);
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
	
	public Pair<T> intersect(Pair<T> other) {
		return this.filter(elem -> other.contains(elem));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Pair)) return false;
		Pair<?> other = (Pair<?>) obj;
		return first.equals(other.first) && second.equals(other.second);
	}
	
}
