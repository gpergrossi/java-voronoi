package com.gpergrossi.util.geom.ranges;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import com.gpergrossi.util.geom.vectors.Double3D;
import com.gpergrossi.util.geom.vectors.Int3D;

public class Int3DRange {
	
	public final int minX, maxX;
	public final int minY, maxY;
	public final int minZ, maxZ;
	public final int width, height, depth;
	
	public Int3DRange(Int3DRange range) {
		this(range.minX, range.minY, range.minZ, range.maxX, range.maxY, range.maxZ);
	}
	
	public Int3DRange(Int3D start, Int3D end) {
		this(start.x(), start.y(), start.z(), end.x(), end.y(), end.z());
	}
	
	public Int3DRange(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.width = Math.max(maxX - minX + 1, 0);
		this.height = Math.max(maxY - minY + 1, 0);
		this.depth = Math.max(maxZ - minZ + 1, 0);
	}

	public Int3DRange resize(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return new Int3DRange(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	public int size() {
		return width*height*depth;
	}
	
	public boolean isEmpty() {
		return size() == 0;
	}
	
	public boolean contains(int x, int y, int z) {
		return (x >= minX && y >= minY && z >= minZ && x <= maxX && y <= maxY && z <= maxZ);
	}
	
	public boolean contains(Int3D pt) {
		return contains(pt.x(), pt.y(), pt.z());
	}

	public boolean contains(Double3D double3d) {
		return contains((int) Math.floor(double3d.x()+0.5), (int) Math.floor(double3d.y()+0.5), (int) Math.floor(double3d.z()+0.5));
	}
	
	public boolean onBorder(int x, int y, int z, int borderPadding) {
		return contains(x, y, z) 
			&& (x < minX + borderPadding && y < minY + borderPadding && z < minZ + borderPadding) 
			&& (x > maxX - borderPadding && y > maxY - borderPadding && y > maxZ - borderPadding);
	}
	
	public boolean onBorder(Int3D pt, int borderPadding) {
		return onBorder(pt.x(), pt.y(), pt.z(), borderPadding);
	}
	
	public Int3DRange grow(int padXMin, int padYMin, int padZMin, int padXMax, int padYMax, int padZMax) {
		return resize(minX-padXMin, minY-padYMin, minZ-padZMin, maxX+padXMax, maxY+padYMax, maxZ+padZMax);
	}
	
	public Int3DRange grow(int padX, int padY, int padZ) {
		return grow(padX, padY, padZ, padX, padY, padZ);
	}

	public Int3DRange grow(int pad) {
		return grow(pad, pad, pad);
	}
	
	public Int3DRange shrink(int insetX, int insetY, int insetZ) {
		return grow(-insetX, -insetY, -insetZ);
	}
	
	public Int3DRange shrink(int inset) {
		return grow(-inset, -inset, -inset);
	}
	
	public Int3DRange intersect(Int3DRange other) {
		int minX = Math.max(this.minX, other.minX);
		int minY = Math.max(this.minY, other.minY);
		int minZ = Math.max(this.minZ, other.minZ);
		int maxX = Math.min(this.maxX, other.maxX);
		int maxY = Math.min(this.maxY, other.maxY);
		int maxZ = Math.min(this.maxZ, other.maxZ);
		return resize(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public Int3DRange offset(int offsetX, int offsetY, int offsetZ) {
		return resize(minX+offsetX, minY+offsetY, minZ+offsetZ, maxX+offsetX, maxY+offsetY, maxZ+offsetZ);
	}
	
	public Int3DRange scale(int scaleUp) {
		return resize(minX*scaleUp, minY*scaleUp, minZ*scaleUp, maxX*scaleUp, maxY*scaleUp, maxZ*scaleUp);
	}
	
	public int indexFor(Int3D pt) {
		return indexFor(pt.x(), pt.y(), pt.z());
	}
	
	private int indexFor(int x, int y, int z) {
		return ((z-minZ)*width + (x-minX))*height + (y-minY);
	}

	public Int3D randomTile(Random random) {
		return new Int3D(randomX(random), randomY(random), randomZ(random));
	}

	public int randomX(Random random) {
		return random.nextInt(width)+minX;
	}
	
	public int randomY(Random random) {
		return random.nextInt(height)+minY;
	}
	
	public int randomZ(Random random) {
		return random.nextInt(depth)+minZ;
	}
	
	public String toString() {
		return "("+minX+", "+minY+", "+minZ+") to ("+maxX+", "+maxY+", "+maxZ+")";
	}
	
	public Iterable<Int3D.WithIndex> getAllMutable() {
		return new Iterable<Int3D.WithIndex>() {
			@Override
			public Iterator<Int3D.WithIndex> iterator() {
				return new Iterator<Int3D.WithIndex>() {
					private Int3D.WithIndex mutable = new Int3D.WithIndex(Int3DRange.this, 0, 0, 0, 0);
					private int x = 0, y = 0, z = 0, index = 0;
					
					@Override
					public boolean hasNext() {
						return index < size();
					}

					@Override
					public Int3D.WithIndex next() {
						if (!hasNext()) throw new NoSuchElementException();
						mutable.x(x+minX);
						mutable.y(y+minY);
						mutable.z(z+minZ);
						mutable.index = index;
						increment();
						return mutable;
					}

					private void increment() {
						index++;
						y++;
						if (y == height) {
							y = 0;
							x++;
							if (x == width) {
								x = 0;
								z++;
							}
						}
					}
				};	
			}
		};
	}
	
	public Iterable<Int3D.WithIndex> getAll() {
		return new Iterable<Int3D.WithIndex>() {
			@Override
			public Iterator<Int3D.WithIndex> iterator() {
				return new Iterator<Int3D.WithIndex>() {
					private int x = 0, y = 0, z = 0, index = 0;
					
					@Override
					public boolean hasNext() {
						return index < size();
					}

					@Override
					public Int3D.WithIndex next() {
						if (!hasNext()) throw new NoSuchElementException();
						Int3D.WithIndex result = new Int3D.WithIndex(Int3DRange.this, x+minX, y+minY, z+minZ, index);
						increment();
						return result;
					}
					
					private void increment() {
						index++;
						y++;
						if (y == height) {
							y = 0;
							x++;
							if (x == width) {
								x = 0;
								z++;
							}
						}
					}
				};	
			}
		};
	}

	public Int3D getCenter() {
		return new Int3D((minX+maxX)/2, (minY+maxY)/2, (minZ+maxZ)/2);
	}

	public Int3DRange copy() {
		return this.resize(minX, minY, minZ, maxX, maxY, maxZ);
	}

	
}
