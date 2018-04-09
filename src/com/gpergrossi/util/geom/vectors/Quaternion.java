package com.gpergrossi.util.geom.vectors;

public class Quaternion implements IVector<Quaternion> {

	public static final Quaternion IDENTITY = new Quaternion(1, 0, 0, 0); 
	
	public static Quaternion fromVector(Double3D vector) {
		return new Quaternion(0, vector.x(), vector.y(), vector.z());
	}
	
	public static Quaternion fromAxisRotation(Double3D axis, double theta) {
		double cos = Math.cos(theta/2.0);
		double sin = Math.sin(theta/2.0);
		Double3D axisNorm = axis.mutable().normalize();
		return new Quaternion(cos, sin * axisNorm.x, sin * axisNorm.y, sin * axisNorm.z);
	}

	protected double w; // real component
	protected double x; // i component
	protected double y; // j component
	protected double z; // k component
	
	public Quaternion(double w, double x, double y, double z) {
		this.w = w;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public double w() {
		return w;
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
	
	public Quaternion redefine(double w, double x, double y, double z) {
		return new Quaternion(w, x, y, z);
	}

	@Override
	public Quaternion copy() {
		return new Quaternion(w, x, y, z);
	}

	@Override
	public Quaternion immutable() {
		return this;
	}

	@Override
	public Quaternion mutable() {
		return new Mutable(w, x, y, z);
	}
	
	@Override
	public Quaternion multiply(double scalar) {
		return new Quaternion(w*scalar, x*scalar, y*scalar, z*scalar);
	}

	@Override
	public Quaternion divide(double scalar) {
		return new Quaternion(w/scalar, x/scalar, y/scalar, z/scalar);
	}

	@Override
	public double dot(Quaternion vector) {
		return (w * vector.w) + (x * vector.x) + (y * vector.y) + (z * vector.z);
	}

	@Override
	public Quaternion add(Quaternion vector) {
		return new Quaternion(w+vector.w, x+vector.x, y+vector.y, z+vector.z);
	}

	@Override
	public Quaternion subtract(Quaternion vector) {
		return new Quaternion(w-vector.w, x-vector.x, y-vector.y, z-vector.z);
	}

	@Override
	public double length() {
		return Math.sqrt(lengthSquared());
	}

	@Override
	public double lengthSquared() {
		return w*w + x*x + y*y + z*z;
	}

	@Override
	public Quaternion normalize() {
		return this.divide(this.length());
	}

	@Override
	public double distanceTo(Quaternion vector) {
		return Math.sqrt(this.distanceSquaredTo(vector));
	}

	@Override
	public double distanceSquaredTo(Quaternion vector) {
		final double dw = this.w - vector.w;
		final double dx = this.x - vector.x;
		final double dy = this.y - vector.y;
		final double dz = this.z - vector.z;
		return dw*dw + dx*dx + dy*dy + dz*dz;
	}

	public int compareTo(Quaternion other) {
		int dw = (int) Math.signum(this.w - other.w);
		if (dw != 0) return dw;
		
		int dz = (int) Math.signum(this.z - other.z);
		if (dz != 0) return dz;
		
		int dy = (int) Math.signum(this.y - other.y);
		if (dy != 0) return dy;

		int dx = (int) Math.signum(this.x - other.x);
		if (dx != 0) return dx;
		
		return 0;
		//return Integer.compare(this.hashCode(), other.hashCode());
	}

	public boolean equals(Quaternion other) {
		return this.distanceSquaredTo(other) < Double2D.EPSILON2;
	}
	
	public Quaternion multiply(Quaternion vector) {
		final Quaternion a = this;
		final Quaternion b = vector;

		final double w = (a.w * b.w) - (a.x * b.x) - (a.y * b.y) - (a.z * b.z);
		final double x = (a.w * b.x) + (a.x * b.w) + (a.y * b.z) - (a.z * b.y);
		final double y = (a.w * b.y) + (a.y * b.w) + (a.z * b.x) - (a.x * b.z);
		final double z = (a.w * b.z) + (a.z * b.w) + (a.x * b.y) - (a.y * b.x);
					
		return this.redefine(w, x, y, z);
	}
	
	public Double3D multiply(Double3D vector) {
		final double x2 = (1f - 2*(y*y + z*z)) * vector.x() + 2*(x*y - w*z) * vector.y() + 2*(x*z + w*y) * vector.z();
		final double y2 = 2*(x*y + w*z) * vector.x() + (1f - 2*(x*x + z*z)) * vector.y() + 2*(y*z - w*x) * vector.z();
		final double z2 = 2*(x*z - w*y) * vector.x() + 2*(y*z + w*x) * vector.y() + (1f - 2*(x*x + y*y)) * vector.z();
		return vector.redefine(x2, y2, z2);
	}

	public Quaternion conjugate() {
		return this.redefine(w, -x, -y, -z);
	}

	public Quaternion inverse() {
		double lengthSquared = this.lengthSquared();
		if (lengthSquared != 0.0) {
			double d = 1.0 / lengthSquared;
			return this.redefine(w*d, -x*d, -y*d, -z*d);
		}
		return this;
	}
	
	public static class Mutable extends Quaternion {

		public Mutable() {
			super(0, 0, 0, 0);
		}
		
		public Mutable(double x, double y, double z, double w) {
			super(x, y, z, w);
		}

		public void w(double w) {
			this.w = w;
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
		
		public Mutable redefine(double w, double x, double y, double z) {
			this.w = w;
			this.x = x;
			this.y = y;
			this.z = z;
			return this;
		}

		public Mutable copyFrom(Quaternion other) {
			this.w = other.w;
			this.x = other.x;
			this.y = other.y;
			this.z = other.z;
			return this;
		}
		
		@Override
		public Mutable copy() {
			return new Mutable(w, x, y, z);
		}

		@Override
		public Quaternion immutable() {
			return new Quaternion(x, y, z, w);
		}
		
		@Override
		public Mutable mutable() {
			return copy();
		}
		
		@Override
		public Mutable multiply(double scalar) {
			this.w *= scalar;
			this.x *= scalar;
			this.y *= scalar;
			this.z *= scalar;
			return this;
		}

		@Override
		public Mutable divide(double scalar) {
			this.w /= scalar;
			this.x /= scalar;
			this.y /= scalar;
			this.z /= scalar;
			return this;
		}

		@Override
		public Mutable add(Quaternion vector) {
			this.w += vector.x;
			this.x += vector.x;
			this.y += vector.y;
			this.z += vector.z;
			return this;
		}

		@Override
		public Mutable subtract(Quaternion vector) {
			this.w -= vector.w;
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
		
		public Mutable multiply(Quaternion vector) {
			final Quaternion a = this;
			final Quaternion b = vector;

			final double w = (a.w * b.w) - (a.x * b.x) - (a.y * b.y) - (a.z * b.z);
			final double x = (a.w * b.x) + (a.x * b.w) + (a.y * b.z) - (a.z * b.y);
			final double y = (a.w * b.y) + (a.y * b.w) + (a.z * b.x) - (a.x * b.z);
			final double z = (a.w * b.z) + (a.z * b.w) + (a.x * b.y) - (a.y * b.x);
						
			return this.redefine(w, x, y, z);
		}

		public Mutable conjugate() {
			return this.redefine(w, -x, -y, -z);
		}

		public Mutable inverse() {
			double lengthSquared = this.lengthSquared();
			if (lengthSquared != 0.0) {
				double d = 1.0 / lengthSquared;
				return this.redefine(w*d, -x*d, -y*d, -z*d);
			}
			return this;
		}
		
	}
}
