package test.gpergrossi.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.gpergrossi.util.data.Tuple2;
import com.gpergrossi.util.geom.ranges.DistanceTransform;
import com.gpergrossi.util.geom.ranges.Int2DRange;
import com.gpergrossi.util.geom.vectors.Int2D;
import com.gpergrossi.util.geom.vectors.Int2D.StoredBit;

public class DistanceTransformTest {

	public static void main(String[] args) {
		
		final int minX = -200;
		final int minY = -200;
		final int maxX = 200;
		final int maxY = 200;
		
		Random random = new Random(10080960680085L);
		Int2DRange.Bits shape = new Int2DRange.Bits(minX, minY, maxX, maxY);
		shape.set((minX+maxX)/2, (minY+maxY)/2, true);
		for (StoredBit bit : shape.getAllBits()) {
			bit.setValue(random.nextFloat() < 0.005);
			if ((bit.x() == minX || bit.x() == maxX) || (bit.y() == minY || bit.y() == maxY)) {
				bit.setValue(true);
			}
		}

		Tuple2<Int2DRange.Floats, Float> resultNew = DistanceTransform.transform(shape);
		showImage("New", drawFloats(resultNew.first, resultNew.second), 2);

		Tuple2<Int2DRange.Floats, Float> resultOld = transformOld(shape);
		showImage("Old", drawFloats(resultOld.first, resultOld.second), 2);

		System.out.println("Warmup...");
		for (int i = 0; i < 10000; i++) {
			if (i % 100 == 0) System.out.println((i/100)+"%");
			transformOld(shape);
			DistanceTransform.transform(shape);
		}
		
		System.out.println("Old method...");
		long start = System.nanoTime();
		for (int i = 0; i < 10000; i++) {
			if (i % 100 == 0) System.out.println((i/100)+"%");
			transformOld(shape);
		}
		double durOld = (System.nanoTime() - start) / (1000000.0);
		System.out.println("Old method took "+durOld+" ms");

		System.out.println("New method...");
		start = System.nanoTime();
		for (int i = 0; i < 10000; i++) {
			if (i % 100 == 0) System.out.println((i/100)+"%");
			DistanceTransform.transform(shape);
		}
		double durNew = (System.nanoTime() - start) / (1000000.0);
		
		System.out.println("Old method took "+durOld+" ms");
		System.out.println("New method took "+durNew+" ms");
	}
	
	public static final BufferedImage drawFloats(Int2DRange.Floats floats, float max) {
		BufferedImage image = new BufferedImage(floats.width, floats.height, BufferedImage.TYPE_INT_ARGB);
		int[] rgba = new int[image.getWidth()*image.getHeight()];
		int index = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int bri = (int) Math.floor(floats.get(index) * 255.0f / max);
				rgba[index] = (0xFF << 24) | (bri << 16) | (bri << 8) | bri;
				index++;
			}
		}
		image.setRGB(0, 0, image.getWidth(), image.getHeight(), rgba, 0, image.getWidth());
		return image;
	}
	
	private static final BufferedImage getScaledImage(BufferedImage src, int w, int h) {
	    BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g2 = resizedImg.createGraphics();

	    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	    g2.drawImage(src, 0, 0, w, h, null);
	    g2.dispose();

	    return resizedImg;
	}
	
	public static final void showImage(String title, BufferedImage img, int scale) {
		BufferedImage scaled = getScaledImage(img, img.getWidth()*scale, img.getHeight()*scale);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new JLabel(new ImageIcon(scaled)));
		frame.pack();
		frame.setTitle(title);
		frame.setVisible(true);
	}
	
	public static Tuple2<Int2DRange.Floats, Float> transformOld(Int2DRange.Bits shape) {		
		Int2DRange range = shape.asRange().grow(1);
		Int2DRange.Floats matrix = range.createFloats();
		
		for (Int2D.StoredBit tile : shape.getAllBits()) {
			matrix.set(tile.x(), tile.y(), tile.getValue() ? 0 : Float.POSITIVE_INFINITY);
		}
		
		float[] array = matrix.data;
		int width = matrix.width;
		int height = matrix.height;
		
		float dist;
		int i = 0;
		for (int y = 1; y < height; y++) {
			for (int x = 1; x < width; x++) {
				int index = y*width+x;
				if (array[index] == 0) continue;
				dist = array[index];
				dist = Math.min(array[index-width]+1, dist);
				dist = Math.min(array[index-1]+1, dist);
				dist = Math.min(array[index-width-1]+1.414f, dist);
				array[index] = dist;
				if (i++ == 200) { Thread.yield(); i = 0; }
			}
			for (int x = width-2; x >= 0; x--) {
				int index = y*width+x;
				if (array[index] == 0) continue;
				dist = array[index];
				dist = Math.min(array[index+1]+1, dist);
				dist = Math.min(array[index-width+1]+1.414f, dist);
				array[index] = dist;
				if (i++ == 200) { Thread.yield(); i = 0; }
			}
		}
		float maxDist = 0;
		for (int y = height-2; y >= 0; y--) {
			for (int x = 1; x < width; x++) {
				int index = y*width+x;
				if (array[index] == 0) continue;
				dist = array[index];
				dist = Math.min(array[index+width]+1, dist);
				dist = Math.min(array[index-1]+1, dist);
				dist = Math.min(array[index+width-1]+1.414f, dist);
				array[index] = dist;
				if (i++ == 200) { Thread.yield(); i = 0; }
			}
			for (int x = width-2; x >= 0; x--) {
				int index = y*width+x;
				if (array[index] == 0) continue;
				dist = array[index];
				dist = Math.min(array[index+1]+1, dist);
				dist = Math.min(array[index+width+1]+1.414f, dist);
				array[index] = dist;
				maxDist = Math.max(maxDist, array[index]);
				if (i++ == 200) { Thread.yield(); i = 0; }
			}
		}
		
		return new Tuple2<>(matrix, maxDist);
	}
	
}
