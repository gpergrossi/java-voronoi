package test.gpergrossi.util;

import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

import org.junit.Rule;

import com.gpergrossi.util.geom.shapes.Circle;
import com.gpergrossi.util.geom.shapes.Concave;
import com.gpergrossi.util.geom.shapes.Convex;
import com.gpergrossi.util.geom.shapes.LineSeg;
import com.gpergrossi.util.geom.shapes.Polygon;
import com.gpergrossi.util.geom.vectors.Double2D;

public class GeometryTest {

	public static Double2D[] createVerts(double... vals) {
		int size = vals.length/2;
		Double2D[] result = new Double2D[size];
		
		for (int i = 0; i < size; i++) {
			result[i] = new Double2D(vals[i*2], vals[i*2+1]);
		}
		
		return result;
	}
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Test
	public void triangleTest() {
		System.out.println("Triangle");
		Double2D[] test = createVerts(10, 10, 0, 30, -10, 10);
		Polygon result = Polygon.create(test);
		assertTrue(result instanceof Convex);
	}
	
	@Test
	public void invertedTriangleTest() {
		System.out.println("Triangle (invert)");
		Double2D[] test = createVerts(10, 10, 0, 30, -10, 10);
		Polygon.invert(test);
		Polygon result = Polygon.create(test);
		assertTrue(result instanceof Convex);
	}

	@Test
	public void quadTest() {
		System.out.println("Quad");
		Double2D[] test = createVerts(0, 0, 20, 0, 20, 20, 0, 20);
		Polygon result = Polygon.create(test);
		assertTrue(result instanceof Convex);
	}
	
	@Test
	public void concaveTest() {
		System.out.println("Concave Shape");
		Double2D[] test = createVerts(0, 0, 20, 0, 10, 10, 20, 20, 0, 20);
		Polygon result = Polygon.create(test);
		assertTrue(result instanceof Concave);
	}

	@Test
	public void segmentIntersectTest() {
		System.out.println("LineSeg intersect");
		LineSeg seg1 = new LineSeg(-10, -10, 5, 5);
		LineSeg seg2 = new LineSeg(10, -10, -5, 5);
		
		assertTrue(seg1.intersects(seg2));
	}
	
	@Test
	public void selfIntersectTest1() {
		exception.expectMessage("self-intersecting");
		Double2D[] test;
		
		System.out.println("Twisted Trapezoid");
		test = createVerts(-10, -10, 10, -10, -5, 5, 5, 5);
		Polygon.create(test);
	}
		
	@Test
	public void selfIntersectTest2() {
		exception.expectMessage("self-intersecting");
		Double2D[] test;
	
		System.out.println("Twisted Trapezoid (invert)");
		test = createVerts(-10, -10, 10, -10, -5, 5, 5, 5);
		Polygon.invert(test);
		Polygon.create(test);
	}
		
	@Test
	public void selfIntersectTest3() {
		exception.expectMessage("self-intersecting");
		Double2D[] test;	
		
		System.out.println("Star");
		test = createVerts(0, 0, 25, 10, -5, 10, 20, 0, 10, 20);
		Polygon.create(test);
	}
		
	@Test
	public void selfIntersectTest4() {
		exception.expectMessage("self-intersecting");
		Double2D[] test;
	

		System.out.println("Star (invert)");
		test = createVerts(0, 0, 25, 10, -5, 10, 20, 0, 10, 20);
		Polygon.invert(test);
		Polygon.create(test);
	}
	
	@Test
	public void intersectionTest() {
		System.out.println("Intersection test");
		int x = -10239;
		int y = 1020;
		
		Circle smallCircle = new Circle(x, y, 20);
		Circle bigCircle = new Circle(x, y, 40);
		
		assertTrue(bigCircle.contains(smallCircle));
		assertTrue(bigCircle.contains(bigCircle));
		assertTrue(smallCircle.contains(smallCircle));
		
	}
	
	@Test
	public void insetEdgeElimination() {
		System.out.println("Inset edge elimination test");
		Double2D[] test = createVerts(100, 1, 0, 4, 0, 0, 100, 0);
		Polygon poly = Polygon.create(test);
		
		poly = poly.inset(1);
		assertEquals(3, poly.getNumSides());
	}
	
}
