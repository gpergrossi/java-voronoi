package com.gpergrossi.voronoi;

import static com.gpergrossi.util.data.btree.IBinaryNode.getTreeBreadthAndDepth;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.gpergrossi.util.data.OrderedPair;
import com.gpergrossi.util.geom.shapes.Circle;
import com.gpergrossi.util.geom.shapes.Rect;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.util.math.func.Function;
import com.gpergrossi.util.math.func.Quadratic;
import com.gpergrossi.util.math.func.Vertical;

public class ShoreTree implements Iterable<ShoreTreeNode> {
	
	ShoreTreeNode root;
	
	ShoreTreeNode getRoot() {
		return root;
	}

	void setRoot(ShoreTreeNode node) {
		if (node == null) throw new RuntimeException("Cannot remove root node from ShoreTree");
		if (!(node instanceof ShoreTreeNode)) throw new RuntimeException("ShoreTree can only have a root of type TreeNode");
		
		root.rootParent = null;
		if (node != null) node.rootParent = this;
		this.root = (ShoreTreeNode) node;
	}
	
	public void initialize(Site site) {
		if (root != null) throw new RuntimeException("Tree has already been initialized");
		root = new ShoreArc(this, site);
		if (Voronoi.DEBUG) System.out.println("initialized "+root);
	}
	
	public boolean isInitialized() {
		return (root != null);
	}
	
	public ShoreArc getArcUnderSite(final BuildState state, Site site) {
		if (root == null) throw new RuntimeException("Tree has not yuet been initialized");
		return root.getArc(state, site.point.x());
	}
		
	public void draw(final BuildState state, Graphics2D g) {
		if (root == null) return;

		boolean debug = Voronoi.DEBUG;
		Voronoi.DEBUG = false;
		
		drawCircleEvents(state, g, root.iterator());
		drawParabolas(state, g, root.iterator());
		drawBreakpoints(state, g, root.iterator());
		
		drawTree(state, g);
		
		Voronoi.DEBUG = debug;
	}

	private static void drawCircleEvents(BuildState state, Graphics2D g, Iterator<ShoreTreeNode> nodes) {
		g.setColor(Color.BLUE);
		
		while (nodes.hasNext()) {
			ShoreTreeNode n = nodes.next();
			
			if (!(n instanceof ShoreArc)) continue;
			ShoreArc arc = (ShoreArc) n;
			
			Event circleEvent = arc.getCircleEvent();
			if (circleEvent == null) {
				n = n.getSuccessor();
				continue;
			}

			ShoreBreakpoint leftBP = (ShoreBreakpoint) arc.getPredecessor();
			ShoreBreakpoint rightBP = (ShoreBreakpoint) arc.getSuccessor();
			
			Double2D intersection = ShoreBreakpoint.getIntersection(state, leftBP, rightBP);
			if (intersection != null) {
				g.setColor(new Color(0,0,128));
				Double2D leftPos = leftBP.getPosition(state);
				Double2D rightPos = rightBP.getPosition(state);
				Line2D lineBP0 = new Line2D.Double(leftPos.toPoint2D(), intersection.toPoint2D());
				Line2D lineBP1 = new Line2D.Double(rightPos.toPoint2D(), intersection.toPoint2D());
				g.draw(lineBP0);
				g.draw(lineBP1);
				g.setColor(Color.BLUE);
			} else {
				g.setColor(Color.RED);
			}
			
			Circle circle = circleEvent.circle;
			
			Ellipse2D bpe = new Ellipse2D.Double(circle.x() - circle.radius(), circle.y() - circle.radius(), circle.radius()*2, circle.radius()*2);
			g.draw(bpe);
			Line2D line0 = new Line2D.Double(circle.x()-3, circle.y()-3, circle.x()+3, circle.y()+3);
			Line2D line1 = new Line2D.Double(circle.x()-3, circle.y()+3, circle.x()+3, circle.y()-3);
			g.draw(line0);
			g.draw(line1);
		}
	}

