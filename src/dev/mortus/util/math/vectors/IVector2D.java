package dev.mortus.util.math.vectors;

public interface IVector2D<T extends IVector2D<T>> extends IVector<T> {
	
	public double cross(T vector);
	public double angle();
	
	public T perpendicular();
	public T rotate(double angle);
	
}
