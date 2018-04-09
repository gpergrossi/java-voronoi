package com.gpergrossi.gui.chunks;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

public class View2DChunkManager<T extends View2DChunk<T>> extends ChunkManager<T> {
	
	Rectangle2D view;
	
	public View2DChunkManager(ChunkLoader<T> loader) {
		super(loader, 4, 40);
	}
	
	public View2DChunkManager(ChunkLoader<T> loader, int numWorkers) {
		super(loader, numWorkers, 40);
	}
	
	public View2DChunkManager(ChunkLoader<T> loader, int numWorkers, int initialQueueSize) {
		super(loader, numWorkers, initialQueueSize);
	}
	
	public void setView(Rectangle2D view) {
		this.view = view;
	}
	
	public void touchAll() {
		Point upperLeft = getChunkCoordinate(view.getMinX(), view.getMinY());
		Point lowerRight = getChunkCoordinate(view.getMaxX(), view.getMaxY());
		int minX = upperLeft.x-1, minY = upperLeft.y-1;
		int maxX = lowerRight.x+1, maxY = lowerRight.y+1;
		
		// For priority evaluation
		int centerX = (minX + maxX) / 2;
		int centerY = (minY + maxY) / 2;
		center = new Point(centerX, centerY);
		
		// Load new chunks / update lastSeen
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				T chunk = loader.getChunk(x, y);
				touch(chunk);
			}
		}
	}
	
	public void draw(Graphics2D g) {
		acquire("loadedChunks", loadedChunksLock);
		Iterator<T> iterator = loadedChunks.iterator();
		while (iterator.hasNext()) {
			iterator.next().draw(g);
		}
		release("loadedChunks", loadedChunksLock);
	}
	
}
