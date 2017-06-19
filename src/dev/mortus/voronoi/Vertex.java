package dev.mortus.voronoi;

import java.awt.geom.Point2D;
import java.util.Arrays;

import dev.mortus.util.data.storage.Storage;
import dev.mortus.util.data.storage.StorageItem;
import dev.mortus.util.math.vectors.Double2D;

public class Vertex implements Comparable<Vertex>, StorageItem {

	public final boolean isBoundary;
	public final double x, y;
	
	protected int numSites;
	protected Site[] sites;
	protected int numEdges;
	protected Edge[] edges;
	
	protected String debug = "";
	
	private Integer storageIndex;
	private Storage<?> storage;

	Vertex(double x, double y, boolean isBoundary) {
		this.x = x;
		this.y = y;
		this.isBoundary = isBoundary;
		this.edges = new Edge[8];
		this.sites = new Site[8];
	}
	
	Vertex(double x, double y) {
		this(x, y, false);
	}
	
	Vertex(Double2D pos) {
		this(pos.x(), pos.y(), false);
	}

	public Double2D toVec2() {
		return new Double2D(x, y);
	}
	
	public Point2D toPoint2D() {
		return new Point2D.Double(x, y);
	}
	
	boolean hasSite(Site site) {
		for (int i = 0; i < numSites; i++) {
			if (sites[i] == site) return true;
		}
		return false;
	}

	void addSite(Site site) {
		if (numSites >= sites.length) this.sites = Arrays.copyOf(sites, sites.length*2);
		sites[numSites++] = site;
	}

	boolean hasEdge(Edge edge) {
		for (int i = 0; i < numEdges; i++) {
			if (edges[i] == edge) return true;
		}
		return false;
	}
	
	void addEdge(Edge edge) {
		if (numEdges >= edges.length) this.edges = Arrays.copyOf(edges, edges.length*2);
		edges[numEdges++] = edge;
	}

	@Override
	public void setStorageIndex(Storage<?> storage, int index) {
		if (this.storage == null || this.storage == storage) {
			this.storage = storage;
			this.storageIndex = index;
			return;
		}
		throw new RuntimeException("The storage saved is "+this.storage+", trying to store additional "+storage);
	}

	@Override
	public Integer getStorageIndex(Storage<?> storage) {
		if (this.storage == storage) return this.storageIndex;
		return null;
	}

	@Override
	public void clearStorageIndex(Storage<?> storage) {
		if (this.storage == storage) this.storage = null;
	}
	
	@Override
	public int compareTo(Vertex o) {
		if (y < o.y) return -1;
		if (y > o.y) return 1;
		
		if (x < o.x) return -1;
		if (x > o.x) return 1;
		
		return Integer.compare(this.hashCode(), o.hashCode());
	}

	@Override
	public String toString() {
		return "Vertex[x="+x+", y="+y+"]";
	}

}
