package com.gpergrossi.util.geom.vectors;

public interface IVector2D<T extends IVector2D<T>> extends IVector<T> {
	
	public double cross(T vector);
	public double angle();
	
	/**
	 * Effective rotation of -90 degrees
	 * @return
	 */
	public T perpendicular();
	public T rotate(double angle);
	
}
