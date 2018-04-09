package com.gpergrossi.util.geom.ranges;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import com.gpergrossi.util.geom.vectors.Int1D;

public class Int1DRange {

	public static final Int1DRange EMPTY = new Int1DRange(0, -1);
	public static final Int1DRange ALL = new Int1DRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
	
	public final int min;
	public final int max;
	public final long size;

	public Int1DRange(Int1DRange range) {
		this(range.min, range.max);
	}
	
	public Int1DRange(int min, int max) {
		this.min = min;
		this.max = max;
		this.size = Math.max(0L, ((long) max) - ((long) min) + 1L);
	}

	protected Int1DRange resize(int min, int max) {
		return new Int1DRange(min, max);
	}
	
	public long size() {
		return size;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public boolean contains(int value) {
		return (value >= min && value <= max);
	}
	
	public boolean onBorder(int value, int borderPadding) {
		return contains(value) && (value < min+borderPadding && value > max-borderPadding);
	}
	
	public Int1DRange grow(int padMin, int padMax) {
		return resize(min-padMin, max+padMax);
	}
	
	public Int1DRange grow(int padding) {
		return grow(padding, padding);
	}
	
	public Int1DRange shrink(int insetMin, int insetMax) {
		return grow(-insetMin, -insetMax);
	}
	
	public Int1DRange shrink(int inset) {
		return grow(-inset, -inset);
	}
	
	public Int1DRange intersect(Int1DRange other) {
		int min = Math.max(this.min, other.min);
		int max = Math.min(this.max, other.max);
		return resize(min, max);
	}

	public Int1DRange offset(int offset) {
		return resize(min+offset, max+offset);
	}
	
	public int indexFor(int value) {
		return value-min;
	}
	
	public int valueFor(long index) {
		if (index >= size || index < 0) throw new IndexOutOfBoundsException();
		return (int) (index+min);
	}

	public int random(Random random) {
		return random.nextInt(max-min+1)+min;
	}
	
	public String toString() {
		if (this.isEmpty()) return "Empty";
		if (min == max) return String.valueOf(min);
		if (size == 2) return min+", "+max;
		return min+" to "+max;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Int1DRange)) return false;
		Int1DRange range = (Int1DRange) obj;
		if (range.isEmpty() && this.isEmpty()) return true;
		if (range.min != this.min) return false;
		if (range.max != this.max) return false;
		return true;
	}
	
	public Iterable<Int1D.WithIndex> getAllMutable() {
		return new Iterable<Int1D.WithIndex>() {
			@Override
			public Iterator<Int1D.WithIndex> iterator() {
				return new Iterator<Int1D.WithIndex>() {
					private Int1D.WithIndex mutable = new Int1D.WithIndex(Int1DRange.this, 0, 0);
					private int index = 0;
					
					@Override
					public boolean hasNext() {
						return index < size();
					}

					@Override
					public Int1D.WithIndex next() {
						if (!hasNext()) throw new NoSuchElementException();
						mutable.x(index+min);
						mutable.index = index;
						index++;
						return mutable;
					}
				};	
			}
		};
	}
	
	public Iterable<Int1D.WithIndex> getAll() {
		return new Iterable<Int1D.WithIndex>() {
			@Override
			public Iterator<Int1D.WithIndex> iterator() {
				return new Iterator<Int1D.WithIndex>() {
					private int index = 0;
					
					@Override
					public boolean hasNext() {
						return index < size();
					}

					@Override
					public Int1D.WithIndex next() {
						if (!hasNext()) throw new NoSuchElementException();
						Int1D.WithIndex result = new Int1D.WithIndex(Int1DRange.this, index+min, index);
						index++;
						return result;
					}
				};	
			}
		};
	}
	
}
