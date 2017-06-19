package dev.mortus.util.math.geom2d;

import dev.mortus.util.math.vectors.Double2D;

public final class Circle {
	
	protected double x, y, radius;
	
	public static Circle fromPoints(Double2D a, Double2D b, Double2D c) {
		double abx = a.x() - b.x();
		double aby = a.y() - b.y();
		double bcx = b.x() - c.x();
		double bcy = b.y() - c.y();
		
		double d = abx*bcy - bcx*aby;
		if (d == 0) return null; // Points are co-linear
		
		double u = (a.x()*a.x() - b.x()*b.x() + a.y()*a.y() - b.y()*b.y()) / 2.0;
		double v = (b.x()*b.x() - c.x()*c.x() + b.y()*b.y() - c.y()*c.y()) / 2.0;
		
		double x = (u*bcy - v*aby) / d;
		double y = (v*abx - u*bcx) / d;
		
		double dx = a.x()-x;
		double dy = a.y()-y;
		return new Circle(x, y, Math.sqrt(dx*dx + dy*dy));
	}
	
	public Circle(double x, double y, double r) {
		this.x = x;
		this.y = y;
		this.radius = r;
	}
	
	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public double radius() {
		return radius;
	}

	@Override
	public String toString() {
		return "Circle[X="+x+", Y="+y+", Radius="+radius+"]";
	}
}
