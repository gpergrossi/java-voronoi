package com.gpergrossi.voronoi.infinite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.gpergrossi.util.geom.ranges.Int2DRange;
import com.gpergrossi.util.geom.shapes.Rect;
import com.gpergrossi.util.geom.vectors.Int2D;
import com.gpergrossi.voronoi.Site;
import com.gpergrossi.voronoi.Voronoi;
import com.gpergrossi.voronoi.VoronoiBuilder;

public class InfiniteVoronoi {
	
	public final long seed;
	public final double gridSize;
	
	protected int allocations;
	Map<Int2D, InfiniteCell> cellCache;

	public InfiniteVoronoi(double gridSize, long seed) {
		this.gridSize = gridSize;
		this.seed = seed;

		this.allocations = 0;
		this.cellCache = new HashMap<>();
	}
	
	/**
	 * Gets a range of cells. Does init(). Does reserve(). Any cell that would be returned 
	 * but is already in the output list will not be reserved(). Any cell that does not 
	 * intersect the given range will not be reserved() or returned. All cells returned
	 * must call cell.release() in order to be removed from the cellCache.
	 */
	public synchronized void getCells(Int2DRange range, List<InfiniteCell> output) {
		int minCellX = (int) Math.floor(range.minX / gridSize) - 2;
		int minCellY = (int) Math.floor(range.minY / gridSize) - 2;
		int maxCellX = (int) Math.floor(range.maxX / gridSize) + 2;
		int maxCellY = (int) Math.floor(range.maxY / gridSize) + 2;
		InfiniteCell[] cells = initRange(minCellX, minCellY, maxCellX, maxCellY);
		
		Rect bounds = new Rect(range.minX, range.minY, range.maxX-range.minX+1, range.maxY-range.minY+1);
		
		for (InfiniteCell cell : cells) {
			if (!output.contains(cell) && cell.getPolygon().intersects(bounds)) {
				output.add(cell);
			} else {
				cell.release();
			}
		}
	}
	
	/**
	 * Gets a cell. Does init(). Does reserve().
	 * Cells returned must call cell.release() in order to be removed from the cellCache.
	 */
	public synchronized InfiniteCell getCell(double x, double y) {
		int cellX = (int) Math.floor(x / gridSize);
		int cellY = (int) Math.floor(y / gridSize);
		
		double lowestDistance = Double.MAX_VALUE;
		InfiniteCell winner = null;

		for (int iy = -2; iy <= 2; iy++) {
			for (int ix = -2; ix <= 2; ix++) {
				InfiniteCell cell = peakCell(cellX+ix, cellY+iy);
				double dist = cell.site.distanceTo(x, y);
				if (dist < lowestDistance) {
					lowestDistance = dist;
					winner = cell;
				}
			}	
		}
		
		winner.reserve();
		winner.init();
		return winner;
	}

	/**
	 * Gets a cell. Does reserve() and init(), meaning the cell will be stored in the cellCache
	 * and the poylgon and neighbor information will be calculated.
	 * Cells returned must call cell.release() in order to be removed from the cellCache.
	 */
	public synchronized InfiniteCell getCell(int x, int y) {
		return getCell(x, y, true);
	}
	
	/**
	 * Gets a cell. Does not reserve() or init(), meaning there is no polygon or neighbor data
	 * and the cell does not take up memory after the returned pointer is discarded.
	 * @param cellKey
	 * @return
	 */
	public synchronized InfiniteCell peakCell(int x, int y) {
		return getCell(x, y, false);
	}
	
	private synchronized InfiniteCell getCell(int x, int y, boolean reserve) {
		InfiniteCell cell = cellCache.get(new Int2D(x, y));
		if (cell == null) cell = new InfiniteCell(this, x, y);
		if (reserve) {
			cell.reserve();
			cell.init();
		}
		return cell;
	}
	
	/**
	 * Initializes a rectangle of cells because it is much less wasteful when possible (one voronoi diagram constructed for all).
	 * The return array will be in array[y*width+x] order, with the Cell corresponding to (minCellX, minCellY) in the 0th index.
	 * All cells returned will be reserved and initialized.
	 * @param container - the InfiniteVoronoi container
	 * @param minCellX - cellX minimum
	 * @param minCellY - cellY minimum
	 * @param maxCellX - cellX maximum
	 * @param maxCellY - cellY maximum
	 * @return
	 */
	protected InfiniteCell[] initRange(int minCellX, int minCellY, int maxCellX, int maxCellY) {		
		int width = maxCellX - minCellX + 1;
		int height = maxCellY - minCellY + 1;
		InfiniteCell[] results = new InfiniteCell[width * height];
		
		final int padding = 2; // # of cells (in all directions) around the outside of the ones we care about

		int workWidth = width + padding*2;
		int workHeight = height + padding*2;
		int[] workIDs = new int[width * height];
		Map<Integer, InfiniteCell> workCellMap = new HashMap<>();

		VoronoiBuilder builder = new VoronoiBuilder();
		final double boundsX = (minCellX-padding) * gridSize;
		final double boundsY = (minCellY-padding) * gridSize;
		final double boundsWidth = workWidth * gridSize;
		final double boundsHeight = workHeight * gridSize;
		builder.setBounds(new Rect(boundsX, boundsY, boundsWidth, boundsHeight));
		
		boolean needBuild = false;
		for (int j = 0; j < workHeight; j++) {
			final boolean jIsPadding = (j < padding || j >= workHeight-padding);
			for (int i = 0; i < workWidth; i++) {
				InfiniteCell cell = peakCell(minCellX-padding+i, minCellY-padding+j);
				int cellIndex = builder.addSite(cell.site);
				workCellMap.put(cellIndex, cell);
				
				if (!jIsPadding && i >= padding && i < workWidth-padding) {
					results[(j-padding)*width+(i-padding)] = cell;
					workIDs[(j-padding)*width+(i-padding)] = cellIndex;
					cell.reserve();
					if (!cell.initialized) needBuild = true;
				}
			}
		}
		if (!needBuild) return results;
		
		Voronoi voronoi = builder.build();
		
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				InfiniteCell cell = results[j*width+i];
				int cellIndex = workIDs[j*width+i];
				
				if (!cell.initialized) {
					Site site = voronoi.getSite(cellIndex);
					Function<Site, Int2D> siteToCellLocation = new Function<Site, Int2D>() {
						public Int2D apply(Site site) {
							InfiniteCell cell = workCellMap.get(site.index);
							if (cell == null) return null;
							return new Int2D(cell.cellX, cell.cellY);
						}
					};
					cell.neighbors = site.getNeighbors().stream().map(siteToCellLocation).toArray(size -> new Int2D[size]);
					cell.polygon = site.getPolygon();
					cell.initialized = true;
				}
			}
		}
		
		return results;
	}

	protected void addCache(InfiniteCell cell) {
		if (cell.inCache) return;
		
		cellCache.put(new Int2D(cell.cellX, cell.cellY), cell);
		cell.inCache = true;
		this.allocations++;
	}

	protected void removeCache(InfiniteCell cell) {
		if (!cell.inCache) return;
		
		cellCache.remove(new Int2D(cell.cellX, cell.cellY));
		cell.inCache = false;
		this.allocations--;
		
	}
	
}
