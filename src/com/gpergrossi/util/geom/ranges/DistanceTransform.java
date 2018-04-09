package com.gpergrossi.util.geom.ranges;

import com.gpergrossi.util.data.Tuple2;

public class DistanceTransform {
	
	/**
	 * 
	 * Distance Transform algorithm from:
	 * 
	 * <br/><br/>
	 * <b>A General Algorithm for Computing Distance Transforms in Linear Time</b><br/>
	 * <i>A. Meijster‚ J.B.T.M. Roerdink and W.H. Hesselink</i><br/>
	 * <i>University of Groningen</i><br/>
	 * 
	 * @param shape
	 * @return
	 */
	public static Tuple2<Int2DRange.Floats, Float> transform(Int2DRange.Bits shape) {
		final float[] yDist = new float[shape.size()];
		final int width = shape.width;
		final int height = shape.height;
		
		// Initialize + First Scan + Second Scan
		for (int x = 0; x < width; x++) {
			int index = x;
			
			yDist[index] = shape.get(x+shape.minX, 0+shape.minY) ? 0 : Float.POSITIVE_INFINITY;
			
			// Initialize + First scan: cell[x,y] = 0 or cell[x,y-1]+1
			for (int y = 1; y < height; y++) {
				index += width;
				if (shape.get(x+shape.minX, y+shape.minY)) yDist[index] = 0; // init
				else yDist[index] = yDist[index - width] + 1; // scan down
			}
			
			// Second scan: cell[x,y] = min(cell[x,y], cell[x,y+1]+1)
			for (int y = height-2; y >= 0; y--) {
				index -= width;
				yDist[index] = min(yDist[index], yDist[index + width] + 1);
			}
		}

		final Int2DRange.Floats result = shape.createFloats();
		final int[] mins = new int[width];
		final int[] partition = new int[width];
		float maxDist = 0;
			
		// Third Scan + Fourth Scan
		for (int y = 0; y < height; y++) {

			final int rowIndex = y*width;

			// computes (x-i)^2 + g(i)^2
			final IFloatFunction2I distFunc = new IFloatFunction2I() {
				@Override
				public float get(int x, int i) {
					final int d = x - i;
					final float gi = yDist[rowIndex + i];
					return d*d + gi*gi;
				}
			};
			
			final IIntFunction2I seperator = new IIntFunction2I() {
				@Override
				public int get(int i, int u) {
					final float gu = yDist[rowIndex + u];
					final float gi = yDist[rowIndex + i];
					return floorDiv(u*u - i*i + gu*gu - gi*gi, 2*(u-i));
				}
			};

			int scout = 0;
			
			for (int x = 1; x < width; x++) {				
				for (; scout >= 0; scout--) {
					final float currentMin = distFunc.get(partition[scout], mins[scout]);
					final float newMin = distFunc.get(partition[scout], x);
					if (newMin >= currentMin) break;
				}
				
				if (scout < 0) {
					scout = 0;
					mins[0] = x;
				} else {
					int intersection = seperator.get(mins[scout], x) + 1;
					if (intersection < width && intersection != Integer.MIN_VALUE) {
						scout++;
						mins[scout] = x;
						partition[scout] = intersection;
					}
				}
			}
			
			for (int x = width-1; x >= 0; x--) {
				final float distance = sqrt(distFunc.get(x, mins[scout]));
				maxDist = max(maxDist, distance);
				result.set(rowIndex+x, distance);
				if (x == partition[scout]) scout--;
			}
		}
		
		return new Tuple2<>(result, maxDist);
	}

	private interface IFloatFunction2I {
		public float get(int x, int y);
	}
	
	private interface IIntFunction2I {
		public int get(int x, int y);
	}

	private static final float min(final float a, final float b) {
		return (b < a) ? b : a;
	}
	
	private static final float max(final float a, final float b) {
		return (b > a) ? b : a;
	}

	private static final float sqrt(final float a) {
		return (float) Math.sqrt(a);
	}

	private static final int floorDiv(final float num, final int den) {
		final float result = num/den;
		return (result < 0) ? ((int) result-1) : ((int) result);
	}
	
}
