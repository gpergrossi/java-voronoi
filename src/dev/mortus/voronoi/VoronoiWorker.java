package dev.mortus.voronoi;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import dev.mortus.util.math.geom2d.Rect;
import dev.mortus.util.math.vectors.Double2D;

public class VoronoiWorker {

	BuildState state;

	Rect bounds;
	Double2D[] siteArray;
	
	public VoronoiWorker(Rect bounds, Double2D[] siteArray) {
		this.bounds = bounds;
		this.siteArray = siteArray;
	}

	public boolean isDone() {
		if (state == null) return false;
		return state.isFinished();
	}
	
	public double getProgressEstimate() {
		if (isDone()) return 1.0;
		return ((double) state.getNumEventsProcessed()) / ((double) state.getTheoreticalMaxSteps());
	}

	/**
	 * Do at least ms milliseconds of work. Will often exceed this value.<pre>
	 * If ms == 0, doWork will return after the smallest amount of progress is made.
	 * If ms == -1, doWork will not return until finished.</pre>
	 * @param ms
	 */
	public int doWork(int ms) {
		if (state == null) {
			state = new BuildState(bounds, siteArray);
		}
		return state.processEvents(ms);
	}

	public Voronoi getResult() {
		if (!isDone()) throw new IllegalStateException("Can't get result because builder is not complete");
		return state.getResult();
	}
	
	
	
	
	public void doWorkVerbose() {
		if (state == null) {
			state = new BuildState(bounds, siteArray);
			System.out.println("Build Init");
			System.out.println("\nBuild Step 0 (out of <"+state.getTheoreticalMaxSteps()+")");
			return;
		}
		System.out.println("\nBuild Step "+state.getNumEventsProcessed()+" (out of <"+state.getTheoreticalMaxSteps()+")");
		state.processNextEvent();
	}

	public void stepBack() {
		if (state == null) return;
		int step = state.getNumEventsProcessed();
		if (step == 0) return;
		
		state = new BuildState(bounds, siteArray);

		while (state.getNumEventsProcessed() < step-1) {		
			System.out.println("Rewind: "+state.getNumEventsProcessed()+"/"+(step-1));
			if (state.getNumEventsProcessed() == step-1) state.processNextEvent(); 
			else state.processNextEvent();
		}
	}

	public void debugAdvanceSweepline(double v) {
		if (state != null) state.debugAdvanceSweepline(v);
	}
	
	public void debugDraw(Graphics2D g) {
		if (state == null || !state.isFinished()) {
			Rectangle2D rect2d = bounds.toRectangle2D();
			g.draw(rect2d);
		}
		if (state == null) {
			for (Double2D site : siteArray) {
				Ellipse2D ellipse = new Ellipse2D.Double(site.x()-1, site.y()-1, 2, 2);
				g.fill(ellipse);
			}
			return;
		}
		state.drawDebugState(g);
	}

}