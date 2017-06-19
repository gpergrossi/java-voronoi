package dev.mortus.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import dev.mortus.gui.ViewerPane;

public abstract class View {
	
	public static final int LEFT_CLICK = 1;
	public static final int MIDDLE_CLICK = 2;
	public static final int RIGHT_CLICK = 4;
	
	public int DRAG_MOUSE_BUTTON = MIDDLE_CLICK;
	
	private ViewerPane viewerPane;
	private boolean initialized = false;
	
	private double viewX, viewY;
	private double viewWidth, viewHeight;
	private double viewZoom;
	
	private double driftVelocityX, driftVelocityY;
	
	private double slowZoom = 1.0;

	private double startDragScreenX, startDragScreenY;
	private double startDragViewX, startDragViewY;
	private boolean panning = false;
	
	private int mouseClick;
	private double mouseScroll;
	private double mouseWorldX, mouseWorldY;
	private double mouseWorldDX, mouseWorldDY;
	private double mouseScreenX, mouseScreenY;
	private double mouseScreenDX, mouseScreenDY;
	private double dragVelocityX, dragVelocityY;
	private KeyEvent keyEvent;

	public View (double x, double y, double width, double height) {
		this.viewX = x;
		this.viewY = y;
		this.viewWidth = width;
		this.viewHeight = height;
		this.viewZoom = 1;
		this.init();
	}

	public final void setAspect(double aspect) { 
		this.viewHeight = this.viewWidth / aspect;
		if (viewerPane != null) viewerPane.needsViewUpdate = true;
	}
	public final void setSlowZoom(double multiplierPerSecond) { 
		this.slowZoom = multiplierPerSecond;
	}
	
	public final int getMouseClick() { return mouseClick; }
	public final double getMouseWorldX() { return mouseWorldX; }
	public final double getMouseWorldY() { return mouseWorldY; }
	public final double getMouseWorldDX() { return mouseWorldDX; }
	public final double getMouseWorldDY() { return mouseWorldDY; }
	public final double getMouseScreenX() { return mouseScreenX; }
	public final double getMouseScreenY() { return mouseScreenY; }
	public final double getMouseScreenDX() { return mouseScreenDX; }
	public final double getMouseScreenDY() { return mouseScreenDY; }
	public final double getMouseScroll() { return mouseScroll; }
	public final KeyEvent getKeyEvent() { return keyEvent; }
	
	public final double getViewX() { return viewX; }
	public final double getViewY() { return viewY; }
	public final double getViewWidth() { return viewWidth; }
	public final double getViewHeight() { return viewHeight; }
	public final Rectangle2D getViewBounds() { 
		double extentX = viewWidth/viewZoom;
		double extentY = viewHeight/viewZoom;
		return new Rectangle2D.Double(viewX-extentX/2, viewY-extentY/2, extentX, extentY);
	}

	public final void startRecording() { this.viewerPane.startRecording(); }
	public final void stopRecording() { this.viewerPane.stopRecording(); }
	public final boolean isRecording() { return this.viewerPane.isRecording(); }
	
	public final double getFPS() { return this.viewerPane.getFPS(); }
	
	public Point2D worldToScreen(Point2D pt) { return this.viewerPane.worldToScreen(pt); }
	public Point2D screenToWorld(Point2D pt) { return this.viewerPane.screenToWorld(pt); }	
	public Point2D worldToScreenVelocity(Point2D pt) { return this.viewerPane.worldToScreenVelocity(pt); }	
	public Point2D screenToWorldVelocity(Point2D pt) { return this.viewerPane.screenToWorldVelocity(pt); }

	public double getViewZoom() { return viewZoom; }
	protected double getMinZoom() { return 0.01; }
	protected double getMaxZoom() { return 100; }
	
	public abstract void init();
	public abstract void start();
	public abstract void stop();
	public abstract void update(double secondsPassed);
	public abstract void drawWorld(Graphics2D g2d);
	public abstract void drawOverlayUI(Graphics2D g2d);
	
	public abstract void mousePressed();
	public abstract void mouseDragged();
	public abstract void mouseReleased();
	public abstract void mouseMoved();
	public abstract void mouseScrolled();
	
	public abstract void keyPressed();
	public abstract void keyReleased();
	public abstract void keyTyped();
	
	
	
	protected void renderSettings(Graphics2D g2d) {
		// Anti-aliasing on
		g2d.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING, 
			RenderingHints.VALUE_ANTIALIAS_ON
		);
		
