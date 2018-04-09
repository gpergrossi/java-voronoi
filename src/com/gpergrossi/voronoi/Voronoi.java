package com.gpergrossi.voronoi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gpergrossi.util.geom.shapes.Convex;
import com.gpergrossi.util.data.storage.GrowingStorage;

public final class Voronoi {

	public static boolean DEBUG = false;
	public static boolean DEBUG_FINISH = false;
	
	protected Convex bounds;
	
	protected List<Site> sites;
	protected List<Edge> edges;
	protected List<Vertex> vertices;
		
	protected Voronoi(Convex bounds) {
		this.bounds = bounds;
		this.sites = new ArrayList<>();
	}
	
	public Convex getBounds() {
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

	
	
	protected void addSite(Site site) {
		this.sites.add(site);
	}
	
	protected void finalizeSites() {
		this.sites = Collections.unmodifiableList(sites);
	}

	private void setVertices(List<Vertex> vertices) {
		this.vertices = Collections.unmodifiableList(vertices);
	}

	protected void setVertices(GrowingStorage<Vertex> mutableVertices) {
		List<Vertex> vertices = new ArrayList<Vertex>(mutableVertices.size());
		for (Vertex v : mutableVertices) {
			vertices.add(v);
		}
		setVertices(vertices);
	}

	private void setEdges(List<Edge> edges) {
		this.edges = Collections.unmodifiableList(edges);
	}

	protected void setEdges(GrowingStorage<Edge> mutableEdges) {
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
