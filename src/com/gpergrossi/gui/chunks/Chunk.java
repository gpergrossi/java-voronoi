package com.gpergrossi.gui.chunks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Chunk<T extends Chunk<T>> {
	
	protected ChunkManager<T> manager;
	protected ChunkLoader<T> loader;
	
	long lastSeen;
	boolean loaded, loading, unloading;
	protected final int chunkX, chunkY;
	final Lock lock = new ReentrantLock(true);
	
	public Chunk(ChunkManager<T> manager, int chunkX, int chunkY) {
		this.manager = manager;
		this.loader = manager.getLoader();
		this.chunkX = chunkX;
		this.chunkY = chunkY;
		loaded = false;
	}
	
	public ChunkLoader<T> getChunkLoader() {
		return loader;
	}
	
	public ChunkManager<T> getChunkManager() {
		return manager;
	}
	
	public abstract void load();
	public abstract void unload();
	
	boolean canLoad() {
		return !loaded && !loading;
	}
	
	boolean canUnload() {
		return loaded && !unloading;
	}
	
	boolean isLoaded() {
		return loaded;
	}

	protected synchronized void internalLoad() {
		if (this.loaded) {
			System.err.println(this+" already loaded!");
			return;
		}
		load();
		loaded = true;
		loading = false;
		this.notifyAll();
	}
	
	protected synchronized void internalUnload() {
		if (!this.loaded) {
			System.err.println(this+" already unloaded!");
			return;
		}
		unload();
		loaded = false;
		unloading = false;
		this.notifyAll();
	}
	
	public String toString() {
		return "Chunk["+chunkX+","+chunkY+"]";
	}
	
	public boolean equals(Chunk<T> other) {
		return this.chunkX == other.chunkX && this.chunkY == other.chunkY;
	}
	
}