	private static void drawParabolas(BuildState state, Graphics2D g, Iterator<ShoreTreeNode> nodes) {
		while (nodes.hasNext()) {
			ShoreTreeNode n = nodes.next();
			
			if (!(n instanceof ShoreArc)) continue;
			ShoreArc arc = (ShoreArc) n;
			
			Function par = Quadratic.fromPointAndLine(arc.getSite().point.x(), arc.getSite().point.y(), state.getSweeplineY());
			
			Rect bounds = state.getBounds().getBounds();
			double minX = bounds.minX();
			double maxX = bounds.maxX();
			double minY = bounds.minY();
			
			ShoreTreeNode pred = n.getPredecessor(); 
			Double2D predBreakpoint = null;
			if (pred != null && pred instanceof ShoreBreakpoint) {
				ShoreBreakpoint breakpoint = (ShoreBreakpoint) pred;
				predBreakpoint = breakpoint.getPosition(state);
				if (predBreakpoint != null) {
					minX = predBreakpoint.x();
				}
			}
			
			ShoreTreeNode succ = n.getSuccessor(); 
			Double2D succBreakpoint = null;
			if (succ != null && succ instanceof ShoreBreakpoint) {
				ShoreBreakpoint breakpoint = (ShoreBreakpoint) succ;
				succBreakpoint = breakpoint.getPosition(state);
				if (succBreakpoint != null) {
					maxX = succBreakpoint.x();
				}
			}
			
			if (minX < bounds.minX()) minX = bounds.minX();
			if (maxX > bounds.maxX()) maxX = bounds.maxX();
			
			g.setColor(Color.GRAY);
			int step = 1;
			if (!(par instanceof Vertical)) {
				for (int s = (int) minX; s < maxX; s += step) {
					double x = s;
					if (s >= maxX) x = maxX;
					if (s <= minX) x = minX;
					double y0 = par.getValue(x);
					double y1 = par.getValue(x + step);
					
					// do not exceed bounds
					if (y0 < minY && y1 < minY) continue;
					if (y1 < minY) y1 = minY;
					if (y0 < minY) y0 = minY;
					Line2D line = new Line2D.Double(x, y0, x + step, y1);
					g.draw(line);
				}
			} else {
				Vertical vert = (Vertical) par;
				Line2D line = new Line2D.Double(vert.getX(), minY, vert.getX(), state.getSweeplineY());
				g.draw(line);
			}
		}
	}
	
	private static void drawBreakpoints(BuildState state, Graphics2D g, Iterator<ShoreTreeNode> nodes) {
		AffineTransform transform = g.getTransform();
		AffineTransform identity = new AffineTransform();
		
		while (nodes.hasNext()) {
			ShoreTreeNode n = nodes.next();
			
			if (!(n instanceof ShoreBreakpoint)) continue;
			ShoreBreakpoint breakpoint = (ShoreBreakpoint) n;
			
			Double2D posVec = breakpoint.getPosition(state);
			if (posVec == null) continue;
			Point2D pos = posVec.toPoint2D();

			// Draw edge line
			g.setColor(new Color(64,64,64));
			drawPartialEdge(g, breakpoint, state);
			
			// Draw breakpoint circle
			g.setColor(new Color(128,0,0));
			Ellipse2D bpe = new Ellipse2D.Double(pos.getX()-1, pos.getY()-1, 2.0, 2.0);
			g.draw(bpe);
			
			// draw breakpoint label
			g.setTransform(identity);
			transform.transform(pos, pos);
			g.setColor(new Color(255,0,0));
			g.drawString(breakpoint.getArcLeft().getSite().index+":"+breakpoint.getArcRight().getSite().index, (int) pos.getX(), (int) pos.getY());
			g.setTransform(transform);
			
		}
	}

