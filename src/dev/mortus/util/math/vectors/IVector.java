package dev.mortus.util.math.vectors;

/**
 * This interface defines a standard for vector classes.
 * It's extending interfaces, IVecotor2D and IVector3D
 * are meant to be implemented by all Vector classes.
 * 
 * This interface, IVecotor2D, and IVector3D are mostly
 * intended to ensure that vector classes have all of the
 * necessary methods to be used as a vector. Making
 * use of the interface to store multiple TYPES of vectors
 * would be cumbersome but do-able. (i.e. List<IVector<?>> vectors)
 * 
 * @author Gregary
 *
 * @param <T> the vector class that implements this interface. 
 * (e.g. Double2D implements IVector2D<'Double2D'>. Quotes because otherwise javadoc comments eat brackets)
 */
public interface IVector<T extends IVector<T>> extends Comparable<T> {

	/**
	 * The only method that returns a new vector.
	 * @return A new vector that is a copy of this one
	 */
	public T copy();
	
	/**
	 * Should return an immutable copy of this instance.
	 * In order for this to be implemented easily, the base
	 * class of all Vector types should be immutable and extend
	 * this interface. Then there should be a mutable class 
	 * extending the base class.
	 * @return
	 */
	public T immutable();
	
	public T multiply(double scalar);
	public T divide(double scalar);

	public double dot(T vector);
	
	public T add(T vector);
	public T subtract(T vector);
	
	public double length();
	public double lengthSquared();
	public T normalize();
	
	public double distanceTo(T vector);
	public double distanceSquaredTo(T vector);
	
	public int compareTo(T other);
	public boolean equals(T other);
	
}
