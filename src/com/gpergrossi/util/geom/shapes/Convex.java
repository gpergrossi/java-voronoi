package com.gpergrossi.util.geom.shapes;

import java.awt.geom.Path2D;
import com.gpergrossi.util.geom.vectors.Double2D;

public class Convex extends Polygon {

	final Double2D[] vertices;
	private Double2D centroid;
	private double area = Double.NaN;
	private Path2D awtShape;
	private Rect bounds;
	
	/**
	 * Creates a polygon using the given array as its BACKING array.
	 * No testing is done to check if this is a valid convex polygon.
	 * The original array should not be modified after being given to
	 * the polygon, otherwise unpredictable behavior could be caused.
	 * @param verts
	 * @return
	 */
	public static Convex createDirect(Double2D[] verts) {
		return new Convex(verts);
	}
	
	private Convex(Double2D[] verts) {
		this.vertices = verts;
	}

	public double distanceToEdge(double x, double y) {
		double dist = Double.POSITIVE_INFINITY;
		Double2D pt = new Double2D(x, y);
		Double2D.Mutable hold = new Double2D.Mutable();
		for (LineSeg edge : getSides()) {
			double d = edge.closestPoint(pt, hold);
			dist = Math.min(dist, d);
		}
		return dist;
	}
	
	
	
	

	@Override
	public int getNumSides() {
		return this.vertices.length;
	}

	@Override
	public LineSeg getSide(int i) {
		if (i < 0 || i >= vertices.length) throw new IndexOutOfBoundsException();
		Double2D pt0 = vertices[i];
		Double2D pt1 = null;
		if (i+1 == vertices.length) pt1 = vertices[0];
		else pt1 = vertices[i+1];
		return new LineSeg(pt0.x(), pt0.y(), pt1.x(), pt1.y());
	}

	@Override
	public int getNumVertices() {
		return this.vertices.length;
	}

	@Override
	public Double2D getVertex(int i) {
		if (i < 0 || i >= vertices.length) throw new IndexOutOfBoundsException();
		return vertices[i];
	}

	@Override
	public boolean isConvex() {
		return true;
	}

	@Override
	public int getNumConvexParts() {
		return 1;
	}

