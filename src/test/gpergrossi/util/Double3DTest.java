package test.gpergrossi.util;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import com.gpergrossi.util.geom.vectors.Double3D;

public class Double3DTest {
	
	public static Double3D randomDouble3D(Random random) {
		double vx = (random.nextDouble()*5+1) * Math.signum(random.nextDouble()-0.5);
		double vy = (random.nextDouble()*5+1) * Math.signum(random.nextDouble()-0.5);
		double vz = (random.nextDouble()*5+1) * Math.signum(random.nextDouble()-0.5);
		return new Double3D(vx, vy, vz);
	}
	
	public static Double3D permuteDouble3D(Double3D src) {
		return new Double3D(src.z(), src.x(), src.y());
	}
	
	public static Double3D randomRotation(Double3D src, Random random) {
		Double3D axis = randomDouble3D(random);
		double theta = random.nextDouble()*Math.PI*2.0;
		return src.copy().rotate(axis,  theta);
	}
	
	public static boolean equal(double a, double b) {
		return (Math.abs(a - b) < 0.0001);
	}
	
	@Test
	public void rotationDoesNotAffectLength() {
		Random random = new Random(7895674983L);
		
		for (int i = 0; i < 50; i++) {
			Double3D vector = randomDouble3D(random);
			double length = vector.length();
	
			Double3D rotated = randomRotation(vector, random);
			double rotatedLength = rotated.length();
			
			assertTrue(equal(length, rotatedLength));
		}
	}

	@Test
	public void rotationAffectsVectorsTheSame() {
		Random random = new Random(7895674983L);
		
		for (int i = 0; i < 50; i++) {
			Double3D vector1 = randomDouble3D(random);
			Double3D vector2 = randomDouble3D(random);
	
			Double3D axis = randomDouble3D(random);
			double theta = random.nextDouble()*Math.PI*2.0;
			Double3D rotated1 = vector1.rotate(axis, theta);
			Double3D rotated2 = vector2.rotate(axis, theta);
			
			double dist = vector1.distanceTo(vector2);
			double dot = vector1.dot(vector2);
			double cross = vector1.copy().cross(vector2).length();

			double rdist = rotated1.distanceTo(rotated2);
			double rdot = rotated1.dot(rotated2);
			double rcross = rotated1.copy().cross(rotated2).length();
			
			assertTrue(equal(dist, rdist));
			assertTrue(equal(dot, rdot));
			assertTrue(equal(cross, rcross));
		}
	}
	
}