	private static void drawPartialEdge(Graphics2D g, ShoreBreakpoint bp, BuildState state) {
		Edge edge = bp.getEdge();
		if (edge != null) {
			Vertex start = edge.getStart();
			Double2D end = null;
			if (edge.isFinished()) end = edge.getEnd().getPosition();
			else end = bp.getPosition(state);
			Line2D line = new Line2D.Double(start.toPoint2D(), end.toPoint2D());
			g.draw(line);
		}
	}
	
	private void drawTree(BuildState state, Graphics2D g) {
		AffineTransform transform = g.getTransform();
		AffineTransform identity = new AffineTransform();
		
		OrderedPair<Integer> breadthAndDepth = getTreeBreadthAndDepth(this.getRoot());
		double dy = 50.0;
		double minY = state.getBounds().getBounds().maxY()+dy;
		double minX = state.getBounds().getBounds().minX();
		
		g.setColor(Color.BLACK);
		Rectangle2D rect = new Rectangle2D.Double(minX, minY, state.getBounds().getBounds().width(), dy*(breadthAndDepth.second+1));
		g.fill(rect);
		
		List<ShoreTreeNode> layer = new ArrayList<>();
		List<ShoreTreeNode> next = new ArrayList<>();
		layer.add(this.getRoot());
		
		Map<ShoreTreeNode, Double2D> positions = new HashMap<>();
		Map<ShoreTreeNode, Double> splitWidth = new HashMap<>();

		g.setColor(Color.ORANGE);
		int depth = 0;
		while (layer.size() > 0) {
			next.clear();
			
			for (ShoreTreeNode n : layer) {
				double x;
				if (n.getParent() == null) {
					x = state.getBounds().getBounds().centerX();
					double width = state.getBounds().getBounds().width();
					splitWidth.put(n, width);
				} else {
					double parX = positions.get(n.getParent()).x();
					double parW = splitWidth.get(n.getParent());
					double xShare = parW / (getTreeBreadthAndDepth(n.getParent()).first);
					double width = (getTreeBreadthAndDepth(n).first)*xShare;
					splitWidth.put(n, width);
					
					if (n.isLeftChild()) {
						x = parX - parW/2 + width/2.0;
					} else {
						double sibW = (getTreeBreadthAndDepth(n.getSibling()).first+1)*xShare;
						x = parX - parW/2 + sibW + width/2.0;
					}
				}
				
				Double2D pos = new Double2D(x, minY + dy*depth);
				positions.put(n, pos);

				// Draw line from parent
				g.setColor(Color.ORANGE);
				if (n.getParent() != null) {
					Line2D line = new Line2D.Double(pos.toPoint2D(), positions.get(n.getParent()).toPoint2D());
					g.draw(line);
				}
				
				// Draw dot
				g.setColor(Color.WHITE);
				Ellipse2D dot = new Ellipse2D.Double(pos.x()-0.25, pos.y()-0.25, 0.5, 0.5);
				g.draw(dot);
				
				// Draw text label
				Point2D label = pos.toPoint2D();
				g.setTransform(identity);
				transform.transform(label, label);
				g.setColor(Color.RED);
				if (n instanceof ShoreBreakpoint) {
					ShoreBreakpoint bp = (ShoreBreakpoint) n;
					g.drawString("  BP:"+bp.getArcLeft().getSite().index+":"+bp.getArcRight().getSite().index, (int) label.getX(), (int) label.getY());
				}
				if (n instanceof ShoreArc) {
					ShoreArc arc = (ShoreArc) n;
					g.drawString("  Arc:"+arc.getSite().index, (int) label.getX(), (int) label.getY());
				}
				g.setTransform(transform);
				
				
				// Draw children next
				if (n.getLeftChild() != null) next.add(n.getLeftChild());
				if (n.getRightChild() != null) next.add(n.getRightChild());
			}
			
			List<ShoreTreeNode> swap = layer;
			layer = next;
			next = swap;
			depth++;
		}
	}

	@Override
	public Iterator<ShoreTreeNode> iterator() {
		return this.root.iterator();
	}
	
}
