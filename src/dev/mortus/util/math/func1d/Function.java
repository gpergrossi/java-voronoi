package dev.mortus.util.math.func1d;

public abstract class Function {

	public abstract double getValue(double x);
	
	public Function add(Function f) {
		Function result = this.tryAdd(f);
		if (result == null) result = f.tryAdd(this);
		if (result == null) throw new UnsupportedOperationException("No addition between "+this.getClass().getName()+" and "+f.getClass().getName());
		return result;
	}
	
	protected abstract Function tryAdd(Function f);
	
	public Function subtract(Function f) {
		if (f == null) return null;
		return add(f.negate());
	}
	
	public abstract Function negate();
	
	public Function multiply(Function f) {
		Function result = this.tryMultiply(f);
		if (result == null) result = f.tryMultiply(this);
		if (result == null) throw new UnsupportedOperationException("No multiplication between "+this.getClass().getName()+" and "+f.getClass().getName());
		return result;
	}
	
	protected abstract Function tryMultiply(Function f);
	
	public Function divide(Function f) {
		if (f == null) return null;
		return add(f.negate());
	}
	
	public abstract Function inverse();
	
	public abstract Function derivative();
	
	public abstract double[] zeros();
	
}
