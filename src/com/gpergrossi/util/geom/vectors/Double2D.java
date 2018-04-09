package com.gpergrossi.util.geom.vectors;

import java.awt.geom.Point2D;

public class Double2D implements IVector2D<Double2D> {

	public static final double EPSILON = 0.001;
	public static final double EPSILON2 = Double2D.EPSILON * Double2D.EPSILON;
	
	public static long ALLOCATION_COUNT;
	
	public static Double2D unit(double angle) {
		return new Double2D(Math.cos(angle), Math.sin(angle));
	}
	
	public static double distance(double x0, double y0, double x1, double y1) {
		double dx = x1-x0;
		double dy = y1-y0;
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	public static double distanceSquared(double x0, double y0, double x1, double y1) {
		double dx = x1-x0;
		double dy = y1-y0;
		return dx*dx + dy*dy;
	}
	
	public static double angle(double x0, double y0, double x1, double y1) {
		double dx = x1-x0;
		double dy = y1-y0;
		return Math.atan2(dy, dx);
	}
	
	public static double cross(double x0, double y0, double x1, double y1) {
		return x0*y1 - y0*x1;
	}
	
	public static double dot(double x0, double y0, double x1, double y1) {
		return x0*x1 + y0*y1;
	}
	
	public static boolean equals(double x0, double y0, double x1, double y1) {
		return (distanceSquared(x0, y0, x1, y1) < EPSILON2);
	}
	
	
	
	protected double x, y;
	
	public Double2D() {
		ALLOCATION_COUNT++;
		this.x = 0;
		this.y = 0;
	}
	
	public Double2D(double x, double y) {
		ALLOCATION_COUNT++;
		this.x = x;
		this.y = y;
	}
	
	public double x() {
		return x;
	}
	
	public double y() {
		return y;
	}
	
	public Double2D redefine(double x, double y) {
		return new Double2D(x, y);
	}
		
	public Point2D toPoint2D() {
		return new Point2D.Double(x, y);
	}
	
	@Override
	public String toString() {
		return "Vec2[x="+x+", y="+y+"]";
	}

	@Override
	public Double2D copy() {
		return this;
	}
	
	@Override
	public Double2D immutable() {
		return this;
	}
	
	@Override
	public Mutable mutable() {
		return new Mutable(this.x, this.y);
	}

	@Override
	public Double2D multiply(double scalar) {
		return new Double2D(x*scalar, y*scalar);
	}

	@Override
	public Double2D divide(double scalar) {
		return new Double2D(x/scalar, y/scalar);
	}

	@Override
	public double dot(Double2D other) {
		return Double2D.dot(this.x, this.y, other.x, other.y);
	}

	@Override
	public Double2D add(Double2D vector) {
		return new Double2D(x+vector.x, y+vector.y);
	}

	@Override
	public Double2D subtract(Double2D vector) {
		return new Double2D(x-vector.x, y-vector.y);
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
	public Double2D normalize() {
		return this.divide(length());
	}

	public double distanceTo(double x, double y) {
		return Math.sqrt(distanceSquaredTo(x, y));
	}

	public double distanceSquaredTo(double x, double y) {
		double dx = this.x - x;
		double dy = this.y - y;
		return dx*dx + dy*dy;
	}
	
	@Override
	public double distanceTo(Double2D vector) {
		return Math.sqrt(distanceSquaredTo(vector.x, vector.y));
	}

	@Override
	public double distanceSquaredTo(Double2D vector) {
		return distanceSquaredTo(vector.x, vector.y);
	}

	@Override
	public int compareTo(Double2D other) {
		int dy = (int) Math.signum(this.y - other.y);
		if (dy != 0) return dy;

		int dx = (int) Math.signum(this.x - other.x);
		if (dx != 0) return dx;
		
		return 0;
		//return Integer.compare(this.hashCode(), other.hashCode());
	}

	@Override
	public boolean equals(Double2D other) {
		if (other == null) return false;
		return Double2D.equals(this.x, this.y, other.x, other.y);
	}

	@Override
	public double cross(Double2D other) {
		return Double2D.cross(this.x, this.y, other.x, other.y);
	}

	@Override
	public double angle() {
		return Math.atan2(y, x);
	}

	@Override
	public Double2D perpendicular() {
		return new Double2D(-y, x);
	}

	@Override
	public Double2D rotate(double angle) {
		double x = this.x;
		double y = this.y;
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double newX = (x * cos - y * sin);
		double newY = (x * sin + y * cos);
		return new Double2D(newX, newY);
	}
	
	
	
	
	public static class Mutable extends Double2D {
		
		public Mutable() {
			this.x = 0;
			this.y = 0;
		}
		
		public Mutable(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
		public Mutable redefine(double x, double y) {
			this.x = x;
			this.y = y;
			return this;
		}
		
		public void x(double x) {
			this.x = x;
		}
		
		public void y(double y) {
			this.y = y;
		}
		
		public Double2D immutable() {
			return new Double2D(x, y);
		}
		
		public Mutable mutable() {
			return this.copy();
		}
		
		@Override
		public Mutable copy() {
			return new Double2D.Mutable(x, y);
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
		public Mutable add(Double2D vector) {
			this.x += vector.x;
			this.y += vector.y;
			return this;
		}

		@Override
		public Mutable subtract(Double2D vector) {
			this.x -= vector.x;
			this.y -= vector.y;
			return this;
		}
		
		@Override
		public Mutable normalize() {
			double length = length();
			this.x /= length;
			this.y /= length;
			return this;
		}

		@Override
		public Mutable perpendicular() {
			double x = this.x;
			this.x = -y;
			this.y = x;
			return this;
		}

		@Override
		public Mutable rotate(double angle) {
			double x = this.x;
			double y = this.y;
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			this.x = (x * cos - y * sin);
			this.y = (x * sin + y * cos);
			return this;
		}
		
	}

}
