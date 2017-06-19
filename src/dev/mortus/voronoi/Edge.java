package dev.mortus.voronoi;

import dev.mortus.util.data.Pair;
import dev.mortus.util.data.storage.Storage;
import dev.mortus.util.data.storage.StorageItem;
import dev.mortus.util.math.geom2d.LineSeg;
import dev.mortus.util.math.vectors.Double2D;

public class Edge implements StorageItem, Comparable<Edge> {
	
	private Integer storageIndex;
	private Storage<?> storage;
	
	protected Pair<Site> sites;
	protected Pair<Vertex> vertices;
	
	protected Double2D center;
	protected LineSeg lineSeg;
	
	protected Edge(Vertex start, Vertex end, Site left, Site right) {
		this.vertices = new Pair<>(start, end);
		this.sites = new Pair<>(left, right);
	}
	
	Edge(ShoreBreakpoint bp, Vertex start) {
		this(start, null, bp.getArcLeft().getSite(), bp.getArcRight().getSite());
	}
	
	void redefine(Vertex start, Vertex end) {
		if (getStart() == start && getEnd() == end) return;
		this.vertices = new Pair<>(start, end);
		this.center = null;
		this.lineSeg = null;
	}

	void combineWith(HalfEdge twin) {
		if (this.getEnd() == null) throw new RuntimeException("Cannot combine, edge has null end");
		if (twin.getEnd() == null) throw new RuntimeException("Cannot combine, twin has null end");
		this.redefine(twin.getEnd(), this.getEnd());
	}
	
	void finish(Vertex end) {
		redefine(getStart(), end);
	}
	
	boolean isHalf() {
		return false;
	}
	
	public LineSeg toLineSeg() {
		return new LineSeg(vertices.first.x, vertices.first.y, vertices.second.x, vertices.second.y);
	}
	
	public Double2D getCenter() {
		if (center == null) {
			double cx = (vertices.first.x + vertices.second.x) / 2.0;
			double cy = (vertices.first.y + vertices.second.y) / 2.0;
			center = new Double2D(cx, cy);
		}
		return center;
	}
	
	public boolean isFinished() {
		return vertices.size() == 2;
	}

	public Pair<Vertex> getVertices() { return vertices; }
	public Vertex getStart() { return vertices.first; }
	public Vertex getEnd() { return vertices.second; }
	
	public Pair<Site> getSites() { return sites; }
	public Site getSiteLeft() { return sites.first; }
	public Site getSiteRight() { return sites.second; }

	@Override
	public int compareTo(Edge o) {
		return this.hashCode() - o.hashCode();
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

	public Site getNeighbor(Site site) {
		if (sites.first == site) return sites.second;
		if (sites.second == site) return sites.first;
		System.err.println("Looking for "+site+" in "+sites);
		return null;
	}
		
}
