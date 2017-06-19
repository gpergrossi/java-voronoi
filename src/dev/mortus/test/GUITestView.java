package dev.mortus.test;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Random;
import dev.mortus.gui.View;
import dev.mortus.gui.ViewerFrame;
import dev.mortus.gui.chunks.ChunkLoader;
import dev.mortus.gui.chunks.InfiniteVoronoiChunk;
import dev.mortus.gui.chunks.InfiniteVoronoiChunkLoader;
import dev.mortus.gui.chunks.View2DChunkManager;
import dev.mortus.util.data.LinkedBinaryNode;
import dev.mortus.util.math.geom2d.Polygon;
import dev.mortus.util.math.vectors.Double2D;
import dev.mortus.voronoi.Site;
import dev.mortus.voronoi.Voronoi;
import dev.mortus.voronoi.VoronoiBuilder;
import dev.mortus.voronoi.VoronoiWorker;

public class GUITestView extends View {
	
	public static ViewerFrame frame;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					System.setProperty("sun.java2d.opengl", "true");
					frame = new ViewerFrame(new GUITestView(0, 0, 1024, 768));
					frame.setVisible(true);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	protected double getMinZoom() {
		return 0.005;
	}
	
	@Override
	protected void renderSettings(Graphics2D g2d) {
		// Background Black
		g2d.setBackground(new Color(0,0,0,255));
	}
	
	boolean useChunkLoader = true;
	
	VoronoiBuilder voronoiBuilder;
	VoronoiWorker voronoiWorker;

	ChunkLoader<InfiniteVoronoiChunk> chunkLoader;
	View2DChunkManager<InfiniteVoronoiChunk> chunkManager;
	
	double seconds;
	double printTime;
	double radiansPerDegree = (Math.PI/180.0);

	public GUITestView (double x, double y, double width, double height) {
		super (x, y, width, height);
		
		voronoiBuilder = new VoronoiBuilder();
		
		if (useChunkLoader) {
			chunkLoader = new InfiniteVoronoiChunkLoader(8964591453215L);
			chunkManager = new View2DChunkManager<InfiniteVoronoiChunk>(chunkLoader, 1);
		}
	}

	
	
	@Override
	public void init() {}
	
	@Override
	public void start() {
		if (useChunkLoader) chunkManager.start();
	}

	@Override
	public void stop() {
		if (useChunkLoader) chunkManager.stop();
	}

	@Override
	public void update(double secondsPassed) {
		seconds += secondsPassed;

		printTime += secondsPassed;
		if (printTime > 1) {
			printTime -= 1;
			frame.setTitle("FPS = "+String.format("%.2f", getFPS()));
		}
	}
	
