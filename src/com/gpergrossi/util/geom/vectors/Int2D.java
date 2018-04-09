package com.gpergrossi.util.geom.vectors;

import java.util.Optional;
import java.util.function.Function;

import com.gpergrossi.util.geom.ranges.Int2DRange;

public class Int2D implements IVector2D<Int2D> {

	protected int x, y;
	
	public Int2D() {
		this(0, 0);
	}
	
	public Int2D(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int x() {
		return x;
	}
	
	public int y() {
		return y;
	}

	public Int2D redefine(int x, int y) {
		return new Int2D(x, y);
	}
	
	@Override
	public Int2D copy() {
		return new Int2D(x, y);
	}
	
	@Override
	public Int2D immutable() {
		return this;
	}
	
	@Override
	public Mutable mutable() {
		return new Mutable(this.x, this.y);
	}

	@Override
	public Int2D multiply(double scalar) {
		this.x *= scalar;
		this.y *= scalar;
		return this;
	}

	@Override
	public Int2D divide(double scalar) {
		this.x /= scalar;
		this.y /= scalar;
		return this;
	}

	@Override
	public double dot(Int2D vector) {
		return this.x * vector.x + this.y * vector.y;
	}

	@Override
	public Int2D add(Int2D vector) {
		this.x += vector.x;
		this.y += vector.y;
		return this;
	}

	@Override
	public Int2D subtract(Int2D vector) {
		this.x -= vector.x;
		this.y -= vector.y;
		return this;
	}

	@Override
	public double length() {
		return Math.sqrt(lengthSquared());
	}

	@Override
	public double lengthSquared() {
		double dx = this.x;
		double dy = this.y;
		return dx*dx + dy*dy;
	}
	
	@Override
	public Int2D normalize() {
		double length = length();
		double x = this.x / length;
		double y = this.y / length;
		
		if (Math.abs(x) >= Math.abs(y)) {
			return new Int2D((int) Math.signum(x), 0);
		} else {
			return new Int2D(0, (int) Math.signum(y));
		}
	}

	@Override
	public double distanceTo(Int2D vector) {
		return Math.sqrt(distanceSquaredTo(vector));
	}

	@Override
	public double distanceSquaredTo(Int2D vector) {
		double dx = this.x - vector.x;
		double dy = this.y - vector.y;
		return dx*dx + dy*dy;
	}

	@Override
	public int compareTo(Int2D other) {
		int dy = this.y - other.y;
		if (dy != 0) return dy;

		int dx = this.x - other.x;
		if (dx != 0) return dx;
		
		return Integer.compare(this.hashCode(), other.hashCode());
	}

	@Override
	public boolean equals(Int2D other) {
		return this.x == other.x && this.y == other.y;
	}

	@Override
	public double cross(Int2D vector) {
		return this.x * vector.y - this.y * vector.x;
	}

	@Override
	public double angle() {
		return Math.atan2(y, x);
	}

	@Override
	public Int2D perpendicular() {
		int x = this.x;
		this.x = -y;
		this.y = x;
		return this;
	}

	@Override
	public Int2D rotate(double angle) {
		double x = this.x+0.5;
		double y = this.y+0.5;

		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		
		this.x = (int) Math.floor(x * cos - y * sin);
		this.y = (int) Math.floor(x * sin + y * cos);
		return this;
	}
	
	@Override
	public int hashCode() {
		return (x * 397) ^ y;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Int2D)) return false;
		Int2D other = (Int2D) obj;
		if (other.x != this.x) return false;
		if (other.y != this.y) return false;
		return true;
	}

	@Override
	public String toString() {
		return "Int2D[x="+x+", y="+y+"]";
	}
	
	public static class WithIndex extends Mutable {
		public final Int2DRange range;
		public int index;
		public WithIndex(Int2DRange range, int x, int y, int index) {
			super(x, y);
			this.range = range;
			this.index = index;
		}
	}
	
	public static class StoredFloat extends WithIndex {
		Int2DRange.Floats floats;
		public StoredFloat(Int2DRange.Floats floats, int x, int y, int index) {
			super(floats, x, y, index);
			this.floats = floats;
		}
		public float getValue() {
			return floats.get(index);
		}
		public void setValue(float value) {
			floats.set(index, value);
		}
		public void setValue(Function<Float, Float> operation) {
			floats.set(index, operation.apply(floats.get(index)));
		}
		public Optional<StoredFloat> getNeighbor(int i, int j) {
			if (!floats.contains(x+i, y+j)) return Optional.empty();
			return Optional.of(new StoredFloat(this.floats, x+i, y+j, index+j*floats.width+i));
		}
	}

	public static class StoredByte extends WithIndex {
		Int2DRange.Bytes bytes;
		public StoredByte(Int2DRange.Bytes bytes, int x, int y, int index) {
			super(bytes, x, y, index);
			this.bytes = bytes;
		}
		public byte getValue() {
			return bytes.get(index);
		}
		public void setValue(byte value) {
			bytes.set(index, value);
		}
		public void setValue(Function<Byte, Byte> operation) {
			bytes.set(index, operation.apply(bytes.get(index)));
		}
		public Optional<StoredByte> getNeighbor(int i, int j) {
			if (!bytes.contains(x+i, y+j)) return Optional.empty();
			return Optional.of(new StoredByte(this.bytes, x+i, y+j, index+j*bytes.width+i));
		}
	}

	public static class StoredInteger extends WithIndex {
		Int2DRange.Integers integers;
		public StoredInteger(Int2DRange.Integers integers, int x, int y, int index) {
			super(integers, x, y, index);
			this.integers = integers;
		}
		public int getValue() {
			return integers.get(index);
		}
		public void setValue(int value) {
			integers.set(index, value);
		}
		public void setValue(Function<Integer, Integer> operation) {
			integers.set(index, operation.apply(integers.get(index)));
		}
		public Optional<StoredInteger> getNeighbor(int i, int j) {
			if (!integers.contains(x+i, y+j)) return Optional.empty();
			return Optional.of(new StoredInteger(this.integers, x+i, y+j, index+j*integers.width+i));
		}
	}
	
	public static class StoredBit extends WithIndex {
		Int2DRange.Bits bits;
		public StoredBit(Int2DRange.Bits bits, int x, int y, int index) {
			super(bits, x, y, index);
			this.bits = bits;
		}
		public boolean getValue() {
			return bits.get(index);
		}
		public void setValue(boolean value) {
			bits.set(index, value);
		}
		public void setValue(Function<Boolean, Boolean> operation) {
			bits.set(index, operation.apply(bits.get(index)));
		}
		public Optional<StoredBit> getNeighbor(int i, int j) {
			if (!bits.contains(x+i, y+j)) return Optional.empty();
			return Optional.of(new StoredBit(this.bits, x+i, y+j, index+j*bits.width+i));
		}
	}
	
	public static class Mutable extends Int2D {
		
		public Mutable() {
			super();
		}
		
		public Mutable(int x, int y) {
			super(x, y);
		}
		
		public void x(int x) {
			this.x = x;
		}
		
		public void y(int y) {
			this.y = y;
		}
		
		public Mutable redefine(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}
		
		@Override
		public Int2D immutable() {
			return new Int2D(x, y);
		}

		@Override
		public Mutable mutable() {
			return copy();
		}

		@Override
		public Mutable copy() {
			return new Mutable(x, y);
		}

		@Override
		public Mutable multiply(double scalar) {
			this.x *= scalar;
			this.y *= scalar;
			return this;
		}

		@Override
		public Mutable divide(double scalar) {
			this.x /= scalar;
			this.y /= scalar;
			return this;
		}

		@Override
		public Mutable add(Int2D vector) {
			this.x += vector.x;
			this.y += vector.y;
			return this;
		}

		@Override
		public Mutable subtract(Int2D vector) {
			this.x -= vector.x;
			this.y -= vector.y;
			return this;
		}
		
		@Override
		public Mutable normalize() {
			double length = length();
			double x = this.x / length;
			double y = this.y / length;
			
			if (Math.abs(x) >= Math.abs(y)) {
				this.x = (int) Math.signum(x);
				this.y = 0;
			} else {
				this.x = 0;
				this.y = (int) Math.signum(y);
			}
			
			return this;
		}

		@Override
		public Mutable perpendicular() {
			int x = this.x;
			this.x = -y;
			this.y = x;
			return this;
		}

		@Override
		public Mutable rotate(double angle) {
			double x = this.x+0.5;
			double y = this.y+0.5;

			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			
			this.x = (int) Math.floor(x * cos - y * sin);
			this.y = (int) Math.floor(x * sin + y * cos);
			return this;
		}
		
	}
	
}
