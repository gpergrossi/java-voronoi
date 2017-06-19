package dev.mortus.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class ViewerPane extends JPanel implements Runnable {

	private static final long serialVersionUID = 5091243043686433403L;
	
	public String recordingFilePrefix = "recording/frame.";
	public int recordingFrame = 0;
	public int recordingMaxFrames = 9999;
	public boolean recording = false;
	public double recordingFPS = 24.0;
	
	protected boolean needsViewUpdate = true;
	
	public int targetFPS = 60;
	public boolean showFPS = false;
	public double averageFPS = 60.0;

	long lastDraw = Long.MAX_VALUE;
	long lastLoop = System.nanoTime();
	long currentLoop = System.nanoTime();
	long nanosPerMilli = 1000000;
	long nanosPerSecond = 1000000000;
	long delta = 1;
	long sleepThreashold = 6 * nanosPerMilli;  // 6 ms
	long yieldThreashold = 2 * nanosPerMilli;  // 2 ms
	
	private AffineTransform freshTransform = new AffineTransform();
	private AffineTransform screenToWorld = new AffineTransform();
	private AffineTransform worldToScreen = new AffineTransform();
	private BufferedImage buffer, bufferLast;
	private Graphics2D bufferG2D, bufferLastG2D;
	private boolean rebuildBuffers = true;
	private boolean running = false;
	private View view;
	private Thread thread;
	
	private ComponentListener componentListener = new ComponentAdapter() {
		public void componentResized(ComponentEvent e) {
			ViewerPane.this.onResize(e.getComponent().getSize());
		}
	};
	
	private MouseAdapter mouseListener = new MouseAdapter() {
		int mouseClick = 0;
		public void mousePressed(MouseEvent e) {
			updateViewTransform(false);
			Point2D pt = screenToWorld(e.getX(), e.getY());
			mouseClick |= (1 << (e.getButton()-1));
			view.internalMousePressed(mouseClick, pt.getX(), pt.getY(), e.getX(), e.getY());
		}
		public void mouseDragged(MouseEvent e) {
			updateViewTransform(false);
			Point2D pt = screenToWorld(e.getX(), e.getY());
			view.internalMouseDragged(mouseClick, pt.getX(), pt.getY(), e.getX(), e.getY());
		}
		public void mouseReleased(MouseEvent e) {
			updateViewTransform(false);
			Point2D pt = screenToWorld(e.getX(), e.getY());
			view.internalMouseReleased(mouseClick, pt.getX(), pt.getY(), e.getX(), e.getY());
			mouseClick &= ~(1 << (e.getButton()-1));
		}
		public void mouseMoved(MouseEvent e) {
			updateViewTransform(false);
			Point2D pt = screenToWorld(e.getX(), e.getY());
			mouseClick = 0;
			view.internalMouseMoved(pt.getX(), pt.getY(), e.getX(), e.getY());
		}
	};
	
	private MouseWheelListener mouseWheelListener = new MouseWheelListener() {
		public void mouseWheelMoved(MouseWheelEvent e) {
			view.internalMouseScrolled(e.getPreciseWheelRotation());			
		}
	};
	
	private KeyAdapter keyListener = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
			view.internalKeyPressed(e);
		}
		public void keyReleased(KeyEvent e) {
			view.internalKeyReleased(e);
		}
		public void keyTyped(KeyEvent e) {
			view.internalKeyTyped(e);
		}
	};
	
	public ViewerPane(View view) {
		this.view = view;
		addComponentListener(componentListener);
		addMouseMotionListener(mouseListener);
		addMouseListener(mouseListener);
		addMouseWheelListener(mouseWheelListener);
		addKeyListener(keyListener);
		onResize(new Dimension(100, 100));
		this.setDoubleBuffered(true);
	}

	private void onResize(Dimension size) {
		// We need a new buffers to write to
		buffer = new BufferedImage(
				(int) size.getWidth(), (int) size.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		bufferLast = new BufferedImage(
				(int) size.getWidth(), (int) size.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		rebuildBuffers = true;
		view.setAspect((double) size.width/size.height);
		this.needsViewUpdate = true;
	}
	
	private void dispose() {
		if (bufferG2D != null) bufferG2D.dispose();
		if (bufferLastG2D != null) bufferLastG2D.dispose();
	}
	
	private void renderSettings(Graphics2D g2d) {
		view.renderSettings(g2d);
	}
	
	private void rebuildBuffers() {
		// Dispose if needed
		dispose();
		
		// Create new
		bufferG2D = buffer.createGraphics();
		bufferLastG2D = bufferLast.createGraphics();
		freshTransform.setTransform(bufferG2D.getTransform());
		
		// Set settings
		renderSettings(bufferG2D);
		renderSettings(bufferLastG2D);
		
		// Mark built
		rebuildBuffers = false;
	}

	private void swap() {
		BufferedImage swap = bufferLast;
		bufferLast = buffer;
		buffer = swap;
		Graphics2D swapG2D = bufferLastG2D;
		bufferLastG2D = bufferG2D;
		bufferG2D = swapG2D;
	}
	
	@Override
	public void paint(Graphics g) {
		if (rebuildBuffers) rebuildBuffers();
		
		// Swap buffers
		swap();
		
		// Show old buffer
		g.drawImage(bufferLast, 0, 0, this);
		
		// Recording?
		if (recording) {
			try {
				if (bufferLast == null) System.err.println("null");
				ImageIO.write(bufferLast, "png", new File(recordingFilePrefix+getFrameNumber()+".png"));
			} catch (IOException e) {
				e.printStackTrace();
				recording = false;
			}
		}
		
		// Render new buffer
		drawFrame(bufferG2D);
		
		if (lastDraw != Long.MAX_VALUE) {
			long drawDelta = System.nanoTime() - lastDraw;
			double FPS = (double) nanosPerSecond/drawDelta;
			if (FPS < targetFPS*2) this.averageFPS = (averageFPS * 59.0 + FPS) / 60.0;
		}
		lastDraw = System.nanoTime();
	}
	
	private String getFrameNumber() {
		this.recordingFrame++;
		if (recordingFrame > recordingMaxFrames) {
			throw new RuntimeException("recorded too many frames");
		}
		String frame = String.valueOf(this.recordingFrame);
		String max = String.valueOf(recordingMaxFrames);
		while (frame.length() < max.length()) frame = "0"+frame;
		return frame;
	}

	public void start() {
		running = true;
		view.internalStart();
		thread = new Thread(this);
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
	}
	
	public void stop() {
		running = false;
		try {
			thread.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		view.internalStop();
		dispose();
	}
	
	private void sync(int fps) {
		long nanos = nanosPerSecond/fps;
		do {
			long timeLeft = lastLoop + nanos - currentLoop;
			if (timeLeft > sleepThreashold) {
				long sleepTime = timeLeft-sleepThreashold;
				sleepTime /= nanosPerMilli;
				try {
					Thread.sleep(sleepTime);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			} else if (timeLeft > yieldThreashold) {
				Thread.yield();
			}
			currentLoop = System.nanoTime();
		} while (currentLoop - lastLoop < nanos);
		delta = currentLoop-lastLoop;
		if (delta == 0) delta++;
		lastLoop = currentLoop;
	}
	
	public void run() {
		while (running) {			
			double updateDelta = (double) delta/nanosPerSecond;
			if (recording) updateDelta = 1.0/recordingFPS;
			
			view.internalUpdate(updateDelta);
			
			this.repaint();
			
			sync(targetFPS);
		}
	}
	
	public double getFPS() {
		return this.averageFPS;
	}
	
	private void drawFrame(Graphics2D g2d) {
		// Clear frame
		resetTransform(g2d);
		g2d.clearRect(0, 0, getWidth(), getHeight());

		// Request draw from view object
		updateViewTransform(false);
		view.internalDrawFrame(g2d, worldToScreen);
	}
	
	private void resetTransform(Graphics2D g2d) {
		g2d.setTransform(freshTransform);
	}
	
	protected void updateViewTransform(boolean force) {
		if (this.needsViewUpdate || force) {
			worldToScreen.setTransform(freshTransform);
			worldToScreen.translate(getWidth()/2.0, getHeight()/2.0);
			worldToScreen.scale(view.getViewZoom(), view.getViewZoom());
			worldToScreen.scale(getWidth()/view.getViewWidth(), getHeight()/view.getViewHeight());
			worldToScreen.translate(-view.getViewX(), -view.getViewY());
			
			screenToWorld.setTransform(freshTransform);
			screenToWorld.translate(view.getViewX(), view.getViewY());
			screenToWorld.scale(view.getViewWidth()/getWidth(),	view.getViewHeight()/getHeight());
			screenToWorld.scale(1.0/view.getViewZoom(), 1.0/view.getViewZoom());
			screenToWorld.translate(-getWidth()/2.0, -getHeight()/2.0);
			
			this.needsViewUpdate = false;
		}
	}

	public Point2D multiply(AffineTransform xform, double x, double y) {
		Point2D pt = new Point2D.Double(x, y);
		return xform.transform(pt, pt);
	}
	
	public Point2D worldToScreen(double x, double y) {
		Point2D pt = new Point2D.Double(x, y);
		return worldToScreen.transform(pt, pt);
	}

	public Point2D worldToScreen(Point2D pt) {
		return worldToScreen.transform(pt, pt);
	}
	
	public Point2D worldToScreenVelocity(Point2D pt) {
		pt.setLocation(pt.getX()*(getWidth()/view.getViewWidth())*view.getViewZoom(), pt.getY()*(getHeight()/view.getViewHeight())*view.getViewZoom());
		return pt;
	}
	
	public Point2D screenToWorld(Point2D pt) {
		return screenToWorld.transform(pt, pt);
	}

	public Point2D screenToWorld(double x, double y) {
		Point2D pt = new Point2D.Double(x, y);
		return screenToWorld.transform(pt, pt);
	}
	
	public Point2D screenToWorldVelocity(Point2D pt) {
		pt.setLocation(pt.getX()*(view.getViewWidth()/getWidth())/view.getViewZoom(), pt.getY()*(view.getViewHeight()/getHeight())/view.getViewZoom());
		return pt;
	}

	public boolean isRecording() {
		return recording;
	}

	public void startRecording() {
		this.recording = true;
		this.recordingFrame = 0;
	}

	public void stopRecording() {
		this.recording = false;
	}
}
