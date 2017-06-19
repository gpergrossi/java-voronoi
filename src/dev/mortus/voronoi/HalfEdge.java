package dev.mortus.voronoi;

import dev.mortus.util.data.Pair;

public class HalfEdge extends Edge {

	private HalfEdge twin;
	
	static Pair<HalfEdge> createTwinPair(Pair<ShoreBreakpoint> bps, Vertex vert) {
		if (bps.size() != 2) throw new RuntimeException("Cannot construct twin pair with a partial pair of breakpoints");
		
		HalfEdge edge = new HalfEdge(bps.first, vert);
		HalfEdge twin = new HalfEdge(bps.second, vert);
		
		edge.twin = twin;
		twin.twin = edge;
		
		Pair<HalfEdge> twins = new Pair<HalfEdge>(edge, edge.twin);
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
