package com.gpergrossi.voronoi;

import com.gpergrossi.util.geom.shapes.Circle;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.util.math.func.Function;
import com.gpergrossi.util.math.func.Quadratic;

public class ShoreBreakpoint extends ShoreTreeNode {
	
	public Edge edge;
	protected ShoreArc arcLeft, arcRight;
	
	public ShoreBreakpoint(ShoreTreeNode left, ShoreTreeNode right) {		
		if (left.equals(right)) throw new RuntimeException("Cannot construct breakpoint between identical arcs!");
		
		setLeftChild(left);
		setRightChild(right);
		
		checkPossible();
	}

	private void setArcLeft(ShoreArc arc) {
		this.arcLeft = arc;
		if (this.edge != null) this.edge.sites.first = arc.getSite();
	}

	private void setArcRight(ShoreArc arc) {
		this.arcRight = arc;
		if (this.edge != null) this.edge.sites.second = arc.getSite();
	}

	@Override
	public void setLeftChild(ShoreTreeNode left) {
		if (left == null) throw new RuntimeException("Cannot set child of a breakpoint to null");
		
		super.setLeftChild(left);
		
		// Update arcLeft
		this.setArcLeft((ShoreArc) left.getLastDescendant());
				
		// Update arcRight
		ShoreArc firstArc = (ShoreArc) left.getFirstDescendant();
		ShoreBreakpoint preBreakpoint = firstArc.getPredecessor();
		if (preBreakpoint != null) preBreakpoint.setArcRight(firstArc);
	}
	
	@Override
	public void setRightChild(ShoreTreeNode right) {
		if (right == null) throw new RuntimeException("Cannot set child of a breakpoint to null");
		
		super.setRightChild(right);
		
		// Update arcRight
		this.setArcRight((ShoreArc) right.getFirstDescendant());

		// Update arcLeft
		ShoreArc lastArc = (ShoreArc) right.getLastDescendant();
		ShoreBreakpoint postBreakpoint = lastArc.getSuccessor();
		if (postBreakpoint != null) postBreakpoint.setArcLeft(lastArc);
	}
	
	private void checkPossible() {
		if ( arcLeft.getSite().point.y() == arcRight.getSite().point.y() &&  arcLeft.getSite().point.x() > arcRight.getSite().point.x()) {
			// The parabolas are exactly side by side, there is only one intersection between
			// them and the X coordinates of the parabola's focii are in the wrong order for
			// the requested breakpoint to exist.
			throw new RuntimeException("There is no such breakpoint!");
		}
	}
	
	/**
	 * Gets the direction this breakpoint moves as the sweepline progresses.
	 * A point is returned representing a vector. The length is not normalized.
	 * @return
	 */
	public Double2D getDirection() {
		double dy = arcRight.getSite().point.y() - arcLeft.getSite().point.y();
		double dx = arcRight.getSite().point.x() - arcLeft.getSite().point.x();
		
		if (Math.abs(dy) < Double2D.EPSILON) {
			if (Math.abs(dx) < Double2D.EPSILON) return new Double2D(0, 0);
			return new Double2D(0, 1);
		}
		if (dy < 0) {
			return new Double2D(1, -dx/dy);
		} else {
			return new Double2D(-1, dx/dy);
		}
	}
	
