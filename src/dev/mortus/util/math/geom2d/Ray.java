package dev.mortus.util.math.geom2d;

import dev.mortus.util.math.vectors.Double2D;

public final class Ray extends Line {

	protected boolean reversed;
	
	public Ray(double x, double y, double dx, double dy) {
		super(x, y, dx, dy);
		this.reversed = false;
	}
	
	public Ray(double x, double y, double dx, double dy, boolean reverse) {
		super(x, y, dx, dy);
		this.reversed = reverse;
	}

	public LineSeg toSegment(double maxExtent) {
		LineSeg seg = null;
		if (reversed) {
			seg = new LineSeg(dx * -maxExtent, dy * -maxExtent, x, y);
		} else {
			seg = new LineSeg(x, y, dx * maxExtent, dy * maxExtent);
		}
		return seg;
	}
	
	public Ray copy() {
		return new Ray(x, y, dx, dy, reversed);
	}
	
	public void extend(double d) {
		d = (reversed ? -d : d);
		x += dx*d;
		y += dy*d;
	}
	
	public double tmin() {
		return (reversed ? Double.NEGATIVE_INFINITY : 0);
	}
	
	public double tmax() {
		return (reversed ? 0 : Double.POSITIVE_INFINITY);
	}

	public void reverse() {
		this.reversed = true;
	}

	public double getStartX() {
		return x;
	}
	
	public double getStartY() {
		return y;
	}
	
	public double getDX() {
		return dx;
	}
	
	public double getDY() {
		return dy;
	}

	public void reposition(Double2D pos) {
		this.x = pos.x();
		this.y = pos.y();
	}

	public Ray createPerpendicular() {
		return new Ray(x, y, -dy, dx);
	}

	/**
	 * Return the distance along this ray to which the given point would be projected
	 */
	public double dot(Double2D pt) {
		double dx = pt.x() - x;
		double dy = pt.y() - y;
		return Double2D.dot(dx, dy, this.dx, this.dy);
	}
	
}
