package com.gpergrossi.gui.chunks;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.gpergrossi.util.geom.vectors.Int2D;

public class View2DChunkLoader<T extends View2DChunk<T>> extends ChunkLoader<T> {
	
	public static interface ChunkConstructor<T extends View2DChunk<T>> {
		public T construct(ChunkManager<T> manager, int x, int y);
	}
	
	ChunkConstructor<T> chunkConstructor;
	
	public int chunkSize;
	public long seed;
	
	Map<Int2D, T> map;
	
	public View2DChunkLoader(int chunkSize, ChunkConstructor<T> chunkConstructor) {
		this(new Random().nextLong(), chunkSize, chunkConstructor);
	}
	
	public View2DChunkLoader(long seed, int chunkSize, ChunkConstructor<T> chunkConstructor) {
		this.chunkSize = chunkSize;
		this.map = new HashMap<>();
		this.seed = seed;
		this.chunkConstructor = chunkConstructor;
	}
	
	@Override
	public double getChunkSize() {
		return chunkSize;
	}
	
	@Override
	public T getChunk(int chunkX, int chunkY) {
		Int2D pt = new Int2D(chunkX, chunkY);
		
		T chunk = map.get(pt);
		if (chunk == null) {
			chunk = chunkConstructor.construct(getManager(), chunkX, chunkY);
			map.put(pt, chunk);
		}
		
		return chunk;
	}
	
}
