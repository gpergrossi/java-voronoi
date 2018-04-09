package com.gpergrossi.util.geom.shapes;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.gpergrossi.util.geom.vectors.Double2D;

public class Spline {
	
	private double catmullRomAlpha = 0.5;
	TreeMap<Double, Double2D> points;
	private Segment segmentCache;
	
	public Spline() {
		this.points = new TreeMap<>();
	}
	
	/**
	 * Should be called whenever any change is made to the curve's shape.
	 * This clears the segment cache that saves a little time when requesting values.
	 */
	private void onChange() {
		segmentCache = null;
	}
	
	public double getCatmullRomAlpha() {
		return catmullRomAlpha;
	}
	
	public void setCatmullRomAlpha(double alpha) {
		this.catmullRomAlpha = alpha;
		onChange();
	}
	
	public void addGuidePoint(double t, Double2D pt) {
		points.put(t, pt.immutable());
		this.onChange();
	}
	
	public void removeGuidePoint(Double2D pt) {
		Iterator<Entry<Double, Double2D>> iter = points.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Double, Double2D> entry = iter.next();
			if (entry.getValue().equals(pt)) {
				iter.remove();
				break;
			}
		}
		this.onChange();
	}
	
	public Double2D getPoint(double t) {
		Double2D.Mutable mutable = new Double2D.Mutable();
		getPoint(mutable, t);
		return mutable.immutable();
	}
	
	public Collection<Double2D> getGuidePoints() {
		return points.values();
	}
	
	public void getPoint(Double2D.Mutable mutable, double t) {		
		if (points.size() == 0) {
			mutable.x(Double.NaN);
			mutable.y(Double.NaN);
			return;
		}
		if (points.containsKey(t)) {
			Double2D point = points.get(t);
			mutable.x(point.x());
			mutable.y(point.y());
			return;
		}
				
		Segment segment = null;
		if (segmentCache != null && t > segmentCache.tmin && t < segmentCache.tmax) {
			segment = segmentCache;
		} else {
			Entry<Double, Double2D> entryP1 = points.floorEntry(t);
			if (entryP1 == null) entryP1 = points.firstEntry();
			if (t < entryP1.getKey()) {
				mutable.x(entryP1.getValue().x());
				mutable.y(entryP1.getValue().y());
				return;
			}
			
			Entry<Double, Double2D> entryP2 = points.ceilingEntry(t);
			if (entryP2 == null) entryP2 = points.lastEntry();
			if (t > entryP2.getKey()) {
				mutable.x(entryP2.getValue().x());
				mutable.y(entryP2.getValue().y());
				return;
			}
			
			final Double2D pt0;
			Entry<Double, Double2D> entryP0 = points.lowerEntry(entryP1.getKey());
			if (entryP0 != null) pt0 = entryP0.getValue();
			else {
				Double2D.Mutable scratch = entryP1.getValue().mutable();
				scratch.subtract(entryP2.getValue());
				scratch.add(entryP1.getValue());
				pt0 = scratch.immutable();
			}

			final Double2D pt3;
			Entry<Double, Double2D> entryP3 = points.higherEntry(entryP2.getKey());
			if (entryP3 != null) pt3 = entryP3.getValue();
			else {
				Double2D.Mutable scratch = entryP2.getValue().mutable();
				scratch.subtract(entryP1.getValue());
				scratch.add(entryP2.getValue());
				pt3 = scratch.immutable();
			}
			
			segment = new Segment(pt0, entryP1, entryP2, pt3, catmullRomAlpha);
			segmentCache = segment;
		}
		
		segment.getPoint(mutable, t);
	}
	
	private static class Segment {
		final double tmin, tmax;
		final Double2D p0, p1, p2, p3;		
		final double t0, t1, t2, t3;
		
		public Segment(Double2D pt0, Entry<Double, Double2D> e1, Entry<Double, Double2D> e2, Double2D pt3, double alpha) {
			this.p0 = pt0;
			this.p1 = e1.getValue();
			this.p2 = e2.getValue();
			this.p3 = pt3;
			
			this.t0 = 0;
			this.t1 = adjustedT(t0, p0, p1, alpha);
			this.t2 = adjustedT(t1, p1, p2, alpha);
			this.t3 = adjustedT(t2, p2, p3, alpha);
			
			this.tmin = e1.getKey();
			this.tmax = e2.getKey();
		}
		
		private static double adjustedT(double t, Double2D p0, Double2D p1, double alpha) {
			double dx = p1.x() - p0.x(); 
			double dy = p1.y() - p0.y();
			double dist = Math.sqrt(dx*dx + dy*dy);
			return Math.pow(dist, alpha) + t;
		}
		
		public void getPoint(Double2D.Mutable mutable, double t) {			
			t = normalize(t, tmin, tmax);
			t = t * (t2-t1) + t1;
			
			final double tNorm01 = normalize(t, t0, t1);
			final double tNorm12 = normalize(t, t1, t2);
			final double tNorm23 = normalize(t, t2, t3);
			final double tNorm02 = normalize(t, t0, t2);
			final double tNorm13 = normalize(t, t1, t3);
			
		    final double a1x = lerp(tNorm01, p0.x(), p1.x());
		    final double a2x = lerp(tNorm12, p1.x(), p2.x());
		    final double a3x = lerp(tNorm23, p2.x(), p3.x());
		    final double b1x = lerp(tNorm02, a1x, a2x);
		    final double b2x = lerp(tNorm13, a2x, a3x);
		    final double cx = lerp(tNorm12, b1x, b2x);
		    
		    final double a1y = lerp(tNorm01, p0.y(), p1.y());
		    final double a2y = lerp(tNorm12, p1.y(), p2.y());
		    final double a3y = lerp(tNorm23, p2.y(), p3.y());
		    final double b1y = lerp(tNorm02, a1y, a2y);
		    final double b2y = lerp(tNorm13, a2y, a3y);
		    final double cy = lerp(tNorm12, b1y, b2y);
			
		    mutable.x(cx);
		    mutable.y(cy);
		}
		
		/**
		 * Linear interpolation from v0 to v1 on the range 0 to 1
		 */
		private final static double lerp(double t, double v0, double v1) {			
			return v0*(1-t) + v1*t;
		}
		
		/**
		 * Normalize t from (t0, t1) to (0 to 1)
		 */
		private final static double normalize(double t, double t0, double t1) {
			return (t-t0)/(t1-t0);
		}
	}
	
}
