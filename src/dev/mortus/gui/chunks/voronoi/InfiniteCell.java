package dev.mortus.gui.chunks.voronoi;

import java.awt.Point;
import java.util.Optional;
import java.util.Random;

import dev.mortus.util.math.geom2d.Polygon;
import dev.mortus.util.math.geom2d.Rect;
import dev.mortus.util.math.vectors.Double2D;
import dev.mortus.voronoi.Edge;
import dev.mortus.voronoi.Site;
import dev.mortus.voronoi.Voronoi;
import dev.mortus.voronoi.VoronoiBuilder;

public class InfiniteCell {		
	
	public static int allocations;
	
	// Constructor
	public final InfiniteVoronoi container;
	public final Point containerKey;
	public final int cellX, cellY;
	public final long seed;
	public final Random random;
	public final Double2D site;
	
	// Initialization
	private boolean initialized = false;
	private boolean inCache = false;
	private Polygon polygon;
	private Point[] neighbors;
	
	// Concurrency
	int refCount;
	
	public Object data;
	
	public InfiniteCell(InfiniteVoronoi container, Point cellKey) {
		this.container = container;
		this.containerKey = cellKey;
		this.cellX = cellKey.x;
		this.cellY = cellKey.y;
		
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
	
	public Polygon getPolygon() {
		return polygon;
	}
	
	public Point[] getNeighbors() {
		return neighbors;
	}
	
	protected void init() {
		if (!inCache) throw new RuntimeException("Only reserved cells (in cache) can be initialized.");
		if (!initialized) initRange(container, cellX, cellY, cellX, cellY, false);
	}
	
	public synchronized void reserve() {
		refCount++;
		if (!inCache) {
			container.cellCache.put(containerKey, this);
			this.inCache = true;
			allocations++;
		}
	}

	public synchronized void release() {
		refCount--;
		if (refCount > 0) return;
		if (refCount < 0) throw new RuntimeException("Negative Reference Count");
		if (container.isFull()) {
			this.delete();
		}
	}
	
	private synchronized void delete() {		
		this.initialized = false;
		this.polygon = null;
		this.neighbors = null;
		
		boolean success = container.cellCache.remove(this.containerKey, this);
		allocations--;
		if (!success) System.err.println("FAILED to remove cell from cell cache");
		else this.inCache = false;
	}
	
	private long createSeed(int chunkX, int chunkY) {
		Random rand = new Random(container.globalSeed);
		long rx = (chunkX * 341873128712L) ^ rand.nextLong();
		long ry = (chunkY * 132897987541L) ^ rand.nextLong();
		return (rx + ry) ^ rand.nextLong();	
	}

	
	/**
	 * Initializes a rectangle of cells because it is much less wasteful when possible (one voronoi diagram constructed for all).
	 * The return array will be in array[y*width+x] order, with the Cell corresponding to (minCellX, minCellY) in the 0th index.
	 * All cells returned will be initialized, but will not be marked 'activelyInUse' and will not be added to the container's cache.
	 * @param container - the InfiniteVoronoi container
	 * @param minCellX - cellX minimum
	 * @param minCellY - cellY minimum
	 * @param maxCellX - cellX maximum
	 * @param maxCellY - cellY maximum
	 * @return
	 */
	protected static InfiniteCell[] initRange(InfiniteVoronoi container, int minCellX, int minCellY, int maxCellX, int maxCellY, boolean reserve) {		
		int width = maxCellX - minCellX + 1;
		int height = maxCellY - minCellY + 1;
		InfiniteCell[] results = new InfiniteCell[width * height];
		
		int padding = 2; // cells around the outside of the ones we care about
		
		int workWidth = width + padding*2;
		int workHeight = height + padding*2;
		InfiniteCell[] workCells = new InfiniteCell[workWidth * workHeight];
		int[] workIDs = new int[workWidth * workHeight];

		VoronoiBuilder builder = new VoronoiBuilder();
		double boundsX = (minCellX-padding)*container.gridSize;
		double boundsY = (minCellY-padding)*container.gridSize;
		double boundsWidth = workWidth*container.gridSize;
		double boundsHeight = workHeight*container.gridSize;
		builder.setBounds(new Rect(boundsX, boundsY, boundsWidth, boundsHeight));
		
		int alreadyInitialized = 0;
		for (int j = 0; j < workHeight; j++) {
			for (int i = 0; i < workWidth; i++) {
				InfiniteCell cell = container.peakCell(new Point(minCellX-padding+i, minCellY-padding+j));
				workCells[j*workWidth+i] = cell;
				workIDs[j*workWidth+i] = builder.addSite(cell.site);
				
				if (i >= padding && j >= padding && i < workWidth-padding && j < workHeight - padding) {
					results[(j-padding)*width+(i-padding)] = cell;
					if (cell.initialized) alreadyInitialized++;
				}
			}
		}
		if (alreadyInitialized == width*height) return results;
		Voronoi voronoi = builder.build();
		
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				InfiniteCell cell = workCells[(j+padding)*workWidth+(i+padding)];
				int id = workIDs[(j+padding)*workWidth+(i+padding)];
				
				if (!cell.initialized) {
					Site site = voronoi.getSite(id);
					cell.neighbors = new Point[site.numEdges()];
					int neighborOn = 0;
					for (Edge edge : site.getEdges()) {
						Double2D point = edge.getNeighbor(site).point;
						Optional<InfiniteCell> neighbor = java.util.Arrays.stream(workCells).filter(n -> n.site == point).findFirst();
						if (neighbor.isPresent() && neighbor.get() != null) cell.neighbors[neighborOn++] = neighbor.get().containerKey;
					}
					cell.polygon = site.getPolygon();
					cell.initialized = true;
				}
				
				if (reserve) cell.reserve();
			}
		}
		
		return results;
	}
	
	@Override
	public String toString() {
		return "Cell ("+containerKey.x+", "+containerKey.y+")";
	}
	
}
