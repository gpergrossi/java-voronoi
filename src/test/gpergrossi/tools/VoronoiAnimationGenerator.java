package test.gpergrossi.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import com.gpergrossi.util.geom.shapes.Convex;
import com.gpergrossi.util.geom.shapes.Rect;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.Site;
import com.gpergrossi.voronoi.Voronoi;
import com.gpergrossi.voronoi.VoronoiBuilder;
import com.gpergrossi.voronoi.VoronoiWorker;

public class VoronoiAnimationGenerator {

	private static Random random = new Random();
	
	public static void main(String[] args) throws IOException {
		
		System.out.println("Sizes (Must choose 1):");
		System.out.println("s - small  (16 points, 256 x 256 canvas)");
		System.out.println("m - medium (256 points, 512 x 512 canvas)");
		System.out.println("l - large  (16,384 points, 2048 x 2048 canvas)");
		System.out.println("x - extra large (1,048,576 points, 8,192 x 8,192 canvas)");
		System.out.println("i - iterate from 1,050,000 down to 10,000 in steps of -10,000 (drawing disabled)");
		System.out.println();
		System.out.println("Options (Can use any combination):");
		System.out.println("v - verbose, multiple allowed");
		System.out.println("g - constrain initial points to a grid (useful with lots of points to prevent overlap errors)");
		System.out.println();
		System.out.println("Animation (Frame sequence based on order of characters seen):");
		System.out.println("a - add a point to the center of the diagram");
		System.out.println("r - relax, move all points to the centroid of their voronoi site and recreate the diagram");
		System.out.println("f - frame, end the current frame and add it to an output gif");
		System.out.println("c - capture current image to a file");
		System.out.println(":<ms> - time between frames in ms, can only be set once! (default is ':41', ~24 fps)");
		System.out.println();
		
		String line = "";
		try (Scanner s = new Scanner(System.in)) {
			line = s.nextLine();
		}
		
		int frameTime = 41;
		if (line.contains(":")) {
			int i = line.indexOf(':');
			String numStr = "";
			while (true) {
				i++;
				if (i >= line.length()) break;
				if (Character.isDigit(line.charAt(i))) numStr += line.charAt(i);
				else break;
			}
			frameTime = Integer.parseInt(numStr);
		}
		
		int verbosity = countChars(line, 'v');
		boolean grid = line.contains("g");
		
		int num = 0;
		double canvasSize = 10000;
		if (line.contains("s")) { num = 16;		 canvasSize = 256;	 }
		if (line.contains("m")) { num = 256;     canvasSize = 512;	 }
		if (line.contains("l")) { num = 16384;	 canvasSize = 2048;	 }
		if (line.contains("x")) { num = 1048576; canvasSize = 8192;	 }
		if (line.contains("i")) { num = -1; 	 canvasSize = 10000; }
		
		StringBuilder sequenceBuilder = new StringBuilder(line.length());
		for (int i = 0; i < line.length(); i++){
			char c = line.charAt(i);
			if (c == 'a' || c == 'r' || c == 'f' || c == 'c') sequenceBuilder.append(c);
		}
		String sequence = sequenceBuilder.toString();
		
		if (num > 0) {
			doAnimation(num, canvasSize, verbosity, grid, sequence, frameTime);
		} else if (num == -1) {
			for (num = 1050000; num > 0; num -= 10000) {
				doAnimation(num, canvasSize, verbosity, grid, "", 1000);
			}
		} else {
			System.err.println("please specify a size!");
		}
		
	}
	
