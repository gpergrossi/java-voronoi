package com.gpergrossi.voronoi;

import com.gpergrossi.util.data.OrderedPair;

public class HalfEdge extends Edge {

	private HalfEdge twin;
	
	static OrderedPair<HalfEdge> createTwinPair(OrderedPair<ShoreBreakpoint> bps, Vertex vert) {
		if (bps.size() != 2) throw new RuntimeException("Cannot construct twin pair with a partial pair of breakpoints");
		
		HalfEdge edge = new HalfEdge(bps.first, vert);
		HalfEdge twin = new HalfEdge(bps.second, vert);
		
		edge.twin = twin;
		twin.twin = edge;
		
		OrderedPair<HalfEdge> twins = new OrderedPair<HalfEdge>(edge, edge.twin);
		return twins;
	}
	
	private HalfEdge(ShoreBreakpoint bp, Vertex start) {
		super(bp, start);
	}
	
	HalfEdge getTwin() {
		return this.twin;
	}
	
	void joinHalves() {
		this.combineWith(this.twin);
	}

	@Override
	boolean isHalf() {
		return true;
	}
	
}
