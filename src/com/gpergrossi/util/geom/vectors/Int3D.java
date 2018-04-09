package com.gpergrossi.util.geom.vectors;

import com.gpergrossi.util.geom.ranges.Int3DRange;

public class Int3D implements IVector3D<Int3D> {

	protected int x, y, z;
	
	public Int3D() {
		this(0, 0, 0);
	}
	
	public Int3D(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int z() {
		return z;
	}

	@Override
	public String toString() {
		return "("+x+", "+y+", "+z+")";
	}

	public Int3D redefine(int x, int y, int z) {
		return new Int3D(x, y, z);
	}

	@Override
	public Int3D copy() {
		return this;
	}

	@Override
	public Int3D immutable() {
		return this;
	}
	
	@Override
	public Mutable mutable() {
		return new Mutable(this.x, this.y, this.z);
	}
	
	public Double3D toDouble() {
		return new Double3D(x, y, z);
	}

	@Override
	public Int3D multiply(double scalar) {
		return new Int3D((int) (x*scalar), (int) (y*scalar), (int) (z*scalar));
	}

	@Override
	public Int3D divide(double scalar) {
		return new Int3D((int) (x/scalar), (int) (y/scalar), (int) (z/scalar));
	}

	@Override
	public double dot(Int3D vector) {
		return x*vector.x + y*vector.y + z*vector.z;
	}

	@Override
	public Int3D add(Int3D vector) {
		return new Int3D(x + vector.x, y + vector.y, z + vector.z);
	}

	@Override
	public Int3D subtract(Int3D vector) {
		return new Int3D(x - vector.x, y - vector.y, z - vector.z);
	}

	@Override
	public double length() {
		return Math.sqrt(this.lengthSquared());
	}

	@Override
	public double lengthSquared() {
		return x*x + y*y + z*z;
	}

	@Override
	public Int3D normalize() {
		double length = length();
		double x = this.x / length;
		double y = this.y / length;
		double z = this.z / length;
		
		if (Math.abs(x) >= Math.abs(y)) {
			if (Math.abs(x) >= Math.abs(z)) {
				return new Int3D((int) Math.signum(x), 0, 0);
			} else {
				return new Int3D(0, 0, (int) Math.signum(z));
			}
		} else {
			if (Math.abs(y) >= Math.abs(z)) {
				return new Int3D(0, (int) Math.signum(y), 0);
			} else {
				return new Int3D(0, 0, (int) Math.signum(z));
			}
		}
	}

	@Override
	public double distanceTo(Int3D vector) {
		return Math.sqrt(this.distanceSquaredTo(vector));
	}

	@Override
	public double distanceSquaredTo(Int3D vector) {
		double dx = x - vector.x;
		double dy = y - vector.y;
		double dz = z - vector.z;
		return dx*dx + dy*dy + dz*dz;
	}

	@Override
	public int compareTo(Int3D other) {
		int dz = this.z - other.z;
		if (dz != 0) return dz;
		
		int dy = this.y - other.y;
		if (dy != 0) return dy;

		int dx = this.x - other.x;
		if (dx != 0) return dx;
		
		return Integer.compare(this.hashCode(), other.hashCode());
	}

	@Override
	public boolean equals(Int3D other) {
		return this.x == other.x && this.y == other.y && this.z == other.z;
	}

	@Override
	public Int3D cross(Int3D vector) {
		int x = this.y*vector.z - this.z*vector.y;
		int y = this.z*vector.x - this.x*vector.z;
		int z = this.x*vector.y - this.y*vector.x;
		return new Int3D(x, y, z);
	}

	@Override
	public Int3D rotate(Int3D axis, double angle) {
		Double3D.Mutable thisDouble = new Double3D.Mutable(x + 0.5, y + 0.5, z + 0.5);
		Double3D.Mutable axisDouble = new Double3D.Mutable(axis.x, axis.y, axis.z);
		Double3D result = thisDouble.rotate(axisDouble, angle);
		return new Int3D((int) Math.floor(result.x()), (int) Math.floor(result.y()), (int) Math.floor(result.z()));
	}
	
	public static class Mutable extends Int3D {
		
		public Mutable() {
			super(0, 0, 0);
		}
		
		public Mutable(int x, int y, int z) {
			super(x, y, z);
		}
		
		public void x(int x) {
			this.x = x;
		}
		
		public void y(int y) {
			this.y = y;
		}
		
		public void z(int z) {
			this.z = z;
		}
		
		public Mutable redefine(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
			return this;
		}

		@Override
		public Mutable copy() {
			return new Mutable(x, y, z);
		}

		public Mutable copy(Int3D other) {
			this.x = other.x;
			this.y = other.y;
			this.z = other.z;
			return this;
		}

		@Override
		public Int3D immutable() {
			return new Int3D(x, y, z);
		}

		@Override
		public Mutable mutable() {
			return copy();
		}

		public Double3D.Mutable toDouble() {
			return new Double3D.Mutable(x, y, z);
		}
		
		@Override
		public Mutable multiply(double scalar) {
			this.x *= scalar;
			this.y *= scalar;
			this.z *= scalar;
			return this;
		}

		@Override
		public Mutable divide(double scalar) {
			this.x /= scalar;
			this.y /= scalar;
			this.z /= scalar;
			return this;
		}

		@Override
		public Mutable add(Int3D vector) {
			this.x += vector.x;
			this.y += vector.y;
			this.z += vector.z;
			return this;
		}

		@Override
		public Mutable subtract(Int3D vector) {
			this.x -= vector.x;
			this.y -= vector.y;
			this.z -= vector.z;
			return this;
		}

		@Override
		public Mutable normalize() {
			double length = length();
			double x = this.x / length;
			double y = this.y / length;
			double z = this.z / length;
			
			if (Math.abs(x) >= Math.abs(y)) {
				if (Math.abs(x) >= Math.abs(z)) {
					this.x = (int) Math.signum(x);
					this.y = 0;
					this.z = 0;
				} else {
					this.x = 0;
					this.y = 0;
					this.z = (int) Math.signum(z);
				}
			} else {
				if (Math.abs(y) >= Math.abs(z)) {
					this.x = 0;
					this.y = (int) Math.signum(y);
					this.z = 0;
				} else {
					this.x = 0;
					this.y = 0;
					this.z = (int) Math.signum(z);
				}
			}
			return this;
		}

		@Override
		public Int3D cross(Int3D vector) {
			int x = this.y*vector.z - this.z*vector.y;
			int y = this.z*vector.x - this.x*vector.z;
			int z = this.x*vector.y - this.y*vector.x;
			this.x = x;
			this.y = y;
			this.z = z;
			return this;
		}

		@Override
		public Int3D rotate(Int3D axis, double angle) {
			Double3D.Mutable thisDouble = new Double3D.Mutable(x + 0.5, y + 0.5, z + 0.5);
			Double3D.Mutable axisDouble = new Double3D.Mutable(axis.x, axis.y, axis.z);
			Double3D result = thisDouble.rotate(axisDouble, angle);
			this.x = (int) Math.floor(result.x());
			this.y = (int) Math.floor(result.y());
			this.z = (int) Math.floor(result.z());
			return this;
		}
		
	}
	
	public static class WithIndex extends Mutable {
		public final Int3DRange range;
		public int index;
		public WithIndex(Int3DRange range, int x, int y, int z, int index) {
			super(x, y, z);
			this.range = range;
			this.index = index;
		}
	}
	
}
