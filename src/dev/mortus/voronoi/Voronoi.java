package dev.mortus.voronoi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import dev.mortus.util.data.storage.GrowingStorage;
import dev.mortus.util.math.geom2d.Rect;

public class Voronoi {

	/** 
	 * A very small number that is still at a good precision in the floating point
	 * format. All inter-site distances should be much larger than this (> 100x for safety)
	 */
	public static boolean DEBUG = false;
	public static boolean DEBUG_FINISH = false;
	
	protected Rect bounds;
	
	protected List<Site> sites;
	protected List<Edge> edges;
	protected List<Vertex> vertices;
	
	protected Voronoi() {}
	
	Voronoi(Rect bounds) {
		this.bounds = bounds;
	}
	
	public Rect getBounds() {
		return bounds;
	}

	public List<Site> getSites() {
		return sites;
	}

	public Site getSite(int id) {
		return sites.get(id);
	}
	
	public List<Edge> getEdges() {
		return edges;
	}

	public List<Vertex> getVertices() {
		return vertices;
	}

	public int numSites() {
		return sites.size();
	}

	
	
	private void setSites(List<Site> sites) {
		this.sites = Collections.unmodifiableList(sites);
	}

	protected void setMutableSites(Site[] sitesArr) {
		List<Site> sites = new ArrayList<>(sitesArr.length);
		for (Site s : sitesArr) {
			sites.add(s);
		}
		setSites(sites);
	}

	private void setVertices(List<Vertex> vertices) {
		this.vertices = Collections.unmodifiableList(vertices);
	}

	protected void setMutableVertices(GrowingStorage<Vertex> mutableVertices) {
		List<Vertex> vertices = new ArrayList<Vertex>(mutableVertices.size());
		for (Vertex v : mutableVertices) {
			vertices.add(v);
		}
		setVertices(vertices);
	}

	private void setEdges(List<Edge> edges) {
		this.edges = Collections.unmodifiableList(edges);
	}

	protected void setMutableEdges(GrowingStorage<Edge> mutableEdges) {
		List<Edge> edges = new ArrayList<Edge>(mutableEdges.size());
		for (Edge e : mutableEdges) {
			edges.add(e);
		}
		setEdges(edges);
	}

	public Voronoi relax(VoronoiBuilder builder) {
		builder.clearSites(true);
		for (Site s : sites) {
			builder.addSite(s.getPolygon().getCentroid());
		}
		return builder.build();
	}
	
}
