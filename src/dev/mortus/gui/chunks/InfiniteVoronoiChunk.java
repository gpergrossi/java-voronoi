package dev.mortus.gui.chunks;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.util.Random;

import dev.mortus.gui.chunks.voronoi.InfiniteCell;
import dev.mortus.util.math.geom2d.Polygon;
import dev.mortus.util.math.geom2d.Rect;

public class InfiniteVoronoiChunk extends View2DChunk<InfiniteVoronoiChunk> {
	
	Rect bounds;
	InfiniteCell cell;
	
	Color color;
	Shape shape;
	
	public InfiniteVoronoiChunk(ChunkManager<InfiniteVoronoiChunk> manager, int chunkX, int chunkY) {
		super(manager, chunkX, chunkY);
	}
	
	@Override
	public InfiniteVoronoiChunkLoader getChunkLoader() {
		return (InfiniteVoronoiChunkLoader) loader;
	}

	@Override
	public void load() {
		InfiniteVoronoiChunkLoader loader = getChunkLoader();
		
		int chunkSize = (int) loader.getChunkSize();
		bounds = new Rect(chunkX * chunkSize, chunkY * chunkSize, chunkSize, chunkSize);

		cell = loader.voronoi.getCell(new Point(chunkX, chunkY));
		
		Polygon poly = cell.getPolygon();
		shape = poly.getShape2D();
		
		Random r = new Random(cell.getSeed());
		color = new Color(Color.HSBtoRGB(r.nextFloat(), 1, r.nextFloat()*0.5f + 0.5f));
	}

	@Override
	public void unload() {
		cell.release();
	}
	
	@Override
	public void draw(Graphics2D g) {		
		int cx = (int) cell.getSite().x();
		int cy = (int) cell.getSite().y();

		g.setColor(color);		
		g.fill(shape);
		
		g.setColor(Color.BLACK);
		g.drawLine(cx-2, cy-2, cx+2, cy+2);
		g.drawLine(cx-2, cy+2, cx+2, cy-2);
		
		g.setColor(Color.WHITE);
		g.drawString(chunkX+", "+chunkY, (int) cell.site.x(), (int) cell.site.y());
		
//		int minX = (int) bounds.minX();
//		int minY = (int) bounds.minY();
//		int maxX = (int) bounds.maxX();
//		int maxY = (int) bounds.maxY();
		
//		g.setColor(Color.BLACK);
		
//		g.setColor(new Color(255, 255, 255, 16));
//		for (int i = 0; i < 16; i++) {
//			g.drawLine(minX + i*16, minY, minX + i*16, maxY);
//			g.drawLine(minX, minY + i*16, maxX, minY + i*16);
//		}
		
//		g.setColor(Color.WHITE);
//		g.drawRect(minX, minY, (int) loader.getChunkSize(), (int) loader.getChunkSize());
		
		
	}

}
