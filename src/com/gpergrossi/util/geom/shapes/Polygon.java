package com.gpergrossi.util.geom.shapes;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.gpergrossi.util.geom.vectors.Double2D;

public abstract class Polygon implements IShape {
	
	/**
	 * Removes consecutive duplicate vertices, reorganizing the array so that
	 * the valid vertices are in the first N slots. Any vertices that are invalid
	 * will be swapped to the end of the array. Returns the number of valid vertices, N.
	 * @param vertices - array of vertices
	 * @return number of valid vertices
	 */
	public static int removeDuplicates(Double2D[] vertices) {	
		int numValid = 0;
		Double2D prevVertex = vertices[vertices.length-1];
		
		for (int i = 0; i < vertices.length; i++) {
			Double2D currVertex = vertices[i];
			
			// Distance > EPSILON?
			if (!currVertex.equals(prevVertex)) {
				Double2D swap = vertices[numValid];
				vertices[numValid] = vertices[i];
				vertices[i] = swap;
				numValid++;
				
				// Previous valid vertex
				prevVertex = currVertex;
			}
		}
		return numValid;
	}
	
	/**
	 * Makes each vertex in the array of vertices immutable.
	 * @param vertices
	 */
	public static void makeImmutable(Double2D[] vertices) {
		for (int i = 0; i < vertices.length; i++) {
			vertices[i] = vertices[i].immutable();
		}
	}
	
	public static Double2D[] copyArray(Double2D[] vertices) {
		return copyArray(vertices, vertices.length);
	}
	
	public static Double2D[] copyArray(Double2D[] vertices, int length) {
		Double2D[] verts = new Double2D[length];
		System.arraycopy(vertices, 0, verts, 0, length);
		return verts;
	}
		
	/**
	 * Calculates the area of the polygon described by the vertices of the array.
	 * Area will be positive for counter-clockwise winding order and negative for clockwise winding order.
	 * If the polygon is 'twisted' (has edges crossing each other) then its area will be the areas of the
	 * negative (clockwise) and positive (counterclockwise) sections.
	 * @param vertices - array of vertices
	 */
	public static double calculateArea(Double2D[] vertices) {
		return calculateArea(vertices, vertices.length);
	}
	
	/**
	 * Calculates the area of the polygon described by the first {@code count} vertices of the array.
	 * Area will be positive for counter-clockwise winding order and negative for clockwise winding order.
	 * If the polygon is 'twisted' (has edges crossing each other) then its area will be the areas of the
	 * negative (clockwise) and positive (counterclockwise) sections.
	 * @param vertices - array of vertices
	 * @param count - effective length of the array
	 */
	public static double calculateArea(Double2D[] vertices, int count) {
		if (vertices.length < count) throw new IllegalArgumentException("The array must have at least as many elements as count describes");
		if (count < 3) throw new NoSuchElementException("Vertex array must have at least 3 vertices");
		
		double area = 0;
		Double2D prev = vertices[count-1];
		
		for (int i = 0; i < count; i++) {
			Double2D curr = vertices[i];
			area += prev.cross(curr);
			prev = curr;
		}
		
		area /= 2;
		return area;
	}

	public static boolean checkTotalTurningAngle(Double2D[] vertices) {
		return checkIsConvexCCW(vertices, vertices.length);
	}
	
	public static boolean checkIsConvexCCW(Double2D[] vertices, int count) {
		return turningAngleSum(vertices, count) < 2.0*Math.PI+0.00001; 
	}

	public static double turningAngleSum(Double2D[] vertices) {
		return turningAngleSum(vertices, vertices.length);
	}
	
	/**
	 * Counts the total "turn angle" for each corner of the polygon. This is the positive (counter clockwise) angle
	 * from each edge to the next. Any proper convex polygon should 
	 * @param vertices
	 * @param count
	 * @return
	 */
	public static double turningAngleSum(Double2D[] vertices, int count) {
		if (vertices.length < count) throw new IllegalArgumentException("The array must have at least as many elements as count describes");
		if (count < 3) throw new NoSuchElementException("Vertex array must have at least 3 vertices");
		
		double turningSum = 0;
		
		Double2D prevVert = vertices[count-1];
		double prevEdgeAngle = prevVert.subtract(vertices[count-2]).angle();
		
		for (int i = 0; i < count; i++) {
			double currEdgeAngle = vertices[i].subtract(prevVert).angle();
			
			double turn = currEdgeAngle - prevEdgeAngle;
			if (turn < 0.0) turn += 2.0*Math.PI;
			turningSum += turn;
			
			prevVert = vertices[i];
			prevEdgeAngle = currEdgeAngle;
		}
		
		return turningSum;
	}
	
