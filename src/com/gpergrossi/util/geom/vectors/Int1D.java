package com.gpergrossi.util.geom.vectors;

import com.gpergrossi.util.geom.ranges.Int1DRange;

public class Int1D implements IVector<Int1D> {

	protected int x;
	
	public Int1D() {
		this(0);
	}
	
	public Int1D(int x) {
		this.x = x;
	}
	
	public int x() {
		return x;
	}

	public Int1D redefine(int x) {
		return new Int1D(x);
	}
	
	@Override
	public Int1D copy() {
		return new Int1D(x);
	}
	
	@Override
	public Int1D immutable() {
		return this;
	}
	
	@Override
	public Mutable mutable() {
		return new Mutable(this.x);
	}

	@Override
	public Int1D multiply(double scalar) {
		this.x *= scalar;
		return this;
	}

	@Override
	public Int1D divide(double scalar) {
		this.x /= scalar;
		return this;
	}

	@Override
	public double dot(Int1D vector) {
		return this.x * vector.x;
	}

	@Override
	public Int1D add(Int1D vector) {
		this.x += vector.x;
		return this;
	}

	@Override
	public Int1D subtract(Int1D vector) {
		this.x -= vector.x;
		return this;
	}

	@Override
	public double length() {
		return Math.abs(this.x);
	}

	@Override
	public double lengthSquared() {
		double dx = this.x;
		return dx*dx;
	}
	
	@Override
	public Int1D normalize() {
		return new Int1D((int) Math.signum(this.x));
	}

	@Override
	public double distanceTo(Int1D vector) {
		return Math.abs(this.x - vector.x);
	}

	@Override
	public double distanceSquaredTo(Int1D vector) {
		double dist = distanceTo(vector);
		return dist*dist;
	}

	@Override
	public int compareTo(Int1D other) {
		int dx = this.x - other.x;
		if (dx != 0) return dx;
		
		return Integer.compare(this.hashCode(), other.hashCode());
	}

	@Override
	public boolean equals(Int1D other) {
		return this.x == other.x;
	}
	
	public static class WithIndex extends Mutable {
		public final Int1DRange range;
		public int index;
		public WithIndex(Int1DRange range, int x, int index) {
			super(x);
			this.range = range;
			this.index = index;
		}
	}
	
	public static class Mutable extends Int1D {
		
		public Mutable() {
			super();
		}
		
		public Mutable(int x) {
			super(x);
		}
		
		public void x(int x) {
			this.x = x;
		}
		
		public Mutable redefine(int x) {
			this.x = x;
			return this;
		}
		
		@Override
		public Int1D immutable() {
			return new Int1D(x);
		}

		@Override
		public Mutable mutable() {
			return copy();
		}

		@Override
		public Mutable copy() {
			return new Mutable(x);
		}

		@Override
		public Mutable multiply(double scalar) {
			this.x *= scalar;
			return this;
		}

		@Override
		public Mutable divide(double scalar) {
			this.x /= scalar;
			return this;
		}

		@Override
		public Mutable add(Int1D vector) {
			this.x += vector.x;
			return this;
		}

		@Override
		public Mutable subtract(Int1D vector) {
			this.x -= vector.x;
			return this;
		}
		
		@Override
		public Mutable normalize() {
			this.x = (int) Math.signum(this.x);
			return this;
		}
		
	}
	
}
