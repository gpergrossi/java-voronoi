package dev.mortus.voronoi;

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
import java.util.PriorityQueue;
import java.util.Random;
import java.util.function.Predicate;

import dev.mortus.util.data.Pair;
import dev.mortus.util.data.queue.FixedSizeArrayQueue;
import dev.mortus.util.data.queue.MultiQueue;
import dev.mortus.util.math.geom2d.LineSeg;
import dev.mortus.util.math.geom2d.Ray;
import dev.mortus.util.math.geom2d.Rect;
import dev.mortus.util.math.vectors.Double2D;
import dev.mortus.voronoi.Event.Type;
import dev.mortus.util.data.storage.GrowingStorage;


/**
 * The BuildState of a voronoi diagram. Does almost all the work.
 * 
 * @author Gregary
 */
public final class BuildState {

	private static final Predicate<ShoreBreakpoint> NEW_BREAKPOINTS = (bp -> bp != null && bp.edge == null);

	private Voronoi voronoi;

	private PriorityQueue<Event> circleQueue;
	private FixedSizeArrayQueue<Event> siteQueue;
	private MultiQueue<Event> eventMultiQueue;
	
	private int totalCircleEvents = 0;
	private int invalidCircleEvents = 0;
	private int numEventsProcessed;
	
	private double sweeplineY = Double.NEGATIVE_INFINITY;
	private boolean debugSweeplineOn;
	private double debugSweeplineY;

	private Rect bounds;
	private ShoreTree shoreTree;
	private GrowingStorage<Edge> edges;
	private GrowingStorage<Vertex> vertices;
	private Site[] sites;
	
	private boolean initialized;
	private boolean finished;
	
	public BuildState (Rect bounds, Double2D[] siteLocations) {
		this.bounds = bounds;
		this.shoreTree = new ShoreTree();

		this.eventMultiQueue = new MultiQueue<Event>();
		this.circleQueue = new PriorityQueue<Event>(siteLocations.length*2);
		eventMultiQueue.addQueue(circleQueue);
		// event Queue is initialized later because it is computationally expensive
		// see the initialize() method, called by processNextEvent()
		
		this.sites = createSites(siteLocations);

		this.voronoi = new Voronoi(bounds);
		this.edges = new GrowingStorage<>(t -> new Edge[t], sites.length*5); // Initial capacity based on experiments
		this.vertices = new GrowingStorage<>(t -> new Vertex[t], sites.length*5); // Initial capacity based on experiments
		
		this.finished = false;
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
		int numSites = sites.length;
		int maxPossibleCircleEvents = numSites*2 - 5;
		if (numSites <= 2) maxPossibleCircleEvents = 0;
		return numSites + maxPossibleCircleEvents;
	}
	
	public double getSweeplineY() {
		return sweeplineY;
	}
	
