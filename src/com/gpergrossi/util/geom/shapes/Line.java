package com.gpergrossi.util.geom.shapes;

import java.awt.Shape;

import com.gpergrossi.util.data.OrderedPair;
import com.gpergrossi.util.geom.vectors.Double2D;

public class Line implements IShape {

	protected final double x, y;
	protected final double dx, dy;
	
	public Line(double x, double y, double dx, double dy) {
		this.x = x;
		this.y = y;
		double length = Double2D.distance(0, 0, dx, dy);
		if (length == 0) {
			this.dx = 0;
			this.dy = 0;
		} else {
			this.dx = dx / length;
			this.dy = dy / length;
		}
	}
	
	public LineSeg toSegment(double tEnd) {
		return new LineSeg(x - dx * tEnd, y - dy * tEnd, x + dx * tEnd, y + dy * tEnd);
	}

	public LineSeg toSegment(double tStart, double tEnd) {
		return new LineSeg(getX(tStart), getY(tStart), getX(tEnd), getY(tEnd));
	}
	
	public void get(Double2D.Mutable ptr, double t) {
		if (ptr == null) return;
		ptr.x(getX(t));
		ptr.y(getY(t));
	}
	
	public double getX(double t) {
		return x+dx*t;
	}
	
	public double getY(double t) {
		return y+dy*t;
	}

	public void getStart(Double2D.Mutable ptr) {
		ptr.x(getStartX());
		ptr.y(getStartY());
	}
	
	public double getStartX() {
		return getX(tmin());
	}
	
	public double getStartY() {
		return getY(tmin());
	}

	public void getEnd(Double2D.Mutable ptr) {
		ptr.x(getEndX());
		ptr.y(getEndY());
	}
	
	public double getEndX() {
		return getX(tmax());
	}
	
	public double getEndY() {
		return getY(tmax());
	}

	public Double2D getDirection() {
		return new Double2D(dx, dy);		
	}
	
	protected Line redefine(double tmin, double tmax) {
		if (tmax < tmin) return null;
		if (tmin == this.tmin() && tmax == this.tmax()) return this;
		
		// Infinite line
		if (tmin == Double.NEGATIVE_INFINITY && tmax == Double.POSITIVE_INFINITY) return new Line(x, y, dx, dy);
		
		// Ray
		if (tmax == Double.POSITIVE_INFINITY) return new Ray(getX(tmin), getY(tmin), dx, dy);
		if (tmin == Double.NEGATIVE_INFINITY) return new Ray(getX(tmax), getY(tmax), dx, dy, true);
		
		// Segment
		return new LineSeg(getX(tmin), getY(tmin), getX(tmax), getY(tmax));
	}

	
	public boolean intersect(Double2D.Mutable ptr, Line other) {
		OrderedPair<Double> tValues = getIntersectTValues(this, other, true);
		if (tValues == null) return false;
		get(ptr, tValues.first);
		return true;
	}
	
	/**
	 * @param canFail - If the operation can fail based on each line's start/end t-values. 
	 * Even if true, null may still be returned for parallel lines or lines with no direction.
	 */
	private static OrderedPair<Double> getIntersectTValues(Line first, Line second, boolean canFail) {
		double deltaX = second.x - first.x;
		double deltaY = second.y - first.y;
		
		// Does either line have a 0 length direction vector?
		if (Double.isNaN(first.dx) || Double.isNaN(first.dy)) return null;
		if (Double.isNaN(second.dx) || Double.isNaN(second.dy)) return null;
		
		double det = Double2D.cross(second.dx, second.dy, first.dx, first.dy);
		if (Math.abs(det) < Double2D.EPSILON) return null; // The rays are parallel
		
		double u = Double2D.cross(second.dx, second.dy, deltaX, deltaY) / det;
		double v = Double2D.cross(first.dx,  first.dy,  deltaX, deltaY) / det;
		
		// No collision if t values outside of [tmin(), tmax()]. However we use EPSILON to resolve rounding issues
		if (canFail) {
			if (u+Double2D.EPSILON < first.tmin()  || u-Double2D.EPSILON > first.tmax() ) return null;
			if (v+Double2D.EPSILON < second.tmin() || v-Double2D.EPSILON > second.tmax()) return null;
		}
	
		// Given that u and v can be EPSILON away from the tmin() and tmax() values
		// We must correct them, in order to prevent rounding errors elsewhere
		u = Math.max(u, first.tmin());
		u = Math.min(u, first.tmax());
		v = Math.max(v, second.tmin());
		v = Math.min(v, second.tmax());
		
		return new OrderedPair<Double>(u, v); // u is the t value for the first line, v is for second
	}
	
	
	public double closestPoint(Double2D in, Double2D.Mutable out) {
		if (in == out) throw new IllegalArgumentException("The arguments 'in' and 'out' must not be the same");
		Line line = new Line(in.x(), in.y(), this.dy, -this.dx);
		OrderedPair<Double> tvals = getIntersectTValues(this, line, false);
		if (tvals == null) {
			System.err.println("no closest point: "+this+" AND "+in);
			return Double.POSITIVE_INFINITY;
		} else {
			out.x(this.getX(tvals.first));
			out.y(this.getY(tvals.first));
			return in.distanceTo(out);
		}
	}
	
	public double tmin() {
		return Double.NEGATIVE_INFINITY;
	}
	
	public double tmax() {
		return Double.POSITIVE_INFINITY;
	}
	
	public double length() {
		if (dx == 0 && dy == 0) return 0;
		return Double.POSITIVE_INFINITY;
	}
	
	/**
	 * Returns the dot product of (the direction vector of this line) dot (the direction vector from the start point of this line to the given pt) 
	 */
	public double dot(Double2D pt) {
		double dx = pt.x() - x;
		double dy = pt.y() - y;
		return Double2D.dot(this.dx, this.dy, dx, dy);
	}
	