	@Override
	public void drawWorld(Graphics2D g2d) {
//		// Clock dots
//		g2d.setColor(Color.WHITE);
//		for (int i = 0; i < 60; i++) {
//			double an = i * (Math.PI / 30.0);
//			double ew = 5, eh = 5;
//			double ex = Math.cos(an)*100-ew/2;
//			double ey = Math.sin(an)*100-eh/2;
//			Ellipse2D ellipse = new Ellipse2D.Double(ex, ey, ew, eh);
//
//			if (ellipse.contains(mX, mY)) g2d.setColor(Color.YELLOW);
//			g2d.fill(ellipse);
//			g2d.setColor(Color.WHITE);
//		}
//		
//		// Clock center
//		Ellipse2D.Double ellipse2 = new Ellipse2D.Double(-5.0, -5.0, 10.0, 10.0);
//		if (ellipse2.contains(mX, mY)) g2d.setColor(Color.YELLOW);
//		g2d.fill(ellipse2);
//		g2d.setColor(Color.WHITE);
//		
//		// Clock hand
//		AffineTransform before = g2d.getTransform();
//		Path2D.Double path = new Path2D.Double();
//		path.moveTo(100, 0);
//		path.lineTo(0, 2);
//		path.lineTo(0, -2);
//		path.closePath();
//		g2d.rotate(seconds*(Math.PI / 30.0));
//		g2d.fill(path);
//		g2d.setTransform(before);
		
		if (useChunkLoader) { 
			Rectangle2D bounds = this.getViewBounds();
			chunkManager.setView(bounds);
			chunkManager.update();
			chunkManager.draw(g2d);
		}
		
		if (voronoiWorker != null) {
			if (voronoiWorker.isDone()) {
//				voronoiWorker.debugDraw(g2d);
				Voronoi v = voronoiWorker.getResult();
				Random r = new Random(0);
				
				for (Site site : v.getSites()) {					
					// Draw shape
					g2d.setColor(Color.getHSBColor(r.nextFloat(), 1.0f, 0.5f + r.nextFloat()*0.5f));
					Polygon poly = site.getPolygon();
					Shape polyShape = poly.getShape2D();
					if (polyShape != null) g2d.fill(polyShape);
					
					// Draw original point
					g2d.setColor(Color.WHITE);
					Ellipse2D sitePt = new Ellipse2D.Double(site.getX()-1, site.getY()-1, 2, 2);
					g2d.fill(sitePt);
					
					// Draw centroid
					g2d.setColor(Color.BLACK);
					Double2D centroid = poly.getCentroid();
					Ellipse2D siteCentroid = new Ellipse2D.Double(centroid.x()-1, centroid.y()-1, 2, 2);
					g2d.fill(siteCentroid);
				}
			} else {
				voronoiWorker.debugDraw(g2d);
			}
		} else {
			for (Double2D site : voronoiBuilder.getSites()) {
				Ellipse2D ellipse = new Ellipse2D.Double(site.x()-1, site.y()-1, 2, 2);
				g2d.fill(ellipse);
			}
		}
		
//		Rect r = new Rect(-60, -40, 120, 80);
//		g2d.draw(r.getShape2D());
//		
//		Vec2[] verts = new Vec2[4];
//		verts[0] = new Vec2(getMouseWorldX()+20, getMouseWorldY());
//		verts[1] = new Vec2(getMouseWorldX(), getMouseWorldY()+20);
//		verts[2] = new Vec2(getMouseWorldX()-20, getMouseWorldY());
//		verts[3] = new Vec2(getMouseWorldX()-20, getMouseWorldY()-20);
//		Polygon tri = new Polygon(verts);
//		
//		if (tri.intersects(r)) g2d.fill(tri.getShape2D());
//		else g2d.draw(tri.getShape2D());
//		
//		Rect r2 = new Rect(200, -5, 10, 10);
//		if (tri.intersects(r2)) g2d.fill(r2.getShape2D());
//		else g2d.draw(r2.getShape2D());
		
//		// Mouse velocity trail
//		g2d.setColor(Color.WHITE);
//		Line2D mVel = new Line2D.Double(mX, mY, mX+mVelX, mY+mVelY);
//		g2d.draw(mVel);
	}

	@Override
	public void drawOverlayUI(Graphics2D g2d) {
		if (useChunkLoader) {
			g2d.setColor(Color.WHITE);
			g2d.clearRect(0, 0, 200, 30);
			g2d.drawString("Chunks loaded: "+chunkManager.getNumLoaded(), 10, 20);
		}
	}

	double startPX, startPY;
	double startViewX, startViewY;
	boolean panning = false;
	
	double mX, mY;
	double mDX, mDY;
	double mVelX, mVelY;

	@Override
	public void mousePressed() {}

	@Override
	public void mouseDragged() {}

