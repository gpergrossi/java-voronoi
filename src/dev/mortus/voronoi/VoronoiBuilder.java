package dev.mortus.voronoi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dev.mortus.util.data.StableArrayList;
import dev.mortus.util.math.geom2d.Rect;
import dev.mortus.util.math.vectors.Double2D;

public class VoronoiBuilder {

	private StableArrayList<Double2D> sites;
	private double padding = 5.0;
	private Rect bounds = null;
	private boolean userBounds = false;
	
	public VoronoiBuilder() {
		this(40);
	}
	
	public VoronoiBuilder(int initialCapacity) {
		sites = new StableArrayList<>(t -> new Double2D[t], initialCapacity);
	}

	public int addSite(Double2D point) {
		if (userBounds && !bounds.contains(point.x(), point.y())) {
			return -1;
		}
		int index = sites.put(point);
		boundsAddPoint(point);
		return index;
	}
	
	public int addSiteSafe(Double2D point) {
		if (userBounds && !bounds.contains(point.x(), point.y())) {
			return -1;
		}
		for (Double2D s : sites) {
			if (point.distanceTo(s) < 1) return -1;
		}
		int index = sites.put(point);
		boundsAddPoint(point);
		return index;
	}

	public void removeSite(Double2D site) {
		sites.remove(site);
	}
	
	public void removeSite(int siteID) {
		sites.remove(siteID);
	}

	public void clearSites(boolean keepBounds) {
		sites.clear();
		if (!keepBounds) {
			bounds = null;
			userBounds = false;
		} else {
			userBounds = true;
		}
	}
	
	public void clearSites() {
		clearSites(false);
	}
	
	public void shrink() {
		sites.shrink();
	}
	
	public VoronoiWorker getBuildWorker() {
		if (sites.size() == 0) throw new RuntimeException("Cannot construct diagram with no sites.");
		return new VoronoiWorker(getBounds(), getSiteArray());
	}
	
	private Double2D[] getSiteArray() {
		Double2D[] siteArray = new Double2D[sites.size()];
		siteArray = sites.toArray(siteArray);
		return siteArray;
	}
	
	/**
	 * Sets the bounds of this diagram
	 * All current and future points outside these bounds will be removed.
	 * @param bounds
	 */
	public void setBounds(Rect bounds) {
		this.bounds = bounds;
		this.userBounds = true;
		for (Double2D v : sites) {
			if (!bounds.contains(v.x(), v.y())) {
				removeSite(v);
			}
		}
	}
	
	public Rect getBounds() {
		return bounds;
	}
	
	private void boundsAddPoint(Double2D point) {
		Rect pointRect = new Rect(point.x(), point.y(), 0, 0);
		if (!userBounds) pointRect.expand(padding);
		if (bounds == null) {
			bounds = pointRect;
		} else {
			bounds.union(pointRect);
		}
	}

	public List<Double2D> getSites() {
		return sites;
	}

	public Voronoi build() {
		VoronoiWorker w = this.getBuildWorker();
		while (!w.isDone()) {
			w.doWork(-1);
		}
		return w.getResult();
	}


	public void savePoints() throws IOException {
		FileOutputStream fos = new FileOutputStream("saved");
		DataOutputStream dos = new DataOutputStream(fos);
		
		List<Double2D> packed = new ArrayList<>();
		sites.iterator().forEachRemaining(e -> packed.add(e));
		
		dos.writeInt(packed.size());
		for (Double2D site : packed) {
			dos.writeDouble(site.x());
			dos.writeDouble(site.y());
		}

		System.out.println("Saved "+packed.size()+" sites");
		dos.close();
	}
	
	public void loadPoints() throws IOException {		
		FileInputStream fis = new FileInputStream("saved");
		DataInputStream dis = new DataInputStream(fis);
		
		int numSites = dis.readInt();
		for (int i = 0; i < numSites; i++) {
			double x = dis.readDouble();
			double y = dis.readDouble();
			
			addSite(new Double2D(x,y));
		}
		
		dis.close();
		System.out.println("Loaded "+numSites+" sites");
	}
	
}
