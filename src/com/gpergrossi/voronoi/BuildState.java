package com.gpergrossi.voronoi;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.function.Predicate;

import com.gpergrossi.util.data.OrderedPair;
import com.gpergrossi.util.data.queue.FixedSizeArrayQueue;
import com.gpergrossi.util.data.queue.PriorityMultiQueue;
import com.gpergrossi.util.data.storage.GrowingStorage;
import com.gpergrossi.util.geom.shapes.Convex;
import com.gpergrossi.util.geom.shapes.LineSeg;
import com.gpergrossi.util.geom.shapes.Ray;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.Event.Type;


/**
 * The BuildState of a voronoi diagram. Does almost all the work.
 * 
 * @author Gregary
 */
public final class BuildState {

	private Voronoi voronoi;

	private PriorityQueue<Event> circleQueue;
	private FixedSizeArrayQueue<Event> siteQueue;
	private PriorityMultiQueue<Event> eventMultiQueue;
	
	private int totalCircleEvents = 0;
	private int invalidCircleEvents = 0;
	private int numEventsProcessed;
	
	private double sweeplineY = Double.NEGATIVE_INFINITY;
	private boolean debugSweeplineOn;
	private double debugSweeplineY;

	private Convex bounds;
	private ShoreTree shoreTree;
	private GrowingStorage<Edge> edges;
	private GrowingStorage<Vertex> vertices;
	private List<Site> sites;
	
	private boolean initialized;
	private boolean finishing;
	private boolean finished;
	
	public BuildState(Voronoi voronoi) {
		this.voronoi = voronoi;
		this.bounds = voronoi.getBounds();
		this.sites = voronoi.getSites();

		// Most construction is done in the initialize() method
		// which is called the first time an event is processed
		
		this.finished = false;
		this.finishing = false;
	}
	
	
	
	

	public int processEvents(int ms) {
		int eventsProcessed = 0;
		
		if (ms == -1) {
			while (!isFinished()) {
				processNextEvent();
				eventsProcessed++;
			}
			return eventsProcessed;
		}
		
		long start = System.currentTimeMillis();
		while (!isFinished()) {
			processNextEvent();
			eventsProcessed++;
			if ((System.currentTimeMillis() - start) >= ms) break;
		}
		return eventsProcessed;
	}
	
	public void processNextEvent() {
		if (!initialized) {
			initialize();
			return;
		}
		
		Event e = eventMultiQueue.poll();
		
		if (e == null) {
			finish();
			return;
		} else if (!e.valid) {
			if (Voronoi.DEBUG) System.out.println("Discarded invalid circle event");
			invalidCircleEvents++;
			return;
		}
		
		advanceSweepLine(e);

		// Process the event
		switch(e.type) {
			case SITE:		processSiteEvent(e.site);	break;
			case CIRCLE:	processCircleEvent(e.arc);	break;
		}
		
		numEventsProcessed++;
		if (Voronoi.DEBUG) printDebugEvent(e);
	}	

