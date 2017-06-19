package dev.mortus.util.math.geom2d;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import dev.mortus.util.data.Pair;
import dev.mortus.util.math.vectors.Double2D;

public final class Rect {

	protected double x, y;
	protected double width, height;
	
	public Rect(Rectangle2D rect2d) {
		this(rect2d.getX(), rect2d.getY(), rect2d.getWidth(), rect2d.getHeight());
	}
	
	public Rect(Double2D pos, Double2D size) {
		this(pos.x(), pos.y(), size.x(), size.y());
	}
	
	public Rect(double x, double y, Double2D size) {
		this(x, y, size.x(), size.y());
	}
	
	public Rect(Double2D pos, double width, double height) {
		this(pos.x(), pos.y(), width, height);
	}
	
	public Rect(double x, double y, double width, double height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	public Rect copy() {
		return new Rect(x, y, width, height);
	}
	
	public void union(Rect r) {
		double minX = Math.min(minX(), r.minX());
		double minY = Math.min(minY(), r.minY());
		double maxX = Math.max(maxX(), r.maxX());
		double maxY = Math.max(maxY(), r.maxY());
		this.x = minX;
		this.y = minY;
		this.width = maxX - minX;
		this.height = maxY - minY;
	}
	
	public boolean contains(double x, double y) {
		if (x < minX() || y < minY()) return false;
		if (x > maxX() || y > maxY()) return false;
		return true;
	}

	// Using the Liang-Barsky approach
	private static Pair<Double> getIntersectTValues(Rect rect, Line line) {		
		double[] p = new double[] { -line.dx, line.dx, -line.dy, line.dy };
		double[] q = new double[] { line.x - rect.minX(), rect.maxX() - line.x, line.y - rect.minY(), rect.maxY() - line.y };
		double t0 = line.tmin();
		double t1 = line.tmax();
		
		for (int i = 0; i < 4; i++) {
			if (p[i] == 0) {
				if (q[i] < 0) return null;
				continue;
			}
			double t = q[i] / p[i];
			if (p[i] < 0 && t0 < t) t0 = t;
			else if (p[i] > 0 && t1 > t) t1 = t;
		}
		
		if (t0 > t1) return null;
		return new Pair<Double>(t0, t1);
	}
	
	public LineSeg clip(Line line) {
		Pair<Double> tValues = getIntersectTValues(this, line);
		if (tValues == null) return null;
		double t0 = tValues.first;
		double t1 = tValues.second;
		return new LineSeg(line.getX(t0), line.getY(t0), line.getX(t1), line.getY(t1));
	}
	
	@Override
	public String toString() {
		return "Rect[x0="+minX()+", y0="+minY()+", x1="+maxX()+", y1="+maxY()+"]";
	}

	public void expand(double padding) {
		this.x -= padding;
		this.y -= padding;
		this.width += padding*2;
		this.height += padding*2;
	}

	public Iterable<LineSeg> edges() {
		return new Iterable<LineSeg>() {
			public Iterator<LineSeg> iterator() {
				return new Iterator<LineSeg>() {
					int index = 0;
					Double2D[] vertices;
					{
						 vertices = new Double2D[4];
						 vertices[0] = new Double2D(minX(), minY());
						 vertices[1] = new Double2D(minX(), maxY());
						 vertices[2] = new Double2D(maxX(), maxY());
						 vertices[3] = new Double2D(maxX(), minY());
					}
					public boolean hasNext() {
						return index < vertices.length;
					}
					public LineSeg next() {
						if (index >= vertices.length) throw new NoSuchElementException();
						Double2D pt0 = vertices[index];
						Double2D pt1 = null;
						index++;
						if (index == vertices.length) pt1 = vertices[0]; 
						else pt1 = vertices[index];
						return new LineSeg(pt0.x(), pt0.y(), pt1.x(), pt1.y());
					}
				};
			}
		};
	}
	
	public Rectangle2D toRectangle2D() {
		return new Rectangle2D.Double(x, y, width, height);
	}

	public double minX() {
		return x;
	}
	
	public double maxX() {
		return x+width;
	}
	
	public double minY() {
		return y;
	}
	
	public double maxY() {
		return y+height;
	}

	public double width() {
		return width;
	}
	
	public double height() {
		return height;
	}
	
	public double centerX() {
		return x + width/2;
	}
	
	public double centerY() {
		return y + height/2;
	}

	public boolean intersects(Rect other) {
		if (this.maxX() < other.minX()) return false;
		if (this.minX() > other.maxX()) return false;
		if (this.maxY() < other.minY()) return false;
		if (this.minY() > other.maxY()) return false;
		return true;
	}
	
	public boolean intersects(Polygon poly) {
		return poly.intersects(this);
	}

	public Rectangle2D getShape2D() {
		return new Rectangle2D.Double(x, y, width, height);
	}

	public void toInt() {
		this.x = Math.floor(minX());
		this.y = Math.floor(minY());
		this.width = Math.ceil(maxX())-x;
		this.height = Math.ceil(maxY())-y;
	}

	public void toGrid(int gridWidth, int gridHeight) {
		double minX = Math.floor(minX()/gridWidth)*gridWidth;
		double minY = Math.floor(minY()/gridHeight)*gridHeight;
		double maxX = Math.ceil(maxX()/gridWidth)*gridWidth;
		double maxY = Math.ceil(maxY()/gridHeight)*gridHeight;
		this.x = minX;
		this.y = minY;
		this.width = maxX-minX;
		this.height = maxY-minY;
	}

	public double getArea() {
		return width*height;
	}

	public Double2D randomPoint(Random random) {
		double x = this.x + random.nextDouble()*width;
		double y = this.y + random.nextDouble()*height;
		return new Double2D(x, y);
	}
	
}