	private static int countChars(String line, char c) {
		int count = 0;
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == c) count++;
		}
		return count;
	}

	private static void doAnimation(int numSites, double canvasSize, int verbosity, boolean useGrid, String animationSequence, int frameTime) throws IOException {		
		Double2D.ALLOCATION_COUNT = 0;

		BufferedImage frameImage = null;
		ImageOutputStream gifOutput = null;
		GifSequenceWriter gifWriter = null;
		
		if (animationSequence.contains("f")) {
			File file = new File("animation.gif");
			if (file.exists()) file.delete();
			gifOutput = new FileImageOutputStream(file);
			gifWriter = new GifSequenceWriter(gifOutput, BufferedImage.TYPE_INT_ARGB, frameTime, false);
		}
		
		VoronoiBuilder builder = new VoronoiBuilder(numSites);
		builder.setBounds(new Rect(0, 0, canvasSize, canvasSize).toPolygon(4));

		if (verbosity >= 1) System.out.println("Generating "+numSites+" points "+(useGrid ? "(grid constrained)" : "")+"...");
		if (!useGrid) generateSitePoints(builder, numSites, canvasSize);
		else generateSitePointsOnGrid(builder, numSites, canvasSize);

		long startTime = System.nanoTime();
		
		if (verbosity >= 1) System.out.println("Constructing diagram...");
		Voronoi voronoi = buildDiagram(builder, verbosity >= 3);

		int relaxCount = 0;
		int captureCount = 0;
		int frameCount = 0;
		int relaxAddPoints = 0; 
		
		for (int i = 0; i < animationSequence.length(); i++) {
			char command = animationSequence.charAt(i);

			boolean relax = false;
			boolean frameToGif = false;
			boolean frameToPng = false;
			
			switch (command) {
				case 'a':	relaxAddPoints++;	break;
				case 'r':	relax = true; 		break;
				case 'f':	frameToGif = true;	frameCount++;	break;
				case 'c':	frameToPng = true;	captureCount++;	break;
			}
			
			if (relax) {
				relaxCount++;
				if (verbosity >= 1) System.out.println("Relaxing "+relaxCount+"...");
				builder.clearSites(true);
				for (Site s : voronoi.getSites()) {
					builder.addSite(s.getPolygon().getCentroid());
				}
				double center = canvasSize/2;
				double range = 0;
				for (int j = 0; j < relaxAddPoints; j++) {
					while (true) {
						double x = center + random.nextDouble()*2.0*range - range;
						double y = center + random.nextDouble()*2.0*range - range;
						int index = builder.addSiteSafe(new Double2D(x, y), 1);
						if (index != -1) break;
						range += 1;
					}
				}
				relaxAddPoints = 0;
				voronoi = buildDiagram(builder, verbosity >= 3);
				frameImage = null;
			}
			
			if (frameToGif || frameToPng) {
				if (verbosity >= 1) System.out.println("Rendering image "+(frameCount+captureCount));
				if (frameImage == null) frameImage = drawDiagram("relax-"+relaxCount, voronoi, canvasSize, createSizeHeatmapColoring(voronoi));
				if (frameToGif) gifWriter.writeToSequence(frameImage);
				if (frameToPng) ImageIO.write(frameImage, "PNG", new File("capture-"+captureCount+".png"));
			}
		}
		
		long runtimeNanos = System.nanoTime()-startTime;
		double runtimeSeconds = runtimeNanos*0.000000001;
		System.out.println(numSites+", "+runtimeSeconds);

		if (verbosity >= 2) System.out.println(Double2D.ALLOCATION_COUNT+" Vec2's allocated");
		
		if (verbosity >= 2) printStats(voronoi, canvasSize);
		if (animationSequence.contains("f")) {
			gifWriter.close();
			gifOutput.close();
		}
	}

	private static void generateSitePoints(VoronoiBuilder builder, int num, double canvasSize) {
		for (int i = 0; i < num; i++) {
			double px = random.nextDouble()*canvasSize;
			double py = random.nextDouble()*canvasSize;
			builder.addSite(new Double2D(px, py));
		}
	}
	
	private static void generateSitePointsOnGrid(VoronoiBuilder builder, int num, double canvasSize) {
		int grid = (int) Math.ceil(Math.sqrt(num));
		double gridSize = canvasSize / grid;
		int i = 0;
		for (int x = 0; x < grid; x++) {
			for (int y = 0; y < grid; y++) {
				double px = x * gridSize + random.nextDouble()*(gridSize-Double2D.EPSILON*8) + Double2D.EPSILON*4;
				double py = y * gridSize + random.nextDouble()*(gridSize-Double2D.EPSILON*8) + Double2D.EPSILON*4;
				builder.addSite(new Double2D(px, py));
				if (i++ >= num) return;
			}
		}
	}
	
	private static Voronoi buildDiagram(VoronoiBuilder builder, boolean verbose) {
		Voronoi.DEBUG_FINISH = verbose;
		
		Voronoi voronoi;
		long lastPrint = System.currentTimeMillis();
		int numResponses = 0;
		int numEventsProcessed = 0;
		VoronoiWorker w = builder.getBuildWorker();
		
		while (!w.isDone()) {
			numEventsProcessed += w.doWork(1000);
			if (verbose) {
				numResponses++;
				if (System.currentTimeMillis() - lastPrint > 500) {
					System.out.println("Progress: "+w.getProgressEstimate()+" ("+numResponses+" returns, "+numEventsProcessed+" events)");
					numResponses = 0;
					numEventsProcessed = 0;
					lastPrint = System.currentTimeMillis();
				}
			}
		}

		Voronoi.DEBUG_FINISH = false;
		
		voronoi = w.getResult();
		if (verbose) System.out.println();
		return voronoi;
	}
	
	private static void printStats(Voronoi voronoi, double canvasSize) {

		int numSites = voronoi.getSites().size();
		int numVerts = voronoi.getVertices().size();
		int numEdges = voronoi.getEdges().size();
		
		System.out.println();
		System.out.println("====== Stats ======");
		System.out.println("Sites: "+numSites);
		System.out.println("Verts: "+numVerts);
		System.out.println("Edges: "+numEdges);
		System.out.println("V-E+F: "+(numVerts-numEdges+(numSites+1))+" (Should be 2 if perfect)");
		
		double edgesPerSite = 0;
		int minEdges = Integer.MAX_VALUE;
		int maxEdges = 0;
		int[] numSitesPerEdgeCount = new int[20];
		for (Site s : voronoi.getSites()) {
			int edges = s.numEdges();
			edgesPerSite += edges;
			maxEdges = Math.max(maxEdges, edges);
			minEdges = Math.min(minEdges, edges);
			if (edges >= numSitesPerEdgeCount.length) numSitesPerEdgeCount = Arrays.copyOf(numSitesPerEdgeCount, edges+5);
			numSitesPerEdgeCount[edges]++;
		}
		numSitesPerEdgeCount = Arrays.copyOfRange(numSitesPerEdgeCount, minEdges, maxEdges+1);
		edgesPerSite /= voronoi.getSites().size();

		System.out.println();
		System.out.println("----- Number of Sides per Site -----");
		System.out.println("Average edges per site:  "+edgesPerSite);
		System.out.println("Least edges on any site: "+minEdges);
		System.out.println("Most edges on any site:  "+maxEdges);
		printDistribution(numSitesPerEdgeCount, 40);

		int i = 0;
		double[] areas = new double[numSites];
		double totalArea = 0, minArea = Double.MAX_VALUE, maxArea = 0;
		for (Site s : voronoi.getSites()) {
			double area = s.getPolygon().getArea();
			areas[i++] = area;
			totalArea += area;
			minArea = Math.min(minArea, area);
			maxArea = Math.max(maxArea, area);
		}
		double averageArea = (totalArea/numSites);
		
		int NUM_AREA_BUCKETS = 20;
		int[] areaBuckets = new int[NUM_AREA_BUCKETS];
		double areaBucketSize = (maxArea - minArea) / NUM_AREA_BUCKETS;
		double areaVariance = 0;
		for (i = 0; i < numSites; i++) {
			double area = areas[i];
			int bucket = (int) ((area - minArea) / areaBucketSize);
			if (bucket == NUM_AREA_BUCKETS) bucket--;
			areaBuckets[bucket]++;
			double delta = area - averageArea;
			areaVariance += (delta * delta);
		}
		areaVariance /= numSites;
		double areaStdDev = Math.sqrt(areaVariance);
		
		System.out.println();
		System.out.println("----- Area per Site -----");
		System.out.println("Diagram area:  "+(canvasSize * canvasSize));
		System.out.println("Site area sum: "+totalArea);
		System.out.println("Avg area: "+averageArea);
		System.out.println("Min area: "+minArea);
		System.out.println("Max area: "+maxArea);
		System.out.println("Variance: "+areaVariance);
		System.out.println("Std. Dev: "+areaStdDev);
		System.out.println();
		System.out.println("Number of buckets: "+NUM_AREA_BUCKETS);
		System.out.println("Bucket size: "+areaBucketSize);
		printDistribution(areaBuckets, 40);
	}
	
	private static void printDistribution(int[] buckets, int maxLineLength) {
		int minBucket = 0; //Integer.MAX_VALUE;
		int maxBucket = 0;

		System.out.println("Bucket data: ");
		for (int i = 0; i < buckets.length; i++) {
			System.out.print(buckets[i]);
			System.out.print(',');
			minBucket = Math.min(minBucket, buckets[i]);
			maxBucket = Math.max(maxBucket, buckets[i]);
		}
		System.out.println();
		System.out.println("Distribution: ");
		for (int i = 0; i < buckets.length; i++) {
			double length = (double) (buckets[i] - minBucket) / (double) (maxBucket - minBucket);
			int numChars = (int) (length*(maxLineLength+1));
			if (numChars == (maxLineLength+1)) numChars--;

			System.out.print('|');
			for (int j = 0; j < numChars; j++) {
				System.out.print('-');
			}
			System.out.println();
		}
	}
	
	private static interface DiagramColoring {
		public Color getColor(Site s);
		public boolean drawPoints();
		public boolean drawCentroids();
	}
	
	public static DiagramColoring RANDOM_COLORING = new DiagramColoring() {
		public Color getColor(Site s) {
			return Color.getHSBColor(random.nextFloat(), 1.0f, 0.5f + random.nextFloat()*0.5f);
		}
		public boolean drawPoints() { return true; }
		public boolean drawCentroids() { return true; }
	};
	
	private static DiagramColoring createSizeHeatmapColoring(Voronoi v) {
		return new DiagramColoring() {
			{ init(); }
			
			Map<Site, Double> sizeNorm;
			
			public void init() {
				sizeNorm = new HashMap<>(v.numSites());
				double minArea = Double.MAX_VALUE, maxArea = 0;
				for (Site s : v.getSites()) {
					double area = s.getPolygon().getArea();
					minArea = Math.min(minArea, area);
					maxArea = Math.max(maxArea, area);
					sizeNorm.put(s, area);
				}
				double range = maxArea-minArea;				
				for (Entry<Site, Double> e : sizeNorm.entrySet()) {
					double area = e.getValue();
					double brightness = (area - minArea) / range;
					e.setValue(brightness);
				}
			}
			
			public Color getColor(Site s) {
				float norm = (float) (double) sizeNorm.get(s);
				
				norm = 1.0f-norm;
				
				float hue = random.nextFloat();
				float sat = 1.0f;
				float bri = norm;
				
				// Heat map
				if (norm < 0.1f) {
					hue = 0.666666666f;
					sat = 1;
					bri = norm*10.0f;
				} else if (norm > 0.9f) {
					hue = 0.166666666f;
					sat = 1.0f - ((norm - 0.9f) * 10f);
					bri = 1;
				} else {
					norm = (norm - 0.1f) / 0.8f;
					norm = (float) (((0.5-Math.cos(norm * Math.PI)*0.5) + norm) / 2.0);
					hue = 0.666666666f + 0.5f*norm;
					sat = 1;
					bri = 1;
				}
				
				return Color.getHSBColor(hue, sat, bri);
			}
			
			public boolean drawPoints() { return false; }
			public boolean drawCentroids() { return false; }
		};
	}

	private static BufferedImage drawDiagram(String fileName, Voronoi v, double canvasSize, DiagramColoring coloring) {
		int numSites = v.getSites().size();		
		double maxArea = (canvasSize*canvasSize / numSites) * 6;
		int padding = (int) Math.ceil(canvasSize*0.00);
		BufferedImage image = new BufferedImage((int)canvasSize+padding*2, (int)canvasSize+padding*2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();
		g2d.translate(padding, padding);
		
		for (Site site : v.getSites()) {
			Convex poly = site.getPolygon();
			double area = poly.getArea();
			if (area <= 0 || area > maxArea || Double.isNaN(area)) {
				System.err.println("Skipping site: "+site.getID()+" due to likely error: "+area);
				continue;
			}
			
			// Draw shape
			g2d.setColor(coloring.getColor(site));
			Shape polyShape = poly.asAWTShape();
			if (polyShape != null) g2d.fill(polyShape);
			
			// Draw centroid
			if (coloring.drawCentroids()) {
				g2d.setColor(Color.BLACK);
				Double2D centroid = poly.getCentroid();
				if (centroid != null) {
					Ellipse2D siteCentroid = new Ellipse2D.Double(centroid.x()-1, centroid.y()-1, 2, 2);
					g2d.fill(siteCentroid);
					g2d.setColor(Color.WHITE);
				} else {
					g2d.setColor(Color.BLACK);
				}
			}
			
			// Draw original point
			if (coloring.drawPoints()) {
				Ellipse2D sitePt = new Ellipse2D.Double(site.getX()-1, site.getY()-1, 2, 2);
				g2d.fill(sitePt);
			}
		}
		
		return image;
	}
	
}
