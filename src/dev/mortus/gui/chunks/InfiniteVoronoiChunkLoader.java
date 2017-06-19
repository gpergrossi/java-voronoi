package dev.mortus.gui.chunks;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import dev.mortus.gui.chunks.voronoi.InfiniteVoronoi;

public class InfiniteVoronoiChunkLoader extends ChunkLoader<InfiniteVoronoiChunk> {
	
	public int chunkSize = 16*16;
	public InfiniteVoronoi voronoi;
	
	Map<Point, InfiniteVoronoiChunk> map;
	
	public InfiniteVoronoiChunkLoader() {
		this(new Random().nextLong());
	}
	
	public InfiniteVoronoiChunkLoader(long seed) {
		this.map = new HashMap<>();
		this.voronoi = new InfiniteVoronoi(chunkSize, seed, 1024);
	}
	
	@Override
	public double getChunkSize() {
		return chunkSize;
	}
	
	@Override
	public InfiniteVoronoiChunk getChunk(int chunkX, int chunkY) {
		Point pt = new Point(chunkX, chunkY);
		
		InfiniteVoronoiChunk chunk = map.get(pt);
		if (chunk == null) {
			chunk = new InfiniteVoronoiChunk(getManager(), chunkX, chunkY);
			map.put(pt, chunk);
		}
		
		return chunk;
	}

}
