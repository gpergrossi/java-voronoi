package com.gpergrossi.util.geom.vectors;

public class Double3D implements IVector3D<Double3D> {
	
	public static final Double3D X_AXIS = new Double3D(1, 0, 0);
	public static final Double3D Y_AXIS = new Double3D(0, 1, 0);
	public static final Double3D Z_AXIS = new Double3D(0, 0, 1);
	
	protected double x, y, z;
	
	public Double3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public double x() {
		return x;
	}
	
	public double y() {
		return y;
	}
	
	public double z() {
		return z;
	}

	public Double3D redefine(double x, double y, double z) {
		return new Double3D(x, y, z);
	}
	
	@Override
	public Double3D copy() {
		return this;
	}

	@Override
	public Double3D immutable() {
		return this;
	}
	
	@Override
	public Mutable mutable() {
		return new Mutable(this.x, this.y, this.z);
	}
	
	@Override
	public Double3D multiply(double scalar) {
		return new Double3D(x*scalar, y*scalar, z*scalar);
	}

	@Override
	public Double3D divide(double scalar) {
		return new Double3D(x/scalar, y/scalar, z/scalar);
	}

	@Override
	public double dot(Double3D vector) {
		return x*vector.x + y*vector.y + z*vector.z;
	}

	@Override
	public Double3D add(Double3D vector) {
		return new Double3D(x + vector.x, y + vector.y, z + vector.z);
	}

	@Override
	public Double3D subtract(Double3D vector) {
		return new Double3D(x - vector.x, y - vector.y, z - vector.z);
	}

	@Override
	public double length() {
		return Math.sqrt(lengthSquared());
	}

	@Override
	public double lengthSquared() {
		return x*x + y*y + z*z;
	}

	@Override
	public Double3D normalize() {
		double length = length();
		return this.divide(length);
	}

	@Override
	public double distanceTo(Double3D vector) {
		return Math.sqrt(this.distanceSquaredTo(vector));
	}

	@Override
	public double distanceSquaredTo(Double3D vector) {
		double dx = x - vector.x;
		double dy = y - vector.y;
		double dz = z - vector.z;
		return dx*dx + dy*dy + dz*dz;
	}

	@Override
	public int compareTo(Double3D other) {
		int dz = (int) Math.signum(this.z - other.z);
		if (dz != 0) return dz;
		
		int dy = (int) Math.signum(this.y - other.y);
		if (dy != 0) return dy;

		int dx = (int) Math.signum(this.x - other.x);
		if (dx != 0) return dx;
		
		return 0;
		//return Integer.compare(this.hashCode(), other.hashCode());
	}

	@Override
	public boolean equals(Double3D other) {
		return this.distanceSquaredTo(other) < Double2D.EPSILON2;
	}
	
	@Override
	public Double3D cross(Double3D vector) {
		double x = this.y*vector.z - this.z*vector.y;
		double y = this.z*vector.x - this.x*vector.z;
		double z = this.x*vector.y - this.y*vector.x;
		return this.redefine(x, y, z);
	}

	@Override
	public Double3D rotate(Double3D axis, double theta) {
		Quaternion rotation = Quaternion.fromAxisRotation(axis, theta);
		return rotation.multiply(this);
	}
	
	@Override
	public String toString() {
		return String.format("Double3D(%.3f, %.3f, %.3f)", x, y, z);
	}
	
	public static class Mutable extends Double3D {

		public Mutable() {
			super(0, 0, 0);
		}
		
		public Mutable(double x, double y, double z) {
			super(x, y, z);
		}
		
		public void x(double x) {
			this.x = x;
		}
		
		public void y(double y) {
			this.y = y;
		}
		
		public void z(double z) {
			this.z = z;
		}
		
		public Mutable redefine(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
			return this;
		}

		public Mutable copyFrom(Double3D other) {
			this.x = other.x;
			this.y = other.y;
			this.z = other.z;
			return this;
		}
		
		@Override
		public Mutable copy() {
			return new Mutable(x, y, z);
		}

		@Override
		public Double3D immutable() {
			return new Double3D(x, y, z);
		}
		
		@Override
		public Mutable mutable() {
			return this.copy();
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
		public Mutable add(Double3D vector) {
			this.x += vector.x;
			this.y += vector.y;
			this.z += vector.z;
			return this;
		}

		@Override
		public Mutable subtract(Double3D vector) {
			this.x -= vector.x;
			this.y -= vector.y;
			this.z -= vector.z;
			return this;
		}

		@Override
		public Mutable normalize() {
			double length = length();
			return this.divide(length);
		}
		
		@Override
		public Mutable cross(Double3D vector) {
			double x = this.y*vector.z - this.z*vector.y;
			double y = this.z*vector.x - this.x*vector.z;
			double z = this.x*vector.y - this.y*vector.x;
			return this.redefine(x, y, z);
		}
		
		@Override
		public Mutable rotate(Double3D axis, double theta) {
			Quaternion rotation = Quaternion.fromAxisRotation(axis, theta);
			return (Mutable) rotation.multiply(this);
		}
		
	}

}
