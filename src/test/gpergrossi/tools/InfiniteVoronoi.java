package test.gpergrossi.tools;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import com.gpergrossi.gui.View;
import com.gpergrossi.gui.ViewerFrame;
import com.gpergrossi.gui.chunks.ChunkLoader;
import com.gpergrossi.gui.chunks.InfiniteVoronoiChunk;
import com.gpergrossi.gui.chunks.InfiniteVoronoiChunkLoader;
import com.gpergrossi.gui.chunks.View2DChunkManager;

public class InfiniteVoronoi extends View {
	
	public static ViewerFrame frame;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					System.setProperty("sun.java2d.opengl", "true");
					frame = new ViewerFrame(new InfiniteVoronoi(0, 0, 1024, 768));
					frame.setVisible(true);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	protected double getMinZoom() {
		return 0.1;
	}
	
	@Override
	protected void renderSettings(Graphics2D g2d) {
		// Background Black
		g2d.setBackground(new Color(0,0,0,255));
	}
	
	
	
	ChunkLoader<InfiniteVoronoiChunk> chunkLoader;
	View2DChunkManager<InfiniteVoronoiChunk> chunkManager;
	
	double seconds;
	double printTime;
	double radiansPerDegree = (Math.PI/180.0);
	
	
	
	public InfiniteVoronoi (double x, double y, double width, double height) {
		super (x, y, width, height);
	}

	
	
	@Override
	public void init() {
		chunkLoader = new InfiniteVoronoiChunkLoader(8964591453215L);
		chunkManager = new View2DChunkManager<InfiniteVoronoiChunk>(chunkLoader, 1);	
	}
	
	@Override
	public void start() {
		chunkManager.start();
	}

	@Override
	public void stop() {
		chunkManager.stop();
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
	public void drawWorld(Graphics2D g2d) {		Rectangle2D bounds = this.getViewWorldBounds();		chunkManager.setView(bounds);		chunkManager.update();		chunkManager.draw(g2d);
	}

	@Override
	public void drawOverlayUI(Graphics2D g2d) {
		g2d.setColor(Color.WHITE);
		g2d.clearRect(0, 0, 200, 30);
		g2d.drawString("Chunks loaded: "+chunkManager.getNumLoaded(), 10, 20);
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
	public void mouseReleased() {}

	@Override
	public void mouseMoved() {}

	@Override
	public void mouseScrolled() {}

	@Override
	public void keyPressed() {
		KeyEvent e = getKeyEvent();
		if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
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