	public Rect getBounds() {
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
	
	private void printSome(Iterable<?> list, int num) {
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
				if (edge.sites.first != null) firstID = edge.sites.first.id;
				int secondID = -1;
				if (edge.sites.second != null) secondID = edge.sites.second.id;
				
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
				g.drawString(""+s.id, (int) pt.getX(), (int) pt.getY());
				g.setTransform(transform);
			}
		} else {
			AffineTransform transform = g.getTransform();
			AffineTransform identity = new AffineTransform();
			
			// Draw edges
			for (Edge edge : edges) {
				g.setColor(Color.WHITE);
				Line2D line = new Line2D.Double(edge.getStart().toPoint2D(), edge.getEnd().toPoint2D());
				g.draw(line);
				
				Double2D center = edge.getCenter();
				int firstID = -1;
				if (edge.sites.first != null) firstID = edge.sites.first.id;
				int secondID = -1;
				if (edge.sites.second != null) secondID = edge.sites.second.id;
				
				g.setTransform(identity);
				Point2D pt = new Point2D.Double(center.x(), center.y());
				transform.transform(pt, pt);
				g.setColor(new Color(0,255,0));
				g.drawString(firstID+" : "+secondID, (int) pt.getX(), (int) pt.getY());
				g.setTransform(transform);
			}
			
			// Draw shore tree (edges, circle events, parabolas)
			this.shoreTree.draw(this, g);
		
			// Draw sweep line
			g.setColor(Color.YELLOW);
			Line2D line = new Line2D.Double(bounds.minX(), sweeplineY, bounds.maxX(), sweeplineY);
			g.draw(line);
			
			// Draw sites and site labels			
			for (Site s : sites) {
				Ellipse2D sitedot = new Ellipse2D.Double(s.point.x()-1, s.point.y()-1, 2, 2);
				g.setColor(new Color(0,128,0));
				if (isCircleEventPassed(s)) g.setColor(Color.RED);				
				g.draw(sitedot);
				
				g.setTransform(identity);
				Point2D pt = new Point2D.Double(s.point.x(), s.point.y());
				transform.transform(pt, pt);
				g.setColor(new Color(0,255,0));
				g.drawString(""+s.id, (int) pt.getX(), (int) pt.getY());
				g.setTransform(transform);
			}
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

	private Site[] createSites(Double2D[] siteLocations) {
		Site[] sites = new Site[siteLocations.length];
		
		// Create Site objects and assign them an ID corresponding to their index in the siteLocations array
		for (int i = 0; i < siteLocations.length; i++) {
			sites[i] = new Site(this.voronoi, i, siteLocations[i]);
		}
		
		return sites;
	}
	
	private void initialize() {
		// Create site events
		Event[] siteEvents = new Event[sites.length];
		for (int i = 0; i < sites.length; i++) {
			siteEvents[i] = Event.createSiteEvent(sites[i]);
		}
		
		// Sort the site events array, could cause an exception because identical sites are not allowed
		try {
			Arrays.sort(siteEvents);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Cannot build, multiple sites with same position");
		}
		
		// Create a queue consuming the siteEvents array
		this.siteQueue = FixedSizeArrayQueue.consume(siteEvents);
		eventMultiQueue.addQueue(siteQueue);
		
		// Initialize shore tree
		Event e = eventMultiQueue.poll();
		if (e == null) throw new RuntimeException("Cannot initialize diagram, no sites provided");
		shoreTree.initialize(e.site);
		this.numEventsProcessed = 1;
		
		initialized = true;
	}

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
		Pair<ShoreBreakpoint> bps = newArc.getBreakpoints().filter(NEW_BREAKPOINTS);
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
			Pair<HalfEdge> twins = HalfEdge.createTwinPair(bps, sharedVertex);
			bps.get(0).edge = twins.get(0);
			bps.get(1).edge = twins.get(1);
			break;
		}
	}





	private void processCircleEvent(ShoreArc arc) {
		// Save these, they will change and we need original values
		Pair<ShoreArc> neighbors = arc.getNeighborArcs(); 
		ShoreBreakpoint predecessor = arc.getPredecessor();
		ShoreBreakpoint successor = arc.getSuccessor();
		
		// Step 1. Finish the edges of each breakpoint
		Double2D predPos = predecessor.getPosition(this);
		Vertex sharedVertex = new Vertex(predPos.x(), predPos.y());
		addVertex(sharedVertex);
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
		sibling.disconnect();
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
		CLIPPING_EDGES			("Clipping edges against bounding box", self -> self.clipEdges()),
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
		
		if (currentFinishStep == null) {
			if (sweeplineY < bounds.maxY()) sweeplineY = bounds.maxY();
			currentFinishStep = FinishingStep.BEGIN;
		}
		
		long startTime = System.currentTimeMillis();
		
		while (!finished) {
			iterationStartTime = System.currentTimeMillis();
			
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
				endPoint = bp.getPosition(this); 
			} else {
				intersect.getEnd(temp);
				endPoint = temp;
			}
			
			Vertex vert = new Vertex(endPoint.x(), endPoint.y(), true);
			addVertex(vert);
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
	
	/**
	 * Clip edges to bounding rectangle
	 */
	private boolean clipEdges() {
		if (clipEdgesIterator == null) {
			clipEdgesIterator = edges.iterator();
		}
		
		while(clipEdgesIterator.hasNext()) {
			// Return to working thread and indicate that we are not yet finished
			if (System.currentTimeMillis() - iterationStartTime > 200) return false;
			
			Edge edge = clipEdgesIterator.next();
			if (!edge.isFinished()) throw new RuntimeException("unfinished edge");
			
			Vertex start = edge.getStart();
			Vertex end = edge.getEnd();
			
			LineSeg seg = edge.toLineSeg();
			seg = bounds.clip(seg);
			
			if (seg == null) {
				// Edge is outside of bounds, both vertices and the edge should be removed from the diagram
				removeVertex(start);
				removeVertex(end);
				clipEdgesIterator.remove();
				continue;
			}
			
			boolean sameStart = Double2D.equals(seg.getStartX(), seg.getStartY(), start.x, start.y);
			boolean sameEnd = Double2D.equals(seg.getEndX(), seg.getEndY(), end.x, end.y);
			if (sameStart && sameEnd) continue;
			
			if (!sameStart) {
				removeVertex(start);
				start = new Vertex(seg.getStartX(), seg.getStartY(), true);
				addVertex(start);
			}
			
			if (!sameEnd) {
				removeVertex(end);
				end = new Vertex(seg.getEndX(), seg.getEndY(), true);
				addVertex(end);
			}
			
			edge.redefine(start, end);
		}
		
		return true; // Step completed
	}
	
	/**
	 * Adds all edges, vertices, and sites to each others' lists using the
	 * references already defined in edges
	 */
	private boolean createLinks() {
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
	
	private void sortSiteLists(Site s) {
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
		Vertex[] corners = new Vertex[4];
		corners[0] = new Vertex(bounds.minX(), bounds.minY(), true);
		corners[1] = new Vertex(bounds.maxX(), bounds.minY(), true);
		corners[2] = new Vertex(bounds.maxX(), bounds.maxY(), true);
		corners[3] = new Vertex(bounds.minX(), bounds.maxY(), true);
		
		// Assign corner vertices to appropriate sites
		for (Vertex corner : corners) {
			addVertex(corner);
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
		this.voronoi.setMutableSites(this.sites);
		this.voronoi.setMutableVertices(this.vertices);
		this.voronoi.setMutableEdges(this.edges);
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
	
	private void addVertex(Vertex v) {
		this.vertices.add(v);
	}
	
	private void removeVertex(Vertex v) {
		this.vertices.remove(v);
	}

	private void addEvent(Event e) {
		if (e == null) throw new RuntimeException("Cannot add null event");
		if (e.type == Type.CIRCLE) {
	 		circleQueue.offer(e);
			totalCircleEvents++;
		} else throw new RuntimeException("Site events are only added in BuildState.initSiteEvents");
	}
	
	private void invalidateEvent(Event e) {
		if (e == null) return;
		e.valid = false;
	}
	
}
