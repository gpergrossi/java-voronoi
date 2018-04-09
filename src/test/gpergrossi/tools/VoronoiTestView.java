package test.gpergrossi.tools;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import com.gpergrossi.util.geom.shapes.Circle;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.gui.View;
import com.gpergrossi.gui.ViewerFrame;
import com.gpergrossi.voronoi.Site;
import com.gpergrossi.voronoi.Voronoi;
import com.gpergrossi.voronoi.VoronoiBuilder;
import com.gpergrossi.voronoi.VoronoiWorker;

public class VoronoiTestView extends View {
	
	public static ViewerFrame frame;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					System.setProperty("sun.java2d.opengl", "true");
					frame = new ViewerFrame(new VoronoiTestView(0, 0, 1024, 768));
					frame.setVisible(true);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	protected double getMinZoom() {
		return 0.25;
	}
	
	@Override
	protected void renderSettings(Graphics2D g2d) {
		// Background Black
		g2d.setBackground(new Color(0,0,0,255));
	}
	
	
	
	VoronoiBuilder voronoiBuilder;
	VoronoiWorker voronoiWorker;
	
	double seconds;
	double printTime;
	double radiansPerDegree = (Math.PI/180.0);
	
	
	
	public VoronoiTestView (double x, double y, double width, double height) {
		super (x, y, width, height);
		
		voronoiBuilder = new VoronoiBuilder();
		voronoiBuilder.setBounds(new Circle(300, 0, 100).toPolygon(5));
	}

	
	
	@Override
	public void init() {}
	
	@Override
	public void start() {}

	@Override
	public void stop() {}

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
		// Clock dots
		g2d.setColor(Color.WHITE);
		for (int i = 0; i < 60; i++) {
			double an = i * (Math.PI / 30.0);
			double ew = 5, eh = 5;
			double ex = Math.cos(an)*100-ew/2;
			double ey = Math.sin(an)*100-eh/2;
			Ellipse2D ellipse = new Ellipse2D.Double(ex, ey, ew, eh);
	
			if (ellipse.contains(mX, mY)) g2d.setColor(Color.YELLOW);
			g2d.fill(ellipse);
			g2d.setColor(Color.WHITE);
		}
		
		// Clock center
		Ellipse2D.Double ellipse2 = new Ellipse2D.Double(-5.0, -5.0, 10.0, 10.0);
		if (ellipse2.contains(mX, mY)) g2d.setColor(Color.YELLOW);
		g2d.fill(ellipse2);
		g2d.setColor(Color.WHITE);
		
		// Clock hand
		AffineTransform before = g2d.getTransform();
		Path2D.Double path = new Path2D.Double();
		path.moveTo(100, 0);
		path.lineTo(0, 2);
		path.lineTo(0, -2);
		path.closePath();
		g2d.rotate(seconds*(Math.PI / 30.0));
		g2d.fill(path);
		g2d.setTransform(before);
		
		if (voronoiWorker != null) {
			voronoiWorker.debugDraw(g2d);
			
			if (voronoiWorker.isDone()) {
				Voronoi v = voronoiWorker.getResult();
				
				for (Site site : v.getSites()) {
					// Draw original point
					g2d.setColor(Color.WHITE);
					Ellipse2D sitePt = new Ellipse2D.Double(site.getX()-1, site.getY()-1, 2, 2);
					g2d.fill(sitePt);
					
					// Draw centroid
					g2d.setColor(Color.BLACK);
					Double2D centroid = site.getPolygon().getCentroid();
					Ellipse2D siteCentroid = new Ellipse2D.Double(centroid.x()-1, centroid.y()-1, 2, 2);
					g2d.fill(siteCentroid);
				}
			}
		} else {
			for (Double2D site : voronoiBuilder.getSites()) {
				Ellipse2D ellipse = new Ellipse2D.Double(site.x()-1, site.y()-1, 2, 2);
				g2d.fill(ellipse);
			}
			g2d.draw(voronoiBuilder.getBounds().asAWTShape());
		}
	}

	@Override
	public void drawOverlayUI(Graphics2D g2d) {}

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
						voronoiBuilder.removeSite(voronoiBuilder.findSiteIndex(site));
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
			voronoiBuilder.clearSites();
			voronoiBuilder.setBounds(new Circle(300, 0, 100).toPolygon(5));
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
