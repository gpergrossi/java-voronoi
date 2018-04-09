package com.gpergrossi.util.geom.shapes;

import java.awt.geom.Line2D;

import com.gpergrossi.util.geom.vectors.Double2D;

public class LineSeg extends Line {
	
	private double length;
	
	public LineSeg(Double2D pt0, Double2D pt1) {
		this(pt0.x(), pt0.y(), pt1.x(), pt1.y());
	}
	
	public LineSeg(double x0, double y0, double x1, double y1) {
		super(x0, y0, x1-x0, y1-y0);
		this.length = Double2D.distance(x0, y0, x1, y1);
	}
	
	public LineSeg toSegment(double maxExtent) {
		return this.copy();
	}

	@Override
	public double tmin() {
		return 0;
	}
	
	@Override
	public double tmax() {
		return length;
	}

	public void getMidpoint(Double2D.Mutable ptr) {
		double t = length/2.0;
		ptr.x(getX(t));
		ptr.y(getY(t));
	}
	
	public double length() {
		return length;
	}
	
	public Line toLine() {
		return new Line(x, y, dx, dy);
	}

	public Ray toRay() {
		return new Ray(x, y, dx, dy);
	}
	
	@Override
	public LineSeg copy() {
		return new LineSeg(x, y, x+dx, y+dy);
	}
	
	@Override
	public LineSeg outset(double amount) {
		return this.inset(-amount);
	}

	@Override
	public LineSeg inset(double amount) {
		Double2D.Mutable work = this.getDirection().mutable();
		work.perpendicular().normalize().multiply(amount);
		return new LineSeg(getStartX() + work.x(), getStartY() + work.y(), getEndX() + work.x(), getEndY() + work.y());
	}
	
	@Override
	public Line2D asAWTShape() {
		return new Line2D.Double(getX(tmin()), getY(tmin()), getX(tmax()), getY(tmax()));
	}
	
}
