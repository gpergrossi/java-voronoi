package com.gpergrossi.util.geom.shapes;

import java.awt.geom.Path2D;

import com.gpergrossi.util.geom.vectors.Double2D;

public class Concave extends Polygon implements IShape {
	
	final Double2D[] vertices;
	private Convex[] convexes;
	
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
	public static Concave createDirect(Double2D[] verts) {
		return new Concave(verts);
	}
	
	private Concave(Double2D[] verts) {
		this.vertices = verts;
		this.decomposeToConvex();
	}
	
	private Concave(Double2D[] verts, Convex[] convexes) {
		this.vertices = verts;
		this.convexes = convexes;
	}

	/**
	 * Creates a minimum list of convex polygons that equal the area of this concave polygon
	 */
	private void decomposeToConvex() {
		//TODO
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
		return false;
	}

	@Override
	public int getNumConvexParts() {
		return convexes.length;
	}

	@Override
	public Convex getConvexPart(int i) {
		if (i < 0 || i >= convexes.length) throw new IndexOutOfBoundsException();
		return convexes[i];
	}
	
	
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Concave[count=").append(vertices.length).append(", verts={");
		
		for (int i = 0; i < vertices.length; i++) {
			sb.append('(').append(vertices[i].x()).append(',').append(vertices[i].y()).append(')');
			if (i < vertices.length-1) sb.append(", ");
		}
		
		sb.append("}]");
		return sb.toString();
	}
	
	@Override
	public Concave copy() {
		Concave copy = new Concave(this.vertices, this.convexes);
		copy.area = this.area;
		copy.bounds = this.bounds;
		copy.awtShape = this.awtShape;
		return copy;
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
	public Concave reflect(Line line) {
		Double2D[] reflectedVertices = new Double2D[vertices.length];
		for (int i = 0; i < vertices.length; i++) {
			reflectedVertices[i] = line.reflect(vertices[i]);
		}
		Convex[] reflectedConvexes = new Convex[convexes.length];
		for (int i = 0; i < convexes.length; i++) {
			reflectedConvexes[i] = convexes[i].reflect(line);
		}
		return new Concave(reflectedVertices, reflectedConvexes);
	}
	
	@Override
	public Polygon outset(double amount) {
		return inset(-amount);
	}

	@Override
	public Polygon inset(double amount) {
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
		return Polygon.create(verts);
	}

	@Override
	public Concave toPolygon(int numSides) {
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

	@Override
	public boolean contains(double x, double y) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(Double2D pt) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(IShape other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean intersects(IShape other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public LineSeg clip(Line line) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