		// Background Black
		g2d.setBackground(new Color(0,0,0,255));
	}
	
	void internalStart() {
		if (!initialized) {
			this.init();
			initialized = true;
		}
		this.start();
	}
	
	void internalStop() {
		this.stop();
	}
	
	void setViewerPane(ViewerPane viewerPane) {
		this.viewerPane = viewerPane;
		if (viewerPane != null) viewerPane.needsViewUpdate = true;
	}
	
	void internalUpdate(double secondsPassed) {
		this.viewX += this.driftVelocityX*secondsPassed;
		this.viewY += this.driftVelocityY*secondsPassed;
		
		double decay = Math.pow(0.75, secondsPassed);
		this.driftVelocityX *= decay;
		this.driftVelocityY *= decay;
		
	    decay = Math.pow(0.50, secondsPassed*60.0);
		this.dragVelocityX *= decay;
		this.dragVelocityY *= decay;
		
		viewWidth *= Math.pow(slowZoom, secondsPassed);
		viewHeight *= Math.pow(slowZoom, secondsPassed);

		if (viewerPane != null) viewerPane.needsViewUpdate = true;
		
		this.update(secondsPassed);
	}
	
	void internalDrawFrame(Graphics2D g2d, AffineTransform viewTransform) {		
		AffineTransform before = g2d.getTransform();
		g2d.setTransform(viewTransform);
		this.drawWorld(g2d);
		g2d.setTransform(before);
		this.drawOverlayUI(g2d);
	}
	
	void internalMousePressed(int click, double worldX, double worldY, double screenX, double screenY) {
		mouseClick = click;
		
		mouseWorldDX = (worldX - mouseWorldX); 
		mouseWorldDY = (worldY - mouseWorldY); 
		mouseWorldX = worldX;
		mouseWorldY = worldY;
		
		mouseScreenDX = (screenX - mouseScreenX); 
		mouseScreenDY = (screenY - mouseScreenY); 
		mouseScreenX = screenX;
		mouseScreenY = screenY;
		
		if (click == DRAG_MOUSE_BUTTON) {
			mouseWorldDX = 0;
			mouseWorldDY = 0;
			
			dragVelocityX = 0;
			dragVelocityY = 0;
			driftVelocityX = 0;
			driftVelocityY = 0;
			
			startDragScreenX = screenX; startDragScreenY = screenY;
			startDragViewX = viewX; startDragViewY = viewY;
			panning = true;
		}
		
		this.mousePressed();
	}

	void internalMouseDragged(int click, double worldX, double worldY, double screenX, double screenY) {
		mouseClick = click;
		
		mouseWorldDX = (worldX - mouseWorldX); 
		mouseWorldDY = (worldY - mouseWorldY); 
		mouseWorldX = worldX;
		mouseWorldY = worldY;
		
		mouseScreenDX = (screenX - mouseScreenX); 
		mouseScreenDY = (screenY - mouseScreenY); 
		mouseScreenX = screenX;
		mouseScreenY = screenY;
		
		if (click == DRAG_MOUSE_BUTTON) {
			Point2D worldMouseMoveSafe = new Point2D.Double(mouseScreenDX, mouseScreenDY);
			this.viewerPane.screenToWorldVelocity(worldMouseMoveSafe);
			dragVelocityX -= worldMouseMoveSafe.getX();
			dragVelocityY -= worldMouseMoveSafe.getY(); 
			driftVelocityX = 0;
			driftVelocityY = 0;
			
			Point2D worldDragVector = new Point2D.Double(screenX - startDragScreenX, screenY - startDragScreenY);
			this.viewerPane.screenToWorldVelocity(worldDragVector);
			this.viewX = startDragViewX - worldDragVector.getX();
			this.viewY = startDragViewY - worldDragVector.getY();
			
			if (viewerPane != null) viewerPane.needsViewUpdate = true;
		}
		
		this.mouseDragged();
	}

	void internalMouseReleased(int click, double worldX, double worldY, double screenX, double screenY) {
		internalMouseDragged(click, worldX, worldY, screenX, screenY);
		
		if (click == DRAG_MOUSE_BUTTON) {
			double driftSpeed = Math.sqrt(dragVelocityX*dragVelocityX + dragVelocityY*dragVelocityY);
			boolean doThrow = (driftSpeed > getThrowThreshold());
			if (doThrow) {
				this.driftVelocityX = dragVelocityX*20;
				this.driftVelocityY = dragVelocityY*20;
			} else {
				this.driftVelocityX = 0;
				this.driftVelocityY = 0;
			}
			panning = false;
		}
		
		this.mouseReleased();
	}

	protected double getThrowThreshold() {
		return 3.0;
	}
	
	void internalMouseMoved(double worldX, double worldY, double screenX, double screenY) {
		mouseWorldDX = (worldX - mouseWorldX); 
		mouseWorldDY = (worldY - mouseWorldY); 
		mouseWorldX = worldX;
		mouseWorldY = worldY;
		
		mouseScreenDX = (screenX - mouseScreenX); 
		mouseScreenDY = (screenY - mouseScreenY); 
		mouseScreenX = screenX;
		mouseScreenY = screenY;
		
		dragVelocityX = 0; 
		dragVelocityY = 0; 
		this.mouseMoved();
	}

	void internalMouseScrolled(double clicks) {
		if (!panning) {
			double multiply = Math.pow(1.2, -clicks);
			viewZoom *= multiply;
			if (viewZoom > getMaxZoom()) viewZoom = getMaxZoom();
			if (viewZoom < getMinZoom()) viewZoom = getMinZoom();
			if (viewerPane != null) viewerPane.needsViewUpdate = true;
		}
		
		mouseScroll = clicks;
		this.mouseScrolled();
	}

	void internalKeyPressed(KeyEvent e) {
		keyEvent = e;
		this.keyPressed();
	}
	
	void internalKeyReleased(KeyEvent e) {
		keyEvent = e;
		this.keyReleased();
	}
	
	void internalKeyTyped(KeyEvent e) {
		keyEvent = e;
		this.keyTyped();
	}
}