	public static boolean checkSelfIntersect(Double2D[] vertices) {
		return checkSelfIntersect(vertices, vertices.length);
	}
	
	public static boolean checkSelfIntersect(Double2D[] vertices, int count) {
		if (vertices.length < count) throw new IllegalArgumentException("The array must have at least as many elements as count describes");
		if (count < 3) throw new NoSuchElementException("Vertex array must have at least 3 vertices");
		
		LineSeg[] sides = new LineSeg[count];
		Double2D prevVertex = vertices[count-1];
		for (int i = 0; i < count; i++) {
			sides[i] = new LineSeg(prevVertex, vertices[i]);
			
			// Check intersection with previous sides
			for (int j = 0; j < i-1; j++) {
				if (i == count-1 && j == 0) continue;
				if (sides[i].intersects(sides[j])) return true;
			}
			
			prevVertex = vertices[i];
		}
		
		return false;
	}
	
	public static int fixSelfIntersection(Double2D[] output, Double2D[] vertices, int count, int start) {
		if (vertices.length < count) throw new IllegalArgumentException("The array must have at least as many elements as count describes");
		if (count < 3) throw new NoSuchElementException("Vertex array must have at least 3 vertices");
		
		LineSeg[] sides = new LineSeg[count];		
		Double2D prevVertex = vertices[count-1];
		for (int i = 0; i < count; i++) {
			sides[i] = new LineSeg(prevVertex, vertices[i]);
			prevVertex = vertices[i];
		}

		boolean[] visited = new boolean[count];
		Double2D.Mutable result = new Double2D.Mutable();
		int outputCount = 0;
		int visitIndex = start;
		
		// Visit sides ass necessary, never visit the same side twice
		while (!visited[visitIndex]) {
			// Mark current index as visited
			visited[visitIndex] = true;

//			System.out.println("Visiting edge: ["+visitIndex+"]="+sides[visitIndex]);
			
			// Look for the intersection that occurs latest after the current edge
			int prevIndex = (visitIndex == 0 ? (count-1) : (visitIndex-1));
			int nextIndex = (visitIndex >= (count-1) ? 0 : (visitIndex+1));
			
			int nextVisitIndex = -1;
			Double2D intersect = null;
			for (int explore = nextIndex; explore != prevIndex; explore = (explore >= count-1 ? 0 : explore+1) ) {
				
				// Do not consider edges that point the wrong way, these edges would form a concave corner
				if (sides[visitIndex].getDirection().cross(sides[explore].getDirection()) < 0) continue;
				
				// Make note of the most recent future edge that intersects this edge
				if (sides[visitIndex].intersect(result, sides[explore])) {
					nextVisitIndex = explore;
					intersect = result.immutable();
				}
				
			}
			
			// If there were no intersecting future edges with the correct orientation,
			// Move on to the very next side of the polygon
			// Do not link to previous, do not add vertex
			if (nextVisitIndex == -1) {
				visitIndex = (visitIndex >= count-1 ? 0 : visitIndex+1);
				continue;				
			}
			
			// Add intersect to output list
			output[outputCount++] = intersect;

//			System.out.println("Intersected with edge: ["+nextVisitIndex+"]="+sides[nextVisitIndex]+" at "+intersect);
			
			// Move to the side that was intersected
			visitIndex = nextVisitIndex;
		}

		return outputCount;
	}
	
	/**
	 * Reverses the order of the vertices in the array
	 * @param vertices - array of vertices
	 */
	public static void invert(Double2D[] vertices) {
		invert(vertices, vertices.length);
	}
	
