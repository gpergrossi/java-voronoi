package dev.mortus.voronoi;

import dev.mortus.util.data.Pair;
import dev.mortus.util.math.func1d.Function;
import dev.mortus.util.math.func1d.Quadratic;
import dev.mortus.util.math.geom2d.Circle;
import dev.mortus.util.math.vectors.Double2D;

/**
 * Arcs are parabolas formed from a site as a focus point and the
 * sweep line as a directrix. The Parabola class -- used by all Arcs
 * for purposes of calculation -- is immutable. Therefore, the arc
 * class serves the purpose of forming appropriate Parabolas for a 
 * site at any given position of the sweep line. It also holds onto 
 * important information about the arc, such as its circle event
 * and neighbors.
 */
public class ShoreArc extends ShoreTreeNode {
	
	private final Site site;
	private Event circleEvent;
	
	protected ShoreArc(ShoreTree rootParent, Site site) {
		super(rootParent);
		this.site = site;
		this.circleEvent = null;
	}
	
	public ShoreArc(Site site) {
		super();
		this.site = site;
		this.circleEvent = null;
	}
	
	private void setCircleEvent(Event circleEvent) {
		if (circleEvent != null && circleEvent.type != Event.Type.CIRCLE) {
			throw new RuntimeException("Event is not a Circle Event!");
		}
		this.circleEvent = circleEvent;
	}
	
	public Event getCircleEvent() {
		return circleEvent;
	}
	
	public Site getSite() {
		return site;
	}
	
	public Function getParabola(double sweeplineY) {
		return Quadratic.fromPointAndLine(site.point.x(), site.point.y(), sweeplineY);
	}

	@Override
	public ShoreArc getArc(final BuildState state, double siteX) {
		return this;
	}

	@Override
	public ShoreBreakpoint getPredecessor() {
		return (ShoreBreakpoint) super.getPredecessor();
	}

	@Override
	public ShoreBreakpoint getSuccessor() {
		return (ShoreBreakpoint) super.getSuccessor();
	}
	
	public Pair<ShoreBreakpoint> getBreakpoints() {
		return new Pair<ShoreBreakpoint>(getPredecessor(), getSuccessor());
	}
	
	public Pair<ShoreArc> getNeighborArcs() {
		return new Pair<ShoreArc>(getLeftNeighborArc(), getRightNeighborArc());
	}
	
	public ShoreArc getLeftNeighborArc() {
		if (this.getPredecessor() == null) return null;
		ShoreArc neighbor = (ShoreArc) this.getPredecessor().getPredecessor();
		return neighbor;
	}
	
	public ShoreArc getRightNeighborArc() {
		if (this.getSuccessor() == null) return null;
		ShoreArc neighbor = (ShoreArc) this.getSuccessor().getSuccessor();
		return neighbor;
	}

	public Event checkCircleEvent(final BuildState state) {
		this.setCircleEvent(null);

		// No circle event if neighbors don't exist or if neighbor arcs are from the same site
		ShoreArc leftNeighbor = this.getLeftNeighborArc();
		ShoreArc rightNeighbor = this.getRightNeighborArc();
		if (leftNeighbor == null || rightNeighbor == null) return null;
		if (leftNeighbor.site == rightNeighbor.site) return null;
		
		// Check for a collision point, fail if none exists
		ShoreBreakpoint leftBP = (ShoreBreakpoint) getPredecessor();
		ShoreBreakpoint rightBP = (ShoreBreakpoint) getSuccessor();
		Double2D intersection = ShoreBreakpoint.getIntersection(state, leftBP, rightBP);
		if (intersection == null) return null;
		
		// Create the circle event
		double radius = intersection.distanceTo(this.site.point);
		Circle circle = new Circle(intersection.x(), intersection.y(), radius);
		Event circleEvent = Event.createCircleEvent(this, circle);
		this.setCircleEvent(circleEvent);
		return circleEvent;
	}
	
	public ShoreArc insertArc(BuildState state, Site site) {
		ShoreBreakpoint newBreakpoint = null;
		ShoreArc newArc = null;
		ShoreArc leftArc = null;
		ShoreArc rightArc = null;
		
		if (Math.abs(this.site.point.y() - site.point.y()) < Double2D.EPSILON) {
			
			// Y coordinates equal, single breakpoint between sites
			leftArc = new ShoreArc(this.site);
			rightArc = newArc = new ShoreArc(site);
			
			// Swap the arcs if not in the right order
			if (this.site.point.x() > site.point.x()) {
				ShoreArc swap = leftArc;
				leftArc = rightArc;
				rightArc = swap;
			}
			
			newBreakpoint = new ShoreBreakpoint(leftArc, rightArc);
			
		} else {
			
			// Normal site creation, two breakpoints around new arc
			leftArc = new ShoreArc(this.site);
			rightArc = new ShoreArc(this.site);
			ShoreArc middle = newArc = new ShoreArc(site);
			ShoreBreakpoint rightBP = new ShoreBreakpoint(middle, rightArc);
			ShoreBreakpoint leftBP = new ShoreBreakpoint(leftArc, rightBP);
			newBreakpoint = leftBP;
			
		}
		
		this.replaceWith(newBreakpoint);
		return newArc;
	}

	@Override
	public String toString() {
		return "Arc["+(debugName != null ? "Name='"+debugName+"', " : "")+"ID="+id+", "
				+ "Site="+site.id+", CircleEvent="+(circleEvent!=null)+"]";
	}

	
}
		