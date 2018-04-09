package com.gpergrossi.util.geom.shapes;

import java.awt.Shape;

import com.gpergrossi.util.geom.vectors.Double2D;

public interface IShape {
	
	/**
	 * Returns an identical but distinct copy of this shape. Same class.
	 * @return
	 */
	public IShape copy();
	
	/**
	 * Gets the area of this shape
	 * @return
	 */
	public double getArea();
	
	/**
	 * Gets the perimeter length of this shape
	 * @return
	 */
	public double getPerimeter();
	
	/**
	 * Gets the centroid of this shape
	 * @return
	 */
	public Double2D getCentroid();
	
	/**
	 * Returns a copy of this shape (perhaps a different class) with a constant perpendicular out-set applied
	 * @param amount - number of units of perpendicular out-set
	 * @return
	 */
	public IShape outset(double amount);
	
	/**
	 * Returns a copy of this shape (perhaps a different class) with a constant perpendicular in-set applied
	 * @param amount - number of units of perpendicular in-set
	 * @return
	 */
	public IShape inset(double amount);

	/**
	 * Returns true if the given point is 'inside' this shape
	 * @param pt
	 * @return
	 */
	public boolean contains(Double2D pt);

	/**
	 * Returns true if the given point is 'inside' this shape
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean contains(double x, double y);
	
	/**
	 * Returns true if the given shape is completely inside of this shape
	 * @param other
	 * @return
	 */
	public boolean contains(IShape other);
	
	/**
	 * Returns true if the given shape is touching or inside of this shape
	 * @param other
	 * @return
	 */
	public boolean intersects(IShape other);
	
	/**
	 * Reduces this line to the portion contained within this shape.
	 * The definition of "inside" depends on the shapes implementation, but
	 * for convex polygons this should be the region inside the shape following
	 * the counter-clockwise winding order.
	 * 
	 * @param line - the line to be clipped
	 * @return the resulting portion of the line fully within this shape, or null if nothing is left
	 */
	public Line clip(Line line);

	/**
	 * Creates a polygon or polygon approximation of this shape 
	 * @param numSides - the maximum number of sides of the output polygon, some shapes may ignore this restriction
	 * @return a polygon or polygon approximation of this shape
	 */
	public Polygon toPolygon(int numSides);
	
	/**
	 * Gets the axis-aligned bounding box of this shape
	 * @return
	 */
	public Rect getBounds();
	
	/**
	 * Gets the java.awt variant of this shape.
	 * @return
	 */
	public Shape asAWTShape();	
	
}