	@Override
	public void mouseReleased() {
		int click = this.getMouseClick();
		double px = this.getMouseWorldX();
		double py = this.getMouseWorldY();
		
		if (click == View.RIGHT_CLICK || click == View.LEFT_CLICK) {
			Point2D clickP = new Point2D.Double(px, py);
			int x = (int) Math.floor((clickP.getX()+2) / 4);
			int y = (int) Math.floor((clickP.getY()+2) / 4);
			clickP = new Point2D.Double(x*4, y*4);

			if (click == View.RIGHT_CLICK) {
				for (Double2D site : voronoiBuilder.getSites()) {
					Point2D point = site.toPoint2D();
					if (clickP.distance(point) < 4) {
						System.out.println("Removing site");
						voronoiBuilder.removeSite(site);
						break;
					}
				}
			}
			
			if (click == View.LEFT_CLICK) {
				int id = voronoiBuilder.addSite(new Double2D(clickP.getX(), clickP.getY()));
				System.out.println("Adding site "+id);
			}
			
			voronoiWorker = null;
		}
	}

	@Override
	public void mouseMoved() {}

	@Override
	public void mouseScrolled() {}

	@Override
	public void keyPressed() {
		KeyEvent e = getKeyEvent();
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			if (voronoiWorker == null) voronoiWorker = voronoiBuilder.getBuildWorker();
			else voronoiWorker.doWork(0);
		} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			if (voronoiWorker == null) {
				voronoiWorker = voronoiBuilder.getBuildWorker();
			} else if (voronoiWorker.isDone()) {
				Voronoi v = voronoiWorker.getResult();
				for (Site s : v.getSites()) {
					if (s.getPolygon().getArea() == 0) {
						throw new RuntimeException("Zero Area!");
					}
				}
				voronoiBuilder.clearSites(true);
				for (Site s : v.getSites()) {
					voronoiBuilder.addSite(s.getPolygon().getCentroid());
				}
				voronoiWorker = voronoiBuilder.getBuildWorker();
			}
			try {
				while (!voronoiWorker.isDone()) {
					voronoiWorker.doWork(0);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				voronoiWorker = null;
			}
		} else if (e.getKeyCode() == KeyEvent.VK_UP) {
			if (voronoiWorker != null) { 
				voronoiWorker.debugAdvanceSweepline(-1);
			}
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			if (voronoiWorker != null) { 
				voronoiWorker.debugAdvanceSweepline(+1);
			}
		} else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			if (voronoiWorker != null) { 
				voronoiWorker.stepBack();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_S) {
			System.out.println("Saving");
			try {
				voronoiBuilder.savePoints();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_L) {
			System.out.println("Loading");
			try {
				voronoiBuilder.loadPoints();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_C) {
			System.out.println("Clearing");
			LinkedBinaryNode.IDCounter = 0;
			voronoiBuilder.clearSites();
			voronoiWorker = null;
		} else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
			this.setSlowZoom(9.0/14.0);
		} else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
			this.setSlowZoom(14.0/9.0);
		} else if (e.getKeyCode() == KeyEvent.VK_R) {
			if (!this.isRecording()) {
				System.out.println("Recording started");
				this.startRecording();
			} else {
				System.out.println("Recording finished");
				this.stopRecording();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_SLASH) {
			for (double d = Math.PI/2.0; d < Math.PI*1.0; d += Math.PI/17.3) {
				voronoiBuilder.addSite(new Double2D(Math.cos(d)*d*100, Math.sin(d)*d*100));
				voronoiBuilder.addSite(new Double2D(Math.cos(d+Math.PI*2.0/3.0)*d*100, Math.sin(d+Math.PI*2.0/3.0)*d*100));
				voronoiBuilder.addSite(new Double2D(Math.cos(d-Math.PI*2.0/3.0)*d*100, Math.sin(d-Math.PI*2.0/3.0)*d*100));
			}
		}
	}

	@Override
	public void keyReleased() {
		KeyEvent e = getKeyEvent();
		if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
			setSlowZoom(1.0);
		} else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
			setSlowZoom(1.0);
		}
	}
	
	@Override
	public void keyTyped() { }
	
}