	public void processNextEventVerbose() {
		boolean hold = Voronoi.DEBUG;
		Voronoi.DEBUG = true;
		processNextEvent();
		Voronoi.DEBUG = hold;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public Voronoi getResult() {
		return this.voronoi;
	}
	
	public int getNumEventsProcessed() {
		return numEventsProcessed;
	}

	public int getTheoreticalMaxSteps() {
		int numSites = sites.size();
		int maxPossibleCircleEvents = numSites*2 - 5;
		if (numSites <= 2) maxPossibleCircleEvents = 0;
		return numSites + maxPossibleCircleEvents;
	}
	
	public double getSweeplineY() {
		return sweeplineY;
	}
	
	public Convex getBounds() {
		return bounds;
	}

	
	
	
	public void debugAdvanceSweepline(double v) {
		this.debugSweeplineOn = true;
		this.debugSweeplineY = sweeplineY;
		this.sweeplineY += v;
	}
	
	private void printDebugEvent(Event e) {
		int siteCount = siteQueue.size();
		int circleCount = circleQueue.size();
		
		System.out.println("Processed: "+numEventsProcessed+" events so far. "+siteCount+" site events and "+circleCount+" circle events remaining.");
		System.out.println("just processed: "+e+", next: ");
		
		System.out.println("========== NEXT EVENTS ==========");
		printSome(eventMultiQueue, 15);
		
		System.out.println("============ TREELIST ===========");
		printSome(shoreTree, 15);
		
		System.out.println();
	}
	
	private static void printSome(Iterable<?> list, int num) {
		int i = 0;
		Iterator<?> iterator = list.iterator();
		while (iterator.hasNext()) {
			Object o = iterator.next();
			if (i < num) System.out.println(o);
			i++;
		}
		if (i > num) System.out.println("("+i+" more...)");
	}
	
	public void drawDebugState(Graphics2D g) {
		g.setFont(new Font("Consolas", Font.PLAIN, 12));
		if (this.isFinished()) {
			AffineTransform transform = g.getTransform();
			AffineTransform identity = new AffineTransform();
			
			// Draw polygons
			Random r = new Random(0);
			for (Site s : sites) {
				Path2D shape = new Path2D.Double();
				boolean started = false;
				for (Vertex vert : s.vertices) {
					if (vert == null) break;
					if (!started) {
						shape.moveTo(vert.x, vert.y);
						started = true;
						continue;
					}
					shape.lineTo(vert.x, vert.y);
				}
				shape.closePath();
				
				g.setColor(Color.getHSBColor(r.nextFloat(), 1.0f, 0.5f + r.nextFloat()*0.5f));
				g.fill(shape);
			}
			
			// Draw vertices
			g.setColor(Color.BLACK);
			for (Vertex vert : vertices) {
				Ellipse2D ellipse = new Ellipse2D.Double(vert.x-3, vert.y-3, 6, 6);
				g.fill(ellipse);
			}
			
			// Draw edges
			for (Edge edge : edges) {
				Point2D v0 = edge.getStart().toPoint2D();
				Point2D v1 = edge.getEnd().toPoint2D();
				
				g.setColor(Color.WHITE);
				Line2D line = new Line2D.Double(v0, v1);
				g.draw(line);
				
				Ellipse2D ellipse0 = new Ellipse2D.Double(v0.getX()-0.75, v0.getY()-0.75, 1.5, 1.5);
				Ellipse2D ellipse1 = new Ellipse2D.Double(v1.getX()-0.75, v1.getY()-0.75, 1.5, 1.5);
				g.fill(ellipse0);
				g.fill(ellipse1);

				g.setColor(new Color(0,255,0));
				Double2D center = edge.getCenter();
				int firstID = -1;
				if (edge.sites.first != null) firstID = edge.sites.first.index;
				int secondID = -1;
				if (edge.sites.second != null) secondID = edge.sites.second.index;
				
				g.setTransform(identity);
				Point2D pt = new Point2D.Double(center.x(), center.y());
				transform.transform(pt, pt);
				g.drawString(firstID+" : "+secondID, (int) pt.getX(), (int) pt.getY());
				g.setTransform(transform);
			}
			
			// Draw sites
			g.setXORMode(Color.BLACK);
			g.setColor(Color.WHITE);
			for (Site s : sites) {
				Ellipse2D sitedot = new Ellipse2D.Double(s.point.x()-1, s.point.y()-1, 2, 2);
				g.fill(sitedot);
			}
			g.setPaintMode();
			
			g.setColor(Color.BLACK);
			for (Site s : sites) {				
				g.setTransform(identity);
				Point2D pt = new Point2D.Double(s.point.x(), s.point.y());
				transform.transform(pt, pt);
				g.drawString(""+s.index, (int) pt.getX(), (int) pt.getY());
				g.setTransform(transform);
			}
		} else if (this.initialized) {
			AffineTransform transform = g.getTransform();
			AffineTransform identity = new AffineTransform();
			
			// Draw edges
			for (Edge edge : edges) {
				g.setColor(Color.WHITE);
				Line2D line = edge.toLineSeg().asAWTShape();
				g.draw(line);
				
				Double2D center = edge.getCenter();
				
				int firstID = -1;
				boolean firstBoundary = false;
				if (edge.sites.first != null) {
					firstID = edge.sites.first.index;
					if (edge.vertices.first.isBoundary) firstBoundary = true;
				}
				
				int secondID = -1;
				boolean secondBoundary = false;
				if (edge.sites.second != null) {
					secondID = edge.sites.second.index;
					if (edge.vertices.second.isBoundary) secondBoundary = true;
				}
				
				String edgeDescription = (firstBoundary ? "*" : "") + firstID+" : "+secondID + (secondBoundary ? "*" : "");
				
				g.setTransform(identity);
				Point2D pt = new Point2D.Double(center.x(), center.y());
				transform.transform(pt, pt);
				
				g.setColor(new Color(0,128,0));
				g.drawString(edgeDescription, (int) pt.getX(), (int) pt.getY());
				
				g.setTransform(transform);
			}
			
			// Draw vertices
			for (Vertex vert : vertices) {
				
				if (vert.sites != null) {
					g.setColor(new Color(0, 0, 64));
					for (Site site : vert.sites) {
						if (site == null) continue;
						Line2D line = new Line2D.Double(site.getX(), site.getY(), vert.x, vert.y);
						g.draw(line);
					}
				}

				g.setColor(Color.DARK_GRAY);
				Ellipse2D ellipse = new Ellipse2D.Double(vert.x-3, vert.y-3, 6, 6);
				g.fill(ellipse);
				
				if (vert.isBoundary) {
					g.setColor(Color.GRAY);
					g.draw(ellipse);
				}
			}
			
			if (!finishing) {
				// Draw shore tree (edges, circle events, parabolas)
				this.shoreTree.draw(this, g);
		
				// Draw sweep line
				g.setColor(Color.YELLOW);
				Line2D line = new Line2D.Double(bounds.getBounds().minX(), sweeplineY, bounds.getBounds().maxX(), sweeplineY);
				g.draw(line);
			}
			
			// Draw sites and site labels			
			for (Site s : sites) {
				Ellipse2D sitedot = new Ellipse2D.Double(s.point.x()-1, s.point.y()-1, 2, 2);
				g.setColor(new Color(0,128,128));
				if (isCircleEventPassed(s)) g.setColor(Color.RED);				
				g.draw(sitedot);
				
				g.setTransform(identity);
				Point2D pt = new Point2D.Double(s.point.x(), s.point.y());
				transform.transform(pt, pt);
				g.setColor(new Color(0,255,255));
				g.drawString(""+s.index, (int) pt.getX(), (int) pt.getY());
				g.drawString("("+s.numVertices+")", (int) pt.getX(), (int) pt.getY()+10);
				g.setTransform(transform);
			}
		} else {
			g.draw(bounds.asAWTShape());
			
			Double2D center = bounds.getCentroid();
			g.drawString("Not initialized", (int) center.x(), (int) center.y());
		}
	}
	
	private boolean isCircleEventPassed(Site s) {
		for (Event e : eventMultiQueue) {
			if (e.circle == null) continue;
			double cy = e.circle.y() + e.circle.radius();
			if (sweeplineY < cy) continue;
			if (e.arc.getSite() == s) return true;
		}
		return false;
	}
	
	
	
	
	
	private void advanceSweepLine(Event e) {
		if (e == null) return;
		
		// Restore debug sweep line
		if (debugSweeplineOn) {
			debugSweeplineOn = false;
			sweeplineY = debugSweeplineY;
		}

		// Advance sweep line
		sweeplineY = e.y;
	}
	
	private void initialize() {
		this.shoreTree = new ShoreTree();
		this.edges = new GrowingStorage<>(t -> new Edge[t], sites.size()*5); // Initial capacity based on experiments
		this.vertices = new GrowingStorage<>(t -> new Vertex[t], sites.size()*5); // Initial capacity based on experiments
		
		// Create site events
		Event[] siteEvents = new Event[sites.size()];
		for (int i = 0; i < sites.size(); i++) {
			siteEvents[i] = Event.createSiteEvent(sites.get(i));
		}
		
		// Sort the site events array, could cause an exception because identical sites are not allowed
		try {
			Arrays.sort(siteEvents);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Cannot build, multiple sites with same position");
		}
		
		// Create a queue consuming the siteEvents array
		this.circleQueue = new PriorityQueue<Event>(sites.size()*2);
		this.siteQueue = FixedSizeArrayQueue.consume(siteEvents);
		this.eventMultiQueue = new PriorityMultiQueue<Event>();
		eventMultiQueue.addQueue(circleQueue);
		eventMultiQueue.addQueue(siteQueue);
		
		// Initialize shore tree
		Event e = eventMultiQueue.poll();
		if (e == null) throw new RuntimeException("Cannot initialize diagram, no sites provided");
		shoreTree.initialize(e.site);
		this.numEventsProcessed = 1;
		
		initialized = true;
	}
	
	private static final Predicate<ShoreBreakpoint> NEW_BREAKPOINTS = (bp -> bp != null && bp.edge == null);

	private void processSiteEvent(Site newSite) {
		
		/* Form a new arc in the shore tree for the newSite */
		ShoreArc arcUnderSite = shoreTree.getArcUnderSite(this, newSite);
		invalidateEvent(arcUnderSite.getCircleEvent());
		ShoreArc newArc = arcUnderSite.insertArc(this, newSite);
		
		/* Update the circle events of the new arc's neighboring arcs */
		for (ShoreArc neighbor : newArc.getNeighborArcs()) {
			invalidateEvent(neighbor.getCircleEvent());
			Event circleEvent = neighbor.checkCircleEvent(this);
			if (circleEvent != null) {
				if (Voronoi.DEBUG) System.out.println("New circle event arising from site event check on "+neighbor);
				addEvent(circleEvent);
			}
		}
		
		/* Form new edges around the newly created arc */
		OrderedPair<ShoreBreakpoint> bps = newArc.getBreakpoints().filter(NEW_BREAKPOINTS);
		if (bps.size() == 0) throw new RuntimeException("Site event did not create any breakpoints!");
		switch (bps.size()) {
		case 1:
			// In the case that the new site was at the same Y coordinate as the site "below" it,
			// which happens when the shore line is very young, there will be only one breakpoint
			ShoreBreakpoint bp = bps.first;
			Vertex vertex = new Vertex(bp.getPosition(this));
			bp.edge = new Edge(bp, vertex);
			break;
		case 2:
			// Otherwise two breakpoints will form, moving exactly opposite each other as the
			// sweep line progresses. We form "twin" edges following these breakpoints and merge them later
			Vertex sharedVertex = new Vertex(bps.first.getPosition(this));
			OrderedPair<HalfEdge> twins = HalfEdge.createTwinPair(bps, sharedVertex);
			bps.get(0).edge = twins.get(0);
			bps.get(1).edge = twins.get(1);
			break;
		}
	}





	private void processCircleEvent(ShoreArc arc) {
		// Save these, they will change and we need original values
		OrderedPair<ShoreArc> neighbors = arc.getNeighborArcs(); 
		ShoreBreakpoint predecessor = arc.getPredecessor();
		ShoreBreakpoint successor = arc.getSuccessor();
		
		// Step 1. Finish the edges of each breakpoint
		Double2D predPos = predecessor.getPosition(this);
		Vertex sharedVertex = new Vertex(predPos.x(), predPos.y());
		vertices.add(sharedVertex);
		for (ShoreBreakpoint bp : arc.getBreakpoints()) {
			if (bp.edge == null) throw new RuntimeException("Circle event expected non-null edge");
			if (bp.edge.isFinished()) throw new RuntimeException("Circle even expected unfinished edge");
			
			bp.edge.finish(sharedVertex);
			addEdge(bp.edge);
			bp.edge = null;
		}
		
		// Step 2. Remove arc and one of its breakpoints
		ShoreTreeNode parentBreakpoint = arc.getParent();
		ShoreTreeNode sibling = arc.getSibling();
		if (parentBreakpoint != successor && parentBreakpoint != predecessor) {
			if (arc.isLeftChild()) throw new RuntimeException("Unexpected successor! "+successor + ", should be "+parentBreakpoint);
			if (arc.isRightChild()) throw new RuntimeException("Unexpected predecessor! "+predecessor + ", should be "+parentBreakpoint);
			throw new RuntimeException("The parent of any arc should be its successor or its predecessor!");
		}
		sibling.removeFromParent();
		parentBreakpoint.replaceWith(sibling);
		
		// Step 3. Update the remaining breakpoint
		ShoreBreakpoint remainingBP = null;
		if (parentBreakpoint == successor) remainingBP = predecessor;
		if (parentBreakpoint == predecessor) remainingBP = successor;
		remainingBP.updateArcs();
		
		// Step 4. Update circle events
		for (ShoreArc neighbor : neighbors) {
			invalidateEvent(neighbor.getCircleEvent());
			Event newCircleEvent = neighbor.checkCircleEvent(this);
			if (newCircleEvent != null) {
				if (Voronoi.DEBUG) System.out.println("New circle event arising from circle event check on "+neighbor);
				addEvent(newCircleEvent);
			}
		}
		
		// Step 5. Form new edge
		remainingBP.edge = new Edge(remainingBP, sharedVertex);
	}
	
	
	
	
	
	private static interface Work {
		/**
		 * Do some work, return true if finished, otherwise keep track of own state 
		 * and return false so that this work can be resume by a later call to work()
		 * @return finished?
		 */
		public boolean work(BuildState self);
	}
	
	private static enum FinishingStep {
		
		BEGIN					("", self -> {return true;}),
		EXTENDING_EDGES 		("Extending unfinished edges", self -> self.extendUnfinishedEdges()),
		JOINING_HALF_EDGES		("Joining half edges", self -> self.joinHalfEdges()),
		CLIPPING_EDGES			("Clipping edges against bounding shape", self -> self.clipEdges()),
		CREATING_LINKS			("Creating links between diagram elements", self -> self.createLinks()),
		SORTING_LISTS			("Sorting vertex and edge lists to counterclockwise order", self -> self.sortSiteLists()),
		CREATING_BOUNDARY		("Creating boundary edges", self -> self.createBoundaryEdges()),
		DONE					("Done", self -> self.finishComplete());
		
		String message;
		Work stepMethod;
		
		private FinishingStep(String msg, Work stepMethod) {
			this.message = msg;
			this.stepMethod = stepMethod;
		}
		
		public static FinishingStep next(FinishingStep step) {
			int nextOrdinal = step.ordinal()+1;
			if (nextOrdinal >= values().length) return DONE;
			return values()[nextOrdinal];
		}
		
		public boolean doWork(BuildState self) {
			return stepMethod.work(self);
		}
		
	}
	
	/**
	 * Record incremental progress because finishing can take a long time and
	 * ideally the worker threads will not stall significantly when calling doWork()
	 * These allow us to return and come back later for more work
	 */
	private FinishingStep currentFinishStep = null;
	private long iterationStartTime, totalStepTimeInvested;
	
	private void finish() {
		if (finished) return;
		finishing = true;
		
		if (currentFinishStep == null) {
			if (sweeplineY < bounds.getBounds().maxY()) sweeplineY = bounds.getBounds().maxY();
			currentFinishStep = FinishingStep.BEGIN;
		}
		
		long startTime = System.currentTimeMillis();
		
		while (!finished) {
			iterationStartTime = System.currentTimeMillis();

			if (Voronoi.DEBUG_FINISH) System.out.println(currentFinishStep.message+"...");
			
			// Do some work on the current step
			boolean stepComplete = currentFinishStep.doWork(this);
			totalStepTimeInvested += (System.currentTimeMillis() - iterationStartTime);
			
			if (stepComplete) {
				// If done with step, move to next step
				FinishingStep previousStep = currentFinishStep;
				currentFinishStep = FinishingStep.next(currentFinishStep);
				
				// Optionally print progress information
				if (Voronoi.DEBUG_FINISH) {
					if (previousStep != FinishingStep.BEGIN) {
						System.out.println("Elapsed time: "+totalStepTimeInvested+" ms");
					}
					if (previousStep != FinishingStep.DONE) {
						System.out.println();
						System.out.println(currentFinishStep.message+"...");
					}
					return;
				}
				
				// Reset step timer
				totalStepTimeInvested = 0;
			} else {
				if (Voronoi.DEBUG_FINISH) System.out.print(".");
			}
			
			if (System.currentTimeMillis() - startTime > 200) return;
		}
	}
	
	/**
	 * Projects all unfinished edges out to the bounding box and gives them a closing vertex
	 */
	private boolean extendUnfinishedEdges() {		
		Double2D.Mutable temp = new Double2D.Mutable();
		for (ShoreTreeNode node : shoreTree) {
			if (!(node instanceof ShoreBreakpoint)) continue;
			ShoreBreakpoint bp = (ShoreBreakpoint) node;
			
			Edge edge = bp.edge;
			if (edge == null || edge.isFinished()) {
				throw new RuntimeException("All breakpoints in shore tree should have partial edges");
			}
			
			Vertex start = edge.getStart();
			Double2D direction = bp.getDirection();
			
			Ray edgeRay = new Ray(start.x, start.y, direction.x(), direction.y());
			LineSeg intersect = bounds.clip(edgeRay);
			
			Double2D endPoint = null;
			if (intersect == null) {
				endPoint = bp.getPosition(this).add(direction); 
			} else {
				intersect.getEnd(temp);
				endPoint = temp;
			}
			
			Vertex vert = new Vertex(endPoint.x(), endPoint.y(), true);
			vertices.add(vert);
			edge.finish(vert);
			addEdge(edge);
			bp.edge = null;
		}
		
		return true; // Step completed
	}
	
	/**
	 * Joins all half edges into full edges.
	 * <pre>
	 * before:  A.end <-- A.start == B.start --> B.end
	 * after:   C.start -----------------------> C.end
	 * </pre>
 	 */
	private boolean joinHalfEdges() {
		Iterator<Edge> edgeIterator = edges.iterator();
		while (edgeIterator.hasNext()) {
			Edge e = edgeIterator.next();
			if (!e.isHalf()) continue;
			
			HalfEdge edge = (HalfEdge) e;
			HalfEdge twin = edge.getTwin();
			
			if (edge.hashCode() > twin.hashCode()) {
				edgeIterator.remove();
				continue;
			}
			
			edge.joinHalves();
		}
		
		return true; // Step completed
	}

	private Iterator<Edge> clipEdgesIterator;
	private Queue<Edge> clipEdgesFixes;
	
	/**
	 * Clip edges to bounding polygon
	 */
	private boolean clipEdges() {
		if (clipEdgesIterator == null) {
			clipEdgesIterator = edges.iterator();
			clipEdgesFixes = new LinkedList<>();
		}
		
		boolean atLeastOne = false;
		
		while (clipEdgesIterator.hasNext()) {
			// Return to working thread and indicate that we are not yet finished
			if (atLeastOne && System.currentTimeMillis() - iterationStartTime > 0) return false;
			atLeastOne = true;
			
			Edge edge = clipEdgesIterator.next();
			if (!edge.isFinished()) throw new RuntimeException("unfinished edge");
			
			Vertex start = edge.getStart();
			Vertex end = edge.getEnd();
			
			LineSeg seg = edge.toLineSeg();
			
			boolean shouldRemove = false;
			
			if (bounds.intersects(seg)) {
				seg = bounds.clip(seg);
				if (seg.length() < Double2D.EPSILON) shouldRemove = true;
			} else {
				shouldRemove = true;
			}
			
			if (shouldRemove) {
				// Edge is outside of bounds and should be removed from the diagram
				clipEdgesIterator.remove();
				
				// Remove start vertex
				if (start.numEdges > 0) {
					for (Edge e : start.edges) {
						if (e == null) break;
						clipEdgesFixes.add(e);
					}
				}
				vertices.remove(start);

				// Remove end vertex
				if (end.numEdges > 0) {
					for (Edge e : end.edges) {
						if (e == null) break;
						clipEdgesFixes.add(e);
					}
				}
				vertices.remove(end);
				
				if (Voronoi.DEBUG_FINISH) {
					int siteLeft = edge.getSiteLeft() == null ? -1 : edge.getSiteLeft().getID();
					int siteRight = edge.getSiteRight() == null ? -1 : edge.getSiteRight().getID();
					
					System.out.println("Edge "+siteLeft+":"+siteRight+" discarded because it is outside the clipping bounds");
					return false;
				}
				continue;
			}
			
			boolean sameStart = Double2D.equals(seg.getStartX(), seg.getStartY(), start.x, start.y);
			boolean sameEnd = Double2D.equals(seg.getEndX(), seg.getEndY(), end.x, end.y);
			
			if (!vertices.contains(start)) sameStart = false;
			if (!vertices.contains(end)) sameEnd = false;
			
			if (!sameStart || !sameEnd) {
				if (Voronoi.DEBUG_FINISH) {
					System.out.println("Edge clipped from ("+start.x+","+start.y+")-("+end.x+","+end.y+")   to   ("+seg.getStartX()+","+seg.getStartY()+")-("+seg.getEndX()+","+seg.getEndY()+")");
					System.out.println();
				}
				
				if (!sameStart) {
					vertices.remove(start);
					start = new Vertex(seg.getStartX(), seg.getStartY(), true);
					vertices.add(start);
				}
				
				if (!sameEnd) {
					vertices.remove(end);
					end = new Vertex(seg.getEndX(), seg.getEndY(), true);
					vertices.add(end);
				}
				
				edge.redefine(start, end);
			}
			start.addEdge(edge);
			end.addEdge(edge);
		}
		
		// Cleanup edges whose vertices got removed
		while (!clipEdgesFixes.isEmpty()) {
			if (atLeastOne && System.currentTimeMillis() - iterationStartTime > 0) return false;
			atLeastOne = true;
			
			Edge edge = clipEdgesFixes.poll();
			
			Vertex start = edge.getStart();
			Vertex end = edge.getEnd();
			
			boolean sameStart = vertices.contains(start);
			boolean sameEnd = vertices.contains(end);
			
			if (!sameStart || !sameEnd) {
				if (!vertices.contains(start)) {
					start = new Vertex(start.x, start.y, true);
					vertices.add(start);
				}
					
				if (!vertices.contains(end)) {
					end = new Vertex(end.x, end.y, true);
					vertices.add(end);
				}
				
				edge.redefine(start, end);
			}
			start.addEdge(edge);
			end.addEdge(edge);
		}
		
		return true; // Step completed
	}
	
	/**
	 * Adds all edges, vertices, and sites to each others' lists using the
	 * references already defined in edges
	 */
	private boolean createLinks() {
		Iterator<Vertex> verts = vertices.iterator();
		while (verts.hasNext()) {
			Vertex vertex = verts.next();
			if (vertex.numEdges == 0) verts.remove();
			vertex.numEdges = 0;
		}
		
		for (Edge edge : edges) {
			if (edge == null) break;
			for (Vertex vertex : edge.vertices) {
				vertex.addEdge(edge);
				for (Site site : edge.sites) {
					if (!vertex.hasSite(site)) {
						vertex.addSite(site);
						site.addVertex(vertex);
					}
					if (!site.hasEdge(edge)) site.addEdge(edge);
				}
			}
		}
		return true; // Step completed
	}
	
	/**
	 * Sorts the vertex and edge lists in each site to be in counterclockwise order
	 */
	private boolean sortSiteLists() {	
		for (Site s : sites) sortSiteLists(s);
		return true; // Step completed
	}
	
	private static void sortSiteLists(Site s) {
		final double[] vertexAngles = new double[s.numVertices];
		for (int i = 0; i < s.numVertices; i++) {
			Vertex vert = s.vertices[i];
			vertexAngles[i] = Double2D.angle(vert.x, vert.y, s.point.x(), s.point.y());
		}
		
		final double[] edgeAngles = new double[s.numEdges];
		for (int i = 0; i < s.numEdges; i++) {
			Double2D center = s.edges[i].getCenter();
			edgeAngles[i] = Double2D.angle(center.x(), center.y(), s.point.x(), s.point.y());
		}
		
		s.sortVertices(vertexAngles);
		s.sortEdges(edgeAngles);
	}
	
	/**
	 * Forms a new edge or edges for sites along the boundary of the diagram.	
	 */
	private boolean createBoundaryEdges() {
		// Create corner vertices
		Vertex[] corners = new Vertex[bounds.getNumVertices()];
		for (int i = 0; i < corners.length; i++) {
			corners[i] = new Vertex(bounds.getVertex(i).x(), bounds.getVertex(i).y(), true);
		}
		
		// Assign corner vertices to appropriate sites
		for (Vertex corner : corners) {
			vertices.add(corner);
			Site closest = null;
			double distance2 = Double.MAX_VALUE;
			for (Site s : sites) {
				double dx = s.point.x() - corner.x;
				double dy = s.point.y() - corner.y;
				double dist2 = dx*dx + dy*dy;
				if (dist2 < distance2) {
					distance2 = dist2;
					closest = s;
				}
			}
			corner.addSite(closest);
			closest.addVertex(corner);
			sortSiteLists(closest);
		}

		// Add new edges
		for (Site s : sites) {
			Vertex prev = null;
			Vertex last = s.getLastVertex();
			if (last == null) continue;
			if (last.isBoundary) prev = last;
			boolean modified = false;
			for (Vertex v : s.vertices) {
				if (v == null) break;
				if (!v.isBoundary) {
					prev = null;
					continue;
				}
				if (prev != null) {		
					Edge edge = new Edge(prev, v, s, null);
					v.addEdge(edge);
					prev.addEdge(edge);
					s.addEdge(edge);
					addEdge(edge);
					modified = true;
				}
				prev = v;
			}
			if (modified) sortSiteLists(s);
		}
		
		return true; // Step completed
	}

	private boolean finishComplete() {
		this.voronoi.finalizeSites();
		this.voronoi.setVertices(this.vertices);
		this.voronoi.setEdges(this.edges);
		finished = true;
		
		double p = ((double) invalidCircleEvents) / ((double) totalCircleEvents) * 100.0;
		int percent = (int) Math.round(p);
		if (Voronoi.DEBUG_FINISH) System.out.println("Invalid circle events: "+invalidCircleEvents+" / "+totalCircleEvents+" ("+percent+"%)");
		
		return true; // Step completed
	}
	
	
	
	
	
	private void addEdge(Edge edge) {
		if (!edge.isFinished()) throw new RuntimeException("Cannot add unfinished edge");
		this.edges.add(edge);
	}

	private void addEvent(Event e) {
		if (e == null) throw new RuntimeException("Cannot add null event");
		if (e.type == Type.CIRCLE) {
	 		circleQueue.offer(e);
			totalCircleEvents++;
		} else throw new RuntimeException("Site events are only added in BuildState.initSiteEvents");
	}
	
	private static void invalidateEvent(Event e) {
		if (e == null) return;
		e.valid = false;
	}
	
}
