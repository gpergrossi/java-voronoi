package com.gpergrossi.voronoi.infinite;

import java.util.Random;

import com.gpergrossi.util.geom.shapes.Convex;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.util.geom.vectors.Int2D;

public final class InfiniteCell {		
	
	// Always available:
	public final InfiniteVoronoi container;
	public final int cellX, cellY;
	public final long seed;
	public final Random random;
	public final Double2D site;
	
	// Only after init():
	protected boolean initialized = false;
	protected boolean inCache = false;
	protected Convex polygon;
	protected Int2D[] neighbors;
	
	// Memory management
	int refCount;
	
	// Attaching data
	public volatile Object data;
	
	public InfiniteCell(InfiniteVoronoi container, int x, int y) {
		this.container = container;
		this.cellX = x;
		this.cellY = y;
		
		this.seed = createSeed(cellX, cellY);
		this.random = new Random(seed);
		double a = random.nextDouble()*Math.PI*2.0;
		double r = Math.sqrt(random.nextDouble())*container.gridSize*0.5;
		this.site = new Double2D((cellX+0.5)*container.gridSize + Math.cos(a)*r, (cellY+0.5)*container.gridSize + Math.sin(a)*r);
	}

	public long getSeed() {
		return seed;
	}

	public Double2D getSite() {
		return site;
	}
	
	public Convex getPolygon() {
		if (!initialized) throw new RuntimeException("Cell not initialized");
		return polygon;
	}
	
	public Int2D[] getNeighbors() {
		if (!initialized) throw new RuntimeException("Cell not initialized");
		return neighbors;
	}
	
	/**
	 * Calculates the cell's polygon and neighbor coordinates
	 */
	protected final void init() {
		if (!inCache) throw new RuntimeException("Only reserved cells (in cache) can be initialized.");
		if (!initialized) container.initRange(cellX, cellY, cellX, cellY);
	}
	
	/**
	 * Increases the reference count on this cell and adds it to the cache if is not already there.
	 * The cache will only release its pointer to this cell when the reference counter reaches zero again.
	 */
	public synchronized void reserve() {
		refCount++;
		container.addCache(this);
	}

	/**
	 * Decreases the reference count on this cell and removes it from the cache if the reference count reaches zero.
	 */
	public synchronized void release() {
		refCount--;
		if (refCount < 0) throw new RuntimeException("Negative Reference Count");
		if (refCount == 0) this.delete();
	}

	private synchronized void delete() {		
		this.initialized = false;
		this.polygon = null;
		this.neighbors = null;
		container.removeCache(this);
	}

	public Int2D getCoord() {
		return new Int2D(cellX, cellY);
	}
	
	@Override
	public String toString() {
		return "Cell (cellX="+cellX+", cellY="+cellY+")";
	}
	
	private long createSeed(int chunkX, int chunkY) {
		Random rand = new Random(container.seed);
		long rx = (chunkX * 341873128712L) ^ rand.nextLong();
		long ry = (chunkY * 132897987541L) ^ rand.nextLong();
		return (rx + ry) ^ rand.nextLong();	
	}
	
}
