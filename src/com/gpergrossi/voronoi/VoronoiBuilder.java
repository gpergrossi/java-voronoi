package com.gpergrossi.voronoi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.gpergrossi.util.geom.shapes.Convex;
import com.gpergrossi.util.geom.shapes.Rect;
import com.gpergrossi.util.geom.vectors.Double2D;

import java.util.TreeMap;

public class VoronoiBuilder {

	private int nextSiteIndex = 0;
	private Map<Integer, Double2D> sites;
	private double padding = 5.0;
	
	private Convex bounds = null;
	private Rect defaultBounds = null;
	private boolean enforceBounds = false;
	
	public VoronoiBuilder() {
		this(40);
	}
	
	public VoronoiBuilder(int initialCapacity) {
		sites = new TreeMap<>();
	}
	
	private int internalAddSite(Double2D point) {
		int index = nextSiteIndex;
		sites.put(nextSiteIndex++, point);
		return index;
	}

	public int addSite(Double2D point) {
		if (!Double.isFinite(point.x()) || !Double.isFinite(point.y())) throw new IllegalArgumentException("Non-finite point: "+point);
		if (enforceBounds && !bounds.contains(point.x(), point.y())) {
			System.err.println("site rejected by bounds: "+point+", "+bounds);
			return -1;
		}		
		if (!enforceBounds) boundsAddPoint(point);
		return internalAddSite(point);
	}
	

	public int addSiteSafe(Double2D point, double minDistance) {
		if (enforceBounds && !bounds.contains(point.x(), point.y())) {
			return -1;
		}
		for (Double2D s : sites.values()) {
			if (point.distanceTo(s) < minDistance) return -1;
		}
		if (!enforceBounds) boundsAddPoint(point);
		return internalAddSite(point);
	}

	public int findSiteIndex(Double2D site) {
		Iterator<Entry<Integer, Double2D>> iter = sites.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, Double2D> entry = iter.next();
			if (entry.getValue() == site) return entry.getKey();
		}
		return -1;
	}
	
	public void removeSite(int siteID) {
		sites.remove(siteID);
		if (sites.size() == 0) nextSiteIndex = 0;
	}

	public void clearSites(boolean keepBounds) {
		sites.clear();
		nextSiteIndex = 0;
		if (!keepBounds) {
			bounds = null;
			defaultBounds = null;
			enforceBounds = false;
		} else {
			enforceBounds = true;
		}
	}
	
	public void clearSites() {
		clearSites(false);
	}
	
	public VoronoiWorker getBuildWorker() {
		if (sites.size() == 0) throw new RuntimeException("Cannot construct diagram with no sites.");
		Voronoi newDiagram = new Voronoi(bounds);
		for (Entry<Integer, Double2D> entry : sites.entrySet()) {
			Site site = new Site(newDiagram, entry.getKey(), entry.getValue());
			newDiagram.addSite(site);
		}
		return new VoronoiWorker(newDiagram);
	}
	
	/**
	 * Sets the bounds of this diagram.
	 * All current and future points outside these bounds will be removed.
	 * @param bounds
	 */
	public void setBounds(Convex bounds) {
		this.bounds = bounds;
		this.enforceBounds = true;
		
		final Iterator<Entry<Integer, Double2D>> iter = sites.entrySet().iterator();
		while (iter.hasNext()) {
			final Entry<Integer, Double2D> entry = iter.next();
			final Double2D point = entry.getValue();
			if (!bounds.contains(point.x(), point.y())) iter.remove();
		}
		if (sites.size() == 0) nextSiteIndex = 0;
	}
	
	public void setBounds(Rect bounds) {
		this.setBounds(bounds.toPolygon(4));
	}
	
	public Convex getBounds() {
		if (enforceBounds) return bounds;
		else return defaultBounds.toPolygon(4);
	}
	
	private void boundsAddPoint(Double2D point) {
		Rect pointRect = new Rect(point.x(), point.y(), 0, 0).outset(padding);
		if (defaultBounds == null) {
			defaultBounds = pointRect;
		} else {
			defaultBounds.union(pointRect);
		}
	}

	public Collection<Double2D> getSites() {
		return new ArrayList<Double2D>(sites.values());
	}

	public Voronoi build() {
		VoronoiWorker w = this.getBuildWorker();
		while (!w.isDone()) {
			w.doWork(-1);
		}
		return w.getResult();
	}


	public void savePoints() throws IOException {
		try (
				FileOutputStream fos = new FileOutputStream("saved");
				DataOutputStream dos = new DataOutputStream(fos);
		) {
			if (bounds == null) {
				System.out.println("Writing bounds with 0 vertices:");
				dos.writeInt(0);
			} else {
				System.out.println("Writing bounds with "+bounds.getNumVertices()+" vertices:");
				dos.writeInt(bounds.getNumVertices());
				Iterable<Double2D> verts = bounds.getVertices();
				for (Double2D vert : verts) {
					dos.writeDouble(vert.x());
					dos.writeDouble(vert.y());
					System.out.println("  Bounds vertex: "+vert.x()+", "+vert.y());
				}
			}
			System.out.println("Writing "+sites.size()+" sites:");
			dos.writeInt(sites.size());
			for (Double2D site : sites.values()) {
				dos.writeDouble(site.x());
				dos.writeDouble(site.y());
				System.out.println("  Site: "+site.x()+", "+site.y());
			}
			System.out.println("Saved "+sites.size()+" sites");
		}
	}
	
	public void loadPoints() throws IOException {
		try (
				FileInputStream fis = new FileInputStream("saved");
				DataInputStream dis = new DataInputStream(fis);
		) {
			int numVerts = dis.readInt();
			if (numVerts > 0) {
				System.out.println("Reading bounds with "+numVerts+" vertices:");
				Double2D[] verts = new Double2D[numVerts];
				for (int i = 0; i < numVerts; i++) {
					double x = dis.readDouble();
					double y = dis.readDouble();
					verts[i] = new Double2D(x, y);
					System.out.println("  Bounds vertex: "+x+", "+y);
				}
				setBounds(Convex.createDirect(verts));
			}
			
			int numSites = dis.readInt();
			System.out.println("Reading "+numSites+" sites:");
			for (int i = 0; i < numSites; i++) {
				double x = dis.readDouble();
				double y = dis.readDouble();
				
				addSite(new Double2D(x,y));
				System.out.println("  Site: "+x+", "+y);
			}
			System.out.println("Loaded "+numSites+" sites");
		}
	}
	
}