	/**
	 * Reverses the order of the vertices in the first {@code count} indices of the array
	 * @param vertices - array of vertices
	 * @param count - effective length of the array
	 */
	public static void invert(Double2D[] vertices, int count) {
		if (vertices.length < count) throw new IllegalArgumentException("The array must have at least as many elements as count describes");
		if (count < 3) throw new NoSuchElementException("Vertex array must have at least 3 vertices");
		
		int i = 0;
		int j = count-1;
		Double2D swap;
		
		while (i < j) {
			swap = vertices[i];
			vertices[i] = vertices[j];
			vertices[j] = swap;			
			i++;
			j--;
		}
	}
	
	/**
	 * This method creates a polygon after sanitizing the input vertices.
	 * If this method was called with a vertex array, the vertex array will
	 * be modified.
	 * @param vertices - input vertices or vertex array (which will be modified)
	 * @return a polygon
	 */
	public static Polygon create(Double2D... vertices) {
		vertices = copyArray(vertices);
		return createInternal(vertices, true);
	}

	/**
	 * Creates a polygon from this list of vertices. This method is safe but wasteful. 
	 * It creates a copy of the given list before sanitizing the input and finally producing
	 * a Polygon object. An intermediate array is created and discarded.
	 * @param vertices - list of vertices (will not be modified)
	 * @return a polygon
	 */
	public static Polygon create(List<Double2D> vertices) {
		Double2D[] array = new Double2D[vertices.size()];
		vertices.toArray(array);
		return createInternal(array, true);
	}
	
	private static Polygon createInternal(Double2D[] vertices, boolean fixDirection) {
		makeImmutable(vertices);
		int count = removeDuplicates(vertices);
		double area = calculateArea(vertices, count);
		
		if (area < 0) {
			if (!fixDirection) {
				throw new IllegalArgumentException("Vertices not in counter-clockwise order");
			} else {
				invert(vertices, count);
			}
		}
		
		// Check if original array was Convex
		if (checkIsConvexCCW(vertices, count)) {
			return Convex.createDirect(copyArray(vertices, count));
		}
		
		// Check if self-intersecting
		if (checkSelfIntersect(vertices, count)) {
			throw new IllegalArgumentException("Vertices describe a self-intersecting polygon! (Edges cross each other)");
		}
		
		// Otherwise, Concave
		return Concave.createDirect(copyArray(vertices, count));
	}
	
	@Override
	public Polygon inset(double amount) {
		if (this instanceof Convex) return ((Convex) this).inset(amount);
		if (this instanceof Concave) return ((Concave) this).inset(amount);
		throw new UnsupportedOperationException();
	}

	@Override
	public Polygon outset(double amount) {
		if (this instanceof Convex) return ((Convex) this).outset(amount);
		if (this instanceof Concave) return ((Concave) this).outset(amount);
		throw new UnsupportedOperationException();
	}

	public abstract Polygon reflect(Line line);
	
	public abstract int getNumSides();
	public abstract LineSeg getSide(int i);
	
	public abstract int getNumVertices();
	public abstract Double2D getVertex(int i);
	
	public abstract boolean isConvex();
	public abstract int getNumConvexParts();
	public abstract Convex getConvexPart(int i);
	
	public Iterable<LineSeg> getSides() {
		return new Iterable<LineSeg>() {
			public Iterator<LineSeg> iterator() {
				return new Iterator<LineSeg>() {
					int index = 0;
					public boolean hasNext() {
						return index < getNumSides();
					}
					public LineSeg next() {
						if (index >= getNumSides()) throw new NoSuchElementException();
						return getSide(index++);
					}
				};
			}
		};
	}
	
	public Iterable<Double2D> getVertices() {
		return new Iterable<Double2D>() {
			public Iterator<Double2D> iterator() {
				return new Iterator<Double2D>() {
					int index = 0;
					public boolean hasNext() {
						return index < getNumVertices();
					}
					public Double2D next() {
						if (index >= getNumVertices()) throw new NoSuchElementException();
						return getVertex(index++);
					}
				};
			}
		};
	}
	
	public Iterable<Convex> getConvexParts() {
		return new Iterable<Convex>() {
			public Iterator<Convex> iterator() {
				return new Iterator<Convex>() {
					int index = 0;
					public boolean hasNext() {
						return index < getNumConvexParts();
					}
					public Convex next() {
						if (index >= getNumConvexParts()) throw new NoSuchElementException();
						return getConvexPart(index);
					}
				};
			}
		};
	}
	
}
