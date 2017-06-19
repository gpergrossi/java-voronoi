package dev.mortus.gui.chunks;

import java.awt.Graphics2D;

public abstract class View2DChunk<T extends Chunk<T>> extends Chunk<T> {

	public View2DChunk(ChunkManager<T> manager, int chunkX, int chunkY) {
		super(manager, chunkX, chunkY);
	}

	public abstract void draw(Graphics2D g);
	
}