	public static Double2D getIntersection(final BuildState state, ShoreBreakpoint left, ShoreBreakpoint right) {	
		if (left.arcRight != right.arcLeft) {
			System.out.println("ERROR: expected a shared site between breakpoints! (left.arcRight="+left.arcRight+", right.arcLeft="+right.arcLeft+")");
			return null;
		}

		Double2D ptLeft = left.arcLeft.getSite().point;
		Double2D ptCenter = left.arcRight.getSite().point;
		Double2D ptRight = right.arcRight.getSite().point;
		
		// Check if these breakpoints diverge
		Double2D dirL = left.getDirection();
		Double2D dirR = right.getDirection();
		
		if (Voronoi.DEBUG) {
			System.out.println("Checking intersect on");
			System.out.println("left:  "+left);
			System.out.println("       pos:"+left.getPosition(state)+" dir:"+dirL);
			System.out.println("right: "+right);
			System.out.println("       pos:"+right.getPosition(state)+" dir:"+dirR);
		}
		
		Double2D delta = ptRight.subtract(ptLeft);
		double r = dirR.dot(delta); // positive if right breakpoint is moving to the "right" (this is based on the delta vector)
		double l = dirL.dot(delta); // positive if left breakpoint is moving to the "left" (this is based on the delta vector)
		if (r > l) {
			if (Voronoi.DEBUG) System.out.println("Diverging: r="+r+", l="+l);
			return null; // Diverging
		}
		
		// Where would the breakpoints between these sites intersect (if they did)?
		Circle circle = Circle.fromPoints(ptLeft, ptCenter, ptRight);
		if (circle == null) {
			if (Voronoi.DEBUG) System.out.println("Co-linear");
			return null; // sites are co-linear
		}

		Double2D result = new Double2D(circle.x(), circle.y());
		if (Voronoi.DEBUG) System.out.println("Collision at "+result);
		return result;
	}

	private double lastRequest = Double.NaN;
	private Double2D lastResult = null;
	
	public Double2D getPosition(final BuildState state) {
		if (state.getSweeplineY()  != lastRequest) {
			lastRequest = state.getSweeplineY();
			lastResult = calculatePosition(lastRequest);
		}
		if (lastResult == null) {
			// null occurs when sites are on the same y value and have no intersection of their "parabolas"
			double x = (arcLeft.getSite().point.x() + arcRight.getSite().point.x()) / 2.0;
			double y = state.getBounds().getBounds().minY()-50000; // TODO this should actually be a backwards intersection to the top boundary, not an average position
			lastResult = new Double2D(x, y);
		}
		return lastResult;
	}
	
	private Double2D calculatePosition(double sweeplineY) {		
		Function leftParabola = arcLeft.getParabola(sweeplineY);
		Function rightParabola = arcRight.getParabola(sweeplineY);
		
		return Quadratic.getIntersect(leftParabola, rightParabola);
	}

	@Override
	public ShoreArc getArc(final BuildState state, double siteX) {
		// Call down the tree based on breakpoint positions
		Double2D pos = this.getPosition(state);
		
		double posX;
		if (pos == null) posX = (this.arcLeft.getSite().point.x() + this.arcRight.getSite().point.x()) / 2.0;
		else posX = pos.x();
				
		if (siteX <= posX) {
			if (Voronoi.DEBUG) System.out.println("X:"+siteX+" <= "+this);
			return getLeftChild().getArc(state, siteX);
		} else {
			if (Voronoi.DEBUG) System.out.println("X:"+siteX+" > "+this);
			return getRightChild().getArc(state, siteX);
		}
	}

	public void updateArcs() {
		this.arcLeft = (ShoreArc) this.getLeftChild().getLastDescendant();
		this.arcRight = (ShoreArc) this.getRightChild().getFirstDescendant();
	}	
	
	@Override
	public String toString() {
		String leftID = (hasLeftChild() ? ""+getLeftChild().ID : "null");
		String rightID = (hasRightChild() ? ""+getRightChild().ID : "null");
		return "Breakpoint["+(this.debugName != null ? "DebugName='"+this.debugName+"', " : "")+"ID="+this.ID+", "
				+ "LeftArc="+arcLeft+", RightArc="+arcRight+", "
				+ "Children:[Left="+leftID+", Right="+rightID+"]]";
	}
	
	public ShoreArc getArcLeft() {
		return this.arcLeft;
	}
	
	public ShoreArc getArcRight() {
		return this.arcRight;
	}

	public void setEdge(Edge e) {
		edge = e;
	}
	
	public Edge getEdge() {
		return edge;
	}
	
}
