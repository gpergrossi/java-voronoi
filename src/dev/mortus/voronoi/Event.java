package dev.mortus.voronoi;

import dev.mortus.util.math.geom2d.Circle;
import dev.mortus.util.math.vectors.Double2D;
import dev.mortus.voronoi.exception.OverlappingSiteException;

public final class Event implements Comparable<Event> {

	public static enum Type {
		SITE, CIRCLE;
	}
	
	public final Type type;
	public final double x, y;
	public final Circle circle;
	
	public final Site site;
	public final ShoreArc arc;
	
	public boolean valid;
	
	public static Event createSiteEvent(Site site) {
		return new Event(site);
	}
	
	private Event(Site site) {
		this.type = Type.SITE;
		this.x = site.point.x();
		this.y = site.point.y();
		this.site = site;
		this.arc = null;
		this.circle = null;
		this.valid = true;
	}
	
	public static Event createCircleEvent(ShoreArc arc, Circle circle) {
		return new Event(arc, circle);
	}
	
	private Event(ShoreArc arc, Circle circle) {
		this.type = Type.CIRCLE;
		this.x = Double.NEGATIVE_INFINITY;
		this.y = circle.y() + circle.radius();
		this.site = arc.getSite();
		this.arc = arc;
		this.circle = circle;
		this.valid = true;
	}
	
	@Override
	public String toString() {
		if (this.type == Type.CIRCLE) {
			return "Event[Type='Circle', "+arc+", "+circle+", y="+y+"]";
		} else {
			return "Event[Type='Site', "+site+", x="+x+", y="+y+"]";
		}
	}

	@Override
	public int compareTo(Event o) {
		double dyd = this.y - o.y;
		double dxd = this.x - o.x;
		if (Math.abs(dyd) > Double2D.EPSILON || Math.abs(dxd) > Double2D.EPSILON) {
			// Lowest Y value first
			int dy = (int) Math.signum(dyd);
			if (dy != 0) return dy;
			
			// Lowest X value first
			int dx = (int) Math.signum(dxd);
			if (dx != 0) return dx;
		}
		
		// Allow equal priority circle events
		if (this.type == Type.CIRCLE) return 0;
		if (o.type == Type.CIRCLE) return 0;
		
		// We cannot allow multiple site events with the same exact position
		throw new OverlappingSiteException(this.site, o.site);
	}
	
	
}