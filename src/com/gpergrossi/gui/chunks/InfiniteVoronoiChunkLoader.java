package com.gpergrossi.gui.chunks;

import java.util.Random;

import com.gpergrossi.voronoi.infinite.InfiniteVoronoi;

public class InfiniteVoronoiChunkLoader extends View2DChunkLoader<InfiniteVoronoiChunk> {
	
	public static final int chunkSize = 16*16;
	public InfiniteVoronoi voronoi;
	
	public InfiniteVoronoiChunkLoader() {
		this(new Random().nextLong());
	}
	
	public InfiniteVoronoiChunkLoader(long seed) {
		super(chunkSize, InfiniteVoronoiChunk::constructor);
		this.voronoi = new InfiniteVoronoi(chunkSize, seed);
	}

}