	/**
	 * Returns the cross product of (the direction vector of this line) cross (the direction vector from the start point of this line to the given pt) 
	 */
	public double cross(Double2D pt) {
		double dx = pt.x() - x;
		double dy = pt.y() - y;
		return Double2D.cross(this.dx, this.dy, dx, dy);
	}
	
	/**
	 * <p>This line slices the given line into two parts and returns the results in a Pair.</p>
	 * <p>All lines are defined by an origin point, a direction, and a range of t values. 
	 * In this system the equation of a line is (origin + t*direction) from tmin to tmax.</p>
	 * <p>The direction determines how slicing is carried out. In the Pair returned, the first 
	 * element is the partial line (Ray/LineSeg) to the LEFT of this line (relative to the direction), 
	 * and the second element is to the RIGHT.</p> 
	 * <p>If this line does not intersect the provided line, one side of the Pair will be null. 
	 * Otherwise, both the left and right partial lines will include their intersection point 
	 * with this line. </p>
	 * <p>The direction of each partial line result will remain the same as the
	 * original, unsliced line. Rays can be oriented (left vs. right) differently than their
	 * direction of extension.</p>
	 * @param line - line to be clipped
	 * @return Pair of partial lines, first = left side, second = right side
	 */
	public OrderedPair<Line> slice(Line line) {
		if (line == null) return new OrderedPair<Line>(null, null);
		OrderedPair<Double> intersect = getIntersectTValues(this, line, true);
		
		if (intersect == null) {
			double deltaX = line.x - this.x;
			double deltaY = line.y - this.y;
			if (Double2D.cross(dx, dy, deltaX, deltaY) > 0) {
				return new OrderedPair<Line>(line, null); 
			} else {
				return new OrderedPair<Line>(null, line);
			}
		}
		
		double t = intersect.second;
		
		Line lower = null, upper = null;
		
		if (t >= line.tmin()) lower = line.redefine(line.tmin(), t);
		if (t <= line.tmax()) upper = line.redefine(t, line.tmax());
		
		if (Double2D.cross(this.dx, this.dy, line.dx, line.dy) > 0) {
			return new OrderedPair<Line>(upper, lower); 
		} else {
			return new OrderedPair<Line>(lower, upper);
		}
	}
	
	
	
	
	@Override
	public String toString() {
		return "Line[x="+x+", y="+y+", dx="+dx+", dy="+dy+", tmin="+tmin()+", tmax="+tmax()+"]";
	}

	@Override
	public Line copy() {
		return new Line(x, y, dx, dy);
	}
	
	public double getArea() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getPerimeter() {
		return length();
	}

	@Override
	public Double2D getCentroid() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Line outset(double amount) {
		return this.inset(-amount);
	}

	@Override
	public Line inset(double amount) {
		Double2D.Mutable work = this.getDirection().mutable();
		work.perpendicular().normalize().multiply(amount);
		return new Line(x + work.x(), y + work.y(), dx, dy);
	}

	@Override
	public boolean contains(Double2D pt) {
		Double2D.Mutable result = new Double2D.Mutable();
		this.closestPoint(pt, result);
		return result.equals(pt);
	}
	
	@Override
	public boolean contains(double x, double y) {
		return this.contains(new Double2D(x, y));
	}

	@Override
	public boolean contains(IShape other) {
		if (other instanceof Line) return contains((Line) other);
		return false;
	}
	
	public boolean contains(Line line) {
		if (line instanceof LineSeg) return contains((LineSeg) line);
		if (line instanceof Ray) return contains(line.toSegment(100000));
		return contains(line.toSegment(-100000, 100000));
	}
	
	public boolean contains(LineSeg seg) {
		Double2D.Mutable result = new Double2D.Mutable();
		Double2D.Mutable result2 = new Double2D.Mutable();
		
		seg.getStart(result);
		if (this.closestPoint(result, result2) > Double2D.EPSILON) return false;
		
		seg.getEnd(result);
		if (this.closestPoint(result, result2) > Double2D.EPSILON) return false;
		
		return true;
	}

	@Override
	public boolean intersects(IShape other) {
		if (other instanceof Circle) this.intersects((Circle) other);
		if (other instanceof Line) this.intersects((Line) other);
		if (other instanceof Rect) this.intersects((Rect) other);
		if (other instanceof Convex) this.intersects((Convex) other);
		throw new UnsupportedOperationException();
	}
	
	public boolean intersects(Circle circ) {
		return circ.intersects(this);
	}
	
	public boolean intersects(Line line) {
		OrderedPair<Double> vals = getIntersectTValues(this, line, true);
		return (vals != null);
	}

	public boolean intersects(Rect rect) {
		return rect.intersects(this);
	}
	
	public boolean intersects(Convex poly) {
		return poly.intersects(this);
	}

	@Override
	public Line clip(Line line) {		
		OrderedPair<Double> intersect = Line.getIntersectTValues(this, line, true);
		if (intersect == null) return line;
		
		if (this.getDirection().cross(line.getDirection()) >= 0) {
			return line.redefine(intersect.second, line.tmax());
		} else {
			return line.redefine(line.tmin(), intersect.second);
		}
	}

	@Override
	public Convex toPolygon(int numSides) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Rect getBounds() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Shape asAWTShape() {
		throw new UnsupportedOperationException();
	}

	public Double2D reflect(Double2D pt) {		
		final double dot = this.dot(pt);
		final double cross = -this.cross(pt);
				
		double x = this.x + this.dx * dot - this.dy * cross;
		double y = this.y + this.dy * dot + this.dx * cross;
		
		return pt.redefine(x, y);
	}
	
}
