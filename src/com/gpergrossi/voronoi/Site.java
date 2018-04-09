package com.gpergrossi.voronoi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

import com.gpergrossi.util.geom.shapes.Convex;
import com.gpergrossi.util.geom.shapes.Polygon;
import com.gpergrossi.util.geom.vectors.Double2D;

public final class Site implements Comparable<Site> {

	private static final IntFunction<Double2D[]> VEC2_ARRAY_ALLOCATOR = new IntFunction<Double2D[]>() {
		public Double2D[] apply(int value) {
			return new Double2D[value];
		}
	};
	
	public final Voronoi voronoi;
	public final int index;
	public final Double2D point;
	
	public Object data;
	
	protected int numVertices;
	protected Vertex[] vertices;
	protected int numEdges;
	protected Edge[] edges;
	
	protected Convex polygon;
	
	protected boolean isFinished;
	protected boolean isBoundary;

	protected Site(Voronoi voronoi, int index, Double2D sitePoint) {
		this.voronoi = voronoi;
		this.index = index;
		this.point = sitePoint;
		this.edges = new Edge[8];
		this.vertices = new Vertex[8];
	}

	public int numVertices() {
		return numVertices;
	}
	
	public Iterable<Vertex> getVertices() {
		return new Iterable<Vertex>() {
			public Iterator<Vertex> iterator() {
				return new Iterator<Vertex>() {
					int index = 0;
					public boolean hasNext() {
						return index < numVertices;
					}
					public Vertex next() {
						if (index >= numVertices) throw new NoSuchElementException();
						return vertices[index++];
					}
				};
			}
		};
	}
	
	public int numEdges() {
		return numEdges;
	}
	
	public Iterable<Edge> getEdges() {
		return new Iterable<Edge>() {
			public Iterator<Edge> iterator() {
				return new Iterator<Edge>() {
					int index = 0;
					public boolean hasNext() {
						return index < numEdges;
					}
					public Edge next() {
						if (index >= numEdges) throw new NoSuchElementException();
						return edges[index++];
					}
				};
			}
		};
	}
	


	public List<Site> getNeighbors() {
		List<Site> neighbors = new ArrayList<>();
		for (Edge edge : getEdges()) {
			Site neighbor = edge.getNeighbor(this);
			if (neighbor != null) neighbors.add(neighbor);
		}
		return neighbors;
	}
	
	public Convex getPolygon() {
		if (polygon == null) {
			Double2D[] verts = Arrays.stream(vertices, 0, numVertices).map(vert -> vert.getPosition()).toArray(VEC2_ARRAY_ALLOCATOR);
			int count = Polygon.removeDuplicates(verts);
			Double2D[] copy = Polygon.copyArray(verts, count);
			Polygon.makeImmutable(copy);
			polygon = Convex.createDirect(copy);
		}
		return polygon;
	}

	public double getX() {
		return point.x();
	}
	
	public double getY() {
		return point.y();
	}
	
	public boolean isBoundary() {
		if (isFinished) return isBoundary;
		if (vertices == null) return true;
		for (Vertex v : vertices) {
			if (v.isBoundary) return true;
		}
		return false;
	}
	
	protected void finish(boolean isBoundary) {
		this.isBoundary = isBoundary();
		this.isFinished = true;
	}
	
	protected boolean hasVertex(Vertex vertex) {
		for (int i = 0; i < numVertices; i++) {
			if (vertices[i] == vertex) return true;
		}
		return false;
	}

	protected void addVertex(Vertex vertex) {
		if (numVertices >= vertices.length) this.vertices = Arrays.copyOf(vertices, vertices.length*2);
		vertices[numVertices++] = vertex;
	}
	
	protected boolean hasEdge(Edge edge) {
		for (int i = 0; i < numEdges; i++) {
			if (edges[i] == edge) return true;
		}
		return false;
	}
	
	protected void addEdge(Edge edge) {
		if (numEdges >= edges.length) this.edges = Arrays.copyOf(edges, edges.length*2);
		edges[numEdges++] = edge;
	}

	protected void sortVertices(double[] vertexValues) {
		sort(vertices, 0, numVertices, vertexValues);
	}

	protected void sortEdges(double[] edgeValues) {
		sort(edges, 0, numEdges, edgeValues);
	}

	/**
	 * A simple selection sort, almost all site arrays are less than 8 items long
	 */
	private static <T> void sort(T[] array, int start, int end, double[] values) {
		for (int i = start; i < end; i++) {
			int bestIndex = i;
			double bestValue = values[i];
			
			// Select best value in sub array from i to end
			for (int j = i+1; j < end; j++) {
				if (values[j] < bestValue) {
					bestIndex = j;
					bestValue = values[j];
				}
			}
			
			// Swap if not the same index
			if (bestIndex != i) {
				T swap = array[i];
				array[i] = array[bestIndex];
				array[bestIndex] = swap;
				values[bestIndex] = values[i];
				values[i] = bestValue;
			}
		}
	}
	
	Vertex getLastVertex() {
		if (numVertices == 0) return null;
		return vertices[numVertices-1];
	}

	@Override
	public int compareTo(Site o) {		
		if (point.y() < o.point.y()) return -1;
		if (point.y() > o.point.y()) return 1;

		if (point.x() < o.point.x()) return -1;
		if (point.x() > o.point.x()) return 1;
		
		return Integer.compare(this.hashCode(), o.hashCode());
	}
	
	@Override
	public String toString() {
		return "Site[ID="+index+", X="+point.x()+", Y="+point.y()+"]";
	}

	public int getID() {
		return index;
	}

}
