package com.gpergrossi.voronoi;

import java.awt.Graphics2D;

public class VoronoiWorker {

	Voronoi voronoiBackup; // Saved backup in case we need to step backward (I.E. start over)
	BuildState state;
	
	public VoronoiWorker(Voronoi voronoi) {
		this.voronoiBackup = voronoi;
		this.restart();
	}

	private void restart() {
		Voronoi voronoiCopy = new Voronoi(voronoiBackup.bounds);
		for (Site site : voronoiBackup.sites) {
			Site siteCopy = new Site(voronoiCopy, site.index, site.point);
			voronoiCopy.addSite(siteCopy);
		}
		
		this.state = new BuildState(voronoiCopy);
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
	 * Do at least ms milliseconds of work. Will often exceed this value.<br />
	 * If ms == 0, doWork will return after the smallest possible amount of progress is made.<br />
	 * If ms == -1, doWork will not return until finished.
	 * @param ms
	 */
	public int doWork(int ms) {
		return state.processEvents(ms);
	}

	public Voronoi getResult() {
		if (!isDone()) throw new IllegalStateException("Can't get result because builder is not complete");
		return state.getResult();
	}
	
	
	
	
	public void doWorkVerbose() {
		System.out.println("\nBuild Step "+state.getNumEventsProcessed()+" (out of <"+state.getTheoreticalMaxSteps()+")");
		state.processNextEvent();
	}

	public void stepBack() {
		if (state == null) return;
		int step = state.getNumEventsProcessed();
		if (step == 0) return;
		
		restart();

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
		if (!state.isFinished()) {
			g.draw(voronoiBackup.bounds.asAWTShape());
		}
		state.drawDebugState(g);
	}

}