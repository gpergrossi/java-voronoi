package com.gpergrossi.gui.chunks;

public abstract class ChunkLoader<T extends Chunk<T>> {

	ChunkManager<T> manager;
	
	public abstract T getChunk(int chunkX, int chunkY);

	public void setManager(ChunkManager<T> manager) {
		this.manager = manager;
	}
	
	public ChunkManager<T> getManager() {
		return manager;
	}
	
	public long getMaxChunkAge() {
		return 0; // 120 = 2 seconds at 60 FPS
	}

	public double getChunkSize() {
		return 200;
	}
	
}
