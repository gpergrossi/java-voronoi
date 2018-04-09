package com.gpergrossi.util.geom.shapes;

import java.awt.geom.Ellipse2D;

import com.gpergrossi.util.geom.vectors.Double2D;

public final class Circle implements IShape {
	
	protected double x, y, radius;
	private double radius2;
	
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
		this.radius2 = radius*radius;
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
	
	public double radiusSquared() {
		return radius2;
	}

	
	
	@Override
	public String toString() {
		return "Circle[X="+x+", Y="+y+", Radius="+radius+"]";
	}

	@Override
	public Circle copy() {
		return new Circle(x, y, radius);
	}
	
	@Override
	public double getArea() {
		return Math.PI * radius * radius;
	}

	@Override
	public double getPerimeter() {
		return 2.0 * Math.PI * radius;
	}

	@Override
	public Double2D getCentroid() {
		return new Double2D(x, y);
	}

	@Override
	public Circle outset(double amount) {
		return new Circle(x, y, radius+amount);
	}

	@Override
	public IShape inset(double amount) {
		return outset(-amount);
	}

	@Override
	public boolean contains(Double2D pt) {
		return (pt.distanceSquaredTo(getCentroid()) <= radius2);
	}

	@Override
	public boolean contains(double x, double y) {
		double distX = x - this.x;
		double distY = y - this.y;
		return (distX*distX + distY*distY <= radius2);
	}
	
	
	@Override
	public boolean contains(IShape other) {
		if (other instanceof Circle) this.contains((Circle) other);
		if (other instanceof Line) this.contains((Line) other);
		if (other instanceof Rect) this.contains((Rect) other);
		if (other instanceof Polygon) this.contains((Polygon) other);
		throw new UnsupportedOperationException();
	}
	
	public boolean contains(Circle circ) {
		if (circ.radius > this.radius) return false;
		return (this.getCentroid().subtract(circ.getCentroid()).length() <= (this.radius - circ.radius));
	}
	
	public boolean contains(Line line) {
		if (line.length() < Double.POSITIVE_INFINITY) {
			if (!this.contains(line.getStartX(), line.getStartY())) return false;
			if (!this.contains(line.getEndX(), line.getEndY())) return false;
			return true;
		}
		return false;
	}

	public boolean contains(Rect rect) {
		if (!this.contains(rect.minX(), rect.minY())) return false;
		if (!this.contains(rect.maxX(), rect.minY())) return false;
		if (!this.contains(rect.minX(), rect.maxY())) return false;
		if (!this.contains(rect.maxX(), rect.maxY())) return false;
		return true;
	}
	
	public boolean contains(Polygon poly) {
		for (Double2D vert : poly.getVertices()) {
			if (!this.contains(vert)) return false;
		}
		return true;
	}

	@Override
	public boolean intersects(IShape other) {
		if (other instanceof Circle) this.intersects((Circle) other);
		if (other instanceof Line) this.intersects((Line) other);
		if (other instanceof Rect) this.intersects((Rect) other);
		if (other instanceof Polygon) this.intersects((Polygon) other);
		throw new UnsupportedOperationException();
	}
	
	public boolean intersects(Circle circ) {
		return this.getCentroid().subtract(circ.getCentroid()).lengthSquared() < (this.radius2 + circ.radius2);
	}
	
	public boolean intersects(Line line) {
		Double2D.Mutable result = new Double2D.Mutable();
		line.closestPoint(this.getCentroid(), result);
		return (result.subtract(this.getCentroid()).lengthSquared() < this.radius2);
	}

	public boolean intersects(Rect rect) {
		// Circle inside of rectangle
		if (rect.contains(this.getCentroid())) return true;
		
		// Circle aligned with rectangle vertically
		if (this.x >= rect.minX() && this.x <= rect.maxX()) {
			return (this.y >= rect.minY()-radius && this.y <= rect.maxY()+radius); 
		}
		
		// Circle aligned with rectangle horizontally
		if (this.y >= rect.minY() && this.y <= rect.maxY()) {
			return (this.x >= rect.minX()-radius && this.x <= rect.maxX()+radius);
		}
		
		// Circle in one of the corner regions
		if (this.contains(rect.minX(), rect.minY())) return true;
		if (this.contains(rect.maxX(), rect.minY())) return true;
		if (this.contains(rect.minX(), rect.maxY())) return true;
		if (this.contains(rect.maxX(), rect.maxY())) return true;
		return false;
	}
	
	public boolean intersects(Polygon poly) {
		return poly.intersects(this);
	}
	
	@Override
	public LineSeg clip(Line line) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Convex toPolygon(int numSides) {
		double angle = Math.PI/2.0;
		double delta = Math.PI*2.0 / numSides;
		
		Double2D[] vertices = new Double2D[numSides];
		for (int i = 0; i < numSides; i++) {
			double a = angle+delta*i;
			double x = this.x + Math.cos(a)*this.radius;
			double y = this.y + Math.sin(a)*this.radius;
			vertices[i] = new Double2D(x, y);
		}
		
		return Convex.createDirect(vertices);
	}

	@Override
	public Rect getBounds() {
		return new Rect(x-radius, y-radius, radius*2, radius*2);
	}

	@Override
	public Ellipse2D asAWTShape() {
		return new Ellipse2D.Double(x-radius, y-radius, radius*2, radius*2);
	}
	
}