	@Override
	public Convex getConvexPart(int i) {
		if (i != 0) throw new IndexOutOfBoundsException();
		return this;
	}
	
	
	
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Convex[count=").append(vertices.length).append(", verts={");
		
		for (int i = 0; i < vertices.length; i++) {
			sb.append('(').append(vertices[i].x()).append(',').append(vertices[i].y()).append(')');
			if (i < vertices.length-1) sb.append(", ");
		}
		
		sb.append("}]");
		return sb.toString();
	}
	
	@Override
	public Convex copy() {
		return this;
	}
	
	@Override
	public double getArea() {
		if (Double.isNaN(area)) area = calculateArea();
		if (area <= 0) System.err.println("Polygon has "+this.area+" area!");
		return area;
	}
	
	protected double calculateArea() {
		double area = 0;
		for (int i = 0; i < vertices.length; i++) {
			Double2D a = vertices[i];
			Double2D b = ((i+1 < vertices.length) ? vertices[i + 1] : vertices[0]);
			area += a.cross(b);
		}
		area /= 2;
		return area;
	}

	@Override
	public double getPerimeter() {
		double perimeter = 0;
		for (LineSeg side : getSides()) {
			perimeter += side.length();
		}
		return perimeter;
	}
	
	@Override
	public Double2D getCentroid() {
		if (centroid == null) {
			double cx = 0, cy = 0;
			double area = 0;
			for (int i = 0; i < vertices.length; i++) {
				Double2D a = vertices[i];
				Double2D b = ((i+1 < vertices.length) ? vertices[i + 1] : vertices[0]);
				double cross = a.cross(b);
				area += cross;
				cx += (a.x() + b.x()) * cross;
				cy += (a.y() + b.y()) * cross;
			}
			area /= 2;
			this.area = area;
			if (this.area <= 0) {
				System.err.println("Polygon has "+this.area+" area!");
				return null;
			}
			
			cx /= (area * 6);
			cy /= (area * 6);
			this.centroid = new Double2D(cx, cy);
		}
		return this.centroid;
	}
	
	@Override
	public Convex reflect(Line line) {
		Double2D[] reflectedVertices = new Double2D[vertices.length];
		for (int i = 0; i < vertices.length; i++) {
			reflectedVertices[vertices.length - 1 - i] = line.reflect(vertices[i]);
		}
		return new Convex(reflectedVertices);
	}
	
	@Override
	public Polygon outset(double amount) {
		return inset(-amount);
	}

	@Override
	public Polygon inset(double amount) {
		if (amount == 0) return this;
		
		Double2D[] verts = new Double2D[vertices.length];
		int i = 0;
		
		Line prevEdge = new LineSeg(vertices[vertices.length-1], vertices[0]).toLine().inset(amount);
		for (LineSeg edge : getSides()) {
			Line currEdge = edge.toLine().inset(amount);
			
			Double2D.Mutable result = new Double2D.Mutable();
			boolean intersecting = currEdge.intersect(result, prevEdge);
			
			if (intersecting) {
				verts[i++] = result.immutable();
			} else {
				edge.getStart(result);
				Double2D.Mutable outsetVector = edge.getDirection().mutable();
				outsetVector.perpendicular().normalize().multiply(amount);
				result.add(outsetVector);
				verts[i++] = result;
			}
			
			prevEdge = currEdge;
		}
		
		// Fix inset edge elimination
		if (amount > 0) {
			int count = Polygon.fixSelfIntersection(verts, verts, i, 0);
			verts = Polygon.copyArray(verts, count);
		}
		
		return Convex.createDirect(verts);
	}

	@Override
	public boolean contains(Double2D pt) {
		for (LineSeg edge : getSides()) {			
			double cross = edge.cross(pt);
			if (cross < 0) return false;
		}
		return true;
	}

	public boolean contains(double x, double y) {
		return contains(new Double2D(x, y));
	}
	
	@Override
	public boolean contains(IShape other) {
		if (other instanceof Circle) this.contains((Circle) other);
		if (other instanceof Line) this.contains((Line) other);
		if (other instanceof Rect) this.contains((Rect) other);
		if (other instanceof Convex) this.contains((Convex) other);
		throw new UnsupportedOperationException();
	}

	public boolean contains(Circle circ) {
		Double2D center = circ.getCentroid();
		for (LineSeg seg : this.getSides()) {
			Double2D norm = seg.getDirection().normalize();
			if (norm.cross(center) < circ.radius) return false;
		}
		return true;
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
		if (!this.contains(rect.minX(), rect.maxY())) return false;
		if (!this.contains(rect.maxX(), rect.minY())) return false;
		if (!this.contains(rect.maxX(), rect.maxY())) return false;
		return true;
	}
	
	public boolean contains(Convex polygon) {
		for (Double2D v : polygon.vertices) {
			if (!this.contains(v.x(), v.y())) return false;
		}
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
		if (this.contains(circ.getCentroid())) return true;
		for (LineSeg edge : getSides()) {
			if (circ.intersects(edge)) return true;
		}
		return false;
	}
	
	public boolean intersects(Line line) {
		Double2D.Mutable result = new Double2D.Mutable();
		
		// Check intersection with rectangle edges
		for (LineSeg seg : this.getSides()) {
			if (seg.intersect(result, line)) return true;	
		}
		
		// Line segments could be inside the rectangle
		if (line.length() < Double.POSITIVE_INFINITY) {
			line.getStart(result);				
			if (this.contains(result)) return true;
			line.getEnd(result);
			if (this.contains(result)) return true;
		}
		
		return false;
	}
	
	public boolean intersects(Rect rect) {
		// Rectangle inside polygon
		if (this.contains(rect.getCentroid())) return true;
		
		// Polygon inside rectangle
		if (rect.contains(this.getCentroid())) return true;
		
		// Intersection
		for (LineSeg seg : this.getSides()) {
			if (rect.intersects(seg)) return true;	
		}
			
		return false;
	}
	
	public boolean intersects(Convex poly) {
		// Rectangle inside polygon
		if (this.contains(poly.getCentroid())) return true;
		
		// Polygon inside rectangle
		if (poly.contains(this.getCentroid())) return true;
		
		// Intersection
		for (LineSeg seg : this.getSides()) {
			if (poly.intersects(seg)) return true;	
		}
			
		return false;
	}

	@Override
	public LineSeg clip(Line line) {
		for (LineSeg side : getSides()) {
			line = side.clip(line);						
		}
		
		if (line instanceof LineSeg) {
			return (LineSeg) line;
		}
		
		return null;
	}

	@Override
	public Convex toPolygon(int numSides) {
		return this;
	}

	@Override
	public Rect getBounds() {
		if (bounds == null) {
			double minX = Double.POSITIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY;
			double maxY = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < vertices.length; i++) {
				Double2D vert = vertices[i];
				minX = Math.min(minX, vert.x());
				maxX = Math.max(maxX, vert.x());
				minY = Math.min(minY, vert.y());
				maxY = Math.max(maxY, vert.y());
			}
			bounds = new Rect(minX, minY, maxX-minX, maxY-minY);
		}
		return bounds;
	}
	
	@Override
	public Path2D asAWTShape() {
		if (awtShape == null) {
			if (vertices.length < 3) {
				System.err.println("Polygon has only has "+vertices.length+" vertices!");
				return null;
			}
			
			Path2D path = new Path2D.Double();
			path.moveTo(vertices[0].x(), vertices[0].y());
			for (int i = 1; i < vertices.length; i++) {
				path.lineTo(vertices[i].x(), vertices[i].y());
			}
			path.closePath();
			awtShape = path;
		}
		return awtShape;
	}

}
