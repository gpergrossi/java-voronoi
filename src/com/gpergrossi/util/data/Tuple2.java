package com.gpergrossi.util.data;

public class Tuple2<T,S> {

	public T first;
	public S second;
	
	public Tuple2(T first, S second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * @return how many items in the tuple are non-null.
	 */
	public int size() {
		if (first != null) {
			if (second != null) return 2;
			else return 1;
	    } else {
			if (second != null) return 1;
			else return 0;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Tuple2)) return false;
		Tuple2<?, ?> tuple = (Tuple2<?, ?>) obj;
		return this.first.equals(tuple.first) && this.second.equals(tuple.second);
	}
	
}
