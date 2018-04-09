package com.gpergrossi.util.geom.shapes;

import com.gpergrossi.util.geom.vectors.Double2D;

public final class Ray extends Line {

	protected final boolean reversed;
	
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
			seg = new LineSeg(x - dx*maxExtent, y - dy*maxExtent, x, y);
		} else {
			seg = new LineSeg(x, y, x + dx*maxExtent, y + dy*maxExtent);
		}
		return seg;
	}
	
	public Ray extend(double d) {
		d = (reversed ? -d : d);
		return new Ray(x + dx*d, y + dy*d, dx, dy);
	}

	@Override
	public double tmin() {
		return (reversed ? Double.NEGATIVE_INFINITY : 0);
	}

	@Override
	public double tmax() {
		return (reversed ? 0 : Double.POSITIVE_INFINITY);
	}

	public Ray reverse() {
		return new Ray(x, y, dx, dy, !reversed);
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

	public Ray reposition(Double2D pos) {
		return new Ray(pos.x(), pos.y(), dx, dy);
	}

	public Ray createPerpendicular() {
		return new Ray(x, y, -dy, dx);
	}
	
	@Override
	public Ray copy() {
		return new Ray(x, y, dx, dy, reversed);
	}
	
	@Override
	public Ray outset(double amount) {
		return this.inset(-amount);
	}

	@Override
	public Ray inset(double amount) {
		Double2D.Mutable work = this.getDirection().mutable();
		work.perpendicular().normalize().multiply(amount);
		return new Ray(x + work.x(), y + work.y(), dx, dy);
	}

	public Line toLine() {
		return this.redefine(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}
	
}
